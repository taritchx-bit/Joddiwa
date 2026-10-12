package com.tarit.slipledger.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Budget months that may start on any day 1-28 (e.g. payday). With start day 25, the period
 * labelled October runs 25 Sep – 24 Oct: it is named after the month it mostly covers and ends in.
 */
data class Period(val startDay: Int) {
    fun startDate(ym: YearMonth): LocalDate =
        if (startDay <= 1) ym.atDay(1) else ym.minusMonths(1).atDay(startDay)

    /** Exclusive end. */
    fun endDate(ym: YearMonth): LocalDate = startDate(ym.plusMonths(1))

    fun contains(ym: YearMonth, date: LocalDate) = !date.isBefore(startDate(ym)) && date.isBefore(endDate(ym))

    /** The period [date] falls into. */
    fun of(date: LocalDate): YearMonth {
        val ym = YearMonth.from(date)
        return if (startDay > 1 && date.dayOfMonth >= startDay) ym.plusMonths(1) else ym
    }

    fun current(): YearMonth = of(LocalDate.now())

    fun startMillis(ym: YearMonth, zone: ZoneId = ZoneId.systemDefault()) =
        startDate(ym).atStartOfDay(zone).toInstant().toEpochMilli()

    fun endMillis(ym: YearMonth, zone: ZoneId = ZoneId.systemDefault()) =
        endDate(ym).atStartOfDay(zone).toInstant().toEpochMilli()

    fun lengthInDays(ym: YearMonth) = (endDate(ym).toEpochDay() - startDate(ym).toEpochDay()).toInt()
}
