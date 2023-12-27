package com.carbidecowboy.intra.domain

import java.io.IOException
import java.nio.ByteBuffer

class ApduUtils {

    companion object {

        const val APDU_OK = 0x9000
        const val APDU_DATA_REMAINING = 0x61
        const val SEND_REMAINING_INS = 0xa5
        const val CALCULATE_ALL_INS = 0xa4
        const val CHALLENGE_TAG: Byte = 0x74
        const val NAME_TAG: Byte = 0x71
        const val NO_RESPONSE_TAG: Byte = 0x77
        const val PUT_INS: Byte = 0x01
        const val KEY_TAG: Byte = 0x73
        const val LIST_INS = 0xa1
        const val NAME_LIST_TAG: Byte = 0x72
        const val IMF_TAG: Byte = 0x7a
        const val CALCULATE_INS = 0xa2
        const val DELETE_INS: Byte = 0x02
        const val VERSION_TAG: Byte = 0x79
        const val RESPONSE_TAG: Byte = 0x75
        const val VALIDATE_INS: Byte = 0xa3.toByte()
        const val SET_CODE_INS: Byte = 0x03

        val AID = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x07, 0x47, 0x00, 0x61, 0xFC.toByte(), 0x54, 0xD5.toByte())

        @Throws(IOException::class)
        fun ByteBuffer.parseTlv(tag: Byte): ByteArray {
            val readTag = get()
            if (readTag != tag) {
                throw IOException("Required tag: %02x, got %02x".format(tag, readTag))
            }
            return ByteArray(0xff and get().toInt()).apply { get(this) }
        }

        fun ByteBuffer.tlv(tag: Byte, data: ByteArray = byteArrayOf()): ByteBuffer {
            return put(tag).put(data.size.toByte()).put(data)
        }

        data class ApduResponse(val data: ByteArray, val statusCode: Int)
    }
}