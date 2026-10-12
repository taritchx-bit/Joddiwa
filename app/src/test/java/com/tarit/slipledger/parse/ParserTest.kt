package com.tarit.slipledger.parse

import com.tarit.slipledger.data.Categories
import com.tarit.slipledger.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** OCR-like text of the sample slips: line order follows the slip layout, with some typical noise. */
class ParserTest {
    private val owner = listOf("ธฤต")

    @Test fun krungthaiBillPayment() {
        val text = """
            Krungthai กรุงไทย
            จ่ายบิลสำเร็จ
            รหัสอ้างอิง
            C202609286271117930 54
            จาก
            นายธฤต เ***
            กรุงไทย
            XXX-X-XX145-9
            ไปยัง
            THAI YU KING CO.,LTD
            (010753600037403)
            หมายเลขร้านค้า1 000002213370109
            เลขที่อ้างอิง2 4501398813110131331
            จำนวนเงิน 35.00 บาท
            ค่าธรรมเนียม 0.00 บาท
            วันที่ทำรายการ 28 ก.ย. 2569 - 11:56
        """.trimIndent()
        val slip = assertNotNull(SlipParser.parse(text, owner))
        assertEquals(3500L, slip.amountSatang)
        assertEquals(TxType.EXPENSE, slip.type)
        assertEquals("THAI YU KING CO.,LTD", slip.to)
        assertEquals(Categories.SHOP, slip.category)
        assertEquals("กรุงไทย", slip.bank)
        assertEquals(LocalDate.of(2026, 9, 28), ThaiDate.parseDate(text))
        assertEquals(LocalTime.of(11, 56), ThaiDate.parseTime(text))
    }

    @Test fun dimeTransfer() {
        val text = """
            โอนเงิน
            100.00 บาท
            ค่าธรรมเนียม 0.00 บาท
            จาก นาย ธฤต เชื้อเอี่ยม
            © x-3931
            ไปยัง นาง เกศกัญญา แสงอรุณ
            @ x-9416 สแกนเพื่อ
            ตรวจสอบสลิป
            วันที่ 28 ก.ย. 2569 - 17:44 น.
            เลขที่สลิป 627117210592 Dime!
            โดย ธ. เกียรตินาคินภัทร
        """.trimIndent()
        val slip = assertNotNull(SlipParser.parse(text, owner))
        assertEquals(10000L, slip.amountSatang)
        assertEquals(TxType.EXPENSE, slip.type)
        assertEquals("นาง เกศกัญญา แสงอรุณ", slip.to)
        assertEquals(Categories.PERSON, slip.category)
        assertEquals("เกียรตินาคินภัทร", slip.bank)
    }

    @Test fun scbTransferWithAmountOnNextLine() {
        val text = """
            SCB
            โอนเงินสำเร็จ
            30 ก.ย. 2569 - 19:02
            รหัสอ้างอิง: 2026093059EUHo7MMw4fy0j2f
            FROM นาย ธฤต เ.
            xxx-xxx394-8
            TO บจก. ร่ำรวยเฮงเฮง 289
            x-7522
            AMOUNT
            7,889.00
        """.trimIndent()
        val slip = assertNotNull(SlipParser.parse(text, owner, SlipQr("014", "2026093059EUHo7")))
        assertEquals(788900L, slip.amountSatang)
        assertEquals("บจก. ร่ำรวยเฮงเฮง 289", slip.to)
        assertEquals("ไทยพาณิชย์", slip.bank)
        assertEquals(LocalDate.of(2026, 9, 30), ThaiDate.parseDate(text))
    }

    // Real Tesseract output of the sample slips on the emulator, noise included.
    @Test fun realOcrKrungthai() {
        val text = """
            ๕   จ่ายบิลสําเร็อ
            รหัสอฮ้างอิง
            020260928627111793054
              อาก
            “ wiesqa.i***
            กรุงไทย
            XXX-X-XX145-9
            i   ไปยัง
            THAI YU KING CO.,LTD
            (010753600037403)
            หมายเลขร้านค้า1                000002213370109
            เลขที่อ้างอิง2                4501398813110131331
            จํานวนเงิน                     35.00 บาท
            ค่าธรรมเนียม                              0.00 บาท
            วันที่ทํารายการ                   28 n.d. 2569 - 11:56
        """.trimIndent()
        val slip = assertNotNull(SlipParser.parse(text, owner))
        assertEquals(3500L, slip.amountSatang)
        assertEquals("THAI YU KING CO.,LTD", slip.to)
        assertEquals(Categories.SHOP, slip.category)
    }

    @Test fun realOcrScb() {
        val text = """
            @ โอนเงินสําเร็จ
            30 ก.ุย. 2569 - 19:02
            รหัสอ้างอิง: 2026093059EUHo7MMwa4 ty0j2¢
            FROM                                        @ บายธฤต..
            XXX-XXX394-8
            TO                               @ von. รํารวยเฮงเฮง 289
            %-7522
            AMOUNT                                               7,889.00
        """.trimIndent()
        val slip = assertNotNull(SlipParser.parse(text, owner))
        assertEquals(788900L, slip.amountSatang)
        assertEquals(LocalDate.of(2026, 9, 30), ThaiDate.parseDate(text))
        assertEquals("von. รำรวยเฮงเฮง 289", slip.to)
    }

