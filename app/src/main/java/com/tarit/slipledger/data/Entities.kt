package com.tarit.slipledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE, TRANSFER }

enum class TxSource { SLIP, NOTIFICATION, MANUAL, RECURRING }

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["sourceKey"], unique = true), Index("timestamp")],
)
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    /** Amount in satang (1 baht = 100 satang) to avoid floating point errors. */
    val amountSatang: Long,
    val timestamp: Long,
    val counterparty: String = "",
    val bank: String = "",
    val account: String = "",
    val category: String = "",
    val note: String = "",
    val source: TxSource,
    /** Unique key per real-world transaction, used to avoid importing the same slip twice. */
    val sourceKey: String,
    val imageUri: String? = null,
    val rawText: String = "",
    /** User labels, comma separated. */
    val tags: String = "",
    /** Wallet the money left (expense, transfer) or arrived in (income); null = not assigned. */
    val walletId: Long? = null,
    /** Destination wallet of a transfer between own wallets. */
    val toWalletId: Long? = null,
    /** Imported automatically but something looked uncertain; shown in the review inbox. */
    val needsReview: Boolean = false,
) {
    val tagList: List<String> get() = Tags.split(tags)

    /** How this transaction changes [wallet]'s balance. */
    fun effectOn(wallet: Long): Long = when {
        type == TxType.TRANSFER && toWalletId == wallet && walletId != wallet -> amountSatang
        walletId != wallet -> 0
        type == TxType.INCOME -> amountSatang
        else -> -amountSatang
    }
}

enum class WalletKind { BANK, CASH, EWALLET }

/** A place money is kept: a bank account, cash, or an e-wallet. */
@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: WalletKind,
    /** Bank name as produced by [com.tarit.slipledger.parse.Banks]; empty for cash. */
    val bank: String = "",
    /** Last 4 digits of the account number, used to match slips and alerts. */
    val accountHint: String = "",
    /** Balance at [openingDate]; transactions from then on are added to it. */
    val openingBalance: Long = 0,
    val openingDate: Long = 0,
    /** False for wallets created automatically from a slip until the user enters the real balance. */
    val balanceSet: Boolean = true,
    val color: Long,
    val sortOrder: Int = 0,
)

object Tags {
    fun split(s: String) = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    fun join(list: Collection<String>) = list.map { it.replace(",", " ").trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
}

/** A category shown in pickers. Built-in ones (see [Categories]) can be restyled but not renamed or deleted. */
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryDef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** INCOME or EXPENSE. */
    val kind: TxType,
    /** Key into the app's icon set, see CategoryIcons. */
    val icon: String,
    /** ARGB colour. */
    val color: Long,
    val sortOrder: Int,
    val builtIn: Boolean = false,
)

@Entity(tableName = "tag_defs")
data class TagDef(@PrimaryKey val name: String, val sortOrder: Int)

/** A transaction added automatically every month, e.g. rent or a phone plan. */
@Entity(tableName = "recurring")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountSatang: Long,
    val category: String,
    val counterparty: String,
    val note: String = "",
    /** 1-28. */
    val dayOfMonth: Int,
    /** Last month this rule produced a transaction, "yyyy-MM"; empty if never. */
    val lastRun: String = "",
    val active: Boolean = true,
    val walletId: Long? = null,
)

/** Every gallery image already examined, slip or not, so it is never OCR'd again. */
@Entity(tableName = "scanned_images")
data class ScannedImage(
    @PrimaryKey val mediaId: Long,
    val isSlip: Boolean,
    val scannedAt: Long,
)

/** Category the user picked for a payee, applied automatically to that payee's future slips. */
@Entity(tableName = "category_rules")
data class CategoryRule(
    /** Normalised payee name, see [CategoryRule.keyOf]. */
    @PrimaryKey val key: String,
    val category: String,
) {
    companion object {
        fun keyOf(counterparty: String) = counterparty.lowercase().replace(Regex("""[\s.,()\-*]"""), "")
    }
}

data class CategoryTotal(val category: String, val total: Long)

object Categories {
    const val FOOD = "อาหาร/เครื่องดื่ม"
    const val GROCERY = "ของใช้/ร้านสะดวกซื้อ"
    const val TRAVEL = "เดินทาง"
    const val SHOPPING = "ช้อปปิ้ง"
    const val BILLS = "บิล/ค่าบริการ"
    const val HEALTH = "สุขภาพ"
    const val FUN = "บันเทิง"
    const val EDUCATION = "การศึกษา"
    const val PERSON = "โอนให้บุคคล"
    const val SHOP = "ร้านค้า/บริษัท"
    const val SALARY = "เงินเดือน"
    const val INCOME_OTHER = "รายรับอื่น"
    const val SELF = "โอนระหว่างบัญชีตัวเอง"
    const val OTHER = "อื่นๆ"

    val expense = listOf(FOOD, GROCERY, TRAVEL, SHOPPING, BILLS, HEALTH, FUN, EDUCATION, PERSON, SHOP, OTHER)
    val income = listOf(SALARY, INCOME_OTHER, OTHER)

    /** Built-in categories with their default icon key and colour. */
    val defaults: List<CategoryDef> = listOf(
        Triple(FOOD, "food", 0xFFFF8A5B), Triple(GROCERY, "grocery", 0xFF2BB673),
        Triple(TRAVEL, "car", 0xFF3E8EF7), Triple(SHOPPING, "bag", 0xFFE5579B),
        Triple(BILLS, "receipt", 0xFF8E6CEF), Triple(HEALTH, "heart", 0xFFEF5350),
        Triple(FUN, "movie", 0xFFF5A524), Triple(EDUCATION, "school", 0xFF00A3BF),
        Triple(PERSON, "person", 0xFF6C8EAD), Triple(SHOP, "store", 0xFFB07A4F),
        Triple(OTHER, "more", 0xFF9AA19D),
    ).mapIndexed { i, (name, icon, color) ->
        CategoryDef(name = name, kind = TxType.EXPENSE, icon = icon, color = color, sortOrder = i, builtIn = true)
    } + listOf(
        Triple(SALARY, "payments", 0xFF16A36B), Triple(INCOME_OTHER, "savings", 0xFF0E8A5C),
    ).mapIndexed { i, (name, icon, color) ->
        CategoryDef(name = name, kind = TxType.INCOME, icon = icon, color = color, sortOrder = i, builtIn = true)
    }
}
