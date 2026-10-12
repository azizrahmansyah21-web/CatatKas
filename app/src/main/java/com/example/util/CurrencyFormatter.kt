package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,###", symbols)

    fun formatRupiah(amount: Double, withPrefix: Boolean = true): String {
        val formatted = formatter.format(amount)
        return if (withPrefix) "Rp $formatted" else formatted
    }

    fun formatSignedRupiah(amount: Double, isIncome: Boolean): String {
        val prefix = if (isIncome) "+Rp " else "-Rp "
        val formatted = formatter.format(kotlin.math.abs(amount))
        return "$prefix$formatted"
    }

    fun parseAmount(input: String): Double {
        val clean = input.filter { it.isDigit() }
        return clean.toDoubleOrNull() ?: 0.0
    }
}
