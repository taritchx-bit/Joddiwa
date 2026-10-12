package com.tarit.slipledger.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.notify.BankNotificationListener
import com.tarit.slipledger.scan.ScanProgress
import com.tarit.slipledger.scan.SlipScanner
import kotlin.math.abs

@Composable
fun HomeScreen(vm: MainViewModel, actions: AppActions) {
    val context = LocalContext.current
    val month by vm.month.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val review by vm.needsReview.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val period = settings.period

    var hasImages by remember { mutableStateOf(SlipScanner.hasPermission(context)) }
    var hasNotif by remember { mutableStateOf(BankNotificationListener.isEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasImages = SlipScanner.hasPermission(context)
        hasNotif = BankNotificationListener.isEnabled(context)
    }

    val summary = Summary.of(txs)
    val forecast = vm.forecast(txs, month, period)
    val range = "${Fmt.shortDate(period.startDate(month))} – ${Fmt.shortDate(period.endDate(month).minusDays(1))}"
    val counted = countedWallets(wallets, settings.excludedWallets)
    var pickingWallets by remember { mutableStateOf(false) }
    val graph = remember(txs, month, period) {
        val start = period.startDate(month)
        val slots = period.lengthInDays(month)
        val upTo = (LocalDate.now().toEpochDay() - start.toEpochDay() + 1).toInt().coerceIn(0, slots)
        val perDay = LongArray(upTo)
        txs.filter { it.type == TxType.EXPENSE }.forEach {
            val i = (Fmt.dateOf(it.timestamp).toEpochDay() - start.toEpochDay()).toInt()
            if (i in 0 until upTo) perDay[i] += it.amountSatang
        }
        GraphData(start, slots, perDay.toList())
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBarSpace.current)) {
        item {
            Box(Modifier.appear(0)) {
                ScreenHeader(
                    title = "Joddiwa",
                    titleSuffix = "by Teen",
                    subtitle = greeting(settings.ownerNames.firstOrNull()),
                    trailing = { StreakChip(streak) },
                )
            }
        }
        item {
            var order = 1
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.appear(order++)) { HeroCard(
                    summary, Fmt.month(month), range, onShift = vm::shiftMonth,
                    walletTotal = walletTotal(wallets, settings.excludedWallets, balances),
                    walletNote = when {
                        counted.isEmpty() -> "แตะเพื่อเลือกกระเป๋า"
                        counted.size == wallets.size -> "รวมทุกกระเป๋า"
                        else -> "รวม ${counted.size} จาก ${wallets.size} กระเป๋า"
                    },
                    onBalanceClick = { if (wallets.none { it.balanceSet }) actions.openWallets() else pickingWallets = true },
                    graph = graph,
                ) }
                Box(Modifier.appear(order++)) {
                    QuickActions(
                        onScan = actions.scan,
                        onExpense = { actions.newTx(TxType.EXPENSE) },
                        onIncome = { actions.newTx(TxType.INCOME) },
                    )
                }
                AnimatedVisibility(scan != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    scan?.let { ScanProgressCard(it) }
                }
                AnimatedVisibility(review.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    ReviewCard(review.size, actions.openReview)
                }
                Box(Modifier.appear(order++)) { WalletStrip(wallets, balances, actions) }
                if (!hasImages) {
                    Box(Modifier.appear(order++)) {
                        SetupCard(Icons.Default.Image, "อนุญาตให้อ่านรูปภาพ", "เพื่อค้นหาสลิปในแกลเลอรี", "อนุญาต", actions.scan)
                    }
                }
                if (!hasNotif) {
                    Box(Modifier.appear(order++)) {
                        SetupCard(
                            Icons.Default.NotificationsActive, "บันทึกเงินเข้าอัตโนมัติ",
                            "เปิดการเข้าถึงการแจ้งเตือน เพื่ออ่านเงินเข้าจาก LINE ของธนาคาร", "เปิด",
                        ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    }
                }
                if (settings.monthlyBudget > 0 || forecast != null) {
                    Box(Modifier.appear(order++)) {
                        BudgetForecastCard(summary.expense, settings.monthlyBudget, forecast, actions.editBudget)
                    }
                }
                Box(Modifier.appear(order++)) { TopCategories(txs, onClick = actions.showCategory) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("รายการล่าสุด", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (txs.isNotEmpty()) TextButton(onClick = actions.showHistory) { Text("ดูทั้งหมด") }
            }
        }
        if (txs.isEmpty() && scan == null) {
            item {
                EmptyState(Icons.Outlined.ReceiptLong, "ยังไม่มีรายการในเดือนนี้", "กด \"สแกนสลิป\" แล้วแอพจะอ่านสลิปในแกลเลอรีมาจดให้")
            }
        } else {
            // One card per transaction, cascading in like the reference's listing cards.
            itemsIndexed(txs.take(6), key = { _, tx -> tx.id }) { i, tx ->
                PressCard(
                    onClick = { actions.edit(tx) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).appear(i).animateItem(),
                ) { TxRow(tx, showDate = true, onClick = null) }
            }
        }
    }
    if (pickingWallets) {
        WalletPickerSheet(wallets, balances, settings.excludedWallets, vm::setWalletCounted, onDismiss = { pickingWallets = false })
    }
}

private fun greeting(name: String?): String {
    val hour = java.time.LocalTime.now().hour
    val hello = when (hour) {
        in 5..11 -> "อรุณสวัสดิ์"
        in 12..17 -> "สวัสดีตอนบ่าย"
        in 18..21 -> "สวัสดีตอนเย็น"
        else -> "ดึกแล้วนะ"
    }
    return if (name.isNullOrBlank()) hello else "$hello, $name"
}

@Composable
private fun StreakChip(days: Int) {
    val active = days > 0
    Surface(
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalFireDepartment, null,
                tint = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (active) "$days วันติด" else "เริ่มจดวันนี้",
                style = MaterialTheme.typography.labelLarge,
                color = if (active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard(
    s: Summary,
    monthLabel: String,
    range: String,
    onShift: (Long) -> Unit,
    walletTotal: Long,
    walletNote: String,
    onBalanceClick: () -> Unit,
    graph: GraphData,
) {
    var cumulative by rememberSaveable { mutableStateOf(false) }
    var direction by remember { mutableIntStateOf(1) }
    InkPanel {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = monthLabel to range,
                        transitionSpec = {
                            (slideInHorizontally { it / 2 * direction } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 2 * direction } + fadeOut())
                        },
                        label = "month",
                    ) { (m, r) ->
                        Column {
                            Text(m, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(r, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                HeroButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "เดือนก่อน") { direction = -1; onShift(-1) }
                Spacer(Modifier.width(8.dp))
                HeroButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "เดือนถัดไป") { direction = 1; onShift(1) }
            }
            Spacer(Modifier.height(18.dp))
            // The two headline numbers get identical tiles so neither outweighs the other.
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    Icons.Default.AccountBalanceWallet, "เงินคงเหลือ", walletTotal, walletNote,
                    Modifier.weight(1f).fillMaxHeight(), onBalanceClick,
                )
                HeroStat(
                    Icons.Default.ArrowUpward, "ใช้ไปเดือนนี้", s.expense, "รายรับ ฿" + Fmt.baht(s.income),
                    Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("กราฟรายจ่าย", color = Color.White, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                ModePill(if (cumulative) "สะสม" else "รายวัน") { cumulative = !cumulative }
            }
            Spacer(Modifier.height(6.dp))
            SpendGraph(
                values = if (cumulative) graph.perDay.runningFold(0L, Long::plus).drop(1) else graph.perDay,
                slots = graph.slots,
                xLabel = { "${graph.start.plusDays(it.toLong()).dayOfMonth}" },
                detail = { Fmt.shortDate(graph.start.plusDays(it.toLong())) },
            )
        }
    }
}

/** Spending per day of the shown period, up to today. */
class GraphData(val start: LocalDate, val slots: Int, val perDay: List<Long>)

@Composable
private fun ModePill(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.pressScale(interaction).glass(CircleShape, 0.8f)
            .clickable(interaction, indication = null, onClick = onClick).padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            label,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "mode",
        ) { Text(it, color = Color.White, style = MaterialTheme.typography.labelMedium) }
        Icon(Icons.Default.SwapVert, null, tint = Lime, modifier = Modifier.padding(start = 4.dp).size(16.dp))
    }
}

@Composable
private fun HeroButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(36.dp).pressScale(interaction, 0.85f).glass(CircleShape)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White) }
}

@Composable
private fun HeroStat(
    icon: ImageVector,
    label: String,
    satang: Long,
    note: String,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.pressScale(interaction, 0.96f).glass(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).background(Lime, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = InkBottom, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        FitText(money(animatedSatang(satang)), MaterialTheme.typography.headlineSmall, Color.White)
        Text(note, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One line of text that steps its font size down until it fits, so large amounts are never cut off. */
@Composable
private fun FitText(text: String, style: TextStyle, color: Color) {
    // Sized for the widest the text has been, so a counting-up number does not shrink step by step.
    var fitted by remember(style) { mutableStateOf(style) }
    var ready by remember(style) { mutableStateOf(false) }
    Text(
        text, color = color, style = fitted, maxLines = 1, softWrap = false,
        modifier = Modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && fitted.fontSize > 14.sp) fitted = fitted.copy(fontSize = fitted.fontSize * 0.92f)
            else ready = true
        },
    )
}

@Composable
private fun QuickActions(onScan: () -> Unit, onExpense: () -> Unit, onIncome: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Icons.Default.DocumentScanner, "สแกนสลิป", LimeDeep, Modifier.weight(1f), onScan)
        QuickAction(Icons.Default.Remove, "จดรายจ่าย", ExpenseColor, Modifier.weight(1f), onExpense)
        QuickAction(Icons.Default.Add, "จดรายรับ", IncomeColor, Modifier.weight(1f), onIncome)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    PressCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, color, 40.dp)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BudgetForecastCard(spent: Long, budget: Long, forecast: Forecast?, onEditBudget: () -> Unit) {
    SectionCard(
        title = if (budget > 0) "งบเดือนนี้" else "แนวโน้มเดือนนี้",
        action = { TextButton(onClick = onEditBudget) { Text(if (budget > 0) "แก้ไข" else "ตั้งงบ") } },
    ) {
        if (budget > 0) BudgetLine("ใช้ไปแล้ว", spent, budget)
        if (forecast != null && forecast.spent > 0) {
            if (budget > 0) Spacer(Modifier.height(14.dp))
            val over = budget > 0 && forecast.projected > budget
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                    .background((if (over) ExpenseColor else MaterialTheme.colorScheme.secondary).copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = if (over) ExpenseColor else MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "คาดว่าสิ้นเดือนจะใช้ ฿${Fmt.bahtShort(forecast.projected)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        when {
                            over -> "อาจเกินงบ ฿${Fmt.bahtShort(forecast.projected - budget)} · เหลืออีก ${forecast.daysLeft} วัน"
                            budget > 0 -> "ใช้ได้อีกวันละ ฿${Fmt.bahtShort(((budget - spent).coerceAtLeast(0)) / forecast.daysLeft.coerceAtLeast(1))} · เหลือ ${forecast.daysLeft} วัน"
                            else -> "คิดจากค่าเฉลี่ยรายวัน · เหลือ ${forecast.daysLeft} วัน"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (over) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCategories(txs: List<com.tarit.slipledger.data.Tx>, onClick: (String) -> Unit) {
    val top = txs.filter { it.type == TxType.EXPENSE }
        .groupBy { it.category.ifBlank { Categories.OTHER } }
        .mapValues { (_, l) -> l.sumOf { it.amountSatang } }
        .entries.sortedByDescending { it.value }
    if (top.isEmpty()) return
    val total = top.sumOf { it.value }.coerceAtLeast(1)
    Column {
        Text("ใช้จ่ายมากสุด", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(top.take(8), key = { it.key }) { (cat, amount) ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest, onClick = { onClick(cat) }) {
                    Column(Modifier.width(132.dp).padding(14.dp)) {
                        CategoryBadge(cat, 38.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(cat, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("฿" + Fmt.bahtShort(amount), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Box(Modifier.fillMaxWidth(amount.toFloat() / total).height(4.dp).clip(CircleShape).background(categoryStyle(cat).color))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(count: Int, onOpen: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer, onClick = onOpen) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FactCheck, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("มี $count รายการรอตรวจ", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("อ่านจากสลิปหรือแจ้งเตือนได้ไม่ครบ แตะเพื่อยืนยันหรือแก้ไข", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun WalletStrip(
    wallets: List<com.tarit.slipledger.data.Wallet>,
    balances: Map<Long, Long>,
    actions: AppActions,
) {
    if (wallets.isEmpty()) return
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("กระเป๋าเงิน", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = actions.openWallets) { Text("จัดการ") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(wallets, key = { _, w -> w.id }) { i, w ->
                PressCard(
                    onClick = { if (w.balanceSet) actions.showWallet(w.id) else actions.openWallets() },
                    modifier = Modifier.appear(i + 2),
                ) {
                    Column(Modifier.width(150.dp).padding(14.dp)) {
                        WalletBadge(w, 36.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(w.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (w.balanceSet) {
                            val b = balances[w.id] ?: 0
                            Text(money(b), style = MaterialTheme.typography.titleSmall, color = if (b < 0) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text("แตะเพื่อตั้งยอด", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanProgressCard(p: ScanProgress) {
    SectionCard {
        Text(
            if (p.total == 0) "กำลังค้นหารูปในแกลเลอรี…" else "กำลังอ่านสลิป ${p.done}/${p.total} · พบแล้ว ${p.found} รายการ",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        if (p.total == 0) LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
        else LinearProgressIndicator(progress = { p.done.toFloat() / p.total }, modifier = Modifier.fillMaxWidth().clip(CircleShape))
    }
}

@Composable
private fun SetupCard(icon: ImageVector, title: String, body: String, action: String, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onClick) { Text(action) }
        }
    }
}

/** Tick the wallets whose money should be added into the total balance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPickerSheet(
    wallets: List<com.tarit.slipledger.data.Wallet>,
    balances: Map<Long, Long>,
    excluded: Set<Long>,
    onCount: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(bottom = 28.dp)) {
            Text("รวมเงินจากกระเป๋าไหนบ้าง", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "ยอดรวม ${money(walletTotal(wallets, excluded, balances))}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            wallets.forEach { w ->
                val checked = w.balanceSet && w.id !in excluded
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = w.balanceSet) { onCount(w.id, !checked) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onCount(w.id, it) }, enabled = w.balanceSet)
                    WalletBadge(w, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(w.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (w.balanceSet) money(balances[w.id] ?: 0) else "ยังไม่ได้ตั้งยอด",
                        style = if (w.balanceSet) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        color = if (w.balanceSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
        }
    }
}