package com.example.data.model

enum class TransactionType(val displayName: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan")
}

enum class WalletType(val displayName: String, val icon: String) {
    CASH("Tunai", "wallet"),
    BANK("Rekening Bank", "bank"),
    EWALLET("E-Wallet", "phone"),
    OTHER("Lainnya", "card");

    companion object {
        fun fromString(value: String): WalletType {
            return entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: CASH
        }
    }
}
