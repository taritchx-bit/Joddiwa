package com.tarit.slipledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodTest {
    private val oct = YearMonth.of(2026, 10)

    @Test fun calendarMonth() {
        val p = Period(1)
        assertEquals(LocalDate.of(2026, 10, 1), p.startDate(oct))
        assertEquals(LocalDate.of(2026, 11, 1), p.endDate(oct))
        assertEquals(31, p.lengthInDays(oct))
        assertEquals(oct, p.of(LocalDate.of(2026, 10, 31)))
    }

    @Test fun paydayPeriod() {
        val p = Period(25)
        assertEquals(LocalDate.of(2026, 9, 25), p.startDate(oct))
        assertEquals(LocalDate.of(2026, 10, 25), p.endDate(oct))
        assertEquals(oct, p.of(LocalDate.of(2026, 9, 25)))
        assertEquals(oct, p.of(LocalDate.of(2026, 10, 24)))
        assertEquals(YearMonth.of(2026, 11), p.of(LocalDate.of(2026, 10, 25)))
        assertTrue(p.contains(oct, LocalDate.of(2026, 10, 24)))
        assertFalse(p.contains(oct, LocalDate.of(2026, 10, 25)))
    }

    @Test fun yearBoundary() {
        val p = Period(25)
        val jan = YearMonth.of(2027, 1)
        assertEquals(LocalDate.of(2026, 12, 25), p.startDate(jan))
        assertEquals(jan, p.of(LocalDate.of(2026, 12, 31)))
    }

    @Test fun tags() {
        assertEquals(listOf("ทริป", "งาน"), Tags.split(" ทริป, งาน ,,"))
        assertEquals("ทริป,งาน", Tags.join(listOf("ทริป", "งาน", "ทริป", " ")))
    }
}
