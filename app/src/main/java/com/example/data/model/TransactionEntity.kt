package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("dateMillis")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val wallet: String = "Tunai",
    val destinationWalletId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val rawDescription: String = "",
    val isAtmWithdrawal: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
