package com.tarit.slipledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.CategoryDef
import com.tarit.slipledger.data.Period
import com.tarit.slipledger.data.Settings
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxType
import java.time.YearMonth

@Composable
fun StatsScreen(vm: MainViewModel, actions: AppActions) {
    val month by vm.month.collectAsStateWithLifecycle()
    val range by vm.statsRange.collectAsStateWithLifecycle()
    val txs by vm.statsTx.collectAsStateWithLifecycle()
    val prevTxs by vm.statsPrevTx.collectAsStateWithLifecycle()
    val trend by vm.trend.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val period = settings.period

    val summary = Summary.of(txs)
    val prev = Summary.of(prevTxs)
    val byCategory = expenseByCategory(txs)
    val prevByCategory = expenseByCategory(prevTxs).toMap()
    val label = when (range) {
        StatsRange.MONTH -> Fmt.monthShort(month)
        StatsRange.QUARTER -> Fmt.quarter(month)
        StatsRange.YEAR -> Fmt.year(month)
    }
    val prevLabel = when (range) {
        StatsRange.MONTH -> "เดือนก่อน"
        StatsRange.QUARTER -> "ไตรมาสก่อน"
        StatsRange.YEAR -> "ปีก่อน"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("สรุป", trailing = { PeriodSwitcher(label) { vm.shiftMonth(it * range.months) } })
        PillTabs(
            StatsRange.entries.map { it.label }, StatsRange.entries.indexOf(range),
            onSelect = { vm.statsRange.value = StatsRange.entries[it] },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp + LocalBarSpace.current),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.appear(0)) { OverviewCard(summary, prev, prevLabel) }
            if (range == StatsRange.MONTH) {
                Box(Modifier.appear(1)) { DailyGraph(month, period, txs.filter { it.type == TxType.EXPENSE }) }
                vm.forecast(txs, month, period)?.takeIf { it.spent > 0 }?.let { Box(Modifier.appear(2)) { ForecastCard(it, settings.monthlyBudget) } }
            }
            Box(Modifier.appear(3)) {
                SectionCard(title = "รายจ่ายตามหมวด") {
                    if (byCategory.isEmpty()) EmptyHint("ยังไม่มีรายจ่ายในช่วงนี้")
                    else CategoryDonut(byCategory, prevByCategory, prevLabel, actions.showCategory)
                }
            }
            if (range == StatsRange.MONTH) {
                Box(Modifier.appear(4)) { BudgetCard(month, byCategory.toMap(), settings, categories, actions.editBudget) }
                Box(Modifier.appear(5)) { SectionCard(title = "6 เดือนล่าสุด") { TrendBars(trend) } }
            } else {
                val (first, last) = vm.rangeMonths(range, month)
                val months = generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(last) }.toList()
                SectionCard(title = "รายเดือน") {
                    TrendBars(months.map { m -> m to Summary.of(txs.filter { period.contains(m, Fmt.dateOf(it.timestamp)) }) })
                }
            }
            TagBreakdown(txs)
        }
    }
}

