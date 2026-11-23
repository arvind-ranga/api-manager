package com.example.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Shared date display/input conventions: DD-MM-YYYY. */
object DateFormats {
    const val EXPIRY_PATTERN = "dd-MM-yyyy"
    private const val DATE_TIME_PATTERN = "dd-MM-yyyy hh:mm a"
    private val EXPIRY_REGEX = Regex("\\d{2}-\\d{2}-\\d{4}")

    fun parseExpiryDate(value: String): Long? {
        val formatted = autoFormatExpiryInput(value)
        if (!EXPIRY_REGEX.matches(formatted)) return null
        return try {
            val format = SimpleDateFormat(EXPIRY_PATTERN, Locale.US)
            format.isLenient = false
            val date: Date = format.parse(formatted) ?: return null
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    fun autoFormatExpiryInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        val day = digits.take(2)
        val month = digits.drop(2).take(2)
        val year = digits.drop(4).take(4)
        return listOf(day, month, year).filter { it.isNotEmpty() }.joinToString("-")
    }

    fun formatExpiryDate(epochMillis: Long): String =
        SimpleDateFormat(EXPIRY_PATTERN, Locale.US).format(Date(epochMillis))

    fun formatDateTime(epochMillis: Long): String =
        SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(Date(epochMillis))

    fun formatDate(epochMillis: Long): String =
        SimpleDateFormat(EXPIRY_PATTERN, Locale.getDefault()).format(Date(epochMillis))
}
