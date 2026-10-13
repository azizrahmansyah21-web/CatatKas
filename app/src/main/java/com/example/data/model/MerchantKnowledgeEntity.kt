package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_knowledge",
    indices = [
        Index(value = ["keyword"], unique = true)
    ]
)
data class MerchantKnowledgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val categoryId: Long,
    val confidenceWeight: Float = 1.0f,
    val timesUsed: Int = 1
)
