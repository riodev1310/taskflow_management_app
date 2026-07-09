package com.taskflow.demo.core.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))

    fun dueLabel(raw: String?): String {
        if (raw.isNullOrBlank()) return "Chưa đặt hạn"
        val date = parseDate(raw) ?: return raw.take(10)
        val today = LocalDate.now()
        return when {
            date.isBefore(today) -> "Quá hạn ${date.format(dateFormatter)}"
            date == today -> "Hôm nay"
            date == today.plusDays(1) -> "Ngày mai"
            else -> date.format(dateFormatter)
        }
    }

    fun toDueAt(dateText: String): String? {
        if (dateText.isBlank()) return null
        return runCatching {
            LocalDate.parse(dateText.trim()).atTime(17, 0).atOffset(java.time.ZoneOffset.UTC).toString()
        }.getOrNull()
    }

    private fun parseDate(raw: String): LocalDate? {
        return runCatching { OffsetDateTime.parse(raw).toLocalDate() }
            .recoverCatching { LocalDate.parse(raw.take(10)) }
            .getOrNull()
    }
}
