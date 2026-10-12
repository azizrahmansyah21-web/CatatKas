package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BackupCategoryDto
import com.example.data.model.BackupPayload
import com.example.data.model.BackupTransactionDto
import com.example.util.DataBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CatatKas", appName)
    }

    @Test
    fun `test backup serialization in android environment`() {
        val payload = BackupPayload(
            version = 1,
            exportedAtMillis = 1700000000000L,
            categories = listOf(
                BackupCategoryDto(
                    id = 1,
                    name = "Makanan",
                    type = "EXPENSE",
                    iconKey = "restaurant",
                    colorHex = "#EF4444",
                    isDefault = true,
                    orderIndex = 1
                )
            ),
            transactions = listOf(
                BackupTransactionDto(
                    id = 10,
                    amount = 25000.0,
                    type = "EXPENSE",
                    categoryId = 1,
                    wallet = "Tunai",
                    dateMillis = 1700000000000L,
                    note = "Makan Siang",
                    createdAtMillis = 1700000000000L
                )
            )
        )

        val json = DataBackupManager.serializeBackup(payload)
        assertTrue(json.contains("Makanan"))
        assertTrue(json.contains("25000"))

        val parsed = DataBackupManager.parseBackup(json)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.categories.size)
        assertEquals("Makanan", parsed.categories[0].name)
        assertEquals(1, parsed.transactions.size)
        assertEquals(25000.0, parsed.transactions[0].amount, 0.001)
    }
}
