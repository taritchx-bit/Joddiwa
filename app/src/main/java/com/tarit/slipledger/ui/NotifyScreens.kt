package com.tarit.slipledger.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.notify.BankNotificationListener
import com.tarit.slipledger.notify.NotifSample
import com.tarit.slipledger.notify.NotifyStore
import com.tarit.slipledger.parse.NotifTemplate
import com.tarit.slipledger.parse.NotificationParser

/** Pick which apps' notifications are read, and teach the app how to read new kinds of alerts. */
@Composable
fun NotifyAppsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { NotifyStore.get(context) }
    val samples by store.samples.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val choices by store.choices.collectAsStateWithLifecycle()
    var listening by remember { mutableStateOf(BankNotificationListener.isEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { listening = BankNotificationListener.isEnabled(context) }
    var teaching by remember { mutableStateOf<String?>(null) }

    // Apps that have sent money alerts, were taught, or were switched on; built-in banks only if installed.
    val installed = remember { NotificationParser.builtInPackages.filter { appInstalled(context, it) } }
    val apps = (samples.map { it.pkg } + rules.map { it.pkg } + choices.keys + installed).distinct()
        .sortedWith(compareByDescending<String> { choices[it] ?: store.defaultOn(it) }
            .thenByDescending { pkg -> samples.filter { it.pkg == pkg }.maxOfOrNull { it.time } ?: 0L })

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        SubHeader("อ่านแจ้งเตือน", onBack)
        Column(
            Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 16.dp + LocalBarSpace.current),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!listening) {
                PressCard(
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    modifier = Modifier.appear(0),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text("ยังไม่ได้อนุญาตให้อ่านการแจ้งเตือน แตะเพื่อเปิด", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            Text(
                "เปิดแอพที่ต้องการให้อ่านยอดเงิน ถ้าแอพไหนอ่านไม่ได้ แตะ \"สอน\" แล้วชี้ว่ายอดเงินกับชื่อร้านอยู่ตรงไหน " +
                    "แอพจะจำรูปแบบไว้ใช้กับแจ้งเตือนต่อ ๆ ไป สอนเพิ่มได้เรื่อย ๆ ตัวอย่างเก็บในเครื่องเท่านั้น",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            if (apps.isEmpty()) {
                EmptyState(
                    Icons.Outlined.NotificationsNone, "ยังไม่เจอแจ้งเตือนที่มียอดเงิน",
                    "เมื่อแอพธนาคารหรือบัตรเครดิตส่งแจ้งเตือนมา จะขึ้นที่นี่ให้เลือกและสอน",
                )
            }
            apps.forEachIndexed { i, pkg ->
                val appSamples = samples.filter { it.pkg == pkg }
                val appRules = rules.filter { it.pkg == pkg }
                SectionCard(Modifier.appear(i + 1), padding = false) {
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(pkg, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(BankNotificationListener.appLabel(context, pkg), style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    "อ่านได้ในตัว".takeIf { pkg in NotificationParser.builtInPackages },
                                    "สอนแล้ว ${appRules.size} แบบ".takeIf { appRules.isNotEmpty() },
                                    "${appSamples.size} ตัวอย่าง".takeIf { appSamples.isNotEmpty() },
                                ).joinToString(" · ").ifEmpty { "ยังไม่มีแจ้งเตือนเรื่องเงิน" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = choices[pkg] ?: store.defaultOn(pkg), onCheckedChange = { store.setOn(pkg, it) })
                    }
                    appRules.forEach { rule ->
                        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (if (rule.type == TxType.INCOME) "รายรับ · " else "รายจ่าย · ") + rule.example.replace('\n', ' '),
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { store.deleteRule(rule.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, "ลบแบบที่สอน", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (appSamples.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 12.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.End) {
                            LimePill(if (appRules.isEmpty()) "สอน" else "สอนเพิ่ม", Icons.Default.School) { teaching = pkg }
                        }
                    } else Spacer(Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }

    teaching?.let { pkg ->
        TeachSheet(
            pkg = pkg,
            samples = samples.filter { it.pkg == pkg },
            vm = vm,
            onDismiss = { teaching = null },
            onSave = { pattern, type, walletId, example ->
                store.addRule(pkg, pattern, type, walletId, example)
                teaching = null
                vm.message.value = "สอนแล้ว แจ้งเตือนแบบนี้จะถูกจดให้อัตโนมัติ"
            },
        )
    }
}

/**
 * The user marks the amount (and optionally the shop) in one sample alert; the sheet shows live how the
 * resulting rule reads the app's other samples before it is saved.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TeachSheet(
    pkg: String,
    samples: List<NotifSample>,
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (pattern: String, type: TxType, walletId: Long?, example: String) -> Unit,
) {
    val context = LocalContext.current
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    var sampleIndex by remember { mutableIntStateOf(0) }
    val sample = samples.getOrNull(sampleIndex) ?: return
    val tokens = remember(sample) { NotifTemplate.tokenize(sample.text) }
    var mode by remember(sample) { mutableIntStateOf(0) }
    var amount by remember(sample) { mutableStateOf(tokens.indexOfLast { it.numeric && '.' in it.text }.takeIf { it >= 0 }) }
    var merchant by remember(sample) { mutableStateOf<IntRange?>(null) }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var walletId by remember { mutableStateOf<Long?>(null) }
    val pattern = amount?.let { NotifTemplate.build(tokens, it, merchant) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("สอนให้อ่านแจ้งเตือน", style = MaterialTheme.typography.titleLarge)
                    Text(BankNotificationListener.appLabel(context, pkg), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (samples.size > 1) {
                Text("1. เลือกแจ้งเตือนตัวอย่าง", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    samples.forEachIndexed { i, s ->
                        val selected = i == sampleIndex
                        Text(
                            s.text.replace('\n', ' '),
                            style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { sampleIndex = i }.padding(10.dp),
                        )
                    }
                }
            }

            Text("${if (samples.size > 1) "2" else "1"}. แตะบอกว่าอะไรคืออะไร", style = MaterialTheme.typography.titleSmall)
            PillTabs(listOf("ยอดเงิน", "ชื่อร้าน (ถ้ามี)"), mode, onSelect = { mode = it },
                track = MaterialTheme.colorScheme.surfaceContainer)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tokens.forEachIndexed { i, t ->
                    val isAmount = i == amount
                    val inMerchant = merchant?.contains(i) == true
                    val enabled = if (mode == 0) t.numeric else true
                    Text(
                        t.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isAmount -> InkBottom
                            inMerchant -> MaterialTheme.colorScheme.onSecondaryContainer
                            enabled -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isAmount -> Lime
                                    inMerchant -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> Color.Transparent
                                },
                            )
                            .border(1.dp, if (enabled && !isAmount && !inMerchant) MaterialTheme.colorScheme.outline else Color.Transparent,
                                RoundedCornerShape(8.dp))
                            .clickable(enabled = enabled) {
                                if (mode == 0) {
                                    amount = i
                                    if (merchant?.contains(i) == true) merchant = null
                                } else {
                                    val m = merchant
                                    merchant = when {
                                        i == amount -> m
                                        m == null -> i..i
                                        i in m -> null
                                        else -> minOf(m.first, i)..maxOf(m.last, i)
                                    }
                                    // A shop name never spans the amount.
                                    if (amount != null && merchant?.contains(amount!!) == true) merchant = i..i
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Text("${if (samples.size > 1) "3" else "2"}. เป็นเงินเข้าหรือออก", style = MaterialTheme.typography.titleSmall)
            val types = listOf(TxType.EXPENSE to "รายจ่าย", TxType.INCOME to "รายรับ")
            PillTabs(
                types.map { it.second }, types.indexOfFirst { it.first == type }, onSelect = { type = types[it].first },
                track = MaterialTheme.colorScheme.surfaceContainer,
                thumb = if (type == TxType.INCOME) IncomeColor else ExpenseColor, selectedText = Color.White,
            )
            if (wallets.isNotEmpty()) {
                Text("กระเป๋า (ไม่บังคับ)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = walletId == w.id,
                            onClick = { walletId = if (walletId == w.id) null else w.id },
                            label = { Text(w.name) },
                            leadingIcon = { Icon(walletIcon(w.kind), null, tint = Color(w.color), modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            // Live check against everything seen from this app.
            if (pattern != null) {
                val results = samples.map { it to NotifTemplate.match(pattern, it.text) }
                Text("ทดสอบกับตัวอย่าง: อ่านได้ ${results.count { it.second != null }} จาก ${results.size}",
                    style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    results.forEach { (s, m) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (m != null) Icons.Default.Check else Icons.Default.Close, null,
                                tint = if (m != null) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (m != null) "฿${Fmt.baht(m.amountSatang)}" + (if (m.merchant.isNotBlank()) " · ${m.merchant}" else "")
                                else "ไม่ตรงแบบ: " + s.text.replace('\n', ' '),
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (m != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("ยกเลิก") }
                Button(enabled = pattern != null, onClick = { onSave(pattern!!, type, walletId, sample.text) }) { Text("บันทึกการสอน") }
            }
        }
    }
}

@Composable
private fun AppIcon(pkg: String, size: Dp) {
    val context = LocalContext.current
    val bitmap = remember(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) Image(bitmap, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)))
    else IconBadge(Icons.Default.NotificationsActive, MaterialTheme.colorScheme.primary, size)
}

private fun appInstalled(context: android.content.Context, pkg: String) =
    runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.isSuccess
