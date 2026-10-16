package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllTransactionsWithCategoryList(): List<TransactionWithCategory>

    @Transaction
    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    suspend fun getTransactionsBetweenList(startMillis: Long, endMillis: Long): List<TransactionWithCategory>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionWithCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE importBatchId = :batchId")
    suspend fun deleteTransactionsByBatchId(batchId: Long): Int

    @Query("SELECT * FROM transactions WHERE importBatchId = :batchId")
    suspend fun getTransactionsByBatchId(batchId: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}
