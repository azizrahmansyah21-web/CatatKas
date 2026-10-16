package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileFormat: String = "EXCEL/CSV", // e.g. "EXCEL", "CSV"
    val importedAtMillis: Long = System.currentTimeMillis(),
    val transactionCount: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val detectedPeriod: String = "", // e.g. "Agustus 2024", "Juli 2024 - Agustus 2024"
    val detectedBank: String = "Auto-detect" // e.g. "BCA", "Mandiri", "BRI", "BNI", "Format Standar"
)
