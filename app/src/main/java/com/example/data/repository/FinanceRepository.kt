package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.TransactionDao
import com.example.data.model.BackupCategoryDto
import com.example.data.model.BackupPayload
import com.example.data.model.BackupTransactionDto
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpending
import com.example.data.model.DailyCashflow
import com.example.data.model.MonthlySummary
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

class FinanceRepository(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    val allTransactions: Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategory()

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<TransactionWithCategory>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startMillis = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endMillis = calendar.timeInMillis

        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    suspend fun getTransactionsForMonthList(year: Int, month: Int): List<TransactionWithCategory> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startMillis = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endMillis = calendar.timeInMillis

        return transactionDao.getTransactionsBetweenList(startMillis, endMillis)
    }

    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> {
        return getTransactionsForMonth(year, month).map { list ->
            calculateSummary(year, month, list)
        }
    }

    private fun calculateSummary(
        year: Int,
        month: Int,
        transactions: List<TransactionWithCategory>
    ): MonthlySummary {
        var totalIncome = 0.0
        var totalExpense = 0.0

        val categorySpendingMap = mutableMapOf<Long, Pair<CategoryEntity, Double>>()
        val categoryCountMap = mutableMapOf<Long, Int>()
        val dailyMap = mutableMapOf<Int, Pair<Double, Double>>() // day -> (income, expense)

        val cal = Calendar.getInstance()

        for (item in transactions) {
            val amount = item.transaction.amount
            cal.timeInMillis = item.transaction.dateMillis
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val currentDaily = dailyMap.getOrDefault(day, Pair(0.0, 0.0))
            if (item.transaction.type == TransactionType.INCOME) {
                totalIncome += amount
                dailyMap[day] = Pair(currentDaily.first + amount, currentDaily.second)
            } else {
                totalExpense += amount
                dailyMap[day] = Pair(currentDaily.first, currentDaily.second + amount)

                item.category?.let { cat ->
                    val prev = categorySpendingMap.getOrDefault(cat.id, Pair(cat, 0.0))
                    categorySpendingMap[cat.id] = Pair(cat, prev.second + amount)
                    categoryCountMap[cat.id] = categoryCountMap.getOrDefault(cat.id, 0) + 1
                }
            }
        }

        val netBalance = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) {
            ((netBalance / totalIncome) * 100f).toFloat().coerceIn(-100f, 100f)
        } else 0f

        val categorySpendings = categorySpendingMap.values.map { (cat, amount) ->
            val percentage = if (totalExpense > 0) ((amount / totalExpense) * 100f).toFloat() else 0f
            CategorySpending(
                category = cat,
                totalAmount = amount,
                percentage = percentage,
                transactionCount = categoryCountMap[cat.id] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        // Max days in this month
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dailyCashflows = (1..maxDays).map { d ->
            val (inc, exp) = dailyMap.getOrDefault(d, Pair(0.0, 0.0))
            cal.set(Calendar.DAY_OF_MONTH, d)
            DailyCashflow(
                dayOfMonth = d,
                dateMillis = cal.timeInMillis,
                income = inc,
                expense = exp
            )
        }

        return MonthlySummary(
            year = year,
            month = month,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            savingsRatePercentage = savingsRate,
            topSpendingCategory = categorySpendings.firstOrNull(),
            categorySpendings = categorySpendings,
            dailyCashflows = dailyCashflows
        )
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun exportBackupPayload(): BackupPayload {
        val categories = categoryDao.getAllCategoriesList().map {
            BackupCategoryDto(
                id = it.id,
                name = it.name,
                type = it.type.name,
                iconKey = it.iconKey,
                colorHex = it.colorHex,
                isDefault = it.isDefault,
                orderIndex = it.orderIndex
            )
        }
        val transactions = transactionDao.getAllTransactionsWithCategoryList().map {
            val t = it.transaction
            BackupTransactionDto(
                id = t.id,
                amount = t.amount,
                type = t.type.name,
                categoryId = t.categoryId,
                wallet = t.wallet,
                dateMillis = t.dateMillis,
                note = t.note,
                createdAtMillis = t.createdAtMillis
            )
        }
        return BackupPayload(
            categories = categories,
            transactions = transactions
        )
    }

    suspend fun restoreBackup(payload: BackupPayload): Boolean {
        return try {
            // Restore categories first
            val categoryEntities = payload.categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    type = try { TransactionType.valueOf(it.type) } catch (e: Exception) { TransactionType.EXPENSE },
                    iconKey = it.iconKey,
                    colorHex = it.colorHex,
                    isDefault = it.isDefault,
                    orderIndex = it.orderIndex
                )
            }
            categoryDao.insertCategories(categoryEntities)

            // Restore transactions
            val transactionEntities = payload.transactions.map {
                TransactionEntity(
                    id = it.id,
                    amount = it.amount,
                    type = try { TransactionType.valueOf(it.type) } catch (e: Exception) { TransactionType.EXPENSE },
                    categoryId = it.categoryId,
                    wallet = it.wallet,
                    dateMillis = it.dateMillis,
                    note = it.note,
                    createdAtMillis = it.createdAtMillis
                )
            }
            transactionDao.insertTransactions(transactionEntities)
            true
        } catch (e: Exception) {
            false
        }
    }
}
