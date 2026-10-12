package com.tarit.slipledger.parse

import com.tarit.slipledger.data.Categories

/** Guesses an expense category from the payee name printed on a slip. Order matters: first match wins. */
object AutoCategory {
    private val rules: List<Pair<Regex, String>> = listOf(
        // Food delivery must come before the generic "grab" travel rule.
        """grab\s*food|line\s*man|lineman|food\s*panda|robinhood|shopee\s*food|kfc|mcdonald|แมคโดนัลด์|starbucks|สตาร์บัคส์|""" +
            """amazon|café|cafe|คาเฟ่|coffee|กาแฟ|ชานม|bubble|mk\s*restaurant|เอ็มเค|ส้มตำ|ก๋วยเตี๋ยว|ข้าวมัน|ข้าวขาหมู|อาหาร|""" +
            """restaurant|bistro|kitchen|ครัว|swensen|สเวนเซ่น|sizzler|ซิซซ์เล่อร์|bonchon|after\s*you|ชาบู|shabu|หมูกระทะ|""" +
            """bakery|เบเกอรี่|pizza|พิซซ่า|burger|เบอร์เกอร์|sushi|ซูชิ|ramen|ราเมน|yayoi|ยาโยอิ|fuji|ฟูจิ|barbq|บาร์บีคิว|""" +
            """dairy\s*queen|chester|เชสเตอร์|cha\s*tra\s*mue|ชาตรามือ|ชาไทย"""
            to Categories.FOOD,
        """7-?\s?eleven|เซเว่น|cp\s*all|ซีพี\s*ออลล์|lotus|โลตัส|big\s*c|บิ๊กซี|makro|แม็คโคร|tops|ท็อปส์|family\s*mart|""" +
            """แฟมิลี่มาร์ท|lawson|ลอว์สัน|villa\s*market|gourmet|กูร์เมต์|foodland|ฟู้ดแลนด์|cj\s*more|ซีเจ|max\s*valu|แม็กซ์แวลู|""" +
            """minimart|มินิมาร์ท|supermarket|ซุปเปอร์มาร์เก็ต|ร้านสะดวกซื้อ"""
            to Categories.GROCERY,
        """\bgrab\b|แกร็บ|bolt|\bbts\b|\bmrt\b|บีทีเอส|เอ็มอาร์ที|รถไฟฟ้า|\bptt\b|ปตท|shell|เชลล์|bangchak|บางจาก|caltex|""" +
            """คาลเท็กซ์|esso|เอสโซ่|pt\s*station|easy\s*pass|m-?flow|การทางพิเศษ|airasia|แอร์เอเชีย|nok\s*air|นกแอร์|""" +
            """thai\s*airways|การบินไทย|vietjet|การรถไฟ|\btaxi\b|แท็กซี่|parking|ที่จอดรถ|muvmi|ขนส่ง"""
            to Categories.TRAVEL,
        """shopee|ช้อปปี้|lazada|ลาซาด้า|tiktok|central|เซ็นทรัล|the\s*mall|เดอะมอลล์|robinson|โรบินสัน|uniqlo|ยูนิโคล่|""" +
            """h&m|zara|ikea|อิเกีย|homepro|โฮมโปร|power\s*buy|เพาเวอร์บาย|jd\s*central|thai\s*watsadu|ไทวัสดุ|global\s*house|""" +
            """โกลบอลเฮ้าส์|banana\s*it|studio\s*7|ivory|daiso|ไดโซะ|miniso|มินิโซ"""
            to Categories.SHOPPING,
        """การไฟฟ้า|กฟน|กฟภ|การประปา|กปน|กปภ|true\s*(move|online|money)?|ทรู|\bais\b|เอไอเอส|dtac|ดีแทค|3bb|""" +
            """internet|อินเทอร์เน็ต|ประกัน|insurance|ค่าเช่า|นิติบุคคล|condo|คอนโด|บัตรเครดิต|credit\s*card|\bktc\b|""" +
            """aeon|อิออน|ผ่อน|สินเชื่อ|loan|leasing|ลีสซิ่ง|ภาษี|กรมสรรพากร|revenue\s*department"""
            to Categories.BILLS,
        """โรงพยาบาล|hospital|คลินิก|clinic|ร้านยา|เภสัช|pharmacy|boots|บู๊ทส์|watsons|วัตสัน|fascino|ฟาซิโน|dental|ทันต|""" +
            """fitness|ฟิตเนส|gym|ยิม"""
            to Categories.HEALTH,
        """netflix|spotify|youtube|disney|major|เมเจอร์|sf\s*cinema|เอสเอฟ|steam|playstation|nintendo|garena|การีน่า|""" +
            """apple\.com|itunes|google\s*play|ticket|บัตรคอนเสิร์ต|concert|คอนเสิร์ต|karaoke|คาราโอเกะ|bowling|โบว์ลิ่ง"""
            to Categories.FUN,
        """มหาวิทยาลัย|university|โรงเรียน|school|ค่าเทอม|tuition|course|คอร์ส|สถาบันกวดวิชา|se-?ed|ซีเอ็ด|นายอินทร์|""" +
            """kinokuniya|คิโนะคุนิยะ|b2s|udemy|coursera"""
            to Categories.EDUCATION,
    ).map { (pattern, category) -> Regex(pattern, RegexOption.IGNORE_CASE) to category }

    private val personRe = Regex("""^(นาย|นาง|นางสาว|น\.ส\.|ด\.ช\.|ด\.ญ\.|mr\.?|mrs\.?|ms\.?|miss)\s*""", RegexOption.IGNORE_CASE)
    // "von." is how Tesseract commonly reads "บจก." on SCB slips.
    private val companyRe = Regex(
        """บจก|บมจ|บริษัท|ห้าง|หจก|ร้าน|co\.?,?\s*ltd|company|limited|corp|plc|shop|store|von\.""",
        RegexOption.IGNORE_CASE,
    )

    fun guess(payee: String): String = when {
        payee.isBlank() -> Categories.OTHER
        else -> rules.firstOrNull { it.first.containsMatchIn(payee) }?.second
            ?: if (personRe.containsMatchIn(payee)) Categories.PERSON
            else if (companyRe.containsMatchIn(payee)) Categories.SHOP
            else Categories.PERSON
    }
}
