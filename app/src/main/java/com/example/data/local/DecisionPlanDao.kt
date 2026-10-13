package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DecisionPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionPlanDao {

    @Query("SELECT * FROM decision_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<DecisionPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DecisionPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: DecisionPlanEntity)

    @Delete
    suspend fun deletePlan(plan: DecisionPlanEntity)

    @Query("DELETE FROM decision_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)
}
