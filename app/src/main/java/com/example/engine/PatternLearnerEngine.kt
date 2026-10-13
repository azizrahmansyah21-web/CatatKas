package com.example.engine

import com.example.data.local.MerchantKnowledgeDao
import com.example.data.model.CategoryEntity
import com.example.data.model.MerchantKnowledgeEntity
import com.example.data.model.TransactionType

data class ClassifiedTransaction(
    val mutation: ParsedMutationItem,
    val selectedCategory: CategoryEntity,
    val confidence: Float,
    val isAutoLearned: Boolean
)

class PatternLearnerEngine(
    private val merchantKnowledgeDao: MerchantKnowledgeDao
) {

    suspend fun classifyMutation(
        item: ParsedMutationItem,
        availableCategories: List<CategoryEntity>
    ): ClassifiedTransaction {
        val upperDesc = item.rawDescription.uppercase()
        val allKnowledge = merchantKnowledgeDao.getAllKnowledge()

        // 1. Check exact or partial match in learned knowledge base
        for (knowledge in allKnowledge) {
            if (upperDesc.contains(knowledge.keyword.uppercase())) {
                val matchedCategory = availableCategories.find { it.id == knowledge.categoryId }
                if (matchedCategory != null) {
                    return ClassifiedTransaction(
                        mutation = item,
                        selectedCategory = matchedCategory,
                        confidence = knowledge.confidenceWeight,
                        isAutoLearned = true
                    )
                }
            }
        }

        // 2. Default Fallback based on transaction type (Debit vs Credit)
        val defaultCategory = if (item.isDebit) {
            availableCategories.firstOrNull { it.id == 1L } // Makanan & Minuman default
                ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }
                ?: availableCategories.first()
        } else {
            availableCategories.firstOrNull { it.id == 11L } // Gaji Pokok default
                ?: availableCategories.firstOrNull { it.type == TransactionType.INCOME }
                ?: availableCategories.first()
        }

        return ClassifiedTransaction(
            mutation = item,
            selectedCategory = defaultCategory,
            confidence = 0.5f,
            isAutoLearned = false
        )
    }

    suspend fun learnUserCorrection(
        cleanedMerchant: String,
        newCategoryId: Long
    ) {
        val cleanKeyword = extractMainKeyword(cleanedMerchant)
        if (cleanKeyword.length < 3) return

        val existing = merchantKnowledgeDao.findKnowledgeByKeyword(cleanKeyword)
        if (existing != null) {
            merchantKnowledgeDao.insertOrUpdate(
                existing.copy(
                    categoryId = newCategoryId,
                    confidenceWeight = 1.0f,
                    timesUsed = existing.timesUsed + 1
                )
            )
        } else {
            merchantKnowledgeDao.insertOrUpdate(
                MerchantKnowledgeEntity(
                    keyword = cleanKeyword,
                    categoryId = newCategoryId,
                    confidenceWeight = 1.0f,
                    timesUsed = 1
                )
            )
        }
    }

    private fun extractMainKeyword(merchantName: String): String {
        val tokens = merchantName.split(" ", "-", "_", ".")
            .map { it.trim().uppercase() }
            .filter { it.length >= 3 }
        return tokens.firstOrNull() ?: merchantName.take(15).uppercase()
    }
}
