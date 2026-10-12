package com.tarit.slipledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.TxType

private enum class FilterKind { TYPE, CATEGORY, WALLET, BANK, TAG, SORT }

@Composable
fun HistoryScreen(vm: MainViewModel, actions: AppActions) {
    val month by vm.month.collectAsStateWithLifecycle()
    val list by vm.history.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val banks by vm.banks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<FilterKind?>(null) }
    val searching = filter.query.isNotBlank()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBarSpace.current)) {
        item {
            ScreenHeader(
                "รายการ",
                trailing = { if (!searching) PeriodSwitcher(Fmt.monthShort(month), vm::shiftMonth) },
            )
        }
        item {
            SearchField(
                filter.query,
                { q -> vm.filter.value = filter.copy(query = q) },
                "ค้นหาชื่อ หมวด บันทึก แท็ก หรือยอดเงิน",
                Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { DropChip("ประเภท", filter.types.size, filter.types.isNotEmpty()) { sheet = FilterKind.TYPE } }
                item { DropChip("หมวด", filter.categories.size, filter.categories.isNotEmpty()) { sheet = FilterKind.CATEGORY } }
                if (wallets.isNotEmpty()) item { DropChip("กระเป๋า", filter.wallets.size, filter.wallets.isNotEmpty()) { sheet = FilterKind.WALLET } }
                if (banks.isNotEmpty()) item { DropChip("ธนาคาร", filter.banks.size, filter.banks.isNotEmpty()) { sheet = FilterKind.BANK } }
                if (tags.isNotEmpty()) item { DropChip("แท็ก", filter.tags.size, filter.tags.isNotEmpty()) { sheet = FilterKind.TAG } }
                item {
                    FilterChip(
                        selected = filter.sort != SortBy.NEWEST,
                        onClick = { sheet = FilterKind.SORT },
                        label = { Text(filter.sort.label) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp)) },
                    )
                }
                if (filter.hasFilters) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { vm.filter.value = TxFilter(query = filter.query, sort = filter.sort) },
                            label = { Text("ล้างตัวกรอง") },
                            leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }
        item {
            val s = Summary.of(list)
            Column(Modifier.padding(horizontal = 16.dp)) {
                FigurePill(
                    "${list.size}" to "รายการ",
                    "฿" + Fmt.bahtShort(animatedSatang(s.expense)) to "จ่าย",
                    "฿" + Fmt.bahtShort(animatedSatang(s.income)) to "รับ",
                )
                if (searching) {
                    Text("ค้นหาทุกเดือน", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp))
                }
            }
        }
        if (list.isEmpty()) {
            item { EmptyState(Icons.Outlined.SearchOff, "ไม่พบรายการ", "ลองเปลี่ยนคำค้นหาหรือตัวกรอง") }
        } else if (filter.sort == SortBy.NEWEST || filter.sort == SortBy.OLDEST) {
            list.groupBy { Fmt.dateOf(it.timestamp) }.forEach { (date, dayTxs) ->
                item(key = "d$date") { DayGroup(date, dayTxs, actions.edit) }
            }
        } else {
            item {
                SectionCard(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), padding = false) {
                    list.forEachIndexed { i, tx ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        TxRow(tx, showDate = true) { actions.edit(tx) }
                    }
                }
            }
        }
    }

    when (sheet) {
        FilterKind.TYPE -> MultiSelectSheet(
            "ประเภท",
            listOf(TxType.EXPENSE.name to "รายจ่าย", TxType.INCOME.name to "รายรับ", TxType.TRANSFER.name to "โอนตัวเอง"),
            filter.types.map { it.name }.toSet(),
            onDismiss = { sheet = null },
        ) { sel -> vm.filter.value = filter.copy(types = sel.map(TxType::valueOf).toSet()) }
        FilterKind.CATEGORY -> MultiSelectSheet(
            "หมวดหมู่",
            categories.map { it.name to it.name },
            filter.categories,
            onDismiss = { sheet = null },
            icon = { CategoryBadge(it, 24.dp) },
        ) { vm.filter.value = filter.copy(categories = it) }
        FilterKind.WALLET -> MultiSelectSheet(
            "กระเป๋าเงิน",
            wallets.map { it.id.toString() to it.name },
            filter.wallets.map { it.toString() }.toSet(),
            onDismiss = { sheet = null },
        ) { sel -> vm.filter.value = filter.copy(wallets = sel.map { it.toLong() }.toSet()) }
        FilterKind.BANK -> MultiSelectSheet("ธนาคาร", banks.map { it to it }, filter.banks, onDismiss = { sheet = null }) {
            vm.filter.value = filter.copy(banks = it)
        }
        FilterKind.TAG -> MultiSelectSheet("แท็ก", tags.map { it.name to "#${it.name}" }, filter.tags, onDismiss = { sheet = null }) {
            vm.filter.value = filter.copy(tags = it)
        }
        FilterKind.SORT -> MultiSelectSheet(
            "เรียงตาม",
            SortBy.entries.map { it.name to it.label },
            setOf(filter.sort.name),
            single = true,
            onDismiss = { sheet = null },
        ) { sel -> sel.firstOrNull()?.let { vm.filter.value = filter.copy(sort = SortBy.valueOf(it)) } }
        null -> Unit
    }
}

@Composable
private fun DropChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (count > 0) "$label ($count)" else label) },
        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultiSelectSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    single: Boolean = false,
    onDismiss: () -> Unit,
    icon: (@Composable (String) -> Unit)? = null,
    onApply: (Set<String>) -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!single && current.isNotEmpty()) TextButton(onClick = { current = emptySet() }) { Text("ล้าง") }
            }
            Spacer(Modifier.padding(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (key, label) ->
                    FilterChip(
                        selected = key in current,
                        onClick = {
                            current = when {
                                single -> setOf(key)
                                key in current -> current - key
                                else -> current + key
                            }
                            if (single) { onApply(current); onDismiss() }
                        },
                        label = { Text(label) },
                        leadingIcon = icon?.let { { it(key) } },
                    )
                }
            }
            if (!single) {
                Spacer(Modifier.padding(8.dp))
                Button(onClick = { onApply(current); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (current.isEmpty()) "แสดงทั้งหมด" else "ใช้ตัวกรอง (${current.size})")
                }
            }
        }
    }
}
