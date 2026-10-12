package com.tarit.slipledger.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

/** What a calendar day is for pay purposes. Weekends and special holidays each have their own rates. */
enum class DayKind { WEEKDAY, HOLIDAY, WEEKEND }

data class DayPlan(val kind: DayKind, val hours: Double)

data class PlanItem(val name: String, val amount: Double)

enum class TaxMode { MANUAL, AUTO }

/** Salary and OT rules for the payslip planner; amounts in baht. */
data class PlanSettings(
    val baseSalary: Double = 18000.0,
    val normalMeal: Double = 35.0,
    val holidayMeal: Double = 70.0,
    val bonusFixed: Double = 2245.0,
    val socialSecurity: Double = 875.0,
    val providentFund: Double = 2800.0,
    val taxMode: TaxMode = TaxMode.MANUAL,
    val taxManual: Double = 3.0,
    val incomeItems: List<PlanItem> = emptyList(),
    val deductItems: List<PlanItem> = emptyList(),
    /** Pay multipliers ("แรง") per hour of the hourly rate. */
    val mult15: Double = 1.5,
    /** Weekend: first 8 hours (extra on top of the monthly salary) and hours after 8. */
    val multHolNormal: Double = 1.0,
    val multHol3: Double = 3.0,
    /** Special holiday: first 8 hours and hours after 8. */
    val multSpecial: Double = 2.0,
    val multSpecialOt: Double = 3.0,
    /** Weekday OT runs from 17:10, so 4 h ends at 21:10. */
    val maxWeekdayOt: Double = 4.0,
    /** Hours after the first 8 on a day off, paid at [multHol3]. */
    val maxHol3: Double = 3.0,
    /** Off = normal meal allowance only on weekdays with OT. */
    val mealAllWeekdays: Boolean = true,
    val rateDays: Double = 30.0,
    val rateHours: Double = 8.0,
    /**
     * Day of the month OT is cut off for payroll, 1–28; 0 = end of month. With 20, October's payslip counts
     * OT, work on days off and meal allowances from 21 September to 20 October.
     */
    val otCutoffDay: Int = 0,
)

/** One day's pay: weekday OT (15), weekend first 8 h (N) and after (3), special holiday first 8 h (S) and after (S3). */
data class DayPay(
    val hours: Double,
    val h15: Double, val hN: Double, val h3: Double,
    val p15: Double, val pN: Double, val p3: Double,
    val mealN: Double, val mealH: Double,
    val hS: Double = 0.0, val hS3: Double = 0.0,
    val pS: Double = 0.0, val pS3: Double = 0.0,
)

data class PlanSummary(
    val hourly: Double,
    val base: Double,
    val ot15: Double, val holN: Double, val hol3: Double,
    val h15: Double, val hN: Double, val h3: Double,
    val mealDaysN: Int, val mealDaysH: Int,
    val mealN: Double, val mealH: Double,
    val bonus: Double, val extraIncome: Double,
    val gross: Double,
    val ss: Double, val pvd: Double, val tax: Double, val extraDeduct: Double,
    val deduct: Double,
    val net: Double,
    val holS: Double = 0.0, val holS3: Double = 0.0,
    val hS: Double = 0.0, val hS3: Double = 0.0,
) {
    val otHours get() = h15 + hN + h3 + hS + hS3
}

/** Payslip arithmetic of the salary & OT planner. */
object OtCalc {
    fun r2(v: Double) = Math.round(v * 100) / 100.0

