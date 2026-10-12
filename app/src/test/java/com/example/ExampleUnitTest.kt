package com.example

import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatRupiah(50000.0)
        assertTrue(formatted.contains("50.000") || formatted.contains("50,000"))

        val parsed = CurrencyFormatter.parseAmount("Rp 150.000")
        assertEquals(150000.0, parsed, 0.001)

        val signedExpense = CurrencyFormatter.formatSignedRupiah(25000.0, isIncome = false)
        assertTrue(signedExpense.startsWith("-Rp"))

        val signedIncome = CurrencyFormatter.formatSignedRupiah(100000.0, isIncome = true)
        assertTrue(signedIncome.startsWith("+Rp"))
    }

    @Test
    fun testDateUtils() {
        val monthName = DateUtils.getMonthName(9) // 0-indexed: 9 is Oktober
        assertEquals("Oktober", monthName)

        val monthYear = DateUtils.formatMonthYear(2026, 9)
        assertEquals("Oktober 2026", monthYear)

        val dateShort = DateUtils.formatDateShort(1700000000000L)
        assertTrue(dateShort.isNotBlank())
    }
}