private fun expenseByCategory(txs: List<Tx>): List<Pair<String, Long>> =
    txs.filter { it.type == TxType.EXPENSE }
        .groupBy { it.category.ifBlank { Categories.OTHER } }
        .map { (k, l) -> k to l.sumOf { it.amountSatang } }
        .sortedByDescending { it.second }

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun OverviewCard(s: Summary, prev: Summary, prevLabel: String) {
    SectionCard {
        Row {
            OverviewItem("รายรับ", s.income, IncomeColor, Fmt.change(s.income, prev.income), upIsGood = true, Modifier.weight(1f))
            OverviewItem("รายจ่าย", s.expense, ExpenseColor, Fmt.change(s.expense, prev.expense), upIsGood = false, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("คงเหลือสุทธิ", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                (if (s.balance < 0) "-฿" else "฿") + Fmt.baht(kotlin.math.abs(s.balance)),
                style = MaterialTheme.typography.titleMedium,
                color = if (s.balance < 0) ExpenseColor else IncomeColor,
            )
        }
        Text(
            "เปอร์เซ็นต์เทียบกับ$prevLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun OverviewItem(label: String, amount: Long, color: Color, change: Int?, upIsGood: Boolean, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, 8)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("฿" + Fmt.bahtShort(animatedSatang(amount)), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        ChangePill(change, upIsGood)
    }
}

@Composable
private fun ForecastCard(f: Forecast, budget: Long) {
    SectionCard(title = "คาดการณ์สิ้นเดือน") {
        val over = budget > 0 && f.projected > budget
        Text("฿" + Fmt.bahtShort(f.projected), style = MaterialTheme.typography.headlineSmall, color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurface)
        Text(
            "ใช้ไปแล้ว ฿${Fmt.bahtShort(f.spent)} · เหลืออีก ${f.daysLeft} วัน" + if (budget > 0) {
                if (over) " · อาจเกินงบ ฿${Fmt.bahtShort(f.projected - budget)}" else " · อยู่ในงบ"
            } else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (budget > 0) {
            Spacer(Modifier.height(12.dp))
            BudgetBar(f.projected, budget)
        }
    }
}

@Composable
private fun CategoryDonut(
    slices: List<Pair<String, Long>>,
    prev: Map<String, Long>,
    prevLabel: String,
    onClick: (String) -> Unit,
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val colors = slices.map { categoryStyle(it.first).color }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(slices) { sweep.snapTo(0f); sweep.animateTo(1f, tween(800)) }
    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(190.dp)) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val gap = if (slices.size > 1) 3f else 0f
            var start = -90f
            slices.forEachIndexed { i, (_, amount) ->
                val angle = 360f * amount / total * sweep.value
                drawArc(
                    colors[i], start + gap / 2, (angle - gap).coerceAtLeast(0.5f),
                    false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
                )
                start += angle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("รายจ่ายรวม", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("฿" + Fmt.bahtShort(animatedSatang(total)), style = MaterialTheme.typography.titleLarge)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("แตะหมวดเพื่อดูรายการ · % เทียบ$prevLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Column {
        slices.forEach { (cat, amount) ->
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onClick(cat) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(cat, size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ChangePill(Fmt.change(amount, prev[cat] ?: 0))
                        Spacer(Modifier.width(8.dp))
                        Text("฿" + Fmt.bahtShort(amount), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Box(Modifier.fillMaxWidth(amount.toFloat() / total).height(6.dp).clip(RoundedCornerShape(3.dp)).background(categoryStyle(cat).color))
                        }
                        Text(
                            "${amount * 100 / total}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(month: YearMonth, spentByCategory: Map<String, Long>, settings: Settings, categories: List<CategoryDef>, onEdit: () -> Unit) {
    val hasBudget = settings.monthlyBudget > 0 || settings.categoryBudgets.isNotEmpty()
    SectionCard(
        title = "งบประมาณ",
        action = { TextButton(onClick = onEdit) { Text(if (hasBudget) "แก้ไข" else "ตั้งงบ") } },
    ) {
        if (!hasBudget) {
            Text(
                "ตั้งงบรายเดือนหรือแยกตามหมวด แอพจะแจ้งเตือนเมื่อใช้ถึง 80% และเมื่อเกินงบ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        val order = categories.map { it.name }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (settings.monthlyBudget > 0) {
                BudgetLine("ทั้งเดือน (${Fmt.month(month)})", spentByCategory.values.sum(), settings.monthlyBudget)
            }
            settings.categoryBudgets.entries.sortedBy { order.indexOf(it.key) }.forEach { (cat, budget) ->
                BudgetLine(cat, spentByCategory[cat] ?: 0, budget) { CategoryBadge(cat, size = 38.dp) }
            }
        }
    }
}

@Composable
private fun DailyGraph(month: YearMonth, period: Period, expenses: List<Tx>) {
    val start = period.startDate(month)
    val days = period.lengthInDays(month)
    val upTo = (java.time.LocalDate.now().toEpochDay() - start.toEpochDay() + 1).toInt().coerceIn(0, days)
    val perDay = LongArray(upTo)
    expenses.forEach {
        val i = (Fmt.dateOf(it.timestamp).toEpochDay() - start.toEpochDay()).toInt()
        if (i in 0 until upTo) perDay[i] += it.amountSatang
    }
    InkPanel {
        Column(Modifier.padding(18.dp)) {
            Text("รายจ่ายรายวัน", color = Color.White, style = MaterialTheme.typography.titleMedium)
            val max = perDay.maxOrNull()?.takeIf { it > 0 }
            Text(
                if (max == null) "ยังไม่มีรายจ่ายในเดือนนี้"
                else "สูงสุด ${Fmt.shortDate(start.plusDays(perDay.indexOfFirst { it == max }.toLong()))} · ฿${Fmt.bahtShort(max)}" +
                    "   เฉลี่ย ฿${Fmt.bahtShort(perDay.sum() / upTo.coerceAtLeast(1))}/วัน",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            SpendGraph(
                values = perDay.toList(),
                slots = days,
                xLabel = { "${start.plusDays(it.toLong()).dayOfMonth}" },
                detail = { Fmt.shortDate(start.plusDays(it.toLong())) },
            )
        }
    }
}
@Composable
private fun TrendBars(trend: List<Pair<YearMonth, Summary>>) {
    val max = trend.maxOfOrNull { maxOf(it.second.income, it.second.expense) }?.takeIf { it > 0 }
    if (max == null) {
        EmptyHint("ยังไม่มีข้อมูล")
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Dot(IncomeColor); Spacer(Modifier.width(6.dp)); Text("รายรับ", style = MaterialTheme.typography.labelMedium) }
        Row(verticalAlignment = Alignment.CenterVertically) { Dot(ExpenseColor); Spacer(Modifier.width(6.dp)); Text("รายจ่าย", style = MaterialTheme.typography.labelMedium) }
    }
    Spacer(Modifier.height(14.dp))
    val grow = remember { Animatable(0f) }
    LaunchedEffect(trend) { grow.snapTo(0f); grow.animateTo(1f, tween(700)) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        trend.forEach { (ym, s) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (s.expense > 0) Fmt.bahtCompact(s.expense) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Row(Modifier.height(110.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    TrendBar(s.income, max, IncomeColor, grow.value)
                    TrendBar(s.expense, max, ExpenseColor, grow.value)
                }
                Spacer(Modifier.height(4.dp))
                Text(Fmt.monthAbbr(ym), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TrendBar(value: Long, max: Long, color: Color, grow: Float) {
    Box(
        Modifier.width(9.dp).fillMaxHeight(if (value > 0) (value.toFloat() / max * grow).coerceAtLeast(0.03f) else 0.015f)
            .background(if (value > 0) color else color.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
    )
}

@Composable
private fun TagBreakdown(txs: List<Tx>) {
    val byTag = txs.filter { it.type == TxType.EXPENSE }
        .flatMap { tx -> tx.tagList.map { it to tx.amountSatang } }
        .groupBy({ it.first }, { it.second })
        .map { (tag, amounts) -> tag to amounts.sum() }
        .sortedByDescending { it.second }
    if (byTag.isEmpty()) return
    SectionCard(title = "รายจ่ายตามแท็ก") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            byTag.forEach { (tag, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(tag)
                    Spacer(Modifier.weight(1f))
                    Text("฿" + Fmt.bahtShort(amount), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(
    settings: Settings,
    categories: List<CategoryDef>,
    onDismiss: () -> Unit,
    onSave: (Long, Map<String, Long>) -> Unit,
) {
    var total by remember { mutableStateOf(settings.monthlyBudget.takeIf { it > 0 }?.let(Fmt::bahtShort).orEmpty()) }
    val perCategory = remember {
        mutableStateMapOf<String, String>().apply {
            settings.categoryBudgets.forEach { (k, v) -> put(k, Fmt.bahtShort(v)) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ตั้งงบประมาณรายเดือน", style = MaterialTheme.typography.titleLarge)
            Text("เว้นว่างไว้ถ้าไม่ต้องการตั้งงบ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("งบรวมทั้งเดือน (บาท)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("งบแยกตามหมวด", style = MaterialTheme.typography.titleSmall)
            categories.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(cat.name, size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = perCategory[cat.name].orEmpty(),
                        onValueChange = { perCategory[cat.name] = it },
                        label = { Text(cat.name) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(onClick = {
                    onSave(
                        Fmt.parseSatang(total) ?: 0L,
                        perCategory.mapNotNull { (k, v) -> Fmt.parseSatang(v)?.let { k to it } }.toMap(),
                    )
                }) { Text("บันทึก") }
            }
        }
    }
}