    @Test fun realOcrDimeDate() {
        assertEquals(LocalDate.of(2026, 9, 28), ThaiDate.parseDate("วันที่      28 ก.ุย. 2569 - 17:44 น."))
        assertEquals(LocalDate.of(2026, 3, 5), ThaiDate.parseDate("5 มี.ค. 2569"))
        assertEquals(LocalDate.of(2026, 1, 5), ThaiDate.parseDate("5 ม.ค. 2569"))
    }

    @Test fun selfTransferAndIncome() {
        assertEquals(TxType.TRANSFER, SlipParser.classify("นาย ธฤต เ.", "นาย ธฤต เชื้อเอี่ยม", owner))
        assertEquals(TxType.INCOME, SlipParser.classify("นาง เกศกัญญา", "นาย ธฤต", owner))
        assertEquals(TxType.EXPENSE, SlipParser.classify("นาย ธฤต", "ร้าน", emptyList()))
    }

    @Test fun nonSlipIsRejected() {
        assertNull(SlipParser.parse("เมนูวันนี้ ข้าวผัด 50 บาท", owner))
        assertNull(SlipParser.parse("", owner))
    }

    @Test fun krungthaiConnextNotification() {
        val title = "Krungthai Connext"
        val text = "เงินเข้า: 1.00 บาท เข้าบัญชี XX1459 เมื่อ 02/10/69"
        assert(NotificationParser.isRelevantSender(NotificationParser.LINE_PACKAGE, title))
        val income = assertNotNull(NotificationParser.parse(title, text))
        assertEquals(100L, income.amountSatang)
        assertEquals(TxType.INCOME, income.type)
        assertEquals("1459", income.account)
        assertEquals("กรุงไทย", income.bank)
        assertEquals(LocalDate.of(2026, 10, 2), ThaiDate.parseDate(text))
    }

    @Test fun scbConnectNotification() {
        val title = "SCB Connect"
        val text = "รายการเงินเข้า 1.00 บาท เข้าบัญชี X-3948 วันที่ 02 ต.ค. 69 @23:12"
        assert(NotificationParser.isRelevantSender(NotificationParser.LINE_PACKAGE, title))
        val income = assertNotNull(NotificationParser.parse(title, text))
        assertEquals(100L, income.amountSatang)
        assertEquals("3948", income.account)
        assertEquals("ไทยพาณิชย์", income.bank)
        assert(income.hasTime)
    }

    @Test fun outgoingAlert() {
        val alert = assertNotNull(NotificationParser.parse("Krungthai Connext", "เงินออก: 500.00 บาท จากบัญชี XX1459 เมื่อ 03/10/69"))
        assertEquals(TxType.EXPENSE, alert.type)
        assertEquals(50000L, alert.amountSatang)
        assertEquals("1459", alert.account)
        val payment = assertNotNull(NotificationParser.parse("K PLUS", "ชำระเงิน 1,250.00 บาท จากบัญชี xxx-x-x1234-x"))
        assertEquals(TxType.EXPENSE, payment.type)
        assertEquals(125000L, payment.amountSatang)
    }

    @Test fun chatIsIgnored() {
        assert(!NotificationParser.isRelevantSender(NotificationParser.LINE_PACKAGE, "แม่"))
        assertNull(NotificationParser.parse("Krungthai Connext", "สวัสดีครับ ยินดีต้อนรับ"))
    }

    @Test fun slipAccounts() {
        val scb = assertNotNull(SlipParser.parse(
            """
            โอนเงินสำเร็จ
            FROM                                        @ บายธฤต..
            XXX-XXX394-8
            TO                               @ von. รำรวยเฮงเฮง 289
            %-7522
            AMOUNT                                               7,889.00
            """.trimIndent(), owner,
        ))
        assertEquals("3948", scb.fromAccount)
        assertEquals("7522", scb.toAccount)

        val dime = assertNotNull(SlipParser.parse(
            """
            โอนเงิน
            100.00 บาท
            จาก     นาย ธฤต เชือเอียม
            @ 3931
            ไปยัง นาง เกศกัญญา แสงอรุณ
            @ 9416
            วันที่ 28 ก.ุย. 2569 - 17:44 น.
            """.trimIndent(), owner,
        ))
        assertEquals("3931", dime.fromAccount)
        assertEquals("9416", dime.toAccount)
        assertEquals("นาง เกศกัญญา แสงอรุณ", dime.to)

        // "จาก" was misread as "อาก": fall back to the first masked number.
        val ktb = assertNotNull(SlipParser.parse(
            """
            จ่ายบิลสําเร็อ
              อาก
            “ wiesqa.i***
            XXX-X-XX145-9
            i   ไปยัง
            THAI YU KING CO.,LTD
            (010753600037403)
            จํานวนเงิน                     35.00 บาท
            """.trimIndent(), owner,
        ))
        assertEquals("1459", ktb.fromAccount)
        assertEquals("THAI YU KING CO.,LTD", ktb.to)
    }

    @Test fun slipQrTlv() {
        val payload = "0037" + "0006000001" + "0103014" + "0216" + "2026093059EUHo7M" + "5102TH" + "91041234"
        val qr = assertNotNull(SlipQrParser.parse(payload))
        assertEquals("014", qr.bankCode)
        assertEquals("2026093059EUHo7M", qr.transRef)
    }

    private fun <T> assertNotNull(value: T?): T {
        assertNotNull(null, value)
        return value!!
    }
}
