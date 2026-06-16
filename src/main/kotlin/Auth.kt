package org.neosahadeo

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import kotlin.math.pow

/**
 * Hmac OTP Class.
 * */
class HTOP(){
    companion object {
        /**
         * Generate an OTP.
         * @param {ByteArray} secret.
         * @param {Long} timestamp; time should be in milliseconds.
         * @return {String} otp; padded to 6 digits.
         * */
        fun generate(secret: ByteArray, timestamp: Long): String {
            val message = intDecode(counterCalc(30, timestamp))
            val mac = Mac.getInstance("HmacSHA1")
            val spec = SecretKeySpec(secret, "HmacSHA1")
            mac.init(spec)
            val digest = mac.doFinal(message)
            val offset = (digest[digest.size - 1].toInt() and 0x0F)
            val otp = (
                    ((digest[offset].toInt() and 0x7F) shl 24) or
                            ((digest[offset + 1].toInt() and 0xFF) shl 16) or
                            ((digest[offset + 2].toInt() and 0xFF) shl 8) or
                            (digest[offset + 3].toInt() and 0xFF)
                    ) % 10.0.pow(6).toInt()

            return otp.toString().padStart(6, '0')
        }
    }
}

/**
 * Converts an integer to an ByteArray.
 * @param {Int} num Int.
 * @returns ByteArray.
 */
fun intDecode(num: Int): ByteArray {
    val store = ByteArray(8);
    var q = num
    for (x in 7 downTo 0){
        store[x] = (q and 0xFF).toByte()
        q = q ushr 7
    }

    return store
}

/**
 * Converts a number to a byte array.
 * @param {Long} num.
 * @return {ByteArray} ByteArray.
 * */
fun intDecode(num: Long): ByteArray {
    val arr = ByteArray(8)
    var acc = num
    for (i in 7 downTo 0) {
        if (acc == 0L) break
        val b = (acc and 0xFF).toInt()
        arr[i] = b.toByte()
        acc -= b
        acc /= 256
    }
    return arr
}


private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
/**
 * Convert base 32 to a byte array.
 * @param {String} input; should be base32;
 * @return {ByteArray} ByteArray.
 * */
fun base32Decode(input: String): ByteArray {
    val cleaned = input.replace("=", "").uppercase()
    val out = ByteArray((cleaned.length * 5) / 8)
    var buffer = 0
    var bitsLeft = 0
    var index = 0
    for (ch in cleaned) {
        val val5 = BASE32_CHARS.indexOf(ch)
        if (val5 < 0) throw IllegalArgumentException("Invalid Base32 char: $ch")
        buffer = (buffer shl 5) or val5
        bitsLeft += 5
        if (bitsLeft >= 8) {
            bitsLeft -= 8
            out[index++] = ((buffer shr bitsLeft) and 0xFF).toByte()
        }
    }
    return if (index == out.size) out else out.copyOf(index)
}

/**
 * Returns an interval based on the period and timestamp.
 * This is used to provide a period of time that an OTP is valid for.
 * @param {Int} period.
 * @param {Long} timestamp; should be in milliseconds.
 * @return {Long} floored time.
 * */
fun counterCalc(period: Int, timestamp: Long): Long {
    return floor(timestamp.toDouble() / 1000/ period).toLong();
}

//fun toHex(bytes: ByteArray) =
//    bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
