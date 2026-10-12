package com.tarit.slipledger.notify

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.Repository
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.parse.AutoCategory
import com.tarit.slipledger.parse.NotifTemplate
import com.tarit.slipledger.parse.NotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Records money in/out alerts that bank LINE accounts / bank apps post as notifications. */
class BankNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        // Catch alerts that arrived while the listener was not running.
        runCatching { activeNotifications }.getOrNull()?.forEach(::handle)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun handle(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val pkg = sbn.packageName
        if (pkg == packageName) return
        val store = NotifyStore.get(applicationContext)

        for ((sender, text, messageTime) in messages(title, extras, sbn.postTime)) {
            val full = "$sender\n$text"
            // Every app's money-looking alerts are sampled so the user can pick and teach apps later.
            store.record(pkg, full, messageTime)
            if (!store.isOn(pkg)) continue

            // What the user taught for this app takes priority over the built-in bank reader.
            val learned = store.apply(pkg, full)
            if (learned != null) {
                recordLearned(pkg, full, messageTime, learned.first, learned.second)
                continue
            }

            if (!NotificationParser.isRelevantSender(pkg, sender)) continue
            val alert = NotificationParser.parse(sender, text) ?: continue

            // Prefer the posting time (it is when the money moved) unless the message itself is much older.
            val timestamp = alert.timestamp
                ?.takeIf { abs(it - messageTime) > TimeUnit.DAYS.toMillis(1) }
                ?: messageTime
            // Identical text means the same alert re-posted; without a time in it, two equal transfers
            // on the same day differ only by when the message arrived (not when LINE re-posted the
            // notification, which bundles older messages again with a fresh post time).
            val keySeed = if (alert.hasTime) "$sender|$text" else "$sender|$text|${messageTime / 60_000}"
            val key = "notif:" + sha1(keySeed)

            scope.launch {
                if (!markProcessed(applicationContext, key)) return@launch
                val repo = Repository.get(applicationContext)
                val walletId = repo.resolveWallet(alert.bank, alert.account, create = true)
                val income = alert.type == TxType.INCOME
                repo.addIfNew(
                    Tx(
                        type = alert.type,
                        amountSatang = alert.amountSatang,
                        timestamp = timestamp,
                        counterparty = (if (income) "เงินเข้า" else "เงินออก") + (if (alert.bank.isNotEmpty()) " · ${alert.bank}" else " · $sender"),
                        bank = alert.bank,
                        account = alert.account,
                        category = if (income) Categories.INCOME_OTHER else Categories.OTHER,
                        source = TxSource.NOTIFICATION,
                        sourceKey = key,
                        rawText = text,
                        walletId = walletId,
                        // Money out from an alert has no payee or category yet; a matching slip fills them in later.
                        needsReview = !income || walletId == null,
                    )
                )
            }
        }
    }

    private fun recordLearned(pkg: String, text: String, postTime: Long, rule: LearnedRule, read: NotifTemplate.Match) {
        val key = "notif:" + sha1("$pkg|$text|${postTime / 60_000}")
        scope.launch {
            if (!markProcessed(applicationContext, key)) return@launch
            val income = rule.type == TxType.INCOME
            Repository.get(applicationContext).addIfNew(
                Tx(
                    type = rule.type,
                    amountSatang = read.amountSatang,
                    timestamp = postTime,
                    counterparty = read.merchant.ifBlank { appLabel(applicationContext, pkg) },
                    category = when {
                        income -> Categories.INCOME_OTHER
                        read.merchant.isNotBlank() -> AutoCategory.guess(read.merchant)
                        else -> Categories.OTHER
                    },
                    source = TxSource.NOTIFICATION,
                    sourceKey = key,
                    rawText = text,
                    walletId = rule.walletId,
                    // Without a shop name the user will want to say what it was.
                    needsReview = !income && read.merchant.isBlank(),
                )
            )
        }
    }

    private data class Message(val sender: String, val text: String, val time: Long)

    /** Messaging-style notifications can bundle several messages, each with its own arrival time. */
    private fun messages(title: String, extras: Bundle, postTime: Long): List<Message> {
        val out = mutableListOf<Message>()
        @Suppress("DEPRECATION")
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.forEach { p ->
            val b = p as? Bundle ?: return@forEach
            val text = b.getCharSequence("text")?.toString() ?: return@forEach
            val sender = b.getCharSequence("sender")?.toString() ?: title
            out += Message(sender, text, b.getLong("time", 0L).takeIf { it > 0 } ?: postTime)
        }
        if (out.isEmpty()) {
            val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            if (text != null) out += Message(title, text.toString(), postTime)
        }
        return out
    }

    companion object {
        private const val PREFS = "notif_seen"
        private const val MAX_SEEN = 1000

        /**
         * Remembers alerts already handled so one the user deleted from the ledger
         * is not re-imported when the listener reconnects. Returns false if already seen.
         */
        @Synchronized
        private fun markProcessed(context: Context, key: String): Boolean {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (sp.contains(key)) return false
            val edit = sp.edit().putLong(key, System.currentTimeMillis())
            val all = sp.all
            if (all.size >= MAX_SEEN) {
                all.entries.sortedBy { it.value as? Long ?: 0L }.take(all.size - MAX_SEEN + 1).forEach { edit.remove(it.key) }
            }
            edit.apply()
            return true
        }

        /** The app's name as shown in the launcher, or its package name if it cannot be looked up. */
        fun appLabel(context: Context, pkg: String): String = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        private fun sha1(s: String): String =
            MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
            val me = ComponentName(context, BankNotificationListener::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
