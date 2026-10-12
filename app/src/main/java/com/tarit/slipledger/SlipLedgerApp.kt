package com.tarit.slipledger

import android.app.Application
import com.tarit.slipledger.budget.RecurringRunner
import com.tarit.slipledger.data.Repository
import com.tarit.slipledger.scan.ScanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SlipLedgerApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ScanWorker.schedule(this)
        scope.launch {
            Repository.get(this@SlipLedgerApp).ensureDefaultCategories()
            Repository.get(this@SlipLedgerApp).ensureDefaultWallets()
            RecurringRunner.run(this@SlipLedgerApp)
        }
    }
}
