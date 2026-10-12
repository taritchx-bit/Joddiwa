package com.tarit.slipledger.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** Names/aliases of the phone owner, used to tell outgoing, incoming and self transfers apart. */
    val ownerNames: List<String> = emptyList(),
    /** Only gallery images added at or after this time (epoch ms) are scanned. 0 = all images. */
    val scanFrom: Long = 0,
    val autoScan: Boolean = true,
    /** Monthly spending budget in satang; 0 = no budget. */
    val monthlyBudget: Long = 0,
    /** Per-category monthly budgets in satang. */
    val categoryBudgets: Map<String, Long> = emptyMap(),
    /** Day of month a budget period starts, 1-28. */
    val monthStartDay: Int = 1,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Ask for fingerprint/face/device PIN when opening the app. */
    val appLock: Boolean = false,
    /** Wallets left out of the total balance on the home screen; new wallets are counted by default. */
    val excludedWallets: Set<Long> = emptySet(),
) {
    val period get() = Period(monthStartDay)
}

class Prefs private constructor(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings

    private fun read(): Settings {
        if (!sp.contains(KEY_SCAN_FROM)) {
            // First run: look back 30 days so the ledger is not flooded with very old slips.
            sp.edit().putLong(KEY_SCAN_FROM, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)).apply()
        }
        return Settings(
            ownerNames = sp.getString(KEY_NAMES, "").orEmpty()
                .split(',').map { it.trim() }.filter { it.isNotEmpty() },
            scanFrom = sp.getLong(KEY_SCAN_FROM, 0),
            autoScan = sp.getBoolean(KEY_AUTO_SCAN, true),
            monthlyBudget = sp.getLong(KEY_BUDGET, 0),
            categoryBudgets = sp.getString(KEY_CATEGORY_BUDGETS, "").orEmpty()
                .split(';').mapNotNull { entry ->
                    val (name, value) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                    value.toLongOrNull()?.takeIf { it > 0 }?.let { name to it }
                }.toMap(),
            monthStartDay = sp.getInt(KEY_MONTH_START, 1).coerceIn(1, 28),
            theme = runCatching { ThemeMode.valueOf(sp.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM),
            appLock = sp.getBoolean(KEY_APP_LOCK, false),
            excludedWallets = sp.getString(KEY_EXCLUDED_WALLETS, "").orEmpty()
                .split(',').mapNotNull { it.trim().toLongOrNull() }.toSet(),
        )
    }

    fun update(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        sp.edit()
            .putString(KEY_NAMES, s.ownerNames.joinToString(","))
            .putLong(KEY_SCAN_FROM, s.scanFrom)
            .putBoolean(KEY_AUTO_SCAN, s.autoScan)
            .putLong(KEY_BUDGET, s.monthlyBudget)
            .putString(
                KEY_CATEGORY_BUDGETS,
                s.categoryBudgets.filterValues { it > 0 }.entries.joinToString(";") { "${it.key}=${it.value}" },
            )
            .putInt(KEY_MONTH_START, s.monthStartDay.coerceIn(1, 28))
            .putString(KEY_THEME, s.theme.name)
            .putBoolean(KEY_APP_LOCK, s.appLock)
            .putString(KEY_EXCLUDED_WALLETS, s.excludedWallets.joinToString(","))
            .apply()
        _settings.value = s
    }

    companion object {
        private const val KEY_NAMES = "owner_names"
        private const val KEY_SCAN_FROM = "scan_from"
        private const val KEY_AUTO_SCAN = "auto_scan"
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_CATEGORY_BUDGETS = "category_budgets"
        private const val KEY_MONTH_START = "month_start_day"
        private const val KEY_THEME = "theme"
        private const val KEY_APP_LOCK = "app_lock"
        private const val KEY_EXCLUDED_WALLETS = "excluded_wallets"

        @Volatile private var instance: Prefs? = null

        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(context.applicationContext).also { instance = it }
        }
    }
}
