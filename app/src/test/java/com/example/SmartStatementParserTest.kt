package com.example

import com.example.engine.ExcelCsvStatementParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SmartStatementParserTest {

    @Test
    fun `test past month statement parsing and date preservation`() {
        // Sample records representing a past month (e.g., August 2024)
        val sampleRecords = ExcelCsvStatementParser.generateSampleLivinRecords()
        assertNotNull(sampleRecords)
        assertTrue(sampleRecords.isNotEmpty())

        val expenseCount = sampleRecords.count { it.isExpense }
        val incomeCount = sampleRecords.count { !it.isExpense }
        assertTrue(expenseCount > 0)
        assertTrue(incomeCount > 0)

        // Verify that dates are parsed and retained
        for (rec in sampleRecords) {
            assertTrue(rec.dateMillis > 0)
            assertTrue(rec.amount > 0)
            assertTrue(rec.merchantCleaned.isNotBlank())
        }
    }

    @Test
    fun `test debit credit identification and categorization`() {
        val sampleRecords = ExcelCsvStatementParser.generateSampleLivinRecords()
        val indomaret = sampleRecords.firstOrNull { it.description.contains("INDOMARET") }
        assertNotNull(indomaret)
        assertTrue(indomaret!!.isExpense)
        assertEquals("Indomaret", indomaret.merchantCleaned)

        val incomeItem = sampleRecords.firstOrNull { !it.isExpense }
        assertNotNull(incomeItem)
        assertFalse(incomeItem!!.isExpense)
    }
}
