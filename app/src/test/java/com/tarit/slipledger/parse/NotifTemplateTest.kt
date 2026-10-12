package com.tarit.slipledger.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotifTemplateTest {
    private fun learn(example: String, amount: String, merchantFrom: String? = null, merchantTo: String? = null): String {
        val tokens = NotifTemplate.tokenize(example)
        val a = tokens.indexOfFirst { it.text == amount }
        val m = merchantFrom?.let { from -> tokens.indexOfFirst { it.text == from }..tokens.indexOfFirst { it.text == (merchantTo ?: from) } }
        return NotifTemplate.build(tokens, a, m)
    }

    @Test fun creditCardSpendWithMerchant() {
        val rule = learn(
            "KTC: ใช้จ่ายบัตร XX1234 จำนวน 1,250.00 บาท ที่ STARBUCKS CENTRAL วันที่ 03/10/69 14:22",
            amount = "1,250.00", merchantFrom = "STARBUCKS", merchantTo = "CENTRAL",
        )
        val m = NotifTemplate.match(rule, "KTC: ใช้จ่ายบัตร XX1234 จำนวน 89.00 บาท ที่ 7-ELEVEN วันที่ 04/10/69 08:01")!!
        assertEquals(8900L, m.amountSatang)
        assertEquals("7-ELEVEN", m.merchant)
    }

    @Test fun gluedThaiTextAndMaskedAccount() {
        val rule = learn("บัตรเครดิตXX9876ใช้จ่าย350.50บาท", amount = "350.50")
        assertEquals(1_200_000L, NotifTemplate.match(rule, "บัตรเครดิตXX5555ใช้จ่าย12,000.00บาท")!!.amountSatang)
    }

    @Test fun ignoresDifferentKindOfAlert() {
        val rule = learn("ใช้จ่ายบัตร XX1234 จำนวน 1,250.00 บาท", amount = "1,250.00")
        assertNull(NotifTemplate.match(rule, "ชำระยอดบัตร XX1234 จำนวน 5,000.00 บาท ขอบคุณค่ะ"))
    }

    @Test fun greetingBeforeContextDoesNotMatter() {
        val rule = learn("สวัสดีค่ะ คุณเอ ยินดีต้อนรับ ยอดใช้จ่ายล่าสุด 420.00 บาท", amount = "420.00")
        assertEquals(10000L, NotifTemplate.match(rule, "สวัสดีค่ะ คุณบี ยินดีต้อนรับ ยอดใช้จ่ายล่าสุด 100 บาท")!!.amountSatang)
    }
}
