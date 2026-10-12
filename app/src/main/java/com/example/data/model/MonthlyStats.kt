package com.example.data.model

data class CategorySpending(
    val category: CategoryEntity,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class DailyCashflow(
    val dayOfMonth: Int,
    val dateMillis: Long,
    val income: Double,
    val expense: Double
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val savingsRatePercentage: Float,
    val topSpendingCategory: CategorySpending?,
    val categorySpendings: List<CategorySpending>,
    val dailyCashflows: List<DailyCashflow>
)

data class BackupCategoryDto(
    val id: Long,
    val name: String,
    val type: String,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean,
    val orderIndex: Int
)

data class BackupTransactionDto(
    val id: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val wallet: String,
    val dateMillis: Long,
    val note: String,
    val createdAtMillis: Long
)

data class BackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val categories: List<BackupCategoryDto>,
    val transactions: List<BackupTransactionDto>
)
