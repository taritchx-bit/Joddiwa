package com.tarit.slipledger.parse

/**
 * Thai bank slips carry a "slip verification" mini QR (EMVCo style TLV):
 * tag 00 = { 00: API id, 01: sending bank code, 02: transaction reference }, 51 = "TH", 91 = CRC.
 */
data class SlipQr(val bankCode: String?, val transRef: String?)

object SlipQrParser {
    fun parse(payload: String): SlipQr? {
        val top = tlv(payload) ?: return null
        val inner = top["00"]?.let { tlv(it) } ?: return null
        val ref = inner["02"]?.takeIf { it.length >= 6 }
        return SlipQr(bankCode = inner["01"], transRef = ref).takeIf { ref != null }
    }

    private fun tlv(s: String): Map<String, String>? {
        val out = mutableMapOf<String, String>()
        var i = 0
        while (i < s.length) {
            if (i + 4 > s.length) return null
            val tag = s.substring(i, i + 2)
            val len = s.substring(i + 2, i + 4).toIntOrNull() ?: return null
            if (i + 4 + len > s.length) return null
            out[tag] = s.substring(i + 4, i + 4 + len)
            i += 4 + len
        }
        return out
    }
}
