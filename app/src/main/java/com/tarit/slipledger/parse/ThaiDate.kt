package com.tarit.slipledger.parse

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Parses the date/time formats printed on Thai bank slips and notifications (Buddhist era years). */
object ThaiDate {
    /**
     * Dots, spaces, tone marks and below-vowels are dropped before lookup: OCR often loses the dots
     * and adds stray marks ("ก.ุย."). Above-vowels stay because they tell มี.ค. (Mar) from ม.ค. (Jan).
     */
    private fun monthKey(s: String) = s.replace(Regex("""[.\sุู็-๎]"""), "").uppercase()

    private val months: Map<String, Int> = buildMap<String, Int> {
        listOf("มค", "กพ", "มีค", "เมย", "พค", "มิย", "กค", "สค", "กย", "ตค", "พย", "ธค")
            .forEachIndexed { i, m -> put(m, i + 1) }
        listOf(
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
        ).forEachIndexed { i, m -> put(m, i + 1) }
        listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            .forEachIndexed { i, m -> put(m, i + 1) }
    }.mapKeys { monthKey(it.key) }

    private val textDateRe = Regex("""(?<![\d.,])(\d{1,2})\s*([ก-๙A-Za-z][ก-๙A-Za-z.\s]{1,12}?)\s*(\d{4}|\d{2})(?!\d)""")
    private val numericDateRe = Regex("""(?<![\d.,])(\d{1,2})[/\-](\d{1,2})[/\-](\d{4}|\d{2})(?!\d)""")
    private val timeRe = Regex("""(?<![\d.,])([01]?\d|2[0-3])\s*:\s*([0-5]\d)(?!\d)""")

    fun parseDate(text: String): LocalDate? {
        val textual = overlappingMatches(textDateRe, text).firstNotNullOfOrNull { m ->
            val key = monthKey(m.groupValues[2])
            months[key]?.let { build(m.groupValues[1].toInt(), it, m.groupValues[3]) }
        }
        return textual ?: overlappingMatches(numericDateRe, text).firstNotNullOfOrNull { m ->
            build(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])
        }
    }

    /** Like findAll, but a rejected match does not swallow the digits a later real date needs. */
    private fun overlappingMatches(re: Regex, text: String) = generateSequence(re.find(text)) { prev ->
        re.find(text, prev.range.first + 1)
    }

    fun parseTime(text: String): LocalTime? =
        timeRe.find(text)?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }

    /** Combined date and time as epoch ms, or null when no date is found. Missing time means midnight. */
    fun parseEpochMillis(text: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val date = parseDate(text) ?: return null
        val time = parseTime(text) ?: LocalTime.MIDNIGHT
        return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
    }

    private fun build(day: Int, month: Int, yearText: String): LocalDate? {
        var year = yearText.toInt()
        if (yearText.length == 2) year += if (year >= 50) 2500 else 2000 // "69" -> 2569 BE, "26" -> 2026 CE
        if (year > 2400) year -= 543 // Buddhist era -> Gregorian
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
}
