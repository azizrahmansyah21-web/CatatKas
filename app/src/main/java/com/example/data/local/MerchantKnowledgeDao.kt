package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MerchantKnowledgeEntity

@Dao
interface MerchantKnowledgeDao {
    @Query("SELECT * FROM merchant_knowledge")
    suspend fun getAllKnowledge(): List<MerchantKnowledgeEntity>

    @Query("SELECT * FROM merchant_knowledge WHERE keyword = :keyword LIMIT 1")
    suspend fun findKnowledgeByKeyword(keyword: String): MerchantKnowledgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: MerchantKnowledgeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeList(list: List<MerchantKnowledgeEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM merchant_knowledge")
    suspend fun getKnowledgeCount(): Int
}
