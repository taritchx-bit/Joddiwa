package com.tarit.slipledger.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tarit.slipledger.MainActivity
import com.tarit.slipledger.R
import com.tarit.slipledger.data.AppDatabase
import com.tarit.slipledger.data.Prefs
import com.tarit.slipledger.ui.Fmt
import com.tarit.slipledger.ui.countedWallets
import com.tarit.slipledger.ui.money
import java.time.LocalDate
import java.time.ZoneId

private data class WidgetData(
    val logo: Bitmap?,
    val monthLabel: String,
    val month: Long,
    val today: Long,
    val budget: Long,
    val balance: Long,
    val walletCount: Int,
)

private val Lime = Color(0xFF9AD936)
private val Ink = Color(0xFF0C1C13)
private val White = ColorProvider(Color.White)
private val Muted = ColorProvider(Color.White.copy(alpha = 0.65f))
private val Glass = ColorProvider(Color.White.copy(alpha = 0.10f))

/**
 * Home-screen widget in the app's look: ink-green card with the script logo, money left across the counted
 * wallets, spending this month and today, the budget bar, and shortcuts to scan, add an expense or plan OT.
 * Shrinks to spending and the main buttons when made small.
 */
class SlipWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { Content(context, data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val settings = Prefs.get(context).settings.value
        val period = settings.period
        val ym = period.current()
        val db = AppDatabase.get(context)
        val dao = db.txDao()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        // Same total as the home screen's "เงินคงเหลือ": ticked wallets that have a balance entered.
        val wallets = db.walletDao().all()
        val counted = countedWallets(wallets, settings.excludedWallets)
        val txs = if (counted.isEmpty()) emptyList() else dao.all()
        val balance = counted.sumOf { w -> w.openingBalance + txs.filter { it.timestamp >= w.openingDate }.sumOf { it.effectOn(w.id) } }
        return WidgetData(
            logo = runCatching { logo(context) }.getOrNull(),
            monthLabel = Fmt.monthShort(ym),
            month = dao.expenseTotal(period.startMillis(ym), period.endMillis(ym)),
            today = dao.expenseTotal(
                today.atStartOfDay(zone).toInstant().toEpochMilli(),
                today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            ),
            budget = settings.monthlyBudget,
            balance = balance,
            walletCount = counted.size,
        )
    }

    /** Widgets cannot use app fonts, so the script logo is drawn into a bitmap. */
    private fun logo(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.pacifico)
            textSize = 22 * density
            color = android.graphics.Color.WHITE
        }
        val small = Paint(paint).apply { textSize = 11 * density; color = 0xFF9AD936.toInt() }
        val word = "Joddiwa"
        val tag = " by Teen"
        val fm = paint.fontMetrics
        val w = paint.measureText(word) + small.measureText(tag) + 4 * density
        val h = fm.descent - fm.ascent
        val bmp = Bitmap.createBitmap(w.toInt() + 1, h.toInt() + 1, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawText(word, 0f, -fm.ascent, paint)
            drawText(tag, paint.measureText(word) + 2 * density, -fm.ascent, small)
        }
        return bmp
    }

    @Composable
    private fun Content(context: Context, d: WidgetData) {
        val compact = LocalSize.current.width < WIDE.width
        Column(
            GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).cornerRadius(24.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (d.logo != null) Image(ImageProvider(d.logo), "Joddiwa", GlanceModifier.height(26.dp))
                else Text("Joddiwa", style = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    d.monthLabel, style = TextStyle(color = White, fontSize = 11.sp),
                    modifier = GlanceModifier.background(Glass).cornerRadius(10.dp).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(GlanceModifier.height(8.dp))

            if (compact) {
                Text("ใช้ไปเดือนนี้", style = TextStyle(color = Muted, fontSize = 11.sp))
                Text(money(d.month), style = TextStyle(color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text("วันนี้ ฿${Fmt.bahtShort(d.today)}", style = TextStyle(color = Muted, fontSize = 11.sp))
            } else {
                Row(GlanceModifier.fillMaxWidth()) {
                    Tile(
                        "เงินคงเหลือ", if (d.walletCount > 0) money(d.balance) else "—",
                        if (d.walletCount > 0) "รวม ${d.walletCount} กระเป๋า" else "ยังไม่ได้ตั้งยอด",
                        GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Tile("ใช้ไปเดือนนี้", money(d.month), "วันนี้ ฿${Fmt.bahtShort(d.today)}", GlanceModifier.defaultWeight())
                }
                if (d.budget > 0) BudgetLine(d.month, d.budget)
            }

            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Pill("สแกนสลิป", actionStartActivity(openApp(context, ACTION_SCAN)), primary = true, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                Pill(if (compact) "+" else "+ จดรายจ่าย", actionStartActivity(openApp(context, ACTION_EXPENSE)),
                    modifier = if (compact) GlanceModifier else GlanceModifier.defaultWeight())
                if (!compact) {
                    Spacer(GlanceModifier.width(6.dp))
                    Pill("OT", actionStartActivity(openApp(context, ACTION_OT)))
                }
            }
        }
    }

    @Composable
    private fun Tile(label: String, value: String, note: String, modifier: GlanceModifier) {
        Column(modifier.background(Glass).cornerRadius(14.dp).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = TextStyle(color = Muted, fontSize = 11.sp), maxLines = 1)
            Text(value, style = TextStyle(color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(note, style = TextStyle(color = Muted, fontSize = 10.sp), maxLines = 1)
        }
    }

    @Composable
    private fun BudgetLine(spent: Long, budget: Long) {
        val ratio = (spent.toFloat() / budget).coerceIn(0f, 1f)
        val color = when {
            spent > budget -> Color(0xFFFF8A80)
            ratio >= 0.8f -> Color(0xFFFFC233)
            else -> Lime
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = ratio,
                modifier = GlanceModifier.defaultWeight().height(6.dp).cornerRadius(3.dp),
                color = ColorProvider(color),
                backgroundColor = Glass,
            )
            Spacer(GlanceModifier.width(8.dp))
            val left = budget - spent
            Text(
                if (left >= 0) "เหลืองบ ฿${Fmt.bahtShort(left)}" else "เกินงบ ฿${Fmt.bahtShort(-left)}",
                style = TextStyle(color = ColorProvider(color), fontSize = 11.sp),
            )
        }
    }

    @Composable
    private fun Pill(label: String, action: Action, primary: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
        Box(
            modifier.height(36.dp).background(if (primary) ColorProvider(Lime) else Glass).cornerRadius(18.dp)
                .padding(horizontal = 12.dp).clickable(action),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label, maxLines = 1,
                style = TextStyle(color = if (primary) ColorProvider(Ink) else White, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
        }
    }

    companion object {
        const val ACTION_SCAN = "scan"
        const val ACTION_EXPENSE = "expense"
        const val ACTION_OT = "ot"

        private val COMPACT = DpSize(160.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 140.dp)

        /** Distinct data URIs keep the buttons' pending intents from overwriting each other. */
        fun openApp(context: Context, action: String) = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("slipledger://$action"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        suspend fun refresh(context: Context) {
            runCatching { SlipWidget().updateAll(context) }
        }
    }
}

class SlipWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SlipWidget()
}
