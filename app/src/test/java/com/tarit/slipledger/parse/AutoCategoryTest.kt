package com.tarit.slipledger.parse

import com.tarit.slipledger.data.Categories
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCategoryTest {
    private fun check(expected: String, vararg payees: String) =
        payees.forEach { assertEquals(it, expected, AutoCategory.guess(it)) }

    @Test fun merchants() {
        check(Categories.FOOD, "GRAB FOOD", "LINE MAN", "STARBUCKS COFFEE", "ร้านก๋วยเตี๋ยวเรือ", "บจก. ชาตรามือ")
        check(Categories.GROCERY, "7-ELEVEN", "บมจ. ซีพี ออลล์", "LOTUS'S", "Big C Supercenter", "TOPS")
        check(Categories.TRAVEL, "GRAB", "Grab Taxi", "BTS", "PTT STATION", "บางจาก")
        check(Categories.SHOPPING, "SHOPEE", "Lazada", "CENTRAL", "HomePro")
        check(Categories.BILLS, "การไฟฟ้านครหลวง", "AIS", "TRUE MOVE H", "KTC")
        check(Categories.HEALTH, "โรงพยาบาลกรุงเทพ", "BOOTS", "Watsons")
        check(Categories.FUN, "NETFLIX", "Major Cineplex")
    }

    @Test fun peopleAndCompanies() {
        check(Categories.PERSON, "นาง เกศกัญญา แสงอรุณ", "นาย สมชาย ใจดี", "น.ส. สมหญิง")
        check(Categories.SHOP, "THAI YU KING CO.,LTD", "บจก. ร่ำรวยเฮงเฮง 289", "von. รำรวยเฮงเฮง 289")
        check(Categories.OTHER, "")
    }
}
