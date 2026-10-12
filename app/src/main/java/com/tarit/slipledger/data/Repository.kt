package com.tarit.slipledger.data

import android.content.Context
import com.tarit.slipledger.budget.BudgetAlerts
import com.tarit.slipledger.widget.SlipWidget
import kotlinx.coroutines.flow.Flow
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Repository private constructor(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val txDao = db.txDao()
    private val rules = db.categoryRuleDao()
    private val categoryDao = db.categoryDao()
    private val tagDao = db.tagDao()
    private val wallets = db.walletDao()
    val recurring = db.recurringDao()
    val scannedImages = db.scannedImageDao()

    fun observeRange(from: Long, to: Long): Flow<List<Tx>> = txDao.observeRange(from, to)
    fun observeAll(): Flow<List<Tx>> = txDao.observeAll()
    fun observeTimestampsSince(from: Long) = txDao.observeTimestampsSince(from)
    fun observeCategories(): Flow<List<CategoryDef>> = categoryDao.observe()
    fun observeTags(): Flow<List<TagDef>> = tagDao.observe()

    suspend fun expenseByCategory(from: Long, to: Long) = txDao.expenseByCategory(from, to)

    /** Creates the built-in categories the first time the app (or this version) runs. */
    suspend fun ensureDefaultCategories() {
        if (categoryDao.count() == 0) categoryDao.insertAll(Categories.defaults)
    }

    /** Category the user previously chose for this payee, if any. */
    suspend fun learnedCategory(counterparty: String): String? =
        counterparty.takeIf { it.isNotBlank() }?.let { rules.find(CategoryRule.keyOf(it)) }

    /**
     * Inserts a transaction unless it is already recorded. The same transfer can reach us
     * twice (a slip in the gallery and a LINE notification, or the same slip saved twice),
     * so anything with the same type and amount within [DUPLICATE_WINDOW_MS] counts as a duplicate.
     * A transfer between your own accounts also triggers a "money in" alert; that alert is not income.
     * Returns true when the row was inserted.
     */
    suspend fun addIfNew(tx: Tx): Boolean {
        val result = addOrMerge(tx)
        if (result) {
            rememberTags(tx.tagList)
            if (tx.type == TxType.EXPENSE) BudgetAlerts.check(context)
            changed()
        }
        return result
    }

    // Alerts and the slip scan run on different threads; without this, a slip and its alert that
    // arrive together can both pass the duplicate check and both be inserted.
    private val addLock = Mutex()

    private suspend fun addOrMerge(tx: Tx): Boolean = addLock.withLock {
        if (tx.source == TxSource.SLIP || tx.source == TxSource.NOTIFICATION) {
            val wide = if (tx.source == TxSource.SLIP) SLIP_ALERT_WINDOW_MS else DUPLICATE_WINDOW_MS
            val found = txDao.findSimilar(tx.amountSatang, tx.timestamp - wide, tx.timestamp + wide)
                .filter { it.source == TxSource.SLIP || it.source == TxSource.NOTIFICATION }
            // Alerts arrive in real time, so an alert is the same payment as another row only if it is
            // seconds away; two equal payments minutes apart are two payments. A slip only duplicates
            // another slip when one of them has no QR reference (distinct references are distinct payments).
            // The wider window is for pairing a slip with its still-unpaired alert (see below).
            fun isDuplicate(old: Tx): Boolean {
                val gap = abs(old.timestamp - tx.timestamp)
                return when {
                    tx.source == TxSource.NOTIFICATION -> gap <= ALERT_DUPLICATE_WINDOW_MS
                    old.source == TxSource.SLIP -> gap <= DUPLICATE_WINDOW_MS &&
                        (tx.sourceKey.startsWith("img:") || old.sourceKey.startsWith("img:"))
                    else -> false
                }
            }
            val similar = found.filter(::isDuplicate)
            // Own-account transfers also raise money in/out alerts; those may lag the slip a little more.
            val nearTransfer = found.any { it.type == TxType.TRANSFER && abs(it.timestamp - tx.timestamp) <= DUPLICATE_WINDOW_MS }

            // A slip for something a LINE alert already recorded: keep one row with the slip's richer details.
            // A slip's time can be far from the alert's (no time printed -> screenshot time), so look wider,
            // but only at alerts no slip has claimed yet; the nearest one wins.
            if (tx.source == TxSource.SLIP) {
                found.filter { it.type == tx.type && it.source == TxSource.NOTIFICATION }
                    .minByOrNull { abs(it.timestamp - tx.timestamp) }?.let { alert ->
                    return@withLock runCatching {
                        txDao.update(
                            tx.copy(
                                id = alert.id,
                                walletId = tx.walletId ?: alert.walletId,
                                tags = alert.tags,
                                note = alert.note,
                            )
                        )
                    }.isSuccess
                }
            }
            if (similar.any { it.type == tx.type }) return@withLock false
            // Moving money between own accounts also triggers "money in/out" alerts; those are not income/expense.
            if (tx.type != TxType.TRANSFER && tx.source == TxSource.NOTIFICATION && nearTransfer) return@withLock false
            if (tx.type == TxType.TRANSFER) {
                // The "money out" and "money in" alerts of this transfer: the nearest of each, not every look-alike.
                found.filter { it.type != TxType.TRANSFER && it.source == TxSource.NOTIFICATION }
                    .groupBy { it.type }
                    .values
                    .mapNotNull { group -> group.minByOrNull { abs(it.timestamp - tx.timestamp) } }
                    .forEach { txDao.delete(it) }
            }
        }
        txDao.insert(tx) != -1L
    }

    /**
     * Saves an edited transaction. When the category changed, remembers it for the payee and,
     * if [applyToPayee], re-categorises the payee's other transactions of the same type too.
     * Editing a transaction counts as reviewing it.
     */
    suspend fun update(tx: Tx, previous: Tx?, applyToPayee: Boolean) {
        txDao.update(tx.copy(needsReview = false))
        rememberTags(tx.tagList)
        val categoryChanged = previous != null && previous.category != tx.category
        if (categoryChanged && tx.counterparty.isNotBlank() && tx.type != TxType.TRANSFER) {
            rules.upsert(CategoryRule(CategoryRule.keyOf(tx.counterparty), tx.category))
            if (applyToPayee) txDao.setCategoryFor(tx.counterparty, tx.type, tx.category)
        }
        if (tx.type == TxType.EXPENSE) BudgetAlerts.check(context)
        changed()
    }

    suspend fun delete(tx: Tx) {
        txDao.delete(tx)
        changed()
    }

    /** Refreshes things that show totals outside the app (the home-screen widget). */
    suspend fun changed() = SlipWidget.refresh(context)

    // ---- Review inbox ----

    fun observeNeedsReview(): Flow<List<Tx>> = txDao.observeNeedsReview()

    suspend fun markReviewed(ids: List<Long>) = txDao.markReviewed(ids)

    // ---- Wallets ----

    fun observeWallets(): Flow<List<Wallet>> = wallets.observe()

    /** A cash wallet so manual entries have somewhere to go from day one. */
    suspend fun ensureDefaultWallets() {
        val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (sp.getBoolean("wallets_seeded", false)) return
        if (wallets.all().isEmpty()) {
            wallets.insert(Wallet(name = "เงินสด", kind = WalletKind.CASH, color = 0xFF12B76A, openingDate = System.currentTimeMillis(), balanceSet = false))
        }
        sp.edit().putBoolean("wallets_seeded", true).apply()
    }

    /**
     * The wallet of a bank account seen on a slip or alert. When none matches and the bank is known,
     * a wallet is created for it (balance to be entered by the user) so balances start tracking at once.
     */
    suspend fun resolveWallet(bank: String, hint: String, create: Boolean): Long? {
        val all = wallets.all()
        WalletMatcher.match(all, bank, hint)?.let { w ->
            // Learn the account digits for a bank wallet that did not have them yet.
            if (hint.isNotEmpty() && w.accountHint.isEmpty()) wallets.update(listOf(w.copy(accountHint = hint)))
            return w.id
        }
        if (!create || bank.isEmpty()) return null
        return wallets.insert(
            Wallet(
                name = com.tarit.slipledger.parse.Banks.short(bank) + if (hint.isNotEmpty()) " ··$hint" else "",
                kind = if (bank == "TrueMoney") WalletKind.EWALLET else WalletKind.BANK,
                bank = bank,
                accountHint = hint,
                openingDate = 0,
                balanceSet = false,
                color = com.tarit.slipledger.parse.Banks.color(bank),
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1,
            )
        )
    }

    suspend fun saveWallet(w: Wallet) {
        if (w.id == 0L) wallets.insert(w.copy(sortOrder = (wallets.all().maxOfOrNull { it.sortOrder } ?: 0) + 1))
        else wallets.update(listOf(w))
        changed()
    }

    suspend fun saveWalletOrder(list: List<Wallet>) = wallets.update(list.mapIndexed { i, w -> w.copy(sortOrder = i) })

    /** Deletes a wallet; its transactions stay but become unassigned. */
    suspend fun deleteWallet(w: Wallet) {
        wallets.detachFrom(w.id)
        wallets.detachTo(w.id)
        wallets.delete(w)
        changed()
    }

    // ---- Categories ----

    suspend fun addCategory(def: CategoryDef) = categoryDao.insert(def)

    /** Saves style/order changes; a renamed custom category also renames it on every transaction. */
    suspend fun saveCategories(list: List<CategoryDef>, renamed: Map<String, String> = emptyMap()) {
        categoryDao.update(list)
        renamed.forEach { (old, new) ->
            txDao.renameCategory(old, new)
            rules.renameCategory(old, new)
        }
    }

    /** Deletes a custom category; its transactions move to "อื่นๆ". */
    suspend fun deleteCategory(def: CategoryDef) {
        categoryDao.delete(def)
        txDao.renameCategory(def.name, if (def.kind == TxType.INCOME) Categories.INCOME_OTHER else Categories.OTHER)
        rules.deleteFor(def.name)
    }

    // ---- Tags ----

    private suspend fun rememberTags(tags: List<String>) {
        var order = tagDao.maxOrder()
        tags.forEach { tagDao.insert(TagDef(it, ++order)) }
    }

    suspend fun addTag(name: String) = rememberTags(listOf(name.replace(",", " ").trim()).filter { it.isNotEmpty() })

    suspend fun saveTagOrder(list: List<TagDef>) = tagDao.update(list.mapIndexed { i, t -> t.copy(sortOrder = i) })

    /** Deletes a tag and removes it from every transaction carrying it. */
    suspend fun deleteTag(tag: TagDef) {
        tagDao.delete(tag)
        txDao.withTagLike(tag.name).filter { tag.name in it.tagList }.forEach {
            txDao.update(it.copy(tags = Tags.join(it.tagList - tag.name)))
        }
    }

    // ---- Export / reset ----

    /** Writes every transaction as CSV (UTF-8 with BOM so Excel shows Thai correctly). */
    suspend fun exportCsv(out: OutputStream): Int {
        val list = txDao.all()
        val zone = ZoneId.systemDefault()
        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val time = DateTimeFormatter.ofPattern("HH:mm")
        fun esc(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        out.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write("วันที่,เวลา,ประเภท,จำนวนเงิน,หมวดหมู่,คู่รายการ,ธนาคาร,บัญชี,แท็ก,บันทึก,ที่มา\n")
            list.forEach { tx ->
                val t = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
                val type = when (tx.type) { TxType.INCOME -> "รายรับ"; TxType.EXPENSE -> "รายจ่าย"; TxType.TRANSFER -> "โอนตัวเอง" }
                val source = when (tx.source) { TxSource.SLIP -> "สลิป"; TxSource.NOTIFICATION -> "แจ้งเตือน"; TxSource.MANUAL -> "จดเอง"; TxSource.RECURRING -> "รายการประจำ" }
                w.write(
                    listOf(
                        date.format(t), time.format(t), type, "%.2f".format(tx.amountSatang / 100.0),
                        tx.category, tx.counterparty, tx.bank, tx.account, tx.tags.replace(",", " "), tx.note, source,
                    ).joinToString(",") { esc(it) } + "\n"
                )
            }
        }
        return list.size
    }

    /** Deletes every transaction, learned rule and scan record. Categories, tags and settings stay. */
    suspend fun resetData() {
        txDao.deleteAll()
        rules.clear()
        scannedImages.clear()
        context.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE).edit().clear().apply()
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 15 * 60 * 1000L

        /** An alert is a repeat of the same payment only when it lands this close to it. */
        const val ALERT_DUPLICATE_WINDOW_MS = 2 * 60 * 1000L

        /** How far a slip's time may be from its alert's (a slip without a printed time uses the screenshot time). */
        const val SLIP_ALERT_WINDOW_MS = 3 * 60 * 60 * 1000L

        @Volatile private var instance: Repository? = null

        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}
