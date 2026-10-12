package com.tarit.slipledger.scan

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tarit.slipledger.budget.RecurringRunner
import com.tarit.slipledger.data.Prefs
import java.util.concurrent.TimeUnit

/** Periodic background work: adds due recurring transactions and picks up new slips. */
class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RecurringRunner.run(applicationContext)
        if (Prefs.get(applicationContext).settings.value.autoScan) SlipScanner(applicationContext).scan()
        return Result.success()
    }

    companion object {
        private const val NAME = "slip-scan"

        /** Always scheduled: recurring transactions need it even when auto scan is off. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScanWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
