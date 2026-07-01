package com.hoker.intra.domain

/**
 * Lightweight Base32 decoding utility (RFC 4648).
 *
 * Replaces org.apache.commons.codec.binary.Base32 to avoid Android's
 * bootclasspath shadowing of the commons-codec library.
 */
object Base32Utils {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val PADDING = '='

    private val DECODE_TABLE = IntArray(128) { -1 }.also { table ->
        for (i in ALPHABET.indices) {
            table[ALPHABET[i].code] = i
        }
    }

    /**
     * Checks whether the given string contains only valid Base32 characters
     * (A-Z, 2-7, and optional '=' padding).
     */
    fun isInAlphabet(data: String): Boolean {
        if (data.isEmpty()) return false
        for (c in data) {
            if (c == PADDING) continue
            if (c.code >= 128 || DECODE_TABLE[c.code] == -1) return false
        }
        return true
    }

    /**
     * Encodes a byte array into a Base32 string (uppercase, with padding).
     */
    fun encodeToString(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val result = StringBuilder(data.size * 8 / 5 + 1)
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                result.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }

        if (bitsLeft > 0) {
            result.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }

        // Add padding to make length a multiple of 8
        while (result.length % 8 != 0) {
            result.append(PADDING)
        }

        return result.toString()
    }

    /**
     * Decodes a Base32-encoded string into a byte array.
     * Input is case-insensitive; padding characters are optional.
     *
     * @throws IllegalArgumentException if the string contains invalid Base32 characters.
     */
    fun decode(encoded: String): ByteArray {
        val input = encoded.uppercase().trimEnd(PADDING)
        if (input.isEmpty()) return ByteArray(0)

        val output = ByteArray(input.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (c in input) {
            val value = if (c.code < 128) DECODE_TABLE[c.code] else -1
            require(value != -1) { "Invalid Base32 character: $c" }

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = (buffer shr bitsLeft and 0xFF).toByte()
            }
        }

        return output.copyOf(index)
    }
}
