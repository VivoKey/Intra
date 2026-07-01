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
