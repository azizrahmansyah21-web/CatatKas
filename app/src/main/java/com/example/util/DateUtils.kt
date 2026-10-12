package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    val INDONESIAN_LOCALE = Locale("id", "ID")

    private val MONTH_NAMES = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    fun getMonthName(month: Int): String {
        return if (month in 0..11) MONTH_NAMES[month] else ""
    }

    fun formatMonthYear(year: Int, month: Int): String {
        return "${getMonthName(month)} $year"
    }

    fun formatDateShort(dateMillis: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", INDONESIAN_LOCALE)
        return sdf.format(Date(dateMillis))
    }

    fun formatDateFull(dateMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", INDONESIAN_LOCALE)
        return sdf.format(Date(dateMillis))
    }

    fun formatDateHeader(dateMillis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = dateMillis }

        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "Hari Ini"

        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Kemarin"

        return formatDateFull(dateMillis)
    }

    fun formatTime(dateMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", INDONESIAN_LOCALE)
        return sdf.format(Date(dateMillis))
    }
}
