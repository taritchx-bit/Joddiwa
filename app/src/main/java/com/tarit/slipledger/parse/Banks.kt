package com.tarit.slipledger.parse

object Banks {
    /** Bank codes used in the slip verification QR (sending bank). */
    private val byCode = mapOf(
        "002" to "กรุงเทพ",
        "004" to "กสิกรไทย",
        "006" to "กรุงไทย",
        "011" to "ทีทีบี",
        "014" to "ไทยพาณิชย์",
        "022" to "ซีไอเอ็มบี",
        "024" to "ยูโอบี",
        "025" to "กรุงศรี",
        "030" to "ออมสิน",
        "033" to "ธอส.",
        "034" to "ธ.ก.ส.",
        "066" to "อิสลาม",
        "067" to "ทิสโก้",
        "069" to "เกียรตินาคินภัทร",
        "073" to "แลนด์ แอนด์ เฮ้าส์",
    )

    private val byKeyword = listOf(
        Regex("(?i)dime|เกียรตินาคิน|kkp") to "เกียรตินาคินภัทร",
        Regex("(?i)krungthai|กรุงไทย|connext|ktb") to "กรุงไทย",
        Regex("(?i)\\bSCB\\b|ไทยพาณิชย์|scb\\s*connect") to "ไทยพาณิชย์",
        Regex("(?i)k\\s*plus|kbank|กสิกร|make by") to "กสิกรไทย",
        Regex("(?i)bangkok bank|bualuang|กรุงเทพ") to "กรุงเทพ",
        Regex("(?i)krungsri|กรุงศรี|kma") to "กรุงศรี",
        Regex("(?i)\\bttb\\b|ทีทีบี|ทหารไทย") to "ทีทีบี",
        Regex("(?i)mymo|ออมสิน|\\bgsb\\b") to "ออมสิน",
        Regex("(?i)true\\s*money|ทรูมันนี่") to "TrueMoney",
    )

    /** Bank names offered when creating a wallet. */
    val all: List<String> = byCode.values.toList() + "TrueMoney"

    /** Brand-ish colour per bank for wallet cards. */
    fun color(bank: String): Long = when (bank) {
        "กสิกรไทย" -> 0xFF138F2D
        "ไทยพาณิชย์" -> 0xFF4E2E7F
        "กรุงไทย" -> 0xFF1BA5E1
        "กรุงเทพ" -> 0xFF1E4598
        "กรุงศรี" -> 0xFFE0A800
        "ทีทีบี" -> 0xFF0050F0
        "ออมสิน" -> 0xFFEB198D
        "เกียรตินาคินภัทร" -> 0xFF5B7C99
        "TrueMoney" -> 0xFFF26F21
        else -> 0xFF667085
    }

    /** Short label for wallet names on small cards. */
    fun short(bank: String): String = when (bank) {
        "เกียรตินาคินภัทร" -> "KKP"
        "แลนด์ แอนด์ เฮ้าส์" -> "LH Bank"
        "ไทยพาณิชย์" -> "SCB"
        "กสิกรไทย" -> "กสิกร"
        else -> bank
    }

    fun fromCode(code: String?): String? = code?.let { byCode[it] }

    fun fromText(text: String): String? = byKeyword.firstOrNull { it.first.containsMatchIn(text) }?.second
}
