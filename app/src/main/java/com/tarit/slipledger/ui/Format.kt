package com.tarit.slipledger.ui

import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object Fmt {
    private val money = DecimalFormat("#,##0.00")
    private val monthsShort = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
    private val monthsLong = listOf(
        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
    )
    private val weekdays = listOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")

    fun baht(satang: Long): String = money.format(satang / 100.0)

    fun parseSatang(text: String): Long? {
        val v = text.replace(",", "").trim().toBigDecimalOrNull() ?: return null
        if (v.signum() <= 0) return null
        return v.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    private val moneyShort = DecimalFormat("#,##0.##")

    /** Drops ".00" for compact labels: 1,250 instead of 1,250.00. */
    fun bahtShort(satang: Long): String = moneyShort.format(satang / 100.0)

    /** 12.5k style for chart axes. */
    fun bahtCompact(satang: Long): String {
        val baht = satang / 100.0
        return when {
            baht >= 1_000_000 -> DecimalFormat("#.#").format(baht / 1_000_000) + "M"
            baht >= 1_000 -> DecimalFormat("#.#").format(baht / 1_000) + "k"
            else -> DecimalFormat("#").format(baht)
        }
    }

    fun month(ym: YearMonth) = "${monthsLong[ym.monthValue - 1]} ${ym.year + 543}"

    fun monthShort(ym: YearMonth) = "${monthsShort[ym.monthValue - 1]} ${(ym.year + 543) % 100}"

    fun monthAbbr(ym: YearMonth) = monthsShort[ym.monthValue - 1]

    fun day(date: LocalDate) =
        "${weekdays[date.dayOfWeek.value - 1]} ${date.dayOfMonth} ${monthsShort[date.monthValue - 1]} ${date.year + 543}"

    fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun time(millis: Long): String {
        val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        return "%02d:%02d".format(t.hour, t.minute)
    }

    fun dateTime(millis: Long) = "${day(dateOf(millis))} ${time(millis)}"

    fun shortDate(date: LocalDate) = "${date.dayOfMonth} ${monthsShort[date.monthValue - 1]}"

    /** "Today", "Yesterday" or the weekday date, for list headers. */
    fun relativeDay(date: LocalDate): String = when (date) {
        LocalDate.now() -> "วันนี้"
        LocalDate.now().minusDays(1) -> "เมื่อวาน"
        else -> day(date)
    }

    /** "+12%" style change between two amounts, or null when there is nothing to compare with. */
    fun change(now: Long, before: Long): Int? = if (before <= 0) null else ((now - before) * 100 / before).toInt()

    fun year(ym: YearMonth) = "${ym.year + 543}"

    fun quarter(ym: YearMonth) = "ไตรมาส ${(ym.monthValue - 1) / 3 + 1}/${(ym.year + 543) % 100}"
}
