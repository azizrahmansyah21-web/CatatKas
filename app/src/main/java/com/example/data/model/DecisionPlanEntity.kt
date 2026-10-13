package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlanType {
    NEEDS, // Kebutuhan (Needs - 50%)
    WANTS  // Keinginan (Wants - 30%)
}

enum class PlanStatus {
    PENDING,    // Menunggu masa pertimbangan (cooling-off period)
    APPROVED,   // Disetujui untuk dibeli
    CANCELLED,  // Dibatalkan (berhasil berhemat!)
    REALIZED    // Sudah dieksekusi menjadi transaksi pengeluaran
}

@Entity(tableName = "decision_plans")
data class DecisionPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val estimatedPrice: Double,
    val planType: PlanType = PlanType.WANTS,
    val urgencyLevel: Int = 3, // Skala 1 (Rendah) hingga 5 (Sangat Mendesak)
    val categoryId: Long? = null,
    val note: String = "",
    val decisionScore: Int = 65, // Skor 1 - 100
    val coolingOffHours: Int = 72, // Aturan jeda 72 jam
    val createdAt: Long = System.currentTimeMillis(),
    val status: PlanStatus = PlanStatus.PENDING
)
