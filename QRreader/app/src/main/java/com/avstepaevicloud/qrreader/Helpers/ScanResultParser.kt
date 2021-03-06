package com.avstepaevicloud.qrreader.Helpers

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.util.*

/**
 * Парсер результата сканирования
 */
class ScanResultParser {
    companion object {

        /**
         * Хардкодный префикс результата сканирования
         */
        private val PREFIX: String = "http://tkt.ac/t/"

        /**
         * Распарсить результат сканирования небезопасным образом
         */
        fun unsafeParse(base64StringWithIpPrefix: String, code: String, context: Context): ParseResult {
            var success = true
            var msg = ""

            val base64String = base64StringWithIpPrefix.replace(PREFIX, "")
            val bytes = Base64.decode(base64String, 8)

            val data = bytes.slice(0..10).toByteArray()
            val sign = bytes.slice(11 until bytes.count()).toByteArray()

            val digestInput = data + code.toByteArray(Charsets.UTF_8)

            val md5Digest = MessageDigest.getInstance("MD5")
            md5Digest.reset()
            md5Digest.update(digestInput)

            val md5Hash = md5Digest.digest().slice(0 until sign.count()).toByteArray()

            if (!Arrays.equals(md5Hash, sign)) {
                //throw ResultParsingException(context.applicationContext.getString(com.avstepaevicloud.qrreader.R.string.digital_signature_is_not_valid))
                success = false
                msg = context.applicationContext.getString(com.avstepaevicloud.qrreader.R.string.digital_signature_is_not_valid)
            }

            val ticketId = ByteBuffer.wrap(data.slice(0..3).toByteArray().reversedArray()).getInt().toULong()
            val eventId = ByteBuffer.wrap(data.slice(4..7).toByteArray().reversedArray()).getInt().toULong()
            val ticketType = data.get(8).toUInt()
            val row = data.get(9).toUInt()
            val seat = data.get(10).toUInt()

            return ParseResult(ScanResult(ticketId, eventId, ticketType, row, seat), success, msg)
        }

        // TODO вынести в Extensions
        fun Byte.toUInt() = this.toInt() and 0xff

        fun Int.toULong() = this.toLong() and 0xff_ff_ff_ff
    }
}

class ParseResult(var scanResult: ScanResult, var success: Boolean, var msg: String)

/**
 * Исключение разбора результата сканирования
 */
class ResultParsingException(message: String) : Exception(message)

/**
 * Результат сканирования
 */
data class ScanResult(val ticketId: Long, val eventId: Long, val ticketType: Int, val row: Int, val seat: Int)