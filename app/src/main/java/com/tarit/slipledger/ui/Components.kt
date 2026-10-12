package com.tarit.slipledger.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    /** Small text set after the title on the same line, e.g. "by Teen". */
    titleSuffix: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (titleSuffix == null) Text(title, style = MaterialTheme.typography.headlineMedium)
            else SignatureTitle(title, titleSuffix)
        }
        trailing?.invoke()
    }
}

/** Bottom room a screen must leave so its last items can scroll clear of the floating tab bar. */
val LocalBarSpace = androidx.compose.runtime.compositionLocalOf { 0.dp }

/** Sub-screen header in the reference style: round back button, centred title, lime pill action on the right. */
@Composable
fun SubHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier.align(Alignment.CenterStart).size(42.dp).pressScale(interaction, 0.85f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable(interaction, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "กลับ", Modifier.size(20.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 56.dp))
        Box(Modifier.align(Alignment.CenterEnd)) { action?.invoke() }
    }
}

/** Small lime pill button, like "RESELL" in the reference. */
@Composable
fun LimePill(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.pressScale(interaction, 0.9f).clip(CircleShape).background(Lime)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(start = if (icon != null) 10.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = InkBottom, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = InkBottom, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** "4 รายการ | ฿1,500 จ่าย" pill of figures separated by thin rules, like the reference's books header. */
@Composable
fun FigurePill(vararg parts: Pair<String, String>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { i, (value, label) ->
            if (i > 0) Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outline))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Compact "< ต.ค. 69 >" pill. */
@Composable
fun PeriodSwitcher(label: String, onShift: (Long) -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShift(-1) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "ก่อนหน้า")
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { onShift(1) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "ถัดไป")
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
    padding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(if (padding) Modifier.padding(18.dp) else Modifier.padding(vertical = 6.dp)) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().then(if (padding) Modifier else Modifier.padding(horizontal = 18.dp, vertical = 12.dp)),
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action?.invoke()
                }
                if (padding) Spacer(Modifier.height(14.dp))
            }
            content()
        }
    }
}

/** Rounded progress bar that turns amber near the limit and red when over it. */
@Composable
fun BudgetBar(spent: Long, budget: Long, modifier: Modifier = Modifier, height: Int = 10) {
    val ratio = if (budget > 0) spent.toFloat() / budget else 0f
    val color = when {
        ratio >= 1f -> ExpenseColor
        ratio >= 0.8f -> WarningColor
        else -> IncomeColor
    }
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(700), label = "budget")
    Box(
        modifier.fillMaxWidth().height(height.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(Modifier.fillMaxWidth(animated).height(height.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun BudgetLine(label: String, spent: Long, budget: Long, leading: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    "฿${Fmt.bahtShort(spent)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (spent > budget) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    " / ฿${Fmt.bahtShort(budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            BudgetBar(spent, budget)
            val left = budget - spent
            Spacer(Modifier.height(6.dp))
            Text(
                if (left >= 0) "เหลือ ฿${Fmt.baht(left)}" else "เกินงบ ฿${Fmt.baht(-left)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseColor,
            )
        }
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) = Box(Modifier.size(size.dp).background(color, CircleShape))

/** "+12%" / "-8%" pill; for spending, going up is bad (red). */
@Composable
fun ChangePill(percent: Int?, upIsGood: Boolean = false) {
    if (percent == null) return
    val bad = if (upIsGood) percent < 0 else percent > 0
    val color = when {
        percent == 0 -> TransferColor
        bad -> ExpenseColor
        else -> IncomeColor
    }
    Text(
        (if (percent > 0) "▲ " else if (percent < 0) "▼ " else "") + "${abs(percent)}%",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Default.Close, "ล้าง",
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.floating().size(84.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun TxRow(tx: Tx, showDate: Boolean = false, onClick: (() -> Unit)?) {
    val (color, sign) = when (tx.type) {
        TxType.INCOME -> IncomeColor to "+"
        TxType.EXPENSE -> MaterialTheme.colorScheme.onSurface to "-"
        TxType.TRANSFER -> TransferColor to ""
    }
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(tx.category)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.counterparty.ifBlank { tx.category.ifBlank { "ไม่ระบุ" } },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val source = when (tx.source) {
                TxSource.SLIP -> "สลิป"
                TxSource.NOTIFICATION -> "แจ้งเตือน"
                TxSource.MANUAL -> "จดเอง"
                TxSource.RECURRING -> "ประจำ"
            }
            val time = if (showDate) Fmt.shortDate(Fmt.dateOf(tx.timestamp)) else Fmt.time(tx.timestamp)
            val wallet = LocalWallets.current.firstOrNull { it.id == tx.walletId }?.name ?: source
            Text(
                listOf(tx.category, time, wallet).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tx.tagList.isNotEmpty()) {
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tx.tagList.take(3).forEach { TagChip(it) }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("$sign${Fmt.baht(tx.amountSatang)}", color = color, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun TagChip(name: String) {
    Text(
        "#$name",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A day's transactions in one card with a header showing the day's net. */
@Composable
fun DayGroup(date: LocalDate, txs: List<Tx>, onClick: (Tx) -> Unit) {
    val net = txs.sumOf {
        when (it.type) {
            TxType.INCOME -> it.amountSatang
            TxType.EXPENSE -> -it.amountSatang
            TxType.TRANSFER -> 0L
        }
    }
    Column(Modifier.appear().padding(horizontal = 16.dp).padding(top = 14.dp)) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            Text(Fmt.relativeDay(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            Text(
                (if (net >= 0) "+" else "-") + Fmt.baht(abs(net)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard(padding = false) {
            txs.forEachIndexed { i, tx ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TxRow(tx) { onClick(tx) }
            }
        }
    }
}

/** Settings-style row with an icon badge, title, optional subtitle and trailing content. */
@Composable
fun MenuRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, color, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
fun MenuDivider() = HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
