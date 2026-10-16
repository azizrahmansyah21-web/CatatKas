package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ImportBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY importedAtMillis DESC")
    fun getAllImportBatches(): Flow<List<ImportBatchEntity>>

    @Query("SELECT * FROM import_batches ORDER BY importedAtMillis DESC")
    suspend fun getAllImportBatchesList(): List<ImportBatchEntity>

    @Query("SELECT * FROM import_batches WHERE id = :id LIMIT 1")
    suspend fun getImportBatchById(id: Long): ImportBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportBatch(batch: ImportBatchEntity): Long

    @Delete
    suspend fun deleteImportBatch(batch: ImportBatchEntity)

    @Query("DELETE FROM import_batches WHERE id = :id")
    suspend fun deleteImportBatchById(id: Long)
}
