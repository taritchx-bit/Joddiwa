package com.tarit.slipledger.parse

import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.TxType

data class ParsedSlip(
    val amountSatang: Long,
    val timestamp: Long?,
    val type: TxType,
    val from: String,
    val to: String,
    val bank: String,
    val category: String,
    /** Last 4 digits of the paying / receiving account, empty if not found. */
    val fromAccount: String = "",
    val toAccount: String = "",
)

/** Turns OCR text of a bank slip into a transaction. Tolerant of OCR noise; returns null for non-slips. */
object SlipParser {
    private val moneyRe = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d+)\s?[.,]\s?(\d{2})(?![\d,])""")
    private val amountKeyRe = Regex("""(?i)จำนวน|ยอดเงิน|ยอดชำระ|ยอดโอน|amount|total""")
    private val bahtRe = Regex("""(?i)บาท|THB|฿""")
    private val feeRe = Regex("""(?i)ค่าธรรมเนียม|fee|ส่วนลด|คงเหลือ|balance""")
    private val slipWordRe = Regex(
        """(?i)สำเร็จ|โอนเงิน|จ่ายบิล|ชำระ|เติมเงิน|รหัสอ้างอิง|เลขที่รายการ|เลขที่สลิป|อ้างอิง|ค่าธรรมเนียม|transfer|success|amount|ref"""
    )
    // Labels may be preceded by 1-2 junk characters that OCR made out of an icon ("i   ไปยัง").
    private val fromKeyRe = Regex("""^\s*(?:\S{1,2}\s+)?(จาก|FROM|ผู้โอน)(?=[\s:]|$)[\s:]*(.*)$""", RegexOption.IGNORE_CASE)
    private val toKeyRe = Regex("""^\s*(?:\S{1,2}\s+)?(ไปยัง|ไปที่|ถึง|TO|ผู้รับ(?:เงิน)?)(?=[\s:]|$)[\s:]*(.*)$""", RegexOption.IGNORE_CASE)
    private val letterRe = Regex("""[ก-๙A-Za-z]""")

    fun parse(ocrText: String, ownerNames: List<String>, qr: SlipQr? = null): ParsedSlip? {
        val text = normalize(ocrText)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || !slipWordRe.containsMatchIn(text)) return null
        val amount = findAmount(lines) ?: return null

        val from = valueAfter(lines, fromKeyRe)
        val to = valueAfter(lines, toKeyRe)
        val type = classify(from, to, ownerNames)
        val bank = Banks.fromCode(qr?.bankCode) ?: Banks.fromText(text).orEmpty()

        // From/to accounts: under their labels, else the masked numbers in reading order (sender first).
        val masked = Accounts.maskedHints(text)
        val fromAccount = Accounts.after(lines, fromKeyRe) ?: masked.firstOrNull()
        val toAccount = Accounts.after(lines, toKeyRe) ?: masked.firstOrNull { it != fromAccount }

        return ParsedSlip(
            fromAccount = fromAccount.orEmpty(),
            toAccount = toAccount.orEmpty(),
            amountSatang = amount,
            timestamp = ThaiDate.parseEpochMillis(text),
            type = type,
            from = from,
            to = to,
            bank = bank,
            category = when (type) {
                TxType.TRANSFER -> Categories.SELF
                TxType.INCOME -> Categories.INCOME_OTHER
                TxType.EXPENSE -> AutoCategory.guess(to)
            },
        )
    }

    /** Tesseract writes สระอำ as nikhahit + sara aa ("จํานวน"), and แ as two เ. */
    private fun normalize(s: String) = s.replace("ํา", "ำ").replace("เเ", "แ")

    /** Picks the slip amount: prefer a number on an "amount" line, then one followed by บาท, never the fee. */
    internal fun findAmount(lines: List<String>): Long? {
        data class Candidate(val satang: Long, val score: Int, val index: Int)
        val candidates = lines.flatMapIndexed { i, line ->
            if (feeRe.containsMatchIn(line)) return@flatMapIndexed emptyList()
            val prev = lines.getOrNull(i - 1).orEmpty()
            moneyRe.findAll(line).mapNotNull { m ->
                val satang = toSatang(m.groupValues[1], m.groupValues[2]) ?: return@mapNotNull null
                if (satang <= 0) return@mapNotNull null
                var score = 0
                if (amountKeyRe.containsMatchIn(line)) score += 3
                else if (amountKeyRe.containsMatchIn(prev) && !feeRe.containsMatchIn(prev)) score += 2
                if (bahtRe.containsMatchIn(line.substring(m.range.last + 1))) score += 2
                Candidate(satang, score, i)
            }.toList()
        }
        return candidates.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { it.satang })?.satang
    }

    private fun toSatang(baht: String, satang: String): Long? =
        baht.replace(",", "").toLongOrNull()?.let { it * 100 + satang.toLong() }

    /** Text right after a label ("จาก", "TO", ...), or the next line when the label stands alone. */
    internal fun valueAfter(lines: List<String>, keyRe: Regex): String {
        for ((i, line) in lines.withIndex()) {
            val m = keyRe.find(line) ?: continue
            val rest = clean(m.groupValues[2])
            if (letterRe.findAll(rest).count() >= 2) return rest
            val next = lines.drop(i + 1).take(2).map(::clean)
                .firstOrNull { letterRe.findAll(it).count() >= 2 && Accounts.hintOf(it) == null }
            if (next != null) return next
        }
        return ""
    }

    /** Strips OCR junk from bank icons in front of names and limits the length. */
    private fun clean(s: String): String =
        s.replace(Regex("""^[^ก-๙A-Za-z]+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(60)

    internal fun classify(from: String, to: String, ownerNames: List<String>): TxType {
        fun mine(s: String) = ownerNames.any { it.isNotBlank() && s.contains(it, ignoreCase = true) }
        return when {
            mine(from) && mine(to) -> TxType.TRANSFER
            mine(to) && !mine(from) -> TxType.INCOME
            else -> TxType.EXPENSE // slips in your own gallery are usually payments you made
        }
    }
}
