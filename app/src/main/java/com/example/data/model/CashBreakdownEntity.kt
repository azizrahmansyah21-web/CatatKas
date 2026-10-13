package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_breakdowns",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parentTransactionId"),
        Index("categoryId")
    ]
)
data class CashBreakdownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentTransactionId: Long,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
