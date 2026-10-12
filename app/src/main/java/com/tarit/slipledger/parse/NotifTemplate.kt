package com.tarit.slipledger.parse

/**
 * Learns how to read an app's notifications from one example the user marks up: which number is the
 * amount and, optionally, which words are the merchant. The example becomes a pattern in which numbers
 * (card digits, dates, times) match any number, the marked parts are captured, and the remaining words
 * must appear as they did, so the same kind of alert with different figures and shops is recognised.
 */
object NotifTemplate {
    data class Token(val text: String, val start: Int, val end: Int, val numeric: Boolean)

    data class Match(val amountSatang: Long, val merchant: String)

    // Numbers, including masked account numbers such as "XX1459" or "**1234", dates and times.
    private const val NUMBER = """(?:[xX*]+-?)?\d[\dxX*,./:-]*"""
    // A run of other non-space characters, stopping before a masked number so "บัญชีXX1459" splits in two.
    private const val WORD = """(?:(?![xX*]+-?\d)[^\s\d])+"""
    private val tokenRe = Regex("$NUMBER|$WORD")

    private const val AMOUNT = """(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    private const val ANY_NUMBER = """[\dxX*][\dxX*,./:-]*"""

    /**
     * How many words (numbers not counted) before the first marked part must also match. Two is enough to
     * tell "spent" from "paid" alerts of the same app while leaving out greetings further back.
     */
    private const val CONTEXT = 2

    fun tokenize(text: String): List<Token> = tokenRe.findAll(text).map {
        Token(it.value, it.range.first, it.range.last + 1, it.value.any(Char::isDigit))
    }.toList()

    /**
     * Pattern for [tokens] with the amount at [amountIndex] and the merchant over [merchant] (token indexes).
     * Only the stretch from a few words before the first marked token to one token after the last is used,
     * so greetings or footers that change between alerts do not matter.
     */
    fun build(tokens: List<Token>, amountIndex: Int, merchant: IntRange?): String {
        require(amountIndex in tokens.indices)
        val marks = listOfNotNull(amountIndex, merchant?.first, merchant?.last)
        var from = marks.min()
        var words = 0
        while (from > 0 && words < CONTEXT) {
            from--
            if (!tokens[from].numeric) words++
        }
        val to = (marks.max() + 1).coerceAtMost(tokens.lastIndex)
        val parts = mutableListOf<String>()
        var i = from
        while (i <= to) {
            when {
                i == amountIndex -> parts += AMOUNT
                merchant != null && i == merchant.first -> {
                    // Lazy when something follows to stop at; otherwise take the rest of the line.
                    parts += if (merchant.last < to) "(?<merchant>.+?)" else "(?<merchant>[^\\n]+)"
                    i = merchant.last
                }
                tokens[i].numeric -> parts += ANY_NUMBER
                else -> parts += Regex.escape(tokens[i].text)
            }
            i++
        }
        return parts.joinToString("""\s*""")
    }

    fun match(pattern: String, text: String): Match? {
        val m = runCatching { Regex(pattern).find(text) }.getOrNull() ?: return null
        val satang = m.groupValues[1].replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong() ?: return null
        if (satang <= 0) return null
        val merchant = runCatching { m.groups["merchant"]?.value }.getOrNull().orEmpty().trim().take(60)
        return Match(satang, merchant)
    }
}
