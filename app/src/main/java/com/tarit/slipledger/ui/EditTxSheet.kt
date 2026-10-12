package com.tarit.slipledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.TagDef
import com.tarit.slipledger.data.Tags
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Category names offered for a transaction type, in the user's order. */
@Composable
fun categoryNamesFor(type: TxType): List<String> {
    if (type == TxType.TRANSFER) return listOf(Categories.SELF)
    return LocalCategories.current.values.filter { it.kind == type }.sortedBy { it.sortOrder }.map { it.name }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTxSheet(
    initial: Tx,
    allTags: List<TagDef>,
    onDismiss: () -> Unit,
    /** Second argument: also re-categorise this payee's other transactions. */
    onSave: (Tx, Boolean) -> Unit,
    onDelete: (Tx) -> Unit,
) {
    var applyToPayee by remember { mutableStateOf(true) }
    var type by remember { mutableStateOf(initial.type) }
    var amount by remember { mutableStateOf(if (initial.amountSatang > 0) Fmt.bahtShort(initial.amountSatang).replace(",", "") else "") }
    var counterparty by remember { mutableStateOf(initial.counterparty) }
    var category by remember { mutableStateOf(initial.category) }
    var note by remember { mutableStateOf(initial.note) }
    var tags by remember { mutableStateOf(initial.tagList.toSet()) }
    var newTag by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(initial.timestamp) }
    var walletId by remember { mutableStateOf(initial.walletId) }
    var toWalletId by remember { mutableStateOf(initial.toWalletId) }
    val wallets = LocalWallets.current
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val amountSatang = Fmt.parseSatang(amount)
    val optionsByType = TxType.entries.associateWith { categoryNamesFor(it) }
    val options = optionsByType.getValue(type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val types = listOf(TxType.EXPENSE to "รายจ่าย", TxType.INCOME to "รายรับ", TxType.TRANSFER to "โอนตัวเอง")
            val typeColor = when (type) {
                TxType.INCOME -> IncomeColor
                TxType.EXPENSE -> ExpenseColor
                TxType.TRANSFER -> TransferColor
            }
            // The thumb takes the type's colour, so the sheet reads as money out / in / moved at a glance.
            PillTabs(
                types.map { it.second }, types.indexOfFirst { it.first == type },
                onSelect = { i ->
                    val t = types[i].first
                    type = t
                    val names = optionsByType.getValue(t)
                    if (category !in names) category = names.firstOrNull().orEmpty()
                },
                track = MaterialTheme.colorScheme.surfaceContainer,
                thumb = typeColor,
                selectedText = Color.White,
            )
            // Big amount input that pops a little whenever the type changes.
            val amountColor by animateColorAsState(typeColor, label = "amountColor")
            val pop = remember { Animatable(1f) }
            LaunchedEffect(type) { pop.snapTo(0.85f); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f)) }
            Column(
                Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("จำนวนเงิน (บาท)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.displaySmall.copy(color = amountColor, textAlign = TextAlign.Center, fontSize = 40.sp),
                    cursorBrush = SolidColor(amountColor),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) Text("0.00", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                            color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = counterparty,
                onValueChange = { counterparty = it },
                label = { Text(if (type == TxType.INCOME) "รับจาก" else "จ่ายให้ / ร้าน") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("หมวดหมู่", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                (options + listOfNotNull(category.takeIf { it.isNotBlank() && it !in options }))
                    .forEach { c ->
                        val style = categoryStyle(c)
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) },
                            leadingIcon = { Icon(style.icon, null, tint = style.color, modifier = Modifier.size(18.dp)) },
                        )
                    }
            }
            if (initial.id != 0L && category != initial.category && counterparty.isNotBlank() && type != TxType.TRANSFER) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyToPayee, onCheckedChange = { applyToPayee = it })
                    Text(
                        "ใช้หมวดนี้กับ \"${counterparty.trim()}\" ทุกรายการ และจำไว้สำหรับสลิปต่อไป",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (wallets.isNotEmpty()) {
                Text(
                    when (type) {
                        TxType.INCOME -> "เข้ากระเป๋า"
                        TxType.EXPENSE -> "จ่ายจากกระเป๋า"
                        TxType.TRANSFER -> "จากกระเป๋า"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                WalletChips(wallets, walletId) { walletId = it }
                if (type == TxType.TRANSFER) {
                    Text("ไปกระเป๋า", style = MaterialTheme.typography.titleSmall)
                    WalletChips(wallets, toWalletId) { toWalletId = it }
                }
            }

            Text("แท็ก", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (allTags.map { it.name } + tags.filter { t -> allTags.none { it.name == t } }).distinct().forEach { t ->
                    FilterChip(
                        selected = t in tags,
                        onClick = { tags = if (t in tags) tags - t else tags + t },
                        label = { Text("#$t") },
                    )
                }
            }
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it.replace(",", "") },
                placeholder = { Text("เพิ่มแท็ก เช่น ทริปเชียงใหม่") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                trailingIcon = {
                    IconButton(enabled = newTag.isNotBlank(), onClick = { tags = tags + newTag.trim(); newTag = "" }) {
                        Icon(Icons.Default.Add, "เพิ่มแท็ก")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("บันทึกช่วยจำ") },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(Fmt.dateTime(timestamp), modifier = Modifier.weight(1f))
                TextButton(onClick = { pickDate = true }) { Text("เปลี่ยน") }
            }
            if (initial.bank.isNotBlank()) {
                Text(
                    "ธนาคาร: ${initial.bank}" + if (initial.account.isNotBlank()) " · ${initial.account}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            initial.imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "สลิป",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(MaterialTheme.shapes.small),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (initial.id != 0L) {
                    OutlinedButton(onClick = { if (confirmDelete) onDelete(initial) else confirmDelete = true }) {
                        Text(if (confirmDelete) "ยืนยันลบ" else "ลบ", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(
                    enabled = amountSatang != null,
                    onClick = {
                        val finalTags = if (newTag.isNotBlank()) tags + newTag.trim() else tags
                        onSave(
                            initial.copy(
                                type = type,
                                amountSatang = amountSatang ?: return@Button,
                                counterparty = counterparty.trim(),
                                category = category,
                                note = note.trim(),
                                timestamp = timestamp,
                                tags = Tags.join(finalTags),
                                walletId = walletId,
                                toWalletId = if (type == TxType.TRANSFER) toWalletId else null,
                            ),
                            applyToPayee,
                        )
                    },
                ) { Text("บันทึก") }
            }
        }
    }

    if (pickDate) {
        val zone = ZoneId.systemDefault()
        val local = Instant.ofEpochMilli(timestamp).atZone(zone)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        timestamp = date.atTime(local.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
                    }
                    pickDate = false
                }) { Text("ตกลง") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("ยกเลิก") } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletChips(wallets: List<com.tarit.slipledger.data.Wallet>, selected: Long?, onSelect: (Long?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        wallets.forEach { w ->
            FilterChip(
                selected = selected == w.id,
                onClick = { onSelect(if (selected == w.id) null else w.id) },
                label = { Text(w.name) },
                leadingIcon = { Icon(walletIcon(w.kind), null, tint = androidx.compose.ui.graphics.Color(w.color), modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

/** Blank transaction for manual entry. */
fun newManualTx(type: TxType, category: String, walletId: Long? = null) = Tx(
    type = type,
    amountSatang = 0,
    timestamp = System.currentTimeMillis(),
    category = category,
    source = TxSource.MANUAL,
    sourceKey = "manual:" + java.util.UUID.randomUUID(),
    walletId = walletId,
)
