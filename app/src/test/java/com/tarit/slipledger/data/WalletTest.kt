package com.tarit.slipledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletTest {
    private val cash = Wallet(id = 1, name = "เงินสด", kind = WalletKind.CASH, color = 0)
    private val scb = Wallet(id = 2, name = "SCB", kind = WalletKind.BANK, bank = "ไทยพาณิชย์", accountHint = "3948", color = 0)
    private val ktb = Wallet(id = 3, name = "KTB", kind = WalletKind.BANK, bank = "กรุงไทย", color = 0)
    private val all = listOf(cash, scb, ktb)

    @Test fun matchByDigitsThenBank() {
        assertEquals(scb, WalletMatcher.match(all, "ไทยพาณิชย์", "3948"))
        assertEquals(scb, WalletMatcher.match(all, "", "3948"))          // digits alone are enough
        assertEquals(ktb, WalletMatcher.match(all, "กรุงไทย", "1459"))   // only KTB wallet, no digits yet
        assertEquals(ktb, WalletMatcher.match(all, "กรุงไทย", ""))
        assertNull(WalletMatcher.match(all, "ไทยพาณิชย์", "1111"))       // a different SCB account
        assertNull(WalletMatcher.match(all, "กสิกรไทย", "2222"))
        assertNull(WalletMatcher.match(all, "", ""))
    }

    private fun tx(type: TxType, amount: Long, from: Long?, to: Long? = null) =
        Tx(type = type, amountSatang = amount, timestamp = 0, source = TxSource.MANUAL, sourceKey = "k", walletId = from, toWalletId = to)

    @Test fun balanceEffects() {
        assertEquals(500L, tx(TxType.INCOME, 500, 2).effectOn(2))
        assertEquals(-300L, tx(TxType.EXPENSE, 300, 2).effectOn(2))
        assertEquals(0L, tx(TxType.EXPENSE, 300, 3).effectOn(2))
        val withdraw = tx(TxType.TRANSFER, 1000, 2, 1) // SCB -> cash
        assertEquals(-1000L, withdraw.effectOn(2))
        assertEquals(1000L, withdraw.effectOn(1))
        assertEquals(0L, withdraw.effectOn(3))
    }
}
