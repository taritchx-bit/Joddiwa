package com.tarit.slipledger.parse

import com.tarit.slipledger.data.TxType

data class ParsedAlert(
    /** INCOME for money in, EXPENSE for money out. */
    val type: TxType,
    val amountSatang: Long,
    /** Time printed in the message, if any. */
    val timestamp: Long?,
    val hasTime: Boolean,
    /** Last 4 digits of the account, empty if not printed. */
    val account: String,
    val bank: String,
)

/**
 * Reads money in/out alerts sent by bank LINE official accounts (Krungthai Connext, SCB Connect, K PLUS ...)
 * or by the bank apps themselves, e.g. "เงินเข้า: 1.00 บาท เข้าบัญชี XX1459 เมื่อ 02/10/69".
 */
object NotificationParser {
    const val LINE_PACKAGE = "jp.naver.line.android"

    private val bankPackages = setOf(
        "ktbcs.netbank", "com.scb.phone", "com.kasikorn.retail.mbanking.wap",
        "com.bbl.mobilebanking", "com.krungsri.kma", "com.ttbbank.oneapp",
        "com.gsb.mymo", "com.kkpfg.dime",
    )
    /** Apps read out of the box (the user can still switch them off). */
    val builtInPackages: Set<String> get() = bankPackages + LINE_PACKAGE

    private val senderRe = Regex(
        """(?i)connex|connect|scb|k\s*plus|kbank|krungsri|kma|ttb|bualuang|bangkok\s*bank|gsb|mymo|dime|kept|""" +
            """กรุงไทย|กสิกร|ไทยพาณิชย์|กรุงศรี|กรุงเทพ|ออมสิน|ธนาคาร"""
    )
    private val incomeRe = Regex("""(?i)เงินเข้า|รับเงิน|โอนเข้า|ได้รับเงิน|money\s*in|deposit|received""")
    private val outgoingRe = Regex("""(?i)เงินออก|โอนออก|โอนเงินออก|ถอนเงิน|ถอน|ชำระ|จ่าย|หักบัญชี|ตัดบัญชี|withdraw|money\s*out|payment""")
    private val amountRe = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d+)\.(\d{2})(?!\d)""")
    private val accountRe = Regex("""(?i)(?:บัญชี|บ/ช|a/c|account)\s*:?\s*([xX*\d][xX*\d-]{2,})""")

    fun isRelevantSender(packageName: String, title: String): Boolean =
        packageName in bankPackages || (packageName == LINE_PACKAGE && senderRe.containsMatchIn(title))

    fun parse(title: String, text: String): ParsedAlert? {
        val income = incomeRe.find(text)
        val outgoing = outgoingRe.find(text)
        // Whichever keyword comes first decides the direction ("เงินออก ... ได้รับเงิน" is money out).
        val (type, keyword) = when {
            income == null && outgoing == null -> return null
            outgoing == null -> TxType.INCOME to income!!
            income == null -> TxType.EXPENSE to outgoing
            income.range.first < outgoing.range.first -> TxType.INCOME to income
            else -> TxType.EXPENSE to outgoing
        }

        val m = amountRe.find(text, keyword.range.first) ?: amountRe.find(text) ?: return null
        val satang = m.groupValues[1].replace(",", "").toLong() * 100 + m.groupValues[2].toLong()
        if (satang <= 0) return null

        return ParsedAlert(
            type = type,
            amountSatang = satang,
            timestamp = ThaiDate.parseEpochMillis(text),
            hasTime = ThaiDate.parseTime(text) != null,
            account = accountRe.find(text)?.groupValues?.get(1)?.let { Accounts.hintOf(it) ?: it.filter(Char::isDigit).takeLast(4) }.orEmpty(),
            bank = Banks.fromText("$title $text").orEmpty(),
        )
    }
}
