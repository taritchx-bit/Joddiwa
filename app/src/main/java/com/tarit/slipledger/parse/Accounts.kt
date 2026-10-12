package com.tarit.slipledger.parse

/**
 * Slips and alerts print account numbers masked, e.g. "XXX-X-XX145-9", "xxx-xxx394-8", "x-3931", "XX1459".
 * The visible digits (usually the last 4) identify which of the user's accounts was used.
 */
object Accounts {
    private val allowed = Regex("""[xX*\d-]+""")
    private val mask = Regex("""[xX*]""")

    /**
     * The last 4 visible digits of an account token, or null if [raw] is not one.
     * Unmasked numbers ("3931") are only trusted when [allowUnmasked], i.e. right under a from/to label.
     */
    fun hintOf(raw: String, allowUnmasked: Boolean = false): String? {
        // Drop OCR junk around the token: "(", "@", "%" (a misread "x"), trailing dots.
        val t = raw.trim { !(it.isDigit() || it == 'x' || it == 'X' || it == '*' || it == '-') }
        if (t.isEmpty() || !allowed.matches(t)) return null
        val digits = t.filter { it.isDigit() }
        val masked = mask.containsMatchIn(t)
        return when {
            masked && digits.length in 3..12 -> digits.takeLast(4)
            !masked && allowUnmasked && digits.length in 4..12 -> digits.takeLast(4)
            else -> null
        }
    }

    /** Masked account hints in reading order, without duplicates. */
    fun maskedHints(text: String): List<String> =
        text.split(Regex("""\s+""")).mapNotNull { hintOf(it) }.distinct()

    /** Account hint printed on the label's line or in the few lines below it. */
    fun after(lines: List<String>, labelRe: Regex): String? {
        val i = lines.indexOfFirst { labelRe.containsMatchIn(it) }
        if (i < 0) return null
        lines[i].split(Regex("""\s+""")).firstNotNullOfOrNull { hintOf(it) }?.let { return it }
        return lines.drop(i + 1).take(3).firstNotNullOfOrNull { line ->
            line.split(Regex("""\s+""")).firstNotNullOfOrNull { hintOf(it, allowUnmasked = true) }
        }
    }
}