    fun defaultKind(date: LocalDate) =
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) DayKind.WEEKEND else DayKind.WEEKDAY

    fun hourly(s: PlanSettings): Double {
        val d = s.rateDays * s.rateHours
        return if (d > 0) s.baseSalary / d else 0.0
    }

    /** The days whose OT is paid with [month]'s salary. */
    fun period(s: PlanSettings, month: YearMonth): List<LocalDate> {
        val cut = s.otCutoffDay
        val (start, end) = if (cut in 1..28) month.minusMonths(1).atDay(cut).plusDays(1) to month.atDay(cut)
        else month.atDay(1) to month.atEndOfMonth()
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    }

    fun maxHours(kind: DayKind, s: PlanSettings) = if (kind == DayKind.WEEKDAY) s.maxWeekdayOt else 8 + s.maxHol3

    fun day(kind: DayKind, hoursIn: Double, s: PlanSettings): DayPay {
        val hr = hourly(s)
        val hours = hoursIn.coerceIn(0.0, maxHours(kind, s))
        return if (kind == DayKind.WEEKDAY) {
            DayPay(
                hours, hours, 0.0, 0.0, hours * hr * s.mult15, 0.0, 0.0,
                mealN = if (s.mealAllWeekdays || hours > 0) s.normalMeal else 0.0, mealH = 0.0,
            )
        } else {
            val first = min(hours, 8.0)
            val after = max(0.0, hours - 8)
            val meal = if (hours > 0) s.holidayMeal else 0.0
            if (kind == DayKind.HOLIDAY) DayPay(
                hours, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, mealN = 0.0, mealH = meal,
                hS = first, hS3 = after, pS = first * hr * s.multSpecial, pS3 = after * hr * s.multSpecialOt,
            ) else DayPay(
                hours, 0.0, first, after, 0.0, first * hr * s.multHolNormal, after * hr * s.multHol3,
                mealN = 0.0, mealH = meal,
            )
        }
    }

    /** Withholding estimate: this month's income × 12 through Thai personal income tax bands. */
    fun autoTax(s: PlanSettings, grossMonthly: Double): Double {
        val annual = grossMonthly * 12
        val expense = min(annual * 0.5, 100_000.0)
        val ss = min(s.socialSecurity * 12, 9_000.0)
        val pvd = minOf(s.providentFund * 12, annual * 0.15, 500_000.0)
        val taxable = max(0.0, annual - expense - 60_000 - ss - pvd)
        val bands = listOf(
            150_000.0 to 0.0, 300_000.0 to .05, 500_000.0 to .10, 750_000.0 to .15,
            1_000_000.0 to .20, 2_000_000.0 to .25, 5_000_000.0 to .30, Double.POSITIVE_INFINITY to .35,
        )
        var tax = 0.0
        var prev = 0.0
        for ((top, rate) in bands) {
            if (taxable > prev) tax += (min(taxable, top) - prev) * rate
            prev = top
        }
        return tax / 12
    }

    fun summarize(s: PlanSettings, month: YearMonth, entryOf: (LocalDate) -> DayPlan): PlanSummary {
        var h15 = 0.0; var hN = 0.0; var h3 = 0.0
        var p15 = 0.0; var pN = 0.0; var p3 = 0.0
        var hS = 0.0; var hS3 = 0.0; var pS = 0.0; var pS3 = 0.0
        var nMealN = 0; var nMealH = 0
        for (date in period(s, month)) {
            val e = entryOf(date)
            val c = day(e.kind, e.hours, s)
            h15 += c.h15; hN += c.hN; h3 += c.h3
            p15 += c.p15; pN += c.pN; p3 += c.p3
            hS += c.hS; hS3 += c.hS3; pS += c.pS; pS3 += c.pS3
            if (c.mealN > 0) nMealN++
            if (c.mealH > 0) nMealH++
        }
        val base = r2(s.baseSalary)
        val ot15 = r2(p15); val holN = r2(pN); val hol3 = r2(p3)
        val mealN = r2(nMealN * s.normalMeal)
        val mealH = r2(nMealH * s.holidayMeal)
        val bonus = r2(s.bonusFixed)
        val extraIncome = r2(s.incomeItems.sumOf { it.amount.coerceAtLeast(0.0) })
        val holS = r2(pS); val holS3 = r2(pS3)
        val gross = r2(base + ot15 + holN + hol3 + holS + holS3 + mealN + mealH + bonus + extraIncome)
        val ss = r2(s.socialSecurity)
        val pvd = r2(s.providentFund)
        val tax = r2(if (s.taxMode == TaxMode.AUTO) autoTax(s, gross) else s.taxManual)
        val extraDeduct = r2(s.deductItems.sumOf { it.amount.coerceAtLeast(0.0) })
        val deduct = r2(ss + pvd + tax + extraDeduct)
        return PlanSummary(
            hourly(s), base, ot15, holN, hol3, h15, hN, h3, nMealN, nMealH, mealN, mealH, bonus, extraIncome,
            gross, ss, pvd, tax, extraDeduct, deduct, r2(gross - deduct),
            holS = holS, holS3 = holS3, hS = hS, hS3 = hS3,
        )
    }
}

