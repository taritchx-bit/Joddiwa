package com.tarit.slipledger.budget

import android.content.Context
import com.tarit.slipledger.data.Repository
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Adds the transactions of recurring rules whose day has come, catching up any missed months. */
object RecurringRunner {
    suspend fun run(context: Context, today: LocalDate = LocalDate.now()) {
        val repo = Repository.get(context)
        val zone = ZoneId.systemDefault()
        for (rule in repo.recurring.active()) {
            val thisMonth = YearMonth.from(today)
            // A new rule starts with the current month; an existing one continues after its last run.
            var month = if (rule.lastRun.isEmpty()) thisMonth else YearMonth.parse(rule.lastRun).plusMonths(1)
            var lastRun = rule.lastRun
            while (!month.isAfter(thisMonth)) {
                val date = month.atDay(rule.dayOfMonth.coerceIn(1, month.lengthOfMonth()))
                if (date.isAfter(today)) break
                repo.addIfNew(
                    Tx(
                        type = rule.type,
                        amountSatang = rule.amountSatang,
                        timestamp = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
                        counterparty = rule.counterparty,
                        category = rule.category,
                        note = rule.note,
                        source = TxSource.RECURRING,
                        walletId = rule.walletId,
                        sourceKey = "recur:${rule.id}:$month",
                    )
                )
                lastRun = month.toString()
                month = month.plusMonths(1)
            }
            if (lastRun != rule.lastRun) repo.recurring.upsert(rule.copy(lastRun = lastRun))
        }
    }
}
