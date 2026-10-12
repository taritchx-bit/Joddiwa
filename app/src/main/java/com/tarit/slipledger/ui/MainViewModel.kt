package com.tarit.slipledger.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tarit.slipledger.budget.BudgetAlerts
import com.tarit.slipledger.budget.RecurringRunner
import com.tarit.slipledger.data.CategoryDef
import com.tarit.slipledger.data.Period
import com.tarit.slipledger.data.Prefs
import com.tarit.slipledger.data.RecurringRule
import com.tarit.slipledger.data.Repository
import com.tarit.slipledger.data.Settings
import com.tarit.slipledger.data.TagDef
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.data.Wallet
import com.tarit.slipledger.scan.ScanProgress
import com.tarit.slipledger.scan.SlipScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class Summary(val income: Long, val expense: Long) {
    val balance get() = income - expense

    companion object {
        fun of(list: List<Tx>) = Summary(
            income = list.filter { it.type == TxType.INCOME }.sumOf { it.amountSatang },
            expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amountSatang },
        )
    }
}

enum class StatsRange(val label: String, val months: Long) { MONTH("เดือน", 1), QUARTER("ไตรมาส", 3), YEAR("ปี", 12) }

enum class SortBy(val label: String) { NEWEST("ใหม่สุด"), OLDEST("เก่าสุด"), AMOUNT_DESC("ยอดมาก → น้อย"), AMOUNT_ASC("ยอดน้อย → มาก") }

data class TxFilter(
    val query: String = "",
    val types: Set<TxType> = emptySet(),
    val categories: Set<String> = emptySet(),
    val banks: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val wallets: Set<Long> = emptySet(),
    val sort: SortBy = SortBy.NEWEST,
) {
    val hasFilters get() = types.isNotEmpty() || categories.isNotEmpty() || banks.isNotEmpty() || tags.isNotEmpty() || wallets.isNotEmpty()

    fun matches(tx: Tx): Boolean {
        if (types.isNotEmpty() && tx.type !in types) return false
        if (categories.isNotEmpty() && tx.category !in categories) return false
        if (banks.isNotEmpty() && tx.bank !in banks) return false
        if (tags.isNotEmpty() && tx.tagList.none { it in tags }) return false
        if (wallets.isNotEmpty() && tx.walletId !in wallets && tx.toWalletId !in wallets) return false
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOf(tx.counterparty, tx.category, tx.note, tx.bank, tx.tags, Fmt.baht(tx.amountSatang))
            .any { it.contains(q, ignoreCase = true) }
    }

    fun sorted(list: List<Tx>) = when (sort) {
        SortBy.NEWEST -> list.sortedByDescending { it.timestamp }
        SortBy.OLDEST -> list.sortedBy { it.timestamp }
        SortBy.AMOUNT_DESC -> list.sortedByDescending { it.amountSatang }
        SortBy.AMOUNT_ASC -> list.sortedBy { it.amountSatang }
    }
}

