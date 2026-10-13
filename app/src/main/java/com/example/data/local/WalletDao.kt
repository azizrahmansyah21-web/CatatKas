package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY id ASC")
    suspend fun getAllWalletsList(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Query("SELECT * FROM wallets WHERE name = :name LIMIT 1")
    suspend fun getWalletByName(name: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>): List<Long>

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletCount(): Int
}
