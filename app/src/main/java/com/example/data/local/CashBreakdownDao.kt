package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CashBreakdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashBreakdownDao {
    @Query("SELECT * FROM cash_breakdowns WHERE parentTransactionId = :transactionId")
    fun getBreakdownsForTransaction(transactionId: Long): Flow<List<CashBreakdownEntity>>

    @Query("SELECT * FROM cash_breakdowns WHERE parentTransactionId = :transactionId")
    suspend fun getBreakdownsForTransactionList(transactionId: Long): List<CashBreakdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakdown(breakdown: CashBreakdownEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakdowns(breakdowns: List<CashBreakdownEntity>): List<Long>

    @Delete
    suspend fun deleteBreakdown(breakdown: CashBreakdownEntity)

    @Query("DELETE FROM cash_breakdowns WHERE parentTransactionId = :transactionId")
    suspend fun deleteBreakdownsForTransaction(transactionId: Long)
}
