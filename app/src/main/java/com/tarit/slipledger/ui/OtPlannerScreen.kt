package com.tarit.slipledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.DayKind
import com.tarit.slipledger.data.OtCalc
import com.tarit.slipledger.data.OtPlannerStore
import com.tarit.slipledger.data.PlanItem
import com.tarit.slipledger.data.PlanSettings
import com.tarit.slipledger.data.PlanSummary
import com.tarit.slipledger.data.TaxMode
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// ---------------- formatting ----------------

private fun baht(v: Double) = Fmt.baht((v * 100).roundToLong())
private fun hrs(h: Double): String = if (h % 1.0 == 0.0) h.toLong().toString() else "%.2f".format(h).trimEnd('0').trimEnd('.')
private fun x1(v: Double) = "%.1f".format(v)
private val thaiLocale = Locale("th", "TH")
private fun dowName(d: LocalDate) = d.dayOfWeek.getDisplayName(JTextStyle.FULL, thaiLocale)

/** OT start is 17:10; the end time of [hours] of OT. */
private fun endTime(hours: Double): String {
    val mins = 17 * 60 + 10 + Math.round(hours * 60).toInt()
    return "%02d:%02d".format((mins / 60) % 24, mins % 60)
}

private fun kindName(k: DayKind) = when (k) {
    DayKind.WEEKDAY -> "วันทำงานปกติ"
    DayKind.HOLIDAY -> "วันหยุดพิเศษ"
    DayKind.WEEKEND -> "เสาร์–อาทิตย์"
}

/** "21–30 ก.ย., 1–5 ต.ค." for a sorted list of dates. */
private fun ranges(days: List<LocalDate>): String {
    val runs = mutableListOf<Pair<LocalDate, LocalDate>>()
    for (d in days) {
        val last = runs.lastOrNull()
        if (last != null && last.second.plusDays(1) == d) runs[runs.lastIndex] = last.first to d else runs += d to d
    }
    return runs.joinToString(", ") { (a, b) ->
        when {
            a == b -> Fmt.shortDate(a)
            a.month == b.month -> "${a.dayOfMonth}–${Fmt.shortDate(b)}"
            else -> "${Fmt.shortDate(a)} – ${Fmt.shortDate(b)}"
        }
    }
}
// ---------------- colours ----------------

private class PlanColors(val ot: Color, val otBg: Color, val x1: Color, val x1Bg: Color, val x3: Color, val x3Bg: Color, val hol: Color, val holBg: Color)

@Composable
private fun planColors(): PlanColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) PlanColors(
        Color(0xFFFFC85C), Color(0xFF3A2A08), Color(0xFF6FE0D6), Color(0xFF0F3434),
        Color(0xFFFF9A92), Color(0xFF40161A), Color(0xFFC3A8FF), Color(0xFF2B2147),
    ) else PlanColors(
        Color(0xFF9A5A00), Color(0xFFFDEBC6), Color(0xFF07635F), Color(0xFFD3F0EC),
        Color(0xFFA8221A), Color(0xFFFDDAD6), Color(0xFF6B4AA8), Color(0xFFE8E0F7),
    )

/** Badge shown in a calendar cell. */
private class DayLook(val text: String, val fg: Color, val bg: Color, val hours: Double, val tone: Color?)

@Composable
private fun dayLook(kind: DayKind, hoursIn: Double, s: PlanSettings, c: PlanColors): DayLook {
    val hours = hoursIn.coerceIn(0.0, OtCalc.maxHours(kind, s))
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        kind == DayKind.WEEKDAY && hours > 0 -> DayLook("OT ${x1(s.mult15)}x", c.ot, c.otBg, hours, c.ot)
        kind == DayKind.WEEKDAY -> DayLook("ปกติ", muted, Color.Transparent, 0.0, null)
        hours <= 0 && kind == DayKind.HOLIDAY -> DayLook("หยุดพิเศษ", c.hol, c.holBg, 0.0, null)
        kind == DayKind.HOLIDAY && hours > 8 -> DayLook("${x1(s.multSpecialOt)}x", c.x3, c.x3Bg, hours, c.x3)
        kind == DayKind.HOLIDAY -> DayLook("${x1(s.multSpecial)}x", c.hol, c.holBg, hours, c.hol)
        hours <= 0 -> DayLook("หยุด", muted, Color.Transparent, 0.0, null)
        hours > 8 -> DayLook("${x1(s.multHol3)}x", c.x3, c.x3Bg, hours, c.x3)
        else -> DayLook("${x1(s.multHolNormal)}x", c.x1, c.x1Bg, hours, c.x1)
    }
}

/** Day editor draft: one day, or several picked in multi-select. */
private data class Draft(val days: List<LocalDate>, val bulk: Boolean, val kind: DayKind, val hours: Double)

// ---------------- screen ----------------

/**
 * Salary & OT planner: mark OT and working days off on a calendar and see the estimated payslip for the
 * month. Independent of the ledger; settings and marked days live in [OtPlannerStore]. Shown as a tab
 * (no [onBack]) or as a pushed screen.
 */