/** Projected spending for the current period, based on the daily average so far. */
data class Forecast(val spent: Long, val projected: Long, val daysLeft: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val prefs = Prefs.get(app)
    private val scanner = SlipScanner(app)
    private val zone = ZoneId.systemDefault()

    val settings: StateFlow<Settings> = prefs.settings
    private val period: Flow<Period> = settings.map { it.period }.distinctUntilChanged()

    val month = MutableStateFlow(prefs.settings.value.period.current())

    private fun <T> Flow<T>.state(initial: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val categories: StateFlow<List<CategoryDef>> = repo.observeCategories().state(emptyList())
    val tags: StateFlow<List<TagDef>> = repo.observeTags().state(emptyList())
    val recurring: StateFlow<List<RecurringRule>> = repo.recurring.observe().state(emptyList())

    /** Transactions in the selected budget period. */
    val transactions: StateFlow<List<Tx>> = combine(month, period) { ym, p -> ym to p }
        .flatMapLatest { (ym, p) -> repo.observeRange(p.startMillis(ym), p.endMillis(ym)) }
        .state(emptyList())

    private val allTx: StateFlow<List<Tx>> = repo.observeAll().state(emptyList())

    val banks: StateFlow<List<String>> = allTx.map { list -> list.map { it.bank }.filter { it.isNotBlank() }.distinct().sorted() }
        .state(emptyList())

    // ---- Wallets ----

    val wallets: StateFlow<List<Wallet>> = repo.observeWallets().state(emptyList())

    /** Current balance per wallet id: opening balance plus everything since the opening date. */
    val balances: StateFlow<Map<Long, Long>> = combine(wallets, allTx) { ws, txs ->
        ws.associate { w -> w.id to w.openingBalance + txs.filter { it.timestamp >= w.openingDate }.sumOf { it.effectOn(w.id) } }
    }.state(emptyMap())

    fun saveWallet(w: Wallet) = viewModelScope.launch { repo.saveWallet(w) }

    /** Ticks or unticks a wallet for the total balance. */
    fun setWalletCounted(id: Long, counted: Boolean) {
        updateSettings { it.copy(excludedWallets = if (counted) it.excludedWallets - id else it.excludedWallets + id) }
        // The widget shows the same total.
        viewModelScope.launch { com.tarit.slipledger.widget.SlipWidget.refresh(getApplication()) }
    }

    /** Sets what the wallet holds right now; later transactions are added from this point. */
    fun setBalance(w: Wallet, satang: Long) = viewModelScope.launch {
        repo.saveWallet(w.copy(openingBalance = satang, openingDate = System.currentTimeMillis(), balanceSet = true))
    }

    fun moveWallet(w: Wallet, delta: Int) = viewModelScope.launch {
        val list = wallets.value.toMutableList()
        val i = list.indexOfFirst { it.id == w.id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@launch
        list.add(j, list.removeAt(i))
        repo.saveWalletOrder(list)
    }

    fun deleteWallet(w: Wallet) = viewModelScope.launch { repo.deleteWallet(w) }

    // ---- Review inbox ----

    val needsReview: StateFlow<List<Tx>> = repo.observeNeedsReview().state(emptyList())

    fun markReviewed(ids: List<Long>) = viewModelScope.launch { repo.markReviewed(ids) }

    /** Action requested from outside the app, e.g. a widget button ("scan" or "expense"). */
    val pendingAction = MutableStateFlow<String?>(null)

    // ---- History ----

    val filter = MutableStateFlow(TxFilter())

    /** Searching looks through every month; plain filtering stays within the selected month. */
    val history: StateFlow<List<Tx>> = combine(filter, transactions, allTx) { f, monthTx, all ->
        val source = if (f.query.isNotBlank()) all else monthTx
        f.sorted(source.filter(f::matches))
    }.state(emptyList())

    // ---- Stats ----

    val statsRange = MutableStateFlow(StatsRange.MONTH)

    /** First and last budget month covered by the stats range containing [ym]. */
    fun rangeMonths(range: StatsRange, ym: YearMonth): Pair<YearMonth, YearMonth> = when (range) {
        StatsRange.MONTH -> ym to ym
        StatsRange.QUARTER -> YearMonth.of(ym.year, (ym.monthValue - 1) / 3 * 3 + 1).let { it to it.plusMonths(2) }
        StatsRange.YEAR -> YearMonth.of(ym.year, 1) to YearMonth.of(ym.year, 12)
    }

    private fun statsFlow(offset: Long): Flow<List<Tx>> = combine(month, statsRange, period) { ym, r, p -> Triple(ym, r, p) }
        .flatMapLatest { (ym, r, p) ->
            val (first, last) = rangeMonths(r, ym.minusMonths(offset * r.months))
            repo.observeRange(p.startMillis(first), p.endMillis(last))
        }

    val statsTx: StateFlow<List<Tx>> = statsFlow(0).state(emptyList())
    val statsPrevTx: StateFlow<List<Tx>> = statsFlow(1).state(emptyList())

    /** Income/expense of the selected month and the 5 months before it, oldest first. */
    val trend: StateFlow<List<Pair<YearMonth, Summary>>> = combine(month, period) { ym, p -> ym to p }
        .flatMapLatest { (ym, p) ->
            val first = ym.minusMonths(5)
            repo.observeRange(p.startMillis(first), p.endMillis(ym)).map { list ->
                (0L..5L).map { i ->
                    val m = first.plusMonths(i)
                    m to Summary.of(list.filter { p.contains(m, dateOf(it.timestamp)) })
                }
            }
        }.state(emptyList())

    // ---- Habits ----

    /** Days in a row (up to today, or yesterday if nothing is recorded yet today) with at least one record. */
    val streak: StateFlow<Int> = repo.observeTimestampsSince(System.currentTimeMillis() - 400L * 86_400_000)
        .map { stamps ->
            val days = stamps.map { dateOf(it) }.toHashSet()
            var day = LocalDate.now().let { if (it in days) it else it.minusDays(1) }
            var count = 0
            while (day in days) { count++; day = day.minusDays(1) }
            count
        }.state(0)

    fun forecast(list: List<Tx>, ym: YearMonth, p: Period): Forecast? {
        val today = LocalDate.now()
        if (!p.contains(ym, today)) return null
        val elapsed = (today.toEpochDay() - p.startDate(ym).toEpochDay() + 1).toInt()
        val length = p.lengthInDays(ym)
        val expenses = list.filter { it.type == TxType.EXPENSE }
        val spent = expenses.sumOf { it.amountSatang }
        // Fixed monthly costs happen once; only day-to-day spending is extrapolated.
        val fixed = expenses.filter { it.source == TxSource.RECURRING }.sumOf { it.amountSatang }
        return Forecast(spent, fixed + (spent - fixed) * length / elapsed, length - elapsed)
    }

    private fun dateOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    // ---- Scanning ----

    private val _scan = MutableStateFlow<ScanProgress?>(null)
    val scan: StateFlow<ScanProgress?> = _scan

    /** One-off message for a snackbar. */
    val message = MutableStateFlow<String?>(null)

    private var scanJob: Job? = null

    fun shiftMonth(delta: Long) {
        month.value = month.value.plusMonths(delta)
    }

    fun scanGallery() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _scan.value = ScanProgress(0, 0, 0)
            try {
                val found = scanner.scan { _scan.value = it }
                message.value = if (found == 0) "ไม่พบสลิปใหม่" else "เพิ่มรายการจากสลิป $found รายการ"
            } finally {
                _scan.value = null
            }
        }
    }

    // ---- Edits ----

    fun save(tx: Tx, previous: Tx, applyToPayee: Boolean) = viewModelScope.launch {
        if (tx.id == 0L) repo.addIfNew(tx) else repo.update(tx, previous, applyToPayee)
    }

    fun delete(tx: Tx) = viewModelScope.launch { repo.delete(tx) }

    fun addCategory(def: CategoryDef) = viewModelScope.launch {
        val order = categories.value.filter { it.kind == def.kind }.maxOfOrNull { it.sortOrder + 1 } ?: 0
        repo.addCategory(def.copy(sortOrder = order))
    }

    fun updateCategory(old: CategoryDef, new: CategoryDef) = viewModelScope.launch {
        val renamed = if (old.name != new.name) mapOf(old.name to new.name) else emptyMap()
        repo.saveCategories(listOf(new), renamed)
        if (renamed.isNotEmpty()) renameBudget(old.name, new.name)
    }

    fun moveCategory(def: CategoryDef, delta: Int) = viewModelScope.launch {
        val list = categories.value.filter { it.kind == def.kind }.toMutableList()
        val i = list.indexOfFirst { it.id == def.id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@launch
        list.add(j, list.removeAt(i))
        repo.saveCategories(list.mapIndexed { k, c -> c.copy(sortOrder = k) })
    }

    fun deleteCategory(def: CategoryDef) = viewModelScope.launch {
        repo.deleteCategory(def)
        prefs.update { it.copy(categoryBudgets = it.categoryBudgets - def.name) }
    }

    private fun renameBudget(old: String, new: String) = prefs.update { s ->
        val budget = s.categoryBudgets[old] ?: return@update s
        s.copy(categoryBudgets = s.categoryBudgets - old + (new to budget))
    }

    fun addTag(name: String) = viewModelScope.launch { repo.addTag(name) }

    fun moveTag(tag: TagDef, delta: Int) = viewModelScope.launch {
        val list = tags.value.toMutableList()
        val i = list.indexOf(tag)
        val j = i + delta
        if (i < 0 || j !in list.indices) return@launch
        list.add(j, list.removeAt(i))
        repo.saveTagOrder(list)
    }

    fun deleteTag(tag: TagDef) = viewModelScope.launch { repo.deleteTag(tag) }

    fun saveRecurring(rule: RecurringRule) = viewModelScope.launch {
        repo.recurring.upsert(rule)
        RecurringRunner.run(getApplication())
    }

    fun deleteRecurring(rule: RecurringRule) = viewModelScope.launch { repo.recurring.delete(rule) }

    fun updateSettings(transform: (Settings) -> Settings) {
        val before = prefs.settings.value
        prefs.update(transform)
        val after = prefs.settings.value
        if (before.monthStartDay != after.monthStartDay) month.value = after.period.current()
        if (before.monthlyBudget != after.monthlyBudget || before.categoryBudgets != after.categoryBudgets ||
            before.monthStartDay != after.monthStartDay
        ) {
            viewModelScope.launch {
                BudgetAlerts.check(getApplication())
                repo.changed()
            }
        }
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        val count = runCatching {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { repo.exportCsv(it) }
            }
        }
        message.value = count.fold({ "ส่งออก $it รายการแล้ว" }, { "ส่งออกไม่สำเร็จ: ${it.message}" })
    }

    fun resetData() = viewModelScope.launch {
        repo.resetData()
        message.value = "ล้างข้อมูลทั้งหมดแล้ว"
    }

    /** Lets every gallery image be examined again, e.g. after adding your name. Existing rows are kept. */
    fun resetScanHistory() = viewModelScope.launch {
        repo.scannedImages.clear()
        message.value = "ล้างประวัติแล้ว กดสแกนเพื่ออ่านรูปทั้งหมดอีกครั้ง"
    }
}
