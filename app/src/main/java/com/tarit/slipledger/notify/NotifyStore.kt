package com.tarit.slipledger.notify

import android.content.Context
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.parse.NotifTemplate
import com.tarit.slipledger.parse.NotificationParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A recent money-looking notification kept so the user can pick an app and teach the app to read it. */
data class NotifSample(val pkg: String, val text: String, val time: Long)

/** How to read one kind of alert from one app, learned from an example (see [NotifTemplate]). */
data class LearnedRule(
    val id: String,
    val pkg: String,
    val pattern: String,
    val type: TxType,
    val walletId: Long?,
    val example: String,
)

/**
 * Which apps' notifications are read, the samples seen from them and the rules taught. Everything stays in
 * this app's private storage; only notifications that contain an amount are sampled, never ordinary chats.
 */
class NotifyStore private constructor(context: Context) {
    private val sp = context.getSharedPreferences("notify_store", Context.MODE_PRIVATE)

    private val _samples = MutableStateFlow(readSamples())
    val samples: StateFlow<List<NotifSample>> = _samples

    private val _rules = MutableStateFlow(readRules())
    val rules: StateFlow<List<LearnedRule>> = _rules

    /** Per-app on/off chosen by the user; apps without an entry use [defaultOn]. */
    private val _choices = MutableStateFlow(readChoices())
    val choices: StateFlow<Map<String, Boolean>> = _choices

    fun defaultOn(pkg: String) = pkg in NotificationParser.builtInPackages

    fun isOn(pkg: String) = _choices.value[pkg] ?: defaultOn(pkg)

    fun setOn(pkg: String, on: Boolean) {
        _choices.value = _choices.value + (pkg to on)
        sp.edit().putString(KEY_CHOICES, JSONObject(_choices.value.mapValues { it.value }).toString()).apply()
    }

    @Synchronized
    fun record(pkg: String, text: String, time: Long) {
        if (!moneyRe.containsMatchIn(text)) return
        val current = _samples.value
        if (current.any { it.pkg == pkg && it.text == text }) return
        val fromApp = (listOf(NotifSample(pkg, text.take(MAX_TEXT), time)) + current.filter { it.pkg == pkg }).take(PER_APP)
        val others = current.filter { it.pkg != pkg }
        // Keep the most recently active apps when there are too many.
        val keepApps = (listOf(pkg) + others.sortedByDescending { it.time }.map { it.pkg }).distinct().take(MAX_APPS).toSet()
        _samples.value = (fromApp + others.filter { it.pkg in keepApps }).sortedByDescending { it.time }
        sp.edit().putString(KEY_SAMPLES, JSONArray(_samples.value.map {
            JSONObject().put("pkg", it.pkg).put("text", it.text).put("time", it.time)
        }).toString()).apply()
    }

    fun addRule(pkg: String, pattern: String, type: TxType, walletId: Long?, example: String) {
        _rules.value = _rules.value + LearnedRule(UUID.randomUUID().toString(), pkg, pattern, type, walletId, example)
        saveRules()
        // Teaching an app implies wanting it read.
        if (!isOn(pkg)) setOn(pkg, true)
    }

    fun deleteRule(id: String) {
        _rules.value = _rules.value.filter { it.id != id }
        saveRules()
    }

    /** First taught rule of [pkg] that reads [text], with what it read. */
    fun apply(pkg: String, text: String): Pair<LearnedRule, NotifTemplate.Match>? =
        _rules.value.asSequence().filter { it.pkg == pkg }
            .mapNotNull { r -> NotifTemplate.match(r.pattern, text)?.let { r to it } }
            .firstOrNull()

    private fun saveRules() {
        sp.edit().putString(KEY_RULES, JSONArray(_rules.value.map {
            JSONObject().put("id", it.id).put("pkg", it.pkg).put("pattern", it.pattern).put("type", it.type.name)
                .put("wallet", it.walletId ?: -1L).put("example", it.example)
        }).toString()).apply()
    }

    private fun readSamples(): List<NotifSample> = parseArray(KEY_SAMPLES) {
        NotifSample(it.getString("pkg"), it.getString("text"), it.getLong("time"))
    }

    private fun readRules(): List<LearnedRule> = parseArray(KEY_RULES) {
        LearnedRule(
            it.getString("id"), it.getString("pkg"), it.getString("pattern"), TxType.valueOf(it.getString("type")),
            it.getLong("wallet").takeIf { w -> w >= 0 }, it.optString("example"),
        )
    }

    private fun readChoices(): Map<String, Boolean> = runCatching {
        val o = JSONObject(sp.getString(KEY_CHOICES, null) ?: return@runCatching emptyMap())
        o.keys().asSequence().associateWith { o.getBoolean(it) }
    }.getOrDefault(emptyMap())

    private fun <T> parseArray(key: String, read: (JSONObject) -> T): List<T> = runCatching {
        val a = JSONArray(sp.getString(key, null) ?: return@runCatching emptyList())
        (0 until a.length()).mapNotNull { i -> runCatching { read(a.getJSONObject(i)) }.getOrNull() }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_SAMPLES = "samples"
        private const val KEY_RULES = "rules"
        private const val KEY_CHOICES = "choices"
        private const val PER_APP = 8
        private const val MAX_APPS = 30
        private const val MAX_TEXT = 500

        /** An amount with satang, or a number next to บาท / ฿ / THB: enough to look like a money alert. */
        private val moneyRe = Regex("""\d\.\d{2}(?!\d)|\d\s*(บาท|฿|THB)|(฿|THB)\s*\d""", RegexOption.IGNORE_CASE)

        @Volatile private var instance: NotifyStore? = null

        fun get(context: Context): NotifyStore = instance ?: synchronized(this) {
            instance ?: NotifyStore(context.applicationContext).also { instance = it }
        }
    }
}
