package com.tarit.slipledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.CategoryDef
import com.tarit.slipledger.data.RecurringRule
import com.tarit.slipledger.data.TxType

@Composable
private fun SubScreenScaffold(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        SubHeader(title, onBack, action)
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 16.dp + LocalBarSpace.current), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun ReorderButtons(onUp: (() -> Unit)?, onDown: (() -> Unit)?) {
    IconButton(onClick = { onUp?.invoke() }, enabled = onUp != null, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.KeyboardArrowUp, "เลื่อนขึ้น")
    }
    IconButton(onClick = { onDown?.invoke() }, enabled = onDown != null, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.KeyboardArrowDown, "เลื่อนลง")
    }
}

// ---------------- Categories ----------------

@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val all by vm.categories.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<CategoryDef?>(null) }

    SubScreenScaffold("หมวดหมู่", onBack, action = {
        LimePill("เพิ่ม", Icons.Default.Add) {
            editing = CategoryDef(name = "", kind = kind, icon = "more", color = CategoryIcons.palette.first(), sortOrder = 0)
        }
    }) {
        val kinds = listOf(TxType.EXPENSE to "รายจ่าย", TxType.INCOME to "รายรับ")
        PillTabs(kinds.map { it.second }, kinds.indexOfFirst { it.first == kind }, onSelect = { kind = kinds[it].first },
            modifier = Modifier.appear(0))
        // The list slides sideways when switching between expense and income categories.
        AnimatedContent(
            kind,
            transitionSpec = {
                val dir = if (targetState == TxType.INCOME) 1 else -1
                (slideInHorizontally { it / 3 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 * dir } + fadeOut())
            },
            label = "kind",
            modifier = Modifier.appear(1),
        ) { shownKind ->
        val list = all.filter { it.kind == shownKind }.sortedBy { it.sortOrder }
        SectionCard(padding = false) {
            list.forEachIndexed { i, def ->
                if (i > 0) MenuDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { editing = def }.padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(def.name, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(def.name, style = MaterialTheme.typography.bodyLarge)
                        if (!def.builtIn) Text("สร้างเอง", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    ReorderButtons(
                        onUp = if (i > 0) ({ vm.moveCategory(def, -1) }) else null,
                        onDown = if (i < list.lastIndex) ({ vm.moveCategory(def, 1) }) else null,
                    )
                }
            }
        }
        }
        Text(
            "หมวดมาตรฐานเปลี่ยนไอคอนและสีได้ แต่เปลี่ยนชื่อหรือลบไม่ได้ เพราะใช้จัดหมวดสลิปอัตโนมัติ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    editing?.let { def ->
        CategoryEditSheet(
            def = def,
            existingNames = all.map { it.name }.toSet() - def.name,
            onDismiss = { editing = null },
            onSave = { updated ->
                if (def.id == 0L) vm.addCategory(updated) else vm.updateCategory(def, updated)
                editing = null
            },
            onDelete = { vm.deleteCategory(def); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditSheet(
    def: CategoryDef,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (CategoryDef) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(def.name) }
    var icon by remember { mutableStateOf(def.icon) }
    var color by remember { mutableStateOf(def.color) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nameError = when {
        name.isBlank() -> "ใส่ชื่อหมวด"
        name.trim() in existingNames -> "มีหมวดชื่อนี้แล้ว"
        else -> null
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(CategoryIcons.of(icon), Color(color), 56.dp)
                Spacer(Modifier.width(14.dp))
                Text(if (def.id == 0L) "หมวดใหม่" else "แก้ไขหมวด", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ชื่อหมวด") },
                enabled = !def.builtIn,
                isError = !def.builtIn && nameError != null && name.isNotEmpty(),
                supportingText = { if (!def.builtIn && name.isNotEmpty()) nameError?.let { Text(it) } },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("ไอคอน", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcons.all.forEach { (key, vector) ->
                    val selected = key == icon
                    Box(
                        Modifier.size(46.dp).clip(MaterialTheme.shapes.small)
                            .background(if (selected) Color(color).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer)
                            .then(if (selected) Modifier.border(2.dp, Color(color), MaterialTheme.shapes.small) else Modifier)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) { Icon(vector, key, tint = if (selected) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text("สี", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryIcons.palette.forEach { c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { color = c },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (def.id != 0L && !def.builtIn) {
                    OutlinedButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "ยืนยันลบ" else "ลบ", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(
                    enabled = def.builtIn || nameError == null,
                    onClick = { onSave(def.copy(name = if (def.builtIn) def.name else name.trim(), icon = icon, color = color)) },
                ) { Text("บันทึก") }
            }
            if (confirmDelete) {
                Text("รายการในหมวดนี้จะย้ายไปหมวด \"อื่นๆ\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------------- Tags ----------------

@Composable
fun TagsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    var newTag by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<String?>(null) }

    SubScreenScaffold("แท็ก", onBack) {
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it.replace(",", "") },
            placeholder = { Text("แท็กใหม่ เช่น ทริปเชียงใหม่") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            trailingIcon = {
                IconButton(enabled = newTag.isNotBlank(), onClick = { vm.addTag(newTag); newTag = "" }) { Icon(Icons.Default.Add, "เพิ่ม") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (tags.isEmpty()) {
            EmptyState(Icons.Outlined.Sell, "ยังไม่มีแท็ก", "แท็กช่วยรวมรายการข้ามหมวด เช่น ทุกค่าใช้จ่ายของทริปเดียวกัน แล้วดูยอดรวมได้ที่หน้าสรุป")
        } else {
            SectionCard(Modifier.appear(1), padding = false) {
                tags.forEachIndexed { i, tag ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TagChip(tag.name)
                        Spacer(Modifier.weight(1f))
                        ReorderButtons(
                            onUp = if (i > 0) ({ vm.moveTag(tag, -1) }) else null,
                            onDown = if (i < tags.lastIndex) ({ vm.moveTag(tag, 1) }) else null,
                        )
                        IconButton(onClick = {
                            if (confirm == tag.name) { vm.deleteTag(tag); confirm = null } else confirm = tag.name
                        }) {
                            Icon(Icons.Default.Delete, "ลบ", tint = if (confirm == tag.name) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (confirm != null) {
                Text("แตะถังขยะอีกครั้งเพื่อลบ #$confirm (แท็กจะถูกเอาออกจากทุกรายการ)",
                    style = MaterialTheme.typography.bodySmall, color = ExpenseColor)
            }
        }
    }
}

// ---------------- Recurring ----------------

@Composable
fun RecurringScreen(vm: MainViewModel, onBack: () -> Unit) {
    val rules by vm.recurring.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    val defaultCategory = categories.firstOrNull { it.kind == TxType.EXPENSE }?.name.orEmpty()

    SubScreenScaffold("รายการประจำ", onBack, action = {
        LimePill("เพิ่ม", Icons.Default.Add) {
            editing = RecurringRule(type = TxType.EXPENSE, amountSatang = 0, category = defaultCategory, counterparty = "", dayOfMonth = 1)
        }
    }) {
        if (rules.isEmpty()) {
            EmptyState(Icons.Outlined.EventRepeat, "ยังไม่มีรายการประจำ", "เพิ่มค่าใช้จ่ายที่จ่ายทุกเดือน เช่น ค่าหอ ค่าโทรศัพท์ หรือรายรับอย่างเงินเดือน แอพจะจดให้เองตามวันที่กำหนด")
        } else {
            SectionCard(Modifier.appear(0), padding = false) {
                rules.forEachIndexed { i, rule ->
                    if (i > 0) MenuDivider()
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = rule }.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryBadge(rule.category, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rule.counterparty.ifBlank { rule.category }, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "ทุกวันที่ ${rule.dayOfMonth} · " + (if (rule.type == TxType.INCOME) "+" else "-") + "฿${Fmt.bahtShort(rule.amountSatang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = rule.active, onCheckedChange = { on ->
                            // Re-enabling must not back-fill the months it was paused.
                            val prev = java.time.YearMonth.now().minusMonths(1).toString()
                            val lastRun = if (on && rule.lastRun.isNotEmpty() && rule.lastRun < prev) prev else rule.lastRun
                            vm.saveRecurring(rule.copy(active = on, lastRun = lastRun))
                        })
                    }
                }
            }
        }
    }

    editing?.let { rule ->
        RecurringEditSheet(
            rule = rule,
            onDismiss = { editing = null },
            onSave = { vm.saveRecurring(it); editing = null },
            onDelete = { vm.deleteRecurring(rule); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringEditSheet(rule: RecurringRule, onDismiss: () -> Unit, onSave: (RecurringRule) -> Unit, onDelete: () -> Unit) {
    var type by remember { mutableStateOf(rule.type) }
    var amount by remember { mutableStateOf(if (rule.amountSatang > 0) Fmt.bahtShort(rule.amountSatang).replace(",", "") else "") }
    var name by remember { mutableStateOf(rule.counterparty) }
    var category by remember { mutableStateOf(rule.category) }
    var day by remember { mutableStateOf(rule.dayOfMonth) }
    var note by remember { mutableStateOf(rule.note) }
    val optionsByType = listOf(TxType.EXPENSE, TxType.INCOME).associateWith { categoryNamesFor(it) }
    val satang = Fmt.parseSatang(amount)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (rule.id == 0L) "รายการประจำใหม่" else "แก้ไขรายการประจำ", style = MaterialTheme.typography.titleLarge)
            val types = listOf(TxType.EXPENSE to "รายจ่าย", TxType.INCOME to "รายรับ")
            PillTabs(
                types.map { it.second }, types.indexOfFirst { it.first == type },
                onSelect = { i ->
                    type = types[i].first
                    val names = optionsByType.getValue(type)
                    if (category !in names) category = names.firstOrNull().orEmpty()
                },
                track = MaterialTheme.colorScheme.surfaceContainer,
                thumb = if (type == TxType.INCOME) IncomeColor else ExpenseColor,
                selectedText = Color.White,
            )
            OutlinedTextField(name, { name = it }, label = { Text("ชื่อ เช่น ค่าหอ, Netflix, เงินเดือน") }, singleLine = true,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { v -> amount = v.filter { it.isDigit() || it == '.' } }, label = { Text("จำนวนเงิน (บาท)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Text("จดทุกวันที่", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..28).forEach { d -> FilterChip(selected = day == d, onClick = { day = d }, label = { Text("$d") }) }
            }
            Text("หมวดหมู่", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                optionsByType.getValue(type).forEach { c ->
                    val style = categoryStyle(c)
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) },
                        leadingIcon = { Icon(style.icon, null, tint = style.color, modifier = Modifier.size(18.dp)) })
                }
            }
            OutlinedTextField(note, { note = it }, label = { Text("บันทึกช่วยจำ") }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rule.id != 0L) OutlinedButton(onClick = onDelete) { Text("ลบ", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(enabled = satang != null, onClick = {
                    onSave(rule.copy(type = type, amountSatang = satang ?: return@Button, counterparty = name.trim(),
                        category = category, dayOfMonth = day, note = note.trim()))
                }) { Text("บันทึก") }
            }
            Text(
                "ถ้าวันที่ $day ของเดือนนี้ผ่านมาแล้ว แอพจะจดรายการของเดือนนี้ให้ทันที",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
