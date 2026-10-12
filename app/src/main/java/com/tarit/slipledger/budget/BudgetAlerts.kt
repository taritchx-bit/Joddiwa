package com.tarit.slipledger.budget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tarit.slipledger.MainActivity
import com.tarit.slipledger.R
import com.tarit.slipledger.data.AppDatabase
import com.tarit.slipledger.data.Prefs
import com.tarit.slipledger.ui.Fmt

/** Notifies once per month when spending reaches 80% and 100% of the total or a category budget. */
object BudgetAlerts {
    private const val CHANNEL = "budget"
    private val thresholds = listOf(100, 80) // highest first: crossing 100% should not also say 80%

    suspend fun check(context: Context) {
        val settings = Prefs.get(context).settings.value
        if (settings.monthlyBudget <= 0 && settings.categoryBudgets.isEmpty()) return

        val period = settings.period
        val month = period.current()
        val byCategory = AppDatabase.get(context).txDao().expenseByCategory(
            period.startMillis(month), period.endMillis(month),
        ).associate { it.category to it.total }

        val checks = buildList {
            if (settings.monthlyBudget > 0) add(Triple("ทั้งหมด", byCategory.values.sum(), settings.monthlyBudget))
            settings.categoryBudgets.forEach { (cat, budget) -> add(Triple(cat, byCategory[cat] ?: 0L, budget)) }
        }
        val sent = context.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
        for ((name, spent, budget) in checks) {
            val percent = (spent * 100 / budget).toInt()
            val threshold = thresholds.firstOrNull { percent >= it } ?: continue
            val key = "$month|$name|$threshold"
            if (sent.getBoolean(key, false)) continue
            // Reaching 100% also marks 80% as done so it is not announced afterwards.
            sent.edit().apply { thresholds.filter { it <= threshold }.forEach { putBoolean("$month|$name|$it", true) } }.apply()

            val label = if (name == "ทั้งหมด") "งบเดือนนี้" else "งบ \"$name\""
            val title = if (threshold >= 100) "ใช้เกิน$label แล้ว" else "ใช้ไป $percent% ของ$label"
            notify(context, key.hashCode(), title, "ใช้ไป ฿${Fmt.baht(spent)} จากงบ ฿${Fmt.baht(budget)}")
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "แจ้งเตือนงบประมาณ", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
