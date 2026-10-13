package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val walletType: String, // BANK, EWALLET, CASH, MERCHANT
    val iconKey: String,
    val initialBalance: Double = 0.0,
    val isDefault: Boolean = false,
    val colorHex: String = "#10B981"
)
