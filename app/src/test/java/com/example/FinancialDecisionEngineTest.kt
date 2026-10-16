package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.engine.FinancialDecisionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinancialDecisionEngineTest {

    private fun createMockItem(
        id: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        categoryName: String,
        dateMillis: Long = System.currentTimeMillis()
    ): TransactionWithCategory {
        val category = CategoryEntity(
            id = categoryId,
            name = categoryName,
            type = type,
            iconKey = "category",
            colorHex = "#10B981",
            isDefault = true,
            orderIndex = 1
        )
        val transaction = TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            wallet = "Tunai",
            dateMillis = dateMillis,
            note = "Test item $id"
        )
        return TransactionWithCategory(transaction, category)
    }

    @Test
    fun `TC-PLAN-001 - test 50-30-20 Rule healthy allocation calculation`() {
        // Income: 10.000.000
        // Needs (cat 2 - Transportasi): 4.000.000 (40% <= 50%)
        // Wants (cat 1 - Hiburan): 2.500.000 (25% <= 30%)
        // Net Savings: 3.500.000 (35% >= 20%)
        val list = listOf(
            createMockItem(1, 10_000_000.0, TransactionType.INCOME, 10, "Gaji"),
            createMockItem(2, 4_000_000.0, TransactionType.EXPENSE, 2, "Transportasi"), // In NEEDS_CATEGORY_IDS
            createMockItem(3, 2_500_000.0, TransactionType.EXPENSE, 1, "Hiburan") // Not in NEEDS_CATEGORY_IDS -> Wants
        )

        val metrics = FinancialDecisionEngine.calculateDecisionMetrics(list, totalCashBalance = 15_000_000.0)

        assertNotNull(metrics)
        assertEquals(4_000_000.0, metrics.ruleAnalysis.needsAmount, 0.01)
        assertEquals(2_500_000.0, metrics.ruleAnalysis.wantsAmount, 0.01)
        assertEquals(3_500_000.0, metrics.ruleAnalysis.savingsAmount, 0.01)

        assertEquals(40.0f, metrics.ruleAnalysis.needsPercentage, 0.5f)
        assertEquals(25.0f, metrics.ruleAnalysis.wantsPercentage, 0.5f)
        assertEquals(35.0f, metrics.ruleAnalysis.savingsPercentage, 0.5f)
        assertTrue("Allocation must be healthy when within thresholds", metrics.ruleAnalysis.isHealthy)
    }

    @Test
    fun `TC-PLAN-002 - test 50-30-20 Rule unhealthy allocation warning when wants exceed limit`() {
        // Income: 5.000.000
        // Needs (cat 3 - Belanja Kebutuhan): 2.000.000 (40%)
        // Wants (cat 5 - Shopping): 2.500.000 (50% > 35%)
        // Net Savings: 500.000 (10% < 15%)
        val list = listOf(
            createMockItem(1, 5_000_000.0, TransactionType.INCOME, 10, "Gaji"),
            createMockItem(2, 2_000_000.0, TransactionType.EXPENSE, 3, "Belanja Kebutuhan"),
            createMockItem(3, 2_500_000.0, TransactionType.EXPENSE, 5, "Shopping")
        )

        val metrics = FinancialDecisionEngine.calculateDecisionMetrics(list, totalCashBalance = 500_000.0)

        assertFalse("Allocation must not be healthy when wants exceed 35%", metrics.ruleAnalysis.isHealthy)
        assertTrue(metrics.ruleAnalysis.recommendation.contains("Pos Keinginan & Jajan"))
    }

    @Test
    fun `TC-PLAN-003 - test spending leak detection for repeated micro transactions`() {
        // 4x micro-expenses <= 50.000 in same category should trigger leak warning
        val list = listOf(
            createMockItem(1, 20_000.0, TransactionType.EXPENSE, 9, "Kopi & Minuman"),
            createMockItem(2, 25_000.0, TransactionType.EXPENSE, 9, "Kopi & Minuman"),
            createMockItem(3, 15_000.0, TransactionType.EXPENSE, 9, "Kopi & Minuman"),
            createMockItem(4, 30_000.0, TransactionType.EXPENSE, 9, "Kopi & Minuman")
        )

        val metrics = FinancialDecisionEngine.calculateDecisionMetrics(list, totalCashBalance = 1_000_000.0)

        assertEquals(1, metrics.topSpendingLeaks.size)
        val leak = metrics.topSpendingLeaks[0]
        assertEquals(4, leak.transactionCount)
        assertEquals(90_000.0, leak.totalAmount, 0.01)
        assertEquals(36_000.0, leak.potentialSavingsMonthly, 0.01) // 40% target
    }

    @Test
    fun `TC-PLAN-004 - test projected runway calculation based on daily burn rate`() {
        val list = listOf(
            createMockItem(1, 300_000.0, TransactionType.EXPENSE, 2, "Transportasi")
        )

        val totalBalance = 3_000_000.0
        val metrics = FinancialDecisionEngine.calculateDecisionMetrics(list, totalCashBalance = totalBalance)

        assertTrue(metrics.dailyBurnRate > 0)
        assertTrue(metrics.projectedRunwayDays in 1..365)
    }

    @Test
    fun `TC-EDGE-001 - test zero state handling without divide by zero crash`() {
        val emptyList = emptyList<TransactionWithCategory>()
        val metrics = FinancialDecisionEngine.calculateDecisionMetrics(emptyList, totalCashBalance = 0.0)

        assertNotNull(metrics)
        assertEquals(0.0, metrics.ruleAnalysis.needsAmount, 0.001)
        assertEquals(0.0, metrics.ruleAnalysis.wantsAmount, 0.001)
        assertEquals(0.0, metrics.ruleAnalysis.savingsAmount, 0.001)
        assertEquals(0.0, metrics.dailyBurnRate, 0.001)
        assertEquals(99, metrics.projectedRunwayDays)
    }
}
