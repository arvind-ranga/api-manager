package com.example.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Shared date display/input conventions: DD-MM-YYYY. */
object DateFormats {
    const val EXPIRY_PATTERN = "dd-MM-yyyy"
    private const val DATE_TIME_PATTERN = "dd-MM-yyyy hh:mm a"
    private val LENIENT_EXPIRY_REGEX = Regex("\\d{1,2}-\\d{1,2}-\\d{4}")

    fun parseExpiryDate(value: String): Long? {
        val trimmed = value.trim()
        val normalized = if (LENIENT_EXPIRY_REGEX.matches(trimmed)) {
            trimmed
        } else {
            autoFormatExpiryInput(trimmed)
        }
        if (!LENIENT_EXPIRY_REGEX.matches(normalized)) return null
        return try {
            val parts = normalized.split("-")
            if (parts.size != 3) return null
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.setLenient(false)
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats raw typing into DD-MM-YYYY while keeping a trailing separator so a
     * partially typed date (e.g. "01-") is preserved instead of being stripped.
     */
    fun autoFormatExpiryInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        val day = digits.take(2)
        val month = digits.drop(2).take(2)
        val year = digits.drop(4).take(4)
        return buildString {
            if (day.isNotEmpty()) append(day)
            if (month.isNotEmpty()) append('-').append(month)
            if (year.isNotEmpty()) append('-').append(year)
        }
    }

    fun formatExpiryDate(epochMillis: Long): String =
        SimpleDateFormat(EXPIRY_PATTERN, Locale.US).format(Date(epochMillis))

    fun formatDateTime(epochMillis: Long): String =
        SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(Date(epochMillis))

    fun formatDate(epochMillis: Long): String =
        SimpleDateFormat(EXPIRY_PATTERN, Locale.getDefault()).format(Date(epochMillis))
}
