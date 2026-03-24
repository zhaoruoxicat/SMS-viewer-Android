package com.lyx.sms.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SmsMessage(
    val id: Long,
    val phone: String,
    val content: String,
    val receivedAt: String,
    val device: String
) {
    val receivedAtMillis: Long = parseReceivedAtMillis(receivedAt)

    val dayLabel: String
        get() = formatReceivedAt("MM月dd日")

    val fullDateTimeLabel: String
        get() = formatReceivedAt("yyyy-MM-dd HH:mm:ss")

    val timeLabel: String
        get() = formatReceivedAt("HH:mm")

    val displayPhone: String
        get() = phone.ifBlank { "未知号码" }

    val displayDevice: String
        get() = device.ifBlank { "短信设备" }

    private fun formatReceivedAt(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return runCatching {
            Instant.ofEpochMilli(receivedAtMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        }.getOrElse { receivedAt.ifBlank { "--" } }
    }

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun parseReceivedAtMillis(raw: String): Long {
            if (raw.isBlank()) return 0L

            return runCatching {
                LocalDateTime.parse(raw, dateTimeFormatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.recoverCatching {
                Instant.parse(raw).toEpochMilli()
            }.getOrDefault(0L)
        }
    }
}

data class PurgeResult(
    val beforeTotal: Int,
    val remainingTotal: Int,
    val deleteMode: String
)
