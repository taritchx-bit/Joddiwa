package com.tarit.slipledger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.FragmentActivity
import com.tarit.slipledger.data.WalletKind
import com.tarit.slipledger.ui.AppLock
import com.tarit.slipledger.ui.LocalWallets
import com.tarit.slipledger.ui.LockScreen
import com.tarit.slipledger.ui.NotifyAppsScreen
import com.tarit.slipledger.ui.OtPlannerScreen
import com.tarit.slipledger.ui.ReviewScreen
import com.tarit.slipledger.ui.WalletsScreen
import com.tarit.slipledger.widget.SlipWidget
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.tarit.slipledger.ui.InkBottom
import com.tarit.slipledger.ui.Lime
import com.tarit.slipledger.ui.PressCard
import com.tarit.slipledger.ui.appear
import com.tarit.slipledger.ui.pressScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.tarit.slipledger.ui.Backdrop
import com.tarit.slipledger.ui.LocalBarSpace
import com.tarit.slipledger.ui.backdropSource
import com.tarit.slipledger.ui.liquidGlass
import com.tarit.slipledger.ui.rememberBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.scan.SlipScanner
import com.tarit.slipledger.ui.AppActions
import com.tarit.slipledger.ui.BudgetSheet
import com.tarit.slipledger.ui.CategoriesScreen
import com.tarit.slipledger.ui.EditTxSheet
import com.tarit.slipledger.ui.ExpenseColor
import com.tarit.slipledger.ui.HistoryScreen
import com.tarit.slipledger.ui.HomeScreen
import com.tarit.slipledger.ui.IconBadge
import com.tarit.slipledger.ui.IncomeColor
import com.tarit.slipledger.ui.LocalCategories
import com.tarit.slipledger.ui.MainViewModel
import com.tarit.slipledger.ui.RecurringScreen
import com.tarit.slipledger.ui.SettingsScreen
import com.tarit.slipledger.ui.SlipLedgerTheme
import com.tarit.slipledger.ui.StatsScreen
import com.tarit.slipledger.ui.SubScreen
import com.tarit.slipledger.ui.TagsScreen
import com.tarit.slipledger.ui.TxFilter
import com.tarit.slipledger.ui.newManualTx

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("หน้าหลัก", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("รายการ", Icons.Outlined.Receipt, Icons.Filled.Receipt),
    Tab("สรุป", Icons.Outlined.PieChart, Icons.Filled.PieChart),
    Tab("OT", Icons.Outlined.MoreTime, Icons.Filled.MoreTime),
    Tab("ตั้งค่า", Icons.Outlined.Settings, Icons.Filled.Settings),
)

/** FragmentActivity because the biometric prompt needs one. */
class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()
    private var locked by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locked = lockEnabled() && savedInstanceState == null
        handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            SlipLedgerTheme(settings.theme) {
                if (locked && settings.appLock) LockScreen { AppLock.prompt(this) { locked = false } }
                else AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Lock again after the app has been in the background for a while.
        if (stoppedAt > 0 && lockEnabled() && SystemClock.elapsedRealtime() - stoppedAt > LOCK_AFTER_MS) locked = true
    }

    /** If the phone's screen lock was removed, unlocking would be impossible, so the app stays open. */
    private fun lockEnabled() = vm.settings.value.appLock && AppLock.available(this)

    /** Widget buttons open the app with slipledger://scan or slipledger://expense. */
    private fun handleIntent(intent: Intent?) {
        intent?.data?.takeIf { it.scheme == "slipledger" }?.host?.let { vm.pendingAction.value = it }
    }

    private companion object {
        const val LOCK_AFTER_MS = 30_000L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val categories by vm.categories.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val pending by vm.pendingAction.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by rememberSaveable { mutableStateOf<SubScreen?>(null) }
    var editing by remember { mutableStateOf<Tx?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var budgetSheet by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.scanGallery()
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val actions = AppActions(
        scan = {
            if (SlipScanner.hasPermission(context)) vm.scanGallery() else permissionLauncher.launch(SlipScanner.imagePermission)
        },
        newTx = { type ->
            val first = categories.filter { it.kind == type }.minByOrNull { it.sortOrder }?.name
            // Manual entries are usually cash.
            val cash = wallets.firstOrNull { it.kind == WalletKind.CASH }?.id
            editing = newManualTx(type, first ?: if (type == TxType.INCOME) Categories.INCOME_OTHER else Categories.OTHER, cash)
        },
        edit = { editing = it },
        showHistory = { vm.filter.value = TxFilter(); sub = null; tab = 1 },
        showCategory = { cat -> vm.filter.value = TxFilter(categories = setOf(cat)); sub = null; tab = 1 },
        showStats = { tab = 2 },
        editBudget = { budgetSheet = true },
        openWallets = { sub = SubScreen.WALLETS },
        showWallet = { id -> vm.filter.value = TxFilter(wallets = setOf(id)); sub = null; tab = 1 },
        openReview = { sub = SubScreen.REVIEW },
    )

    LaunchedEffect(pending) {
        when (pending) {
            SlipWidget.ACTION_SCAN -> actions.scan()
            SlipWidget.ACTION_EXPENSE -> actions.newTx(TxType.EXPENSE)
            SlipWidget.ACTION_OT -> { sub = null; tab = 3 }
        }
        vm.pendingAction.value = null
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.message.value = null }
    }
    BackHandler(enabled = sub != null || tab != 0) { if (sub != null) sub = null else tab = 0 }

    CompositionLocalProvider(LocalCategories provides categories.associateBy { it.name }, LocalWallets provides wallets) {
        // Screens run edge to edge so they scroll underneath the floating glass tab bar, which refracts them.
        val backdrop = rememberBackdrop()
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val barSpace = if (sub == null) navInset + 96.dp else navInset
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().backdropSource(backdrop).background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
              CompositionLocalProvider(LocalBarSpace provides barSpace) {
                AnimatedContent(
                    targetState = sub to tab,
                    transitionSpec = {
                        if (targetState.first != null) slideInHorizontally { it / 3 } + fadeIn() togetherWith fadeOut()
                        else fadeIn() togetherWith fadeOut()
                    },
                    label = "screen",
                ) { (s, t) ->
                    when (s) {
                        SubScreen.CATEGORIES -> CategoriesScreen(vm) { sub = null }
                        SubScreen.TAGS -> TagsScreen(vm) { sub = null }
                        SubScreen.RECURRING -> RecurringScreen(vm) { sub = null }
                        SubScreen.WALLETS -> WalletsScreen(vm, onBack = { sub = null }, onOpenWallet = actions.showWallet)
                        SubScreen.REVIEW -> ReviewScreen(vm, onBack = { sub = null }, onEdit = actions.edit)
                        SubScreen.NOTIFY_APPS -> NotifyAppsScreen(vm) { sub = null }
                        null -> when (t) {
                            0 -> HomeScreen(vm, actions)
                            1 -> HistoryScreen(vm, actions)
                            2 -> StatsScreen(vm, actions)
                            3 -> OtPlannerScreen()
                            else -> SettingsScreen(vm, actions) { sub = it }
                        }
                    }
                }
              }
            }
            AnimatedVisibility(
                sub == null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                BottomBar(tab, addMenu, backdrop, onSelect = { tab = it }, onAdd = { addMenu = true })
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = barSpace))
        }

        if (addMenu) {
            ModalBottomSheet(onDismissRequest = { addMenu = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("เพิ่มรายการ", style = MaterialTheme.typography.titleLarge)
                    AddOption(0, Icons.Default.DocumentScanner, MaterialTheme.colorScheme.primary, "สแกนสลิปจากแกลเลอรี", "อ่านสลิปใหม่ทั้งหมดอัตโนมัติ") {
                        addMenu = false; actions.scan()
                    }
                    AddOption(1, Icons.Default.Remove, ExpenseColor, "จดรายจ่าย", "เงินสด บัตรเครดิต หรือรายการที่ไม่มีสลิป") {
                        addMenu = false; actions.newTx(TxType.EXPENSE)
                    }
                    AddOption(2, Icons.Default.Add, IncomeColor, "จดรายรับ", "เงินเดือน เงินสด หรือรายรับอื่น") {
                        addMenu = false; actions.newTx(TxType.INCOME)
                    }
                }
            }
        }

        if (budgetSheet) {
            val settings by vm.settings.collectAsStateWithLifecycle()
            BudgetSheet(settings, categories.filter { it.kind == TxType.EXPENSE }, onDismiss = { budgetSheet = false }) { total, perCategory ->
                vm.updateSettings { it.copy(monthlyBudget = total, categoryBudgets = perCategory) }
                budgetSheet = false
                vm.message.value = if (total > 0 || perCategory.isNotEmpty()) "บันทึกงบประมาณแล้ว" else "ยกเลิกงบประมาณแล้ว"
                // Alerts need this permission on Android 13+.
                if ((total > 0 || perCategory.isNotEmpty()) && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        editing?.let { tx ->
            EditTxSheet(
                initial = tx,
                allTags = tags,
                onDismiss = { editing = null },
                onSave = { updated, applyToPayee -> vm.save(updated, tx, applyToPayee); editing = null },
                onDelete = { vm.delete(it); editing = null },
            )
        }
    }
}

@Composable
private fun AddOption(index: Int, icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    PressCard(onClick = onClick, modifier = Modifier.appear(index), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color, 44.dp)
            Spacer(Modifier.size(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Glass pill of evenly spaced tabs, with the round lime "add" button floating beside it (turns into × while the
 * add menu is open). Keeping the add button out of the row lets any number of tabs sit symmetrically.
 */
@Composable
private fun BottomBar(selected: Int, addOpen: Boolean, backdrop: Backdrop, onSelect: (Int) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      // Floating liquid-glass pill: the screen behind is blurred inside and bends around its rim.
      Box(
        Modifier.weight(1f)
            .shadow(18.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .liquidGlass(
                CircleShape, backdrop, blur = 5.dp, refraction = 16.dp, rimWidth = 24.dp,
                tint = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.42f),
            ),
      ) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, t ->
                val active = selected == i
                val pill by animateFloatAsState(if (active) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "pill")
                val bump by animateFloatAsState(if (active) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "bump")
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).pressScale(interaction, 0.9f).clip(MaterialTheme.shapes.small)
                        .clickable(interaction, indication = null) { onSelect(i) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Pill grows out from the icon's centre when the tab becomes active.
                        Box(
                            Modifier.size(width = 48.dp, height = 30.dp)
                                .graphicsLayer { scaleX = 0.4f + 0.6f * pill; alpha = pill }
                                .clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        )
                        Icon(
                            if (active) t.selectedIcon else t.icon, null,
                            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer { scaleX = bump; scaleY = bump },
                        )
                    }
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
      }
        Spacer(Modifier.width(10.dp))
        val interaction = remember { MutableInteractionSource() }
        val turn by animateFloatAsState(if (addOpen) 135f else 0f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "turn")
        Box(
            Modifier.size(62.dp).pressScale(interaction, 0.85f).shadow(14.dp, CircleShape, spotColor = Lime).clip(CircleShape)
                .background(Lime).clickable(interaction, indication = null, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "เพิ่มรายการ", tint = InkBottom, modifier = Modifier.size(30.dp).rotate(turn))
        }
    }
}
