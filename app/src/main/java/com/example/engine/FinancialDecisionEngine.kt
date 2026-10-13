package com.example.engine

import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import java.util.Calendar

data class Rule50_30_20Analysis(
    val needsAmount: Double,
    val needsPercentage: Float,
    val wantsAmount: Double,
    val wantsPercentage: Float,
    val savingsAmount: Double,
    val savingsPercentage: Float,
    val isHealthy: Boolean,
    val recommendation: String
)

data class SpendingLeakItem(
    val categoryName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val potentialSavingsMonthly: Double,
    val suggestion: String
)

data class FinancialDecisionMetrics(
    val ruleAnalysis: Rule50_30_20Analysis,
    val dailyBurnRate: Double,
    val projectedRunwayDays: Int,
    val totalCashBalance: Double,
    val topSpendingLeaks: List<SpendingLeakItem>,
    val weekendVsWeekdayRatio: Float, // > 1.0 means weekend is heavier
    val generalAdvice: String
)

object FinancialDecisionEngine {

    // Needs category IDs: Transportasi (2), Belanja Kebutuhan (3), Tagihan (4), Kesehatan (6), Tempat Tinggal (8)
    private val NEEDS_CATEGORY_IDS = setOf(2L, 3L, 4L, 6L, 8L)

    fun calculateDecisionMetrics(
        transactions: List<TransactionWithCategory>,
        totalCashBalance: Double
    ): FinancialDecisionMetrics {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var needsExpense = 0.0
        var wantsExpense = 0.0

        var weekdayExpense = 0.0
        var weekdayCount = 0
        var weekendExpense = 0.0
        var weekendCount = 0

        val cal = Calendar.getInstance()
        val microExpenseMap = mutableMapOf<Long, MutableList<Double>>() // categoryId -> list of micro-amounts

        for (item in transactions) {
            val t = item.transaction
            val amount = t.amount

            cal.timeInMillis = t.dateMillis
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            if (t.type == TransactionType.INCOME) {
                totalIncome += amount
            } else {
                totalExpense += amount

                val catId = t.categoryId
                if (NEEDS_CATEGORY_IDS.contains(catId)) {
                    needsExpense += amount
                } else {
                    wantsExpense += amount
                }

                if (isWeekend) {
                    weekendExpense += amount
                    weekendCount++
                } else {
                    weekdayExpense += amount
                    weekdayCount++
                }

                // Detect micro spending (< Rp 50.000)
                if (amount <= 50_000.0) {
                    val list = microExpenseMap.getOrPut(catId) { mutableListOf() }
                    list.add(amount)
                }
            }
        }

        // 1. Calculate 50/30/20 Rule
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0.0)
        val incomeBase = if (totalIncome > 0) totalIncome else totalExpense.coerceAtLeast(1.0)

        val needsPct = ((needsExpense / incomeBase) * 100f).toFloat().coerceIn(0f, 100f)
        val wantsPct = ((wantsExpense / incomeBase) * 100f).toFloat().coerceIn(0f, 100f)
        val savingsPct = ((netSavings / incomeBase) * 100f).toFloat().coerceIn(0f, 100f)

        val isHealthy = needsPct <= 55f && wantsPct <= 35f && savingsPct >= 15f

        val ruleRecommendation = when {
            wantsPct > 35f ->
                "Pos Keinginan & Jajan (${wantsPct.toInt()}%) melebihi anjuran 30%. Batasi makan luar dan hiburan agar tabunganmu bertumbuh."
            needsPct > 60f ->
                "Kebutuhan Pokok (${needsPct.toInt()}%) cukup tinggi. Cek apakah ada pengeluaran listrik atau belanja bulanan yang bisa diefisienkan."
            savingsPct >= 20f ->
                "Alokasi ideal! Tabunganmu mencapai ${savingsPct.toInt()}%, sangat aman untuk dana darurat dan investasi masa depan."
            else ->
                "Perhatikan rasio pengeluaranmu. Usahakan menyisihkan minimal 20% di awal setiap menerima pemasukan."
        }

        val ruleAnalysis = Rule50_30_20Analysis(
            needsAmount = needsExpense,
            needsPercentage = needsPct,
            wantsAmount = wantsExpense,
            wantsPercentage = wantsPct,
            savingsAmount = netSavings,
            savingsPercentage = savingsPct,
            isHealthy = isHealthy,
            recommendation = ruleRecommendation
        )

        // 2. Calculate Burn Rate & Runway
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val dailyBurnRate = totalExpense / dayOfMonth
        val projectedRunwayDays = if (dailyBurnRate > 0) {
            (totalCashBalance / dailyBurnRate).toInt().coerceIn(0, 365)
        } else {
            99
        }

        // 3. Top Spending Leaks
        val leaks = mutableListOf<SpendingLeakItem>()
        for ((catId, amounts) in microExpenseMap) {
            if (amounts.size >= 4) {
                val totalLeak = amounts.sum()
                val catName = transactions.firstOrNull { it.transaction.categoryId == catId }?.category?.name ?: "Pengeluaran Mikro"
                val potentialSavings = totalLeak * 0.4 // 40% reduction target
                leaks.add(
                    SpendingLeakItem(
                        categoryName = catName,
                        transactionCount = amounts.size,
                        totalAmount = totalLeak,
                        potentialSavingsMonthly = potentialSavings,
                        suggestion = "Terdeteksi ${amounts.size}x transaksi kecil di $catName. Menguranginya bisa menghemat sekitar Rp ${potentialSavings.toInt()} bulan ini."
                    )
                )
            }
        }
        leaks.sortByDescending { it.totalAmount }

        // 4. Weekend vs Weekday Ratio
        val avgWeekday = if (weekdayCount > 0) weekdayExpense / weekdayCount else 0.0
        val avgWeekend = if (weekendCount > 0) weekendExpense / weekendCount else 0.0
        val weekendRatio = if (avgWeekday > 0) (avgWeekend / avgWeekday).toFloat() else 1.0f

        // 5. General Summary Advice
        val generalAdvice = when {
            weekendRatio > 1.4f ->
                "Pengeluaranmu meningkat ${((weekendRatio - 1f) * 100).toInt()}% di akhir pekan. Buat batas anggaran terpisah khusus hari Sabtu-Minggu."
            projectedRunwayDays < 15 ->
                "Peringatan: Berdasarkan laju pengeluaran saat ini, sisa saldo diproyeksikan bertahan $projectedRunwayDays hari. Rem pengeluaran sekunder."
            else ->
                "Arus kasmu berjalan stabil. Pertahankan pencatatan dan evaluasi sisa anggaran setiap minggu."
        }

        return FinancialDecisionMetrics(
            ruleAnalysis = ruleAnalysis,
            dailyBurnRate = dailyBurnRate,
            projectedRunwayDays = projectedRunwayDays,
            totalCashBalance = totalCashBalance,
            topSpendingLeaks = leaks.take(3),
            weekendVsWeekdayRatio = weekendRatio,
            generalAdvice = generalAdvice
        )
    }
}
