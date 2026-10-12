package com.tarit.slipledger.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.ThemeMode
import com.tarit.slipledger.notify.BankNotificationListener
import java.time.LocalDate
import java.util.concurrent.TimeUnit

enum class SubScreen { CATEGORIES, TAGS, RECURRING, WALLETS, REVIEW, NOTIFY_APPS }

private enum class SettingsDialog { NAMES, MONTH_START, THEME, SCAN_RANGE, RESET }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel, actions: AppActions, onOpen: (SubScreen) -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val recurring by vm.recurring.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val review by vm.needsReview.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var notifEnabled by remember { mutableStateOf(BankNotificationListener.isEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notifEnabled = BankNotificationListener.isEnabled(context) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }

    val darkTheme = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scanRanges =listOf(7L to "7 วัน", 30L to "30 วัน", 90L to "90 วัน", 365L to "1 ปี", 0L to "ทั้งหมด")
    val scanLabel = if (settings.scanFrom == 0L) "ทุกรูปในเครื่อง"
    else "รูปตั้งแต่ ${Fmt.day(Fmt.dateOf(settings.scanFrom))}"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("ตั้งค่า")
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp + LocalBarSpace.current), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupTitle("ทั่วไป")
            SectionCard(Modifier.appear(1), padding = false) {
                MenuRow(
                    Icons.Default.Person, Color(0xFF6172F3), "ชื่อของคุณ",
                    settings.ownerNames.joinToString(", ").ifEmpty { "ใช้แยกเงินเข้า/ออก/โอนให้ตัวเอง" },
                    onClick = { dialog = SettingsDialog.NAMES },
                )
                MenuDivider()
                MenuRow(
                    Icons.Default.DateRange, Color(0xFF06AED4), "วันเริ่มรอบเดือน",
                    if (settings.monthStartDay == 1) "วันที่ 1 ของทุกเดือน" else "วันที่ ${settings.monthStartDay} ของทุกเดือน (เช่น วันเงินเดือนออก)",
                    onClick = { dialog = SettingsDialog.MONTH_START },
                )
                MenuDivider()
                MenuRow(
                    Icons.Default.DarkMode, Color(0xFF475467), "ธีม",
                    when (settings.theme) { ThemeMode.SYSTEM -> "ตามระบบ"; ThemeMode.LIGHT -> "สว่าง"; ThemeMode.DARK -> "มืด" },
                    onClick = { dialog = SettingsDialog.THEME },
                    trailing = {
                        DayNightSwitch(checked = darkTheme, onCheckedChange = { on ->
                            vm.updateSettings { it.copy(theme = if (on) ThemeMode.DARK else ThemeMode.LIGHT) }
                        })
                    },
                )
            }

            GroupTitle("การจด")
            SectionCard(Modifier.appear(2), padding = false) {
                MenuRow(Icons.Default.AccountBalanceWallet, Color(0xFF0FA86E), "กระเป๋าเงิน",
                    "${wallets.size} กระเป๋า · ธนาคาร เงินสด e-Wallet", onClick = { onOpen(SubScreen.WALLETS) })
                MenuDivider()
                MenuRow(Icons.Default.FactCheck, Color(0xFFF79009), "รอตรวจ",
                    if (review.isEmpty()) "ไม่มีรายการรอตรวจ" else "${review.size} รายการ", onClick = { onOpen(SubScreen.REVIEW) })
                MenuDivider()
                MenuRow(Icons.Default.Category, Color(0xFFFF8A5B), "หมวดหมู่", "${categories.size} หมวด · สร้าง แก้ไข จัดลำดับ",
                    onClick = { onOpen(SubScreen.CATEGORIES) })
                MenuDivider()
                MenuRow(Icons.Default.Sell, Color(0xFFE5579B), "แท็ก", if (tags.isEmpty()) "ติดป้ายให้รายการ เช่น ทริป งานแต่ง" else "${tags.size} แท็ก",
                    onClick = { onOpen(SubScreen.TAGS) })
                MenuDivider()
                MenuRow(Icons.Default.EventRepeat, Color(0xFF9E77ED), "รายการประจำ",
                    if (recurring.isEmpty()) "จดค่าใช้จ่ายรายเดือนให้อัตโนมัติ" else "${recurring.count { it.active }} รายการที่เปิดอยู่",
                    onClick = { onOpen(SubScreen.RECURRING) })
                MenuDivider()
                MenuRow(Icons.Default.Savings, Color(0xFF12B76A), "งบประมาณ",
                    when {
                        settings.monthlyBudget > 0 -> "งบเดือนละ ฿${Fmt.bahtShort(settings.monthlyBudget)}" +
                            if (settings.categoryBudgets.isNotEmpty()) " · แยก ${settings.categoryBudgets.size} หมวด" else ""
                        settings.categoryBudgets.isNotEmpty() -> "ตั้งงบแยก ${settings.categoryBudgets.size} หมวด"
                        else -> "ตั้งงบรายเดือนหรือแยกตามหมวด"
                    },
                    onClick = actions.editBudget)
            }

            GroupTitle("สแกนสลิป")
            SectionCard(Modifier.appear(3), padding = false) {
                MenuRow(Icons.Default.DocumentScanner, Color(0xFF0FA86E), "ช่วงรูปที่จะสแกน", scanLabel,
                    onClick = { dialog = SettingsDialog.SCAN_RANGE })
                MenuDivider()
                MenuRow(
                    Icons.Default.Autorenew, Color(0xFF3E8EF7), "สแกนอัตโนมัติ", "ตรวจหาสลิปใหม่ทุก 6 ชั่วโมง",
                    trailing = { Switch(checked = settings.autoScan, onCheckedChange = { on -> vm.updateSettings { it.copy(autoScan = on) } }) },
                )
                MenuDivider()
                MenuRow(Icons.Default.History, Color(0xFF667085), "อ่านรูปใหม่ทั้งหมด", "ใช้เมื่อเพิ่มชื่อ หรืออยากให้จัดหมวดใหม่",
                    onClick = vm::resetScanHistory)
            }

            GroupTitle("แจ้งเตือน")
            SectionCard(Modifier.appear(4), padding = false) {
                MenuRow(
                    Icons.Default.NotificationsActive, Color(0xFFF79009), "เงินเข้าจาก LINE ธนาคาร",
                    if (notifEnabled) "เปิดอยู่ · Krungthai Connext, SCB Connect, K PLUS ฯลฯ" else "ปิดอยู่ · แตะเพื่อเปิด",
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                )
                MenuDivider()
                MenuRow(
                    Icons.Default.School, Color(0xFF9E77ED), "เลือกแอพและสอนการอ่าน",
                    "บัตรเครดิต e-Wallet หรือแอพอื่นที่แจ้งยอดเงิน",
                    onClick = { onOpen(SubScreen.NOTIFY_APPS) },
                )
                MenuDivider()
                MenuRow(
                    Icons.Default.BatteryChargingFull, Color(0xFF15B79E), "แบตเตอรี่และการทำงานเบื้องหลัง",
                    "มือถือ OPPO/realme/Xiaomi ให้ตั้งเป็น \"ไม่จำกัด\"",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    },
                )
            }

            GroupTitle("ความปลอดภัย")
            SectionCard(Modifier.appear(5), padding = false) {
                MenuRow(
                    Icons.Default.Fingerprint, Color(0xFF6172F3), "ล็อกแอพ",
                    "ใช้ลายนิ้วมือ ใบหน้า หรือรหัสเครื่องเมื่อเปิดแอพ",
                    trailing = {
                        Switch(checked = settings.appLock, onCheckedChange = { on ->
                            if (on && !AppLock.available(context)) {
                                vm.message.value = "ตั้งรหัสล็อกหน้าจอหรือสแกนนิ้วในมือถือก่อน"
                            } else vm.updateSettings { it.copy(appLock = on) }
                        })
                    },
                )
            }


            GroupTitle("ข้อมูล")
            SectionCard(Modifier.appear(6), padding = false) {
                MenuRow(Icons.Default.FileDownload, Color(0xFF6172F3), "ส่งออกเป็น CSV", "เปิดใน Excel หรือ Google Sheets ได้",
                    onClick = { exportLauncher.launch("joddiwa-${LocalDate.now()}.csv") })
                MenuDivider()
                MenuRow(Icons.Default.DeleteForever, ExpenseColor, "ล้างข้อมูลทั้งหมด", "ลบทุกรายการ เริ่มจดใหม่",
                    onClick = { dialog = SettingsDialog.RESET })
            }
            Text(
                "Joddiwa by Teen · ข้อมูลทั้งหมดอยู่ในมือถือของคุณเท่านั้น",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, start = 6.dp),
            )
        }
    }

    when (dialog) {
        SettingsDialog.NAMES -> {
            var names by remember { mutableStateOf(settings.ownerNames.joinToString(", ")) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("ชื่อของคุณ") },
                text = {
                    Column {
                        Text("ใส่ได้หลายชื่อ คั่นด้วยจุลภาค ใช้แยกว่าสลิปเป็นเงินออก เงินเข้า หรือโอนระหว่างบัญชีตัวเอง",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(names, { names = it }, placeholder = { Text("เช่น ธฤต, TARIT") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateSettings { s -> s.copy(ownerNames = names.split(',').map { it.trim() }.filter { it.isNotEmpty() }) }
                        dialog = null
                    }) { Text("บันทึก") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("ยกเลิก") } },
            )
        }
        SettingsDialog.MONTH_START -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("วันเริ่มรอบเดือน") },
            text = {
                Column {
                    Text("เช่น ถ้าเงินเดือนออกวันที่ 25 ให้เลือก 25 แล้วสรุปเดือน ต.ค. จะนับ 25 ก.ย. – 24 ต.ค.",
                        style = MaterialTheme.typography.bodySmall)
                    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..28).forEach { d ->
                            FilterChip(
                                selected = settings.monthStartDay == d,
                                onClick = { vm.updateSettings { it.copy(monthStartDay = d) }; dialog = null },
                                label = { Text("$d") },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("ปิด") } },
        )
        SettingsDialog.THEME -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("ธีม") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to "ตามระบบ", ThemeMode.LIGHT to "สว่าง", ThemeMode.DARK to "มืด").forEach { (m, label) ->
                        FilterChip(
                            selected = settings.theme == m,
                            onClick = { vm.updateSettings { it.copy(theme = m) }; dialog = null },
                            label = { Text(label) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("ปิด") } },
        )
        SettingsDialog.SCAN_RANGE -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("ช่วงรูปที่จะสแกน") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val now = System.currentTimeMillis()
                    scanRanges.forEach { (days, label) ->
                        val from = if (days == 0L) 0L else now - TimeUnit.DAYS.toMillis(days)
                        FilterChip(
                            selected = false,
                            onClick = { vm.updateSettings { it.copy(scanFrom = from) }; dialog = null },
                            label = { Text(label) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("ปิด") } },
        )
        SettingsDialog.RESET -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("ล้างข้อมูลทั้งหมด?") },
            text = { Text("ทุกรายการจะถูกลบและกู้คืนไม่ได้ หมวดหมู่ แท็ก และการตั้งค่ายังอยู่ แนะนำให้ส่งออก CSV เก็บไว้ก่อน") },
            confirmButton = {
                TextButton(onClick = { vm.resetData(); dialog = null }) { Text("ลบทั้งหมด", color = ExpenseColor) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("ยกเลิก") } },
        )
        null -> Unit
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp),
    )
}
