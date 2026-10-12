package com.tarit.slipledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.data.Wallet
import com.tarit.slipledger.data.WalletKind
import com.tarit.slipledger.parse.Banks
import kotlin.math.abs

fun walletSubtitle(w: Wallet): String = when (w.kind) {
    WalletKind.CASH -> "เงินสด"
    else -> listOf(w.bank, w.accountHint.takeIf { it.isNotEmpty() }?.let { "··$it" }).filterNotNull().filter { it.isNotEmpty() }.joinToString(" ")
}

fun money(satang: Long) = (if (satang < 0) "-฿" else "฿") + Fmt.baht(abs(satang))

/** Wallets counted in the total balance: ticked by the user and with a balance entered. */
fun countedWallets(wallets: List<Wallet>, excluded: Set<Long>) = wallets.filter { it.balanceSet && it.id !in excluded }

fun walletTotal(wallets: List<Wallet>, excluded: Set<Long>, balances: Map<Long, Long>) =
    countedWallets(wallets, excluded).sumOf { balances[it.id] ?: 0 }

// ---------------- Wallets ----------------

@Composable
fun WalletsScreen(vm: MainViewModel, onBack: () -> Unit, onOpenWallet: (Long) -> Unit) {
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Wallet?>(null) }
    val counted = countedWallets(wallets, settings.excludedWallets)
    val total = walletTotal(wallets, settings.excludedWallets, balances)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        SubHeader("กระเป๋าเงิน", onBack) {
            LimePill("เพิ่ม", Icons.Default.Add) {
                editing = Wallet(name = "", kind = WalletKind.BANK, color = CategoryIcons.palette[5], balanceSet = true)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp + LocalBarSpace.current, top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InkPanel(Modifier.appear(0)) {
                Column(Modifier.padding(20.dp)) {
                    Text("ยอดเงินทั้งหมด", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                    Text(money(animatedSatang(total)), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("ติ๊กช่องหน้ากระเป๋าเพื่อเลือกว่าจะรวมอันไหน", color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            FigurePill(
                "${wallets.size}" to "กระเป๋า",
                "${counted.size}" to "ที่รวมยอด",
                modifier = Modifier.appear(1),
            )
            wallets.forEachIndexed { i, w ->
                PressCard(onClick = { editing = w }, modifier = Modifier.appear(i + 2)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // A wallet without a balance cannot be added up yet, so its box stays off until one is entered.
                        Checkbox(
                            checked = w.balanceSet && w.id !in settings.excludedWallets,
                            onCheckedChange = { on -> vm.setWalletCounted(w.id, on) },
                            enabled = w.balanceSet,
                        )
                        WalletBadge(w)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(walletSubtitle(w), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (w.balanceSet) {
                            Text(money(balances[w.id] ?: 0), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(CircleShape).clickable { onOpenWallet(w.id) }.padding(4.dp))
                        } else {
                            Text("ตั้งยอด", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Column {
                            IconButton(onClick = { vm.moveWallet(w, -1) }, enabled = i > 0, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "เลื่อนขึ้น")
                            }
                            IconButton(onClick = { vm.moveWallet(w, 1) }, enabled = i < wallets.lastIndex, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "เลื่อนลง")
                            }
                        }
                    }
                }
            }
            Text(
                "สลิปและแจ้งเตือนจะถูกหักหรือเพิ่มเข้ากระเป๋าที่ธนาคารและเลขท้ายบัญชี 4 หลักตรงกัน " +
                    "ถ้าเจอบัญชีใหม่ แอพจะสร้างกระเป๋าให้เอง แตะ \"ตั้งยอด\" เพื่อใส่ยอดเงินตอนนี้",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    editing?.let { w ->
        WalletEditSheet(
            wallet = w,
            currentBalance = balances[w.id],
            onDismiss = { editing = null },
            onSave = { updated, newBalance ->
                val saved = if (newBalance != null) updated.copy(openingBalance = newBalance, openingDate = System.currentTimeMillis(), balanceSet = true) else updated
                vm.saveWallet(saved)
                editing = null
            },
            onDelete = { vm.deleteWallet(w); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WalletEditSheet(
    wallet: Wallet,
    currentBalance: Long?,
    onDismiss: () -> Unit,
    /** Second argument: the balance the user typed, or null if unchanged. */
    onSave: (Wallet, Long?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(wallet.name) }
    var kind by remember { mutableStateOf(wallet.kind) }
    var bank by remember { mutableStateOf(wallet.bank) }
    var hint by remember { mutableStateOf(wallet.accountHint) }
    var color by remember { mutableStateOf(wallet.color) }
    val initialBalance = if (wallet.balanceSet && wallet.id != 0L) currentBalance?.let { Fmt.bahtShort(it).replace(",", "") }.orEmpty() else ""
    var balance by remember { mutableStateOf(initialBalance) }
    var confirmDelete by remember { mutableStateOf(false) }
    val parsedBalance = balance.trim().let { b -> if (b.isEmpty()) null else b.replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(walletIcon(kind), Color(color), 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(if (wallet.id == 0L) "กระเป๋าใหม่" else "แก้ไขกระเป๋า", style = MaterialTheme.typography.titleLarge)
            }
            val kinds = listOf(WalletKind.BANK to "ธนาคาร", WalletKind.CASH to "เงินสด", WalletKind.EWALLET to "e-Wallet")
            PillTabs(kinds.map { it.second }, kinds.indexOfFirst { it.first == kind }, onSelect = { kind = kinds[it].first },
                track = MaterialTheme.colorScheme.surfaceContainer)
            if (kind != WalletKind.CASH) {
                Text("ธนาคาร / ผู้ให้บริการ", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Banks.all.forEach { b ->
                        FilterChip(selected = bank == b, onClick = {
                            bank = b
                            color = Banks.color(b)
                            if (name.isBlank() || Banks.all.any { name.startsWith(it) || name.startsWith(Banks.short(it)) }) {
                                name = Banks.short(b) + if (hint.isNotEmpty()) " ··$hint" else ""
                            }
                        }, label = { Text(b) })
                    }
                }
                OutlinedTextField(
                    value = hint,
                    onValueChange = { v -> hint = v.filter { it.isDigit() }.takeLast(4) },
                    label = { Text("เลขท้ายบัญชี 4 หลัก") },
                    supportingText = { Text("ใช้จับคู่สลิปกับกระเป๋านี้ เช่น บัญชี xxx-x-x1459-x ใส่ 1459") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(name, { name = it }, label = { Text("ชื่อกระเป๋า") }, singleLine = true,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = balance,
                onValueChange = { v -> balance = v.filter { it.isDigit() || it == '.' || it == '-' } },
                label = { Text("ยอดเงินตอนนี้ (บาท)") },
                supportingText = { Text("รายการหลังจากนี้จะบวก/หักจากยอดนี้") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("สี", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (listOf(Banks.color(bank)) + CategoryIcons.palette).distinct().forEach { c ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                            .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { color = c },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (wallet.id != 0L) {
                    OutlinedButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "ยืนยันลบ" else "ลบ", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(enabled = name.isNotBlank(), onClick = {
                    val updated = wallet.copy(
                        name = name.trim(), kind = kind,
                        bank = if (kind == WalletKind.CASH) "" else bank,
                        accountHint = if (kind == WalletKind.BANK) hint else "",
                        color = color,
                        openingDate = if (wallet.id == 0L) System.currentTimeMillis() else wallet.openingDate,
                        balanceSet = wallet.balanceSet || parsedBalance != null,
                    )
                    val changedBalance = parsedBalance.takeIf { balance != initialBalance || wallet.id == 0L } ?: if (wallet.id == 0L) 0L else null
                    onSave(updated, changedBalance)
                }) { Text("บันทึก") }
            }
            if (confirmDelete) {
                Text("รายการเดิมจะยังอยู่ แต่จะไม่ผูกกับกระเป๋าใด", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------------- Review inbox ----------------

/** Why an imported transaction landed in the review inbox. */
private fun reviewReasons(tx: Tx): List<String> = buildList {
    if (tx.source == TxSource.NOTIFICATION && tx.type == TxType.EXPENSE) add("เงินออกจากแจ้งเตือน ยังไม่รู้ว่าจ่ายให้ใคร")
    if (tx.counterparty.isBlank()) add("อ่านชื่อผู้รับไม่ได้")
    if (tx.walletId == null) add("ยังไม่ระบุกระเป๋า")
    if (isEmpty()) add("วันที่หรือข้อมูลบางส่วนอาจไม่ถูกต้อง")
}

@Composable
fun ReviewScreen(vm: MainViewModel, onBack: () -> Unit, onEdit: (Tx) -> Unit) {
    val list by vm.needsReview.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp + LocalBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SubHeader("รอตรวจ", onBack) {
                if (list.isNotEmpty()) LimePill("ถูกหมด", Icons.Default.DoneAll) { vm.markReviewed(list.map { it.id }) }
            }
        }
        item {
            if (list.isEmpty()) {
                EmptyState(Icons.Outlined.TaskAlt, "ตรวจครบแล้ว", "รายการที่อ่านจากสลิปหรือแจ้งเตือนได้ไม่ครบจะมารออยู่ที่นี่")
            } else {
                Text("แตะรายการเพื่อแก้ไข หรือกด \"ถูกต้อง\" ถ้าข้อมูลถูกแล้ว",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp))
            }
        }
        // Confirmed items slide away and the rest close the gap.
        itemsIndexed(list, key = { _, tx -> tx.id }) { i, tx ->
                PressCard(onClick = { onEdit(tx) }, modifier = Modifier.padding(horizontal = 16.dp).appear(i).animateItem()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        if (tx.imageUri != null) {
                            AsyncImage(tx.imageUri, "สลิป", contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 56.dp, height = 84.dp).clip(MaterialTheme.shapes.small))
                        } else CategoryBadge(tx.category)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(tx.counterparty.ifBlank { "ไม่ทราบชื่อ" }, style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text((if (tx.type == TxType.INCOME) "+" else "-") + Fmt.baht(tx.amountSatang), style = MaterialTheme.typography.titleSmall)
                            }
                            Text("${Fmt.dateTime(tx.timestamp)} · ${tx.category}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            reviewReasons(tx).forEach {
                                Text("• $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEdit(tx) }) { Text("แก้ไข") }
                                TextButton(onClick = { vm.markReviewed(listOf(tx.id)) }) { Text("ถูกต้อง") }
                            }
                        }
                    }
                }
        }
    }
}