/** The planner's settings and the days the user marked, kept apart from the ledger in their own preferences. */
class OtPlannerStore private constructor(context: Context) {
    private val sp = context.getSharedPreferences("ot_planner", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<PlanSettings> = _settings

    /** "yyyy-MM" → day of month → plan; only days that differ from the default are stored. */
    private val _months = MutableStateFlow(readMonths())
    val months: StateFlow<Map<String, Map<Int, DayPlan>>> = _months

    fun entry(date: LocalDate, months: Map<String, Map<Int, DayPlan>> = _months.value): DayPlan =
        months[key(YearMonth.from(date))]?.get(date.dayOfMonth) ?: DayPlan(OtCalc.defaultKind(date), 0.0)

    fun setEntry(date: LocalDate, kind: DayKind, hours: Double) {
        val k = key(YearMonth.from(date))
        val month = (_months.value[k] ?: emptyMap()).toMutableMap()
        if (kind == OtCalc.defaultKind(date) && hours <= 0) month.remove(date.dayOfMonth)
        else month[date.dayOfMonth] = DayPlan(kind, hours)
        _months.value = if (month.isEmpty()) _months.value - k else _months.value + (k to month)
        saveMonths()
    }

    /** Puts [dates] back to their default kind with no hours. */
    fun resetDates(dates: List<LocalDate>) {
        var all = _months.value
        dates.groupBy { YearMonth.from(it) }.forEach { (m, ds) ->
            val k = key(m)
            val month = (all[k] ?: emptyMap()) - ds.map { it.dayOfMonth }.toSet()
            all = if (month.isEmpty()) all - k else all + (k to month)
        }
        _months.value = all
        saveMonths()
    }

    fun update(transform: (PlanSettings) -> PlanSettings) {
        _settings.value = transform(_settings.value)
        saveSettings()
    }

    fun resetSettings() = update { PlanSettings() }

    private fun key(m: YearMonth) = m.toString()

    private fun saveMonths() {
        val o = JSONObject()
        _months.value.forEach { (k, days) ->
            val m = JSONObject()
            days.forEach { (d, e) -> m.put(d.toString(), JSONObject().put("type", e.kind.name).put("hours", e.hours)) }
            o.put(k, m)
        }
        sp.edit().putString("months", o.toString()).apply()
    }

    private fun readMonths(): Map<String, Map<Int, DayPlan>> = runCatching {
        val o = JSONObject(sp.getString("months", null) ?: return@runCatching emptyMap())
        o.keys().asSequence().associateWith { k ->
            val m = o.getJSONObject(k)
            m.keys().asSequence().mapNotNull { d ->
                runCatching {
                    val e = m.getJSONObject(d)
                    d.toInt() to DayPlan(DayKind.valueOf(e.getString("type")), e.getDouble("hours").coerceAtLeast(0.0))
                }.getOrNull()
            }.toMap()
        }
    }.getOrDefault(emptyMap())

    private fun saveSettings() {
        val s = _settings.value
        fun items(list: List<PlanItem>) = JSONArray(list.map { JSONObject().put("name", it.name).put("amount", it.amount) })
        val o = JSONObject()
            .put("baseSalary", s.baseSalary).put("normalMeal", s.normalMeal).put("holidayMeal", s.holidayMeal)
            .put("bonusFixed", s.bonusFixed).put("socialSecurity", s.socialSecurity).put("providentFund", s.providentFund)
            .put("taxMode", s.taxMode.name).put("taxManual", s.taxManual)
            .put("incomeItems", items(s.incomeItems)).put("deductItems", items(s.deductItems))
            .put("mult15", s.mult15).put("multHolNormal", s.multHolNormal).put("multHol3", s.multHol3)
            .put("multSpecial", s.multSpecial).put("multSpecialOt", s.multSpecialOt)
            .put("maxWeekdayOt", s.maxWeekdayOt).put("maxHol3", s.maxHol3).put("mealAllWeekdays", s.mealAllWeekdays)
            .put("rateDays", s.rateDays).put("rateHours", s.rateHours).put("otCutoffDay", s.otCutoffDay)
        sp.edit().putString("settings", o.toString()).apply()
    }

    private fun readSettings(): PlanSettings = runCatching {
        val o = JSONObject(sp.getString("settings", null) ?: return@runCatching PlanSettings())
        val d = PlanSettings()
        fun n(k: String, def: Double) = o.optDouble(k, def).takeIf { !it.isNaN() } ?: def
        fun items(k: String) = o.optJSONArray(k)?.let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let { PlanItem(it.optString("name"), it.optDouble("amount", 0.0)) } }
        } ?: emptyList()
        PlanSettings(
            baseSalary = n("baseSalary", d.baseSalary), normalMeal = n("normalMeal", d.normalMeal),
            holidayMeal = n("holidayMeal", d.holidayMeal), bonusFixed = n("bonusFixed", d.bonusFixed),
            socialSecurity = n("socialSecurity", d.socialSecurity), providentFund = n("providentFund", d.providentFund),
            taxMode = runCatching { TaxMode.valueOf(o.getString("taxMode")) }.getOrDefault(d.taxMode),
            taxManual = n("taxManual", d.taxManual),
            incomeItems = items("incomeItems"), deductItems = items("deductItems"),
            mult15 = n("mult15", d.mult15), multHolNormal = n("multHolNormal", d.multHolNormal), multHol3 = n("multHol3", d.multHol3),
            multSpecial = n("multSpecial", d.multSpecial), multSpecialOt = n("multSpecialOt", d.multSpecialOt),
            maxWeekdayOt = n("maxWeekdayOt", d.maxWeekdayOt), maxHol3 = n("maxHol3", d.maxHol3),
            mealAllWeekdays = o.optBoolean("mealAllWeekdays", d.mealAllWeekdays),
            rateDays = n("rateDays", d.rateDays), rateHours = n("rateHours", d.rateHours),
            otCutoffDay = o.optInt("otCutoffDay", d.otCutoffDay).coerceIn(0, 28),
        )
    }.getOrDefault(PlanSettings())

    companion object {
        @Volatile private var instance: OtPlannerStore? = null

        fun get(context: Context): OtPlannerStore = instance ?: synchronized(this) {
            instance ?: OtPlannerStore(context.applicationContext).also { instance = it }
        }
    }
}