@Composable
fun OtPlannerScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val store = remember { OtPlannerStore.get(context) }
    val s by store.settings.collectAsStateWithLifecycle()
    val months by store.months.collectAsStateWithLifecycle()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthKey)
    var direction by remember { mutableStateOf(1) }
    fun shift(by: Long) { direction = if (by > 0) 1 else -1; monthKey = month.plusMonths(by).toString() }

    var selectMode by remember { mutableStateOf(false) }
    var selected by remember(monthKey) { mutableStateOf(emptySet<LocalDate>()) }
    var draft by remember { mutableStateOf<Draft?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val entryOf = { d: LocalDate -> store.entry(d, months) }
    val sum = OtCalc.summarize(s, month, entryOf)
    val period = OtCalc.period(s, month)
    val colors = planColors()
    // Room for the floating tab bar (or just the gesture bar when pushed as a sub-screen).
    val barSpace = LocalBarSpace.current

    fun openDay(date: LocalDate) {
        val e = entryOf(date)
        draft = Draft(listOf(date), bulk = false, kind = e.kind, hours = e.hours)
    }

    fun openBulk() {
        val days = selected.sorted()
        if (days.isEmpty()) return
        val entries = days.map { entryOf(it) }
        val defaults = days.map { OtCalc.defaultKind(it) }.toSet()
        val same = entries.all { it.kind == entries[0].kind && abs(it.hours - entries[0].hours) < 1e-9 }
        draft = if (same) Draft(days, true, entries[0].kind, entries[0].hours)
        else Draft(days, true, if (defaults.size == 1) defaults.first() else DayKind.WEEKDAY, 0.0)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val actions: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIcon(Icons.Default.DeleteSweep, "ล้าง OT รอบนี้") { confirmClear = true }
                    RoundIcon(Icons.Default.Settings, "ตั้งค่า") { showSettings = true }
                }
            }
            if (onBack != null) SubHeader("วางแผน OT", onBack, actions)
            else ScreenHeader("คำนวณ OT", subtitle = "วางแผนเงินเดือน", trailing = actions)
            Column(
                Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = barSpace + if (selectMode) 110.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MonthBar(month, sum.hourly, period.takeIf { s.otCutoffDay in 1..28 }, direction, onShift = ::shift,
                    onToday = { monthKey = YearMonth.now().toString() })
                Box(Modifier.appear(0)) { NetCard(month, sum) }
                Box(Modifier.appear(1)) { Chips(sum, s, colors) }
                Box(Modifier.appear(2)) {
                    CalendarCard(
                        month, period, s, colors, entryOf, selectMode, selected,
                        onSelectMode = { on -> selectMode = on; if (!on) selected = emptySet() },
                        onSelected = { selected = it },
                        onOpenDay = ::openDay,
                        onSwipe = ::shift,
                    )
                }
                Box(Modifier.appear(3)) { SlipCard(sum, s) }
                Text(
                    "ตัวเลขเป็นการประมาณการเพื่อวางแผน อาจต่างจากสลิปจริงของบริษัท",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Floating bar while picking several days.
        AnimatedVisibility(
            selectMode,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = barSpace + 12.dp).padding(horizontal = 12.dp),
            enter = slideInVertically { it * 2 } + fadeIn(),
            exit = slideOutVertically { it * 2 } + fadeOut(),
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 12.dp) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("เลือกแล้ว ${selected.size} วัน", style = MaterialTheme.typography.titleSmall)
                        Text("แตะ หรือกดค้างแล้วลาก", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = ::openBulk, enabled = selected.isNotEmpty()) {
                        Text(if (selected.isEmpty()) "ตั้งค่า" else "ตั้งค่า ${selected.size} วัน")
                    }
                }
            }
        }
    }

    draft?.let { d ->
        DaySheet(
            d, s, colors,
            onChange = { draft = it },
            onDismiss = { draft = null },
            onSave = { days, kind, hours ->
                days.forEach { store.setEntry(it, kind, hours) }
                if (d.bulk) selected = emptySet()
                draft = null
            },
            onReset = { days ->
                store.resetDates(days)
                if (d.bulk) selected = emptySet()
                draft = null
            },
        )
    }
    if (confirmClear) {
        val marked = period.count { store.entry(it, months).let { e -> e.hours > 0 || e.kind != OtCalc.defaultKind(it) } }
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Default.DeleteSweep, null, tint = ExpenseColor) },
            title = { Text("ล้าง OT รอบนี้?") },
            text = {
                Text(
                    "วันที่ ${Fmt.shortDate(period.first())} – ${Fmt.shortDate(period.last())} " +
                        (if (marked > 0) "ที่ตั้งไว้ $marked วัน " else "") +
                        "จะกลับเป็นวันทำงานปกติ/วันหยุดตามปฏิทิน การตั้งค่าเงินเดือนไม่เปลี่ยน",
                )
            },
            confirmButton = {
                TextButton(onClick = { store.resetDates(period); selected = emptySet(); confirmClear = false }) {
                    Text("ล้าง", color = ExpenseColor)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("ยกเลิก") } },
        )
    }
    if (showSettings) {
        PlannerSettingsSheet(
            s, sum, store,
            onClearMonth = { store.resetDates(period) },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun MonthBar(month: YearMonth, hourly: Double, cutoffPeriod: List<LocalDate>?, direction: Int, onShift: (Long) -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "เดือนก่อนหน้า") { onShift(-1) }
        AnimatedContent(
            month,
            transitionSpec = {
                (slideInHorizontally { it / 2 * direction } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 * direction } + fadeOut())
            },
            label = "month",
            modifier = Modifier.weight(1f),
        ) { m ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToday).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Fmt.month(m), style = MaterialTheme.typography.titleMedium)
                if (cutoffPeriod != null) {
                    Text("รอบ OT ${Fmt.shortDate(cutoffPeriod.first())} – ${Fmt.shortDate(cutoffPeriod.last())}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text("ค่าแรงต่อชั่วโมง ฿${baht(hourly)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RoundIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "เดือนถัดไป") { onShift(1) }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(44.dp).pressScale(interaction, 0.85f).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetCard(month: YearMonth, sum: PlanSummary) {
    InkPanel {
        Column(Modifier.padding(20.dp)) {
            Text("ยอดสุทธิโดยประมาณ · ${Fmt.month(month)}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
            val net = animatedSatang((sum.net * 100).roundToLong())
            Text(money(net), color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NetFigure("รวมเงินได้", "฿${baht(sum.gross)}")
                NetFigure("รายการหัก", "฿${baht(sum.deduct)}")
                NetFigure("ชั่วโมง OT/ทำงานวันหยุด", hrs(sum.otHours))
            }
        }
    }
}

@Composable
private fun NetFigure(label: String, value: String) {
    Row {
        Text("$label ", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chips(sum: PlanSummary, s: PlanSettings, c: PlanColors) {
    val chips = listOf(
        Triple("เงินเดือน", "", sum.base) to null,
        Triple("ค่าอาหาร", "${sum.mealDaysN + sum.mealDaysH} วัน", sum.mealN + sum.mealH) to null,
        Triple("เบี้ยขยัน/อื่น ๆ", "", sum.bonus + sum.extraIncome) to null,
        Triple("OT ${x1(s.mult15)}x", "${hrs(sum.h15)} ชม.", sum.ot15) to c.ot,
        Triple("ส–อา ${x1(s.multHolNormal)}x", "${hrs(sum.hN)} ชม.", sum.holN) to c.x1,
        Triple("ส–อา ${x1(s.multHol3)}x", "${hrs(sum.h3)} ชม.", sum.hol3) to c.x3,
        Triple("หยุดพิเศษ ${x1(s.multSpecial)}x", "${hrs(sum.hS)} ชม.", sum.holS) to c.hol,
        Triple("หยุดพิเศษ ${x1(s.multSpecialOt)}x", "${hrs(sum.hS3)} ชม.", sum.holS3) to c.x3,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (t, accent) -> Chip(t.first, t.second, t.third, accent, Modifier.weight(1f)) }
                // Keep chips the same width when the last row is short.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("รายการหักทั้งหมด", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ประกันสังคม · กองทุน · ภาษี · อื่น ๆ", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Text("−${baht(sum.deduct)}", style = MaterialTheme.typography.titleMedium, color = ExpenseColor)
            }
        }
    }
}

@Composable
private fun Chip(label: String, sub: String, value: Double, accent: Color?, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier.drawBehind { if (accent != null) drawRect(accent, size = size.copy(height = 3.dp.toPx())) }
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.height(16.dp))
            Text(baht(value), style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

// ---------------- calendar ----------------

private val dowLabels = listOf("จ", "อ", "พ", "พฤ", "ศ", "ส", "อา")

@Composable
private fun CalendarCard(
    month: YearMonth,
    period: List<LocalDate>,
    s: PlanSettings,
    c: PlanColors,
    entryOf: (LocalDate) -> com.tarit.slipledger.data.DayPlan,
    selectMode: Boolean,
    selected: Set<LocalDate>,
    onSelectMode: (Boolean) -> Unit,
    onSelected: (Set<LocalDate>) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSwipe: (Long) -> Unit,
) {
    // The grid shows the OT period, which may start in the previous month when OT is cut off early.
    val start = period.first()
    val first = start.dayOfWeek.value - 1 // Monday-first column of the first day
    val days = period.size
    fun col(d: LocalDate) = d.dayOfWeek.value - 1
    val haptics = LocalHapticFeedback.current
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    val today = LocalDate.now()

    SectionCard(padding = false) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!selectMode) {
                    Text("แตะวันที่เพื่อตั้งค่า\nกดค้างแล้วลากเพื่อเลือกหลายวัน", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 4.dp))
                } else {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuickChip("จ–ศ") { onSelected(selected + period.filter { col(it) < 5 }) }
                        QuickChip("ส–อา") { onSelected(selected + period.filter { col(it) >= 5 }) }
                        QuickChip("ทั้งรอบ") { onSelected(period.toSet()) }
                        QuickChip("ล้าง") { onSelected(emptySet()) }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (selectMode) LimePill("เสร็จ", Icons.Default.Check) { onSelectMode(false) }
                else OutlinePill("เลือกหลายวัน") { onSelectMode(true) }
            }
            // Day-of-week headers: tapping one picks (or un-picks) that whole column.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dowLabels.forEachIndexed { i, label ->
                    Text(
                        label, textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = if (i >= 5) c.x3 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selectMode) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
                            .clickable {
                                if (!selectMode) onSelectMode(true)
                                val colDays = period.filter { col(it) == i }
                                onSelected(if (colDays.all { it in selected }) selected - colDays.toSet() else selected + colDays)
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            val rows = (first + days + 6) / 7
            val selectModeNow by rememberUpdatedState(selectMode)
            val selectedNow by rememberUpdatedState(selected)
            val onSelectedNow by rememberUpdatedState(onSelected)
            val onSelectModeNow by rememberUpdatedState(onSelectMode)
            val onOpenDayNow by rememberUpdatedState(onOpenDay)
            val onSwipeNow by rememberUpdatedState(onSwipe)
            fun dayAt(p: Offset): LocalDate? {
                if (gridSize.width == 0) return null
                val cw = gridSize.width / 7f
                val rh = gridSize.height / rows.toFloat()
                val cIdx = (p.x / cw).toInt().coerceIn(0, 6)
                val rIdx = (p.y / rh).toInt()
                if (p.y < 0 || rIdx >= rows) return null
                val i = rIdx * 7 + cIdx - first
                return if (i in 0 until days) start.plusDays(i.toLong()) else null
            }

            Column(
                Modifier.onSizeChanged { gridSize = it }
                    // Keyed on the month only: switching select mode or picking days mid-gesture must not restart it.
                    .pointerInput(start, rows) {
                        // Tap opens a day (or toggles it when picking); long-press then drag picks a range;
                        // a horizontal swipe changes month.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startDay = dayAt(down.position)
                            val slop = 10.dp.toPx()
                            // Null when the finger stayed still for the long-press time.
                            val early = withTimeoutOrNull(320) {
                                var ch: PointerInputChange
                                do ch = awaitPointerEvent().changes.first()
                                while (ch.pressed && (ch.position - down.position).getDistance() <= slop)
                                ch
                            }
                            if (early == null) {
                                val anchor: LocalDate = startDay ?: return@awaitEachGesture
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!selectModeNow) onSelectModeNow(true)
                                val base = if (selectModeNow) selectedNow else emptySet()
                                val add = anchor !in base
                                var cur: LocalDate = anchor
                                fun apply() {
                                    val lo = minOf(anchor, cur)
                                    val hi = maxOf(anchor, cur)
                                    val r = period.filter { !it.isBefore(lo) && !it.isAfter(hi) }
                                    onSelectedNow(if (add) base + r else base - r.toSet())
                                }
                                apply()
                                while (true) {
                                    val ch = awaitPointerEvent().changes.first()
                                    if (!ch.pressed) break
                                    ch.consume()
                                    val d = dayAt(ch.position)
                                    if (d != null && d != cur) { cur = d; apply() }
                                }
                                return@awaitEachGesture
                            }
                            if (!early.pressed) {
                                startDay?.let { d ->
                                    if (selectModeNow) onSelectedNow(if (d in selectedNow) selectedNow - d else selectedNow + d) else onOpenDayNow(d)
                                }
                                return@awaitEachGesture
                            }
                            var last: PointerInputChange = early
                            while (last.pressed) last = awaitPointerEvent().changes.first()
                            val delta = last.position - down.position
                            if (!selectModeNow && abs(delta.x) > 70.dp.toPx() && abs(delta.y) < 40.dp.toPx()) onSwipeNow(if (delta.x < 0) 1 else -1)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (r in 0 until rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (cIdx in 0 until 7) {
                            val i = r * 7 + cIdx - first
                            if (i !in 0 until days) Spacer(Modifier.weight(1f).aspectRatio(1f / 1.38f))
                            else {
                                val date = start.plusDays(i.toLong())
                                val e = entryOf(date)
                                DayCell(
                                    // Month labels only matter when the OT period crosses into another month.
                                    date, showMonth = s.otCutoffDay in 1..28 && (date.dayOfMonth == 1 || i == 0),
                                    e.kind, dayLook(e.kind, e.hours, s, c), c,
                                    isToday = date == today, picked = selectMode && date in selected,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(Modifier.padding(top = 10.dp, start = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Legend(c.otBg, c.ot, "OT ${x1(s.mult15)}x")
                Legend(c.x1Bg, c.x1, "ส–อา ${x1(s.multHolNormal)}x")
                Legend(c.holBg, c.hol, "★ หยุดพิเศษ ${x1(s.multSpecial)}x")
                Legend(c.x3Bg, c.x3, "เกิน 8 ชม. ${x1(s.multHol3)}x")
                Legend(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.outline, "เสาร์–อาทิตย์")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, showMonth: Boolean, kind: DayKind, look: DayLook, c: PlanColors, isToday: Boolean, picked: Boolean, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        when {
            picked -> primary.copy(alpha = 0.16f)
            kind != DayKind.WEEKDAY -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        }, label = "cell",
    )
    val shape = RoundedCornerShape(11.dp)
    Box(modifier.aspectRatio(1f / 1.38f)) {
        Column(
            Modifier.fillMaxSize()
                .then(if (isToday) Modifier.border(2.dp, primary, shape) else Modifier)
                .padding(if (isToday) 2.dp else 0.dp)
                .clip(shape).background(bg)
                .border(if (picked) 2.dp else 1.dp, if (picked) primary else look.tone ?: MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(vertical = 4.dp, horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (kind == DayKind.HOLIDAY) Text("★", fontSize = 8.sp, color = c.hol)
                if (showMonth) Text(Fmt.monthAbbr(YearMonth.from(date)) + " ", fontSize = 8.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("${date.dayOfMonth}", fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (kind != DayKind.WEEKDAY) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            }
            Text(
                look.text, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold, color = look.fg, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(look.bg).padding(horizontal = 3.dp, vertical = 2.dp),
            )
            Text(if (look.hours > 0) "${hrs(look.hours)} ชม." else "", fontSize = 9.sp, lineHeight = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1)
        }
        if (picked) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-5).dp).size(18.dp).clip(CircleShape).background(primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp)) }
        }
    }
}

@Composable
private fun Legend(fill: Color, edge: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(fill).border(1.dp, edge, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.pressScale(interaction, 0.9f).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interaction, indication = null, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun OutlinePill(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        label, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.pressScale(interaction, 0.9f).clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(interaction, indication = null, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

// ---------------- payslip ----------------

@Composable
private fun SlipCard(sum: PlanSummary, s: PlanSettings) {
    var open by rememberSaveable { mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, label = "chev")
    SectionCard(padding = false) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("รายละเอียดสลิปเดือนนี้", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp)) {
                SlipHeading("เงินได้")
                SlipRow("เงินเดือน", sum.base)
                SlipRow("OT ${x1(s.mult15)}x", sum.ot15, "${hrs(sum.h15)} ชม.")
                SlipRow("เสาร์–อาทิตย์ ${x1(s.multHolNormal)}x", sum.holN, "${hrs(sum.hN)} ชม.")
                SlipRow("OT เสาร์–อาทิตย์ ${x1(s.multHol3)}x", sum.hol3, "${hrs(sum.h3)} ชม.")
                SlipRow("หยุดพิเศษ ${x1(s.multSpecial)}x", sum.holS, "${hrs(sum.hS)} ชม.")
                SlipRow("OT หยุดพิเศษ ${x1(s.multSpecialOt)}x", sum.holS3, "${hrs(sum.hS3)} ชม.")
                SlipRow("ค่าอาหารปกติ", sum.mealN, "${sum.mealDaysN} วัน × ${baht(s.normalMeal)}")
                SlipRow("ค่าอาหารวันหยุด", sum.mealH, "${sum.mealDaysH} วัน × ${baht(s.holidayMeal)}")
                SlipRow("เบี้ยขยัน/ค่าตำแหน่ง", sum.bonus)
                s.incomeItems.filter { it.amount > 0 }.forEach { SlipRow(it.name.ifBlank { "รายได้อื่น ๆ" }, it.amount) }
                SlipTotal("รวมเงินได้", baht(sum.gross))
                SlipHeading("รายการหัก")
                SlipRow("ประกันสังคม", sum.ss, negative = true)
                SlipRow("กองทุนสำรองเลี้ยงชีพ", sum.pvd, negative = true)
                SlipRow("ภาษีหัก ณ ที่จ่าย", sum.tax, if (s.taxMode == TaxMode.AUTO) "ประมาณการ" else null, negative = true)
                s.deductItems.filter { it.amount > 0 }.forEach { SlipRow(it.name.ifBlank { "รายการหักอื่น ๆ" }, it.amount, negative = true) }
                SlipTotal("รวมรายการหัก", "−${baht(sum.deduct)}", ExpenseColor)
                Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text("ยอดสุทธิ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(baht(sum.net), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SlipHeading(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))

@Composable
private fun SlipRow(label: String, value: Double, note: String? = null, negative: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().drawBehind {
            drawLine(Color.Gray.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }.padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (note != null) Text("  $note", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), modifier = Modifier.align(Alignment.CenterVertically))
        Spacer(Modifier.weight(1f))
        Text((if (negative) "−" else "") + baht(value), style = MaterialTheme.typography.bodyMedium,
            color = if (negative) ExpenseColor else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SlipTotal(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

// ---------------- day sheet ----------------

private class HourOption(val hours: Double, val title: String, val sub: String)

private fun optionsFor(kind: DayKind, s: PlanSettings): List<HourOption> {
    val m1 = if (kind == DayKind.HOLIDAY) s.multSpecial else s.multHolNormal
    val m3 = if (kind == DayKind.HOLIDAY) s.multSpecialOt else s.multHol3
    val list = if (kind == DayKind.WEEKDAY) listOf(
        HourOption(0.0, "ไม่ทำ OT", "เลิกงาน 16:40"),
        HourOption(2.0, "17:10 – 19:10", "2 ชม. · ${x1(s.mult15)}x"),
        HourOption(s.maxWeekdayOt, "ถึง ${endTime(s.maxWeekdayOt)}", "${hrs(s.maxWeekdayOt)} ชม. · ${x1(s.mult15)}x"),
    ) else listOf(
        HourOption(0.0, "ไม่ทำงาน", "วันหยุด"),
        HourOption(8.0, "8 ชม.", "8 ชม. × ${x1(m1)}x"),
        HourOption(10.0, "10 ชม.", "8 ชม. × ${x1(m1)}x + 2 ชม. × ${x1(m3)}x"),
        HourOption(8 + s.maxHol3, "ถึง 21:00", "8 ชม. × ${x1(m1)}x + ${hrs(s.maxHol3)} ชม. × ${x1(m3)}x"),
    )
    val max = OtCalc.maxHours(kind, s)
    return list.filter { it.hours <= max + 1e-9 }.distinctBy { "%.2f".format(it.hours) }
}

/** Weekday settings apply to Mon–Fri only, weekend to Sat–Sun only; a special holiday can go on any day. */
private fun bulkTargets(days: List<LocalDate>, kind: DayKind) =
    days.filter { kind == DayKind.HOLIDAY || OtCalc.defaultKind(it) == kind }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    d: Draft,
    s: PlanSettings,
    c: PlanColors,
    onChange: (Draft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (List<LocalDate>, DayKind, Double) -> Unit,
    onReset: (List<LocalDate>) -> Unit,
) {
    val targets = if (d.bulk) bulkTargets(d.days, d.kind) else d.days
    val n = targets.size
    val hours = d.hours.coerceIn(0.0, OtCalc.maxHours(d.kind, s))
    val pay = OtCalc.day(d.kind, hours, s)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (d.bulk) {
                val skipped = d.days.size - n
                Text("ตั้งค่า ${d.days.size} วันที่เลือก", style = MaterialTheme.typography.titleLarge)
                Text(
                    "วันที่ ${ranges(d.days)}\n" +
                        (if (n > 0) "ประเภทนี้ใช้กับ $n วัน" else "ไม่มีวันที่ตรงกับประเภทนี้ เลือกประเภทอื่น") +
                        (if (skipped > 0) " · ข้าม $skipped วัน (${if (d.kind == DayKind.WEEKDAY) "เสาร์–อาทิตย์" else "จันทร์–ศุกร์"})" else ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val date = d.days.first()
                Text("${dowName(date)}ที่ ${date.dayOfMonth} ${Fmt.month(YearMonth.from(date))}", style = MaterialTheme.typography.titleLarge)
                Text("ตามปกติวันนี้คือ${kindName(OtCalc.defaultKind(date))} · เลือกประเภทวันและช่วงเวลาทำงาน",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val kinds = listOf(DayKind.WEEKDAY to "วันทำงาน", DayKind.HOLIDAY to "หยุดพิเศษ", DayKind.WEEKEND to "เสาร์–อาทิตย์")
            PillTabs(
                kinds.map { it.second }, kinds.indexOfFirst { it.first == d.kind },
                onSelect = { i -> val k = kinds[i].first; if (k != d.kind) onChange(d.copy(kind = k, hours = 0.0)) },
                track = MaterialTheme.colorScheme.surfaceContainer,
            )

            optionsFor(d.kind, s).forEach { o ->
                val on = abs(o.hours - hours) < 0.005
                val interaction = remember { MutableInteractionSource() }
                Row(
                    Modifier.fillMaxWidth().pressScale(interaction, 0.97f).clip(RoundedCornerShape(16.dp))
                        .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.5.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable(interaction, indication = null) { onChange(d.copy(hours = o.hours)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(o.title, style = MaterialTheme.typography.titleSmall)
                        Text(o.sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(2.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                }
            }

            // Fine-tune in half hours.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (d.kind == DayKind.WEEKDAY) "ชั่วโมง OT" else "ชั่วโมงทำงาน", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (d.kind == DayKind.WEEKDAY) (if (hours > 0) "17:10 – ${endTime(hours)}" else "ไม่ทำ OT")
                        else "สูงสุด ${hrs(OtCalc.maxHours(d.kind, s))} ชม. (ถึง 21:00)",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StepButton(Icons.Default.Remove, "ลด 30 นาที") {
                    onChange(d.copy(hours = ((hours - 0.5) * 100).roundToLong() / 100.0).let { it.copy(hours = it.hours.coerceAtLeast(0.0)) })
                }
                AnimatedContent(hrs(hours), label = "hours", modifier = Modifier.width(62.dp)) {
                    Text(it, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                StepButton(Icons.Default.Add, "เพิ่ม 30 นาที") {
                    onChange(d.copy(hours = (hours + 0.5).coerceAtMost(OtCalc.maxHours(d.kind, s))))
                }
            }

            // What this choice adds to the month.
            val times = if (d.bulk) " × $n วัน" else ""
            val mult = if (d.bulk) n.toDouble() else 1.0
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (d.kind == DayKind.WEEKDAY) {
                    PreviewRow("OT ${hrs(pay.h15)} ชม. × ${x1(s.mult15)}x$times", pay.p15 * mult)
                    if (!d.bulk) PreviewRow("ค่าอาหาร (รวมในยอดเดือน)", pay.mealN)
                } else {
                    if (d.kind == DayKind.HOLIDAY) {
                        PreviewRow("8 ชม.แรก: ${hrs(pay.hS)} ชม. × ${x1(s.multSpecial)}x$times", pay.pS * mult)
                        PreviewRow("หลัง 8 ชม.: ${hrs(pay.hS3)} ชม. × ${x1(s.multSpecialOt)}x$times", pay.pS3 * mult)
                    } else {
                        PreviewRow("8 ชม.แรก: ${hrs(pay.hN)} ชม. × ${x1(s.multHolNormal)}x$times", pay.pN * mult)
                        PreviewRow("หลัง 8 ชม.: ${hrs(pay.h3)} ชม. × ${x1(s.multHol3)}x$times", pay.p3 * mult)
                    }
                    PreviewRow("ค่าอาหารวันหยุด$times", pay.mealH * mult)
                }
                val total = (if (d.kind == DayKind.WEEKDAY) pay.p15 else pay.pN + pay.p3 + pay.pS + pay.pS3 + pay.mealH) * mult
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(MaterialTheme.colorScheme.outline))
                Row {
                    Text(
                        if (d.bulk) "รวม $n วัน" else if (d.kind == DayKind.WEEKDAY) "ค่า OT วันนี้" else "รายได้เพิ่มจากวันนี้",
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                    )
                    Text("฿${baht(total)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp)) { Text("ยกเลิก") }
                Button(
                    onClick = { onSave(targets, d.kind, hours) }, enabled = n > 0,
                    modifier = Modifier.weight(1.4f).height(50.dp),
                ) { Text(if (d.bulk) "ใช้กับ $n วัน" else "บันทึก") }
            }
            TextButton(onClick = { onReset(d.days) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (d.bulk) "ล้างวันที่เลือก (กลับเป็นค่าเริ่มต้น)" else "ล้างวันนี้ (กลับเป็นค่าเริ่มต้น)", color = ExpenseColor)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: Double) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("฿${baht(value)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(44.dp).pressScale(interaction, 0.85f).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label) }
}

// ---------------- settings sheet ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerSettingsSheet(
    s: PlanSettings,
    sum: PlanSummary,
    store: OtPlannerStore,
    onClearMonth: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun set(t: (PlanSettings) -> PlanSettings) = store.update(t)
    var open by remember { mutableStateOf(setOf("inc")) }
    fun toggle(k: String) { open = if (k in open) open - k else open + k }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ตั้งค่า", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                LimePill("เสร็จสิ้น", onClick = onDismiss)
            }
            Text(
                "บันทึกอัตโนมัติในเครื่อง · ค่าแรง/ชม. = เงินเดือน ÷ (${hrs(s.rateDays)} วัน × ${hrs(s.rateHours)} ชม.) = ฿${baht(sum.hourly)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Accordion("เงินได้", "inc" in open, { toggle("inc") }) {
                NumField("เงินเดือน", s.baseSalary, "บาท") { v -> set { it.copy(baseSalary = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("ค่าอาหารปกติ/วัน", s.normalMeal, "บาท", Modifier.weight(1f)) { v -> set { it.copy(normalMeal = v) } }
                    NumField("ค่าอาหารวันหยุด/วัน", s.holidayMeal, "บาท", Modifier.weight(1f)) { v -> set { it.copy(holidayMeal = v) } }
                }
                NumField("เบี้ยขยัน / ค่าตำแหน่ง / คงที่", s.bonusFixed, "บาท", note = "รวมเงินได้คงที่รายเดือนอื่น ๆ ไว้ช่องนี้ได้") { v ->
                    set { it.copy(bonusFixed = v) }
                }
                ItemsEditor("รายได้อื่น ๆ (เพิ่มได้หลายรายการ)", "เพิ่มรายได้", s.incomeItems) { list -> set { it.copy(incomeItems = list) } }
            }

            Accordion("รายการหัก", "ded" in open, { toggle("ded") }) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("ประกันสังคม", s.socialSecurity, "บาท", Modifier.weight(1f)) { v -> set { it.copy(socialSecurity = v) } }
                    NumField("กองทุนสำรองฯ (PVD)", s.providentFund, "บาท", Modifier.weight(1f)) { v -> set { it.copy(providentFund = v) } }
                }
                Text("ภาษีหัก ณ ที่จ่าย", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PillTabs(
                    listOf("กรอกเอง", "คำนวณให้ (ประมาณ)"), if (s.taxMode == TaxMode.MANUAL) 0 else 1,
                    onSelect = { i -> set { it.copy(taxMode = if (i == 0) TaxMode.MANUAL else TaxMode.AUTO) } },
                    track = MaterialTheme.colorScheme.surfaceContainer,
                )
                if (s.taxMode == TaxMode.MANUAL) {
                    NumField("ภาษีต่อเดือน", s.taxManual, "บาท") { v -> set { it.copy(taxManual = v) } }
                } else {
                    Text("ภาษีต่อเดือน (ประมาณการ)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "฿${baht(sum.tax)}", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                    Text(
                        "คิดจากรายได้ทั้งปี = รายได้เดือนนี้ × 12 หักค่าใช้จ่าย 50% (ไม่เกิน 100,000), ลดหย่อนส่วนตัว 60,000, " +
                            "ประกันสังคม และ PVD ตามอัตราก้าวหน้า ยังไม่รวมลดหย่อนอื่น ๆ",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ItemsEditor("รายการหักอื่น ๆ (กู้ ค่าสหภาพ ประกัน ฯลฯ)", "เพิ่มรายการหัก", s.deductItems) { list -> set { it.copy(deductItems = list) } }
            }

            Accordion("กติกา OT", "rule" in open, { toggle("rule") }) {
                CutoffPicker(s.otCutoffDay) { day -> set { it.copy(otCutoffDay = day) } }
                Text("อัตราค่าแรงแต่ละแบบ (กี่แรงของค่าแรงต่อชั่วโมง)", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                NumField("OT วันทำงาน", s.mult15, "แรง") { v -> set { it.copy(mult15 = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("ส–อา 8 ชม.แรก", s.multHolNormal, "แรง", Modifier.weight(1f), "ส่วนเพิ่มจากเงินเดือน") { v ->
                        set { it.copy(multHolNormal = v) }
                    }
                    NumField("ส–อา หลัง 8 ชม.", s.multHol3, "แรง", Modifier.weight(1f)) { v -> set { it.copy(multHol3 = v) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("หยุดพิเศษ 8 ชม.แรก", s.multSpecial, "แรง", Modifier.weight(1f)) { v -> set { it.copy(multSpecial = v) } }
                    NumField("หยุดพิเศษ หลัง 8 ชม.", s.multSpecialOt, "แรง", Modifier.weight(1f)) { v -> set { it.copy(multSpecialOt = v) } }
                }
                NumField("ทำงานวันหยุดเกิน 8 ชม. ได้สูงสุด", s.maxHol3, "ชม.", note = "ถึง 21:00 = 3 ชม.") { v -> set { it.copy(maxHol3 = v) } }
                NumField("OT วันทำงานสูงสุด", s.maxWeekdayOt, "ชม.", note = "17:10 – 21:10 = 4 ชม.") { v -> set { it.copy(maxWeekdayOt = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumField("วัน/เดือน (หารค่าแรง)", s.rateDays, "วัน", Modifier.weight(1f)) { v -> set { it.copy(rateDays = v) } }
                    NumField("ชม./วัน", s.rateHours, "ชม.", Modifier.weight(1f)) { v -> set { it.copy(rateHours = v) } }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("นับค่าอาหารปกติทุกวันทำงาน", style = MaterialTheme.typography.bodyMedium)
                        Text("ปิด = นับเฉพาะวันที่ทำ OT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = s.mealAllWeekdays, onCheckedChange = { on -> set { it.copy(mealAllWeekdays = on) } })
                }
            }

            ConfirmTwiceButton("ล้างปฏิทินเดือนนี้", onClearMonth)
            ConfirmTwiceButton("คืนค่าเริ่มต้นการตั้งค่าทั้งหมด") { store.resetSettings() }
        }
    }
}

@Composable
private fun Accordion(title: String, open: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, label = "acc")
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(turn))
            }
            AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
            }
        }
    }
}

/** Number input that keeps what is typed (e.g. "1.") while passing every valid value on straight away. */
@Composable
private fun NumField(label: String, value: Double, suffix: String, modifier: Modifier = Modifier.fillMaxWidth(), note: String? = null, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value == 0.0) "" else hrs(value)) }
    LaunchedEffect(value) {
        // Follow outside changes (e.g. "reset to defaults") without fighting the user's typing.
        if ((text.toDoubleOrNull() ?: 0.0) != value) text = if (value == 0.0) "" else hrs(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v.filter { it.isDigit() || it == '.' }
            onValue(text.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        suffix = { Text(suffix) },
        supportingText = note?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(13.dp),
        modifier = modifier,
    )
}

@Composable
private fun ItemsEditor(title: String, addLabel: String, items: List<PlanItem>, onChange: (List<PlanItem>) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    items.forEachIndexed { i, it ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = it.name, onValueChange = { v -> onChange(items.toMutableList().also { l -> l[i] = it.copy(name = v) }) },
                placeholder = { Text("ชื่อรายการ") }, singleLine = true, shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f),
            )
            NumField("จำนวน", it.amount, "", Modifier.width(110.dp)) { v -> onChange(items.toMutableList().also { l -> l[i] = l[i].copy(amount = v) }) }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
                    .clickable { onChange(items.filterIndexed { j, _ -> j != i }) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, "ลบรายการ", tint = ExpenseColor, modifier = Modifier.size(18.dp)) }
        }
    }
    OutlinedButton(onClick = { onChange(items + PlanItem("", 0.0)) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(addLabel)
    }
}

/** Destructive action that needs a second tap within three seconds. */
@Composable
private fun ConfirmTwiceButton(label: String, onConfirm: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }
    TextButton(onClick = { if (armed) { armed = false; onConfirm() } else armed = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (armed) "แตะอีกครั้งเพื่อยืนยัน" else label, color = ExpenseColor)
    }
}

/** End of month, or a fixed day 1–28 on which OT is cut off for the month's payslip. */
@Composable
private fun CutoffPicker(day: Int, onDay: (Int) -> Unit) {
    val custom = day in 1..28
    Text("วันตัดยอด OT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PillTabs(
        listOf("สิ้นเดือน", "กำหนดวัน"), if (custom) 1 else 0,
        onSelect = { i -> onDay(if (i == 0) 0 else if (custom) day else 20) },
        track = MaterialTheme.colorScheme.surfaceContainer,
    )
    AnimatedVisibility(custom, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("ตัดยอดทุกวันที่", style = MaterialTheme.typography.titleSmall)
                val d = day.coerceIn(1, 28)
                Text(
                    "OT วันที่ ${if (d == 28) 29 else d + 1} เดือนก่อน – $d เดือนนี้ เข้าสลิปเดือนนี้",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StepButton(Icons.Default.Remove, "ก่อนหน้า") { onDay((day - 1).coerceAtLeast(1)) }
            AnimatedContent("${day.coerceIn(1, 28)}", label = "cutoff", modifier = Modifier.width(52.dp)) {
                Text(it, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            StepButton(Icons.Default.Add, "ถัดไป") { onDay((day + 1).coerceAtMost(28)) }
        }
    }
}