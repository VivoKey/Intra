package com.hoker.intra.domain

/**
 * Lightweight hex encoding/decoding utilities.
 *
 * Replaces org.apache.commons.codec.binary.Hex
 */
object HexUtils {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * Encodes a byte array into a lowercase hex string.
     * Returns an empty string if data is null.
     */
    fun encodeHexString(data: ByteArray?): String {
        if (data == null) return ""
        val result = CharArray(data.size * 2)
        for (i in data.indices) {
            val v = data[i].toInt() and 0xFF
            result[i * 2] = HEX_CHARS[v ushr 4]
            result[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(result)
    }

    /**
     * Encodes a ByteBuffer (from position to limit) into a lowercase hex string.
     * Returns an empty string if buffer is null.
     */
    fun encodeHexString(buffer: java.nio.ByteBuffer?): String {
        if (buffer == null) return ""
        val bytes = ByteArray(buffer.remaining())
        buffer.duplicate().get(bytes)
        return encodeHexString(bytes)
    }

    /**
     * Decodes a hex string into a byte array.
     * Equivalent to Hex.decodeHex(String).
     *
     * @throws IllegalArgumentException if the string length is odd or contains non-hex characters.
     */
    fun decodeHex(hexString: String): ByteArray {
        require(hexString.length % 2 == 0) { "Hex string must have an even length: ${hexString.length}" }
        return ByteArray(hexString.length / 2) { i ->
            val hi = Character.digit(hexString[i * 2], 16)
            val lo = Character.digit(hexString[i * 2 + 1], 16)
            require(hi != -1 && lo != -1) { "Invalid hex character at index ${i * 2}" }
            ((hi shl 4) or lo).toByte()
        }
    }
}
