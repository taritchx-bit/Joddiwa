package com.tarit.slipledger.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class OtCalcTest {
    private val s = PlanSettings()

    @Test fun hourlyRate() = assertEquals(75.0, OtCalc.hourly(s), 1e-9)

    @Test fun weekdayOtAndMeal() {
        val d = OtCalc.day(DayKind.WEEKDAY, 4.0, s)
        assertEquals(4 * 18000.0 / 240 * 1.5, d.p15, 1e-9)
        assertEquals(35.0, d.mealN, 0.0)
    }

    @Test fun dayOffSplitsAtEightHoursAndCapsAtMax() {
        val d = OtCalc.day(DayKind.WEEKEND, 20.0, s)
        assertEquals(11.0, d.hours, 0.0)
        assertEquals(8.0, d.hN, 0.0)
        assertEquals(3.0, d.h3, 0.0)
        assertEquals(70.0, d.mealH, 0.0)
    }

    /** A special holiday pays 2× for the first 8 hours and 3× after, unlike a weekend's 1× and 3×. */
    @Test fun specialHolidayRates() {
        val hr = OtCalc.hourly(s)
        val hol = OtCalc.day(DayKind.HOLIDAY, 10.0, s)
        assertEquals(8 * hr * 2.0, hol.pS, 1e-9)
        assertEquals(2 * hr * 3.0, hol.pS3, 1e-9)
        assertEquals(0.0, hol.pN, 0.0)
        val wkd = OtCalc.day(DayKind.WEEKEND, 10.0, s)
        assertEquals(8 * hr * 1.0, wkd.pN, 1e-9)
        assertEquals(0.0, wkd.pS, 0.0)
        // Custom rates are honoured.
        assertEquals(8 * hr * 2.5, OtCalc.day(DayKind.HOLIDAY, 8.0, s.copy(multSpecial = 2.5)).pS, 1e-9)
    }

    /** October 2026 has 22 weekdays; with nothing marked the net is salary + meals + bonus − deductions. */
    @Test fun emptyMonthSummary() {
        val m = YearMonth.of(2026, 10)
        val sum = OtCalc.summarize(s, m) { DayPlan(OtCalc.defaultKind(it), 0.0) }
        assertEquals(22, sum.mealDaysN)
        assertEquals(18000 + 22 * 35 + 2245.0, sum.gross, 1e-9)
        assertEquals(875 + 2800 + 3.0, sum.deduct, 1e-9)
        assertEquals(sum.gross - sum.deduct, sum.net, 1e-9)
    }

    @Test fun cutoffPeriodSpansTwoMonths() {
        val p = OtCalc.period(s.copy(otCutoffDay = 20), YearMonth.of(2026, 10))
        assertEquals(java.time.LocalDate.of(2026, 9, 21), p.first())
        assertEquals(java.time.LocalDate.of(2026, 10, 20), p.last())
        assertEquals(30, p.size)
    }

    /** OT marked on 25 September is paid with October's salary when OT is cut off on the 20th. */
    @Test fun cutoffMovesOtIntoNextPayslip() {
        val ot = java.time.LocalDate.of(2026, 9, 25)
        val entry = { d: java.time.LocalDate -> DayPlan(OtCalc.defaultKind(d), if (d == ot) 4.0 else 0.0) }
        val withCutoff = s.copy(otCutoffDay = 20)
        assertEquals(4.0, OtCalc.summarize(withCutoff, YearMonth.of(2026, 10), entry).h15, 0.0)
        assertEquals(0.0, OtCalc.summarize(withCutoff, YearMonth.of(2026, 9), entry).h15, 0.0)
        assertEquals(0.0, OtCalc.summarize(s, YearMonth.of(2026, 10), entry).h15, 0.0)
    }

    @Test fun autoTaxBands() {
        // 40,000/month: 480,000 − 100,000 − 60,000 − 9,000 − 33,600 = 277,400 taxable → (277,400 − 150,000) × 5%.
        val tax = OtCalc.autoTax(s, 40_000.0)
        assertEquals((277_400 - 150_000) * 0.05 / 12, tax, 1e-6)
    }
}
