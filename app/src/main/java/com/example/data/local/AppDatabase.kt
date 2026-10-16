package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CashBreakdownEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.DecisionPlanEntity
import com.example.data.model.MerchantKnowledgeEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.ImportBatchEntity
import com.example.data.model.WalletEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        WalletEntity::class,
        CashBreakdownEntity::class,
        MerchantKnowledgeEntity::class,
        DecisionPlanEntity::class,
        ImportBatchEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun cashBreakdownDao(): CashBreakdownDao
    abstract fun merchantKnowledgeDao(): MerchantKnowledgeDao
    abstract fun decisionPlanDao(): DecisionPlanDao
    abstract fun importBatchDao(): ImportBatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "catatkas_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            CategoryEntity(
                id = 1,
                name = "Makanan & Minuman",
                type = TransactionType.EXPENSE,
                iconKey = "restaurant",
                colorHex = "#EF4444",
                isDefault = true,
                orderIndex = 1
            ),
            CategoryEntity(
                id = 2,
                name = "Transportasi",
                type = TransactionType.EXPENSE,
                iconKey = "directions_car",
                colorHex = "#F59E0B",
                isDefault = true,
                orderIndex = 2
            ),
            CategoryEntity(
                id = 3,
                name = "Belanja & Kebutuhan",
                type = TransactionType.EXPENSE,
                iconKey = "shopping_cart",
                colorHex = "#3B82F6",
                isDefault = true,
                orderIndex = 3
            ),
            CategoryEntity(
                id = 4,
                name = "Tagihan & Utilitas",
                type = TransactionType.EXPENSE,
                iconKey = "receipt_long",
                colorHex = "#8B5CF6",
                isDefault = true,
                orderIndex = 4
            ),
            CategoryEntity(
                id = 5,
                name = "Hiburan & Hobi",
                type = TransactionType.EXPENSE,
                iconKey = "movie",
                colorHex = "#EC4899",
                isDefault = true,
                orderIndex = 5
            ),
            CategoryEntity(
                id = 6,
                name = "Kesehatan & Obat",
                type = TransactionType.EXPENSE,
                iconKey = "local_hospital",
                colorHex = "#10B981",
                isDefault = true,
                orderIndex = 6
            ),
            CategoryEntity(
                id = 7,
                name = "Biaya Admin & Bank",
                type = TransactionType.EXPENSE,
                iconKey = "payments",
                colorHex = "#64748B",
                isDefault = true,
                orderIndex = 7
            ),
            CategoryEntity(
                id = 8,
                name = "Tempat Tinggal",
                type = TransactionType.EXPENSE,
                iconKey = "home",
                colorHex = "#6366F1",
                isDefault = true,
                orderIndex = 8
            ),
            CategoryEntity(
                id = 9,
                name = "Keluarga & Hadiah",
                type = TransactionType.EXPENSE,
                iconKey = "favorite",
                colorHex = "#F43F5E",
                isDefault = true,
                orderIndex = 9
            ),
            CategoryEntity(
                id = 10,
                name = "Pengeluaran Lainnya",
                type = TransactionType.EXPENSE,
                iconKey = "more_horiz",
                colorHex = "#64748B",
                isDefault = true,
                orderIndex = 10
            ),

            // Income Categories
            CategoryEntity(
                id = 11,
                name = "Gaji Pokok",
                type = TransactionType.INCOME,
                iconKey = "payments",
                colorHex = "#10B981",
                isDefault = true,
                orderIndex = 11
            ),
            CategoryEntity(
                id = 12,
                name = "Pemasukan QRIS Usaha",
                type = TransactionType.INCOME,
                iconKey = "storefront",
                colorHex = "#059669",
                isDefault = true,
                orderIndex = 12
            ),
            CategoryEntity(
                id = 13,
                name = "Bonus & Insentif",
                type = TransactionType.INCOME,
                iconKey = "savings",
                colorHex = "#3B82F6",
                isDefault = true,
                orderIndex = 13
            ),
            CategoryEntity(
                id = 14,
                name = "Investasi & Dividen",
                type = TransactionType.INCOME,
                iconKey = "trending_up",
                colorHex = "#8B5CF6",
                isDefault = true,
                orderIndex = 14
            ),
            CategoryEntity(
                id = 15,
                name = "Pemasukan Lainnya",
                type = TransactionType.INCOME,
                iconKey = "account_balance_wallet",
                colorHex = "#64748B",
                isDefault = true,
                orderIndex = 15
            )
        )

        val DEFAULT_WALLETS = listOf(
            WalletEntity(
                id = 1,
                name = "Rekening Mandiri",
                walletType = "BANK",
                iconKey = "account_balance",
                isDefault = true,
                colorHex = "#3B82F6"
            ),
            WalletEntity(
                id = 2,
                name = "GoPay Pribadi",
                walletType = "EWALLET",
                iconKey = "phone_android",
                isDefault = true,
                colorHex = "#06B6D4"
            ),
            WalletEntity(
                id = 3,
                name = "OVO Pribadi",
                walletType = "EWALLET",
                iconKey = "account_balance_wallet",
                isDefault = true,
                colorHex = "#8B5CF6"
            ),
            WalletEntity(
                id = 4,
                name = "GoPay Merchant QRIS",
                walletType = "MERCHANT",
                iconKey = "storefront",
                isDefault = true,
                colorHex = "#10B981"
            ),
            WalletEntity(
                id = 5,
                name = "Dompet Tunai",
                walletType = "CASH",
                iconKey = "payments",
                isDefault = true,
                colorHex = "#F59E0B"
            )
        )

        val SEED_MERCHANT_KNOWLEDGE = listOf(
            // Food & Beverage (CategoryID = 1)
            MerchantKnowledgeEntity(keyword = "KOPI", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "KENANGAN", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "FORE", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "STARBUCKS", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "WARTEG", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "RESTO", categoryId = 1, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "KULINER", categoryId = 1, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "MCDONALD", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "KFC", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "HOKBEN", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "MIE GACOAN", categoryId = 1, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "GOFOOD", categoryId = 1, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "SHOPEEFOOD", categoryId = 1, confidenceWeight = 0.9f),

            // Transportation (CategoryID = 2)
            MerchantKnowledgeEntity(keyword = "PERTAMINA", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "SPBU", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "SHELL", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "GORIDE", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "GOCAR", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "GRAB", categoryId = 2, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "KAI", categoryId = 2, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "PARKIR", categoryId = 2, confidenceWeight = 0.9f),

            // Groceries / Shopping (CategoryID = 3)
            MerchantKnowledgeEntity(keyword = "INDOMARET", categoryId = 3, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "ALFAMART", categoryId = 3, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "ALFAMIDI", categoryId = 3, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "SUPERINDO", categoryId = 3, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "TOKOPEDIA", categoryId = 3, confidenceWeight = 0.8f),
            MerchantKnowledgeEntity(keyword = "SHOPEE", categoryId = 3, confidenceWeight = 0.8f),

            // Bills & Utilities (CategoryID = 4)
            MerchantKnowledgeEntity(keyword = "PLN", categoryId = 4, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "INDIHOME", categoryId = 4, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "BPJS", categoryId = 4, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "TELKOM", categoryId = 4, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "PULSA", categoryId = 4, confidenceWeight = 0.9f),

            // Bank Admin & Taxes (CategoryID = 7)
            MerchantKnowledgeEntity(keyword = "BIAYA ADM", categoryId = 7, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "BI-FAST", categoryId = 7, confidenceWeight = 0.9f),
            MerchantKnowledgeEntity(keyword = "PAJAK", categoryId = 7, confidenceWeight = 1.0f),

            // Incomes (CategoryID = 11 & 12)
            MerchantKnowledgeEntity(keyword = "PAYROLL", categoryId = 11, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "GAJI", categoryId = 11, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "QRIS MERCHANT", categoryId = 12, confidenceWeight = 1.0f),
            MerchantKnowledgeEntity(keyword = "BUNGA TABUNGAN", categoryId = 14, confidenceWeight = 1.0f)
        )
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DEFAULT_CATEGORIES)
            }

            val walletDao = database.walletDao()
            if (walletDao.getWalletCount() == 0) {
                walletDao.insertWallets(DEFAULT_WALLETS)
            }

            val merchantKnowledgeDao = database.merchantKnowledgeDao()
            if (merchantKnowledgeDao.getKnowledgeCount() == 0) {
                merchantKnowledgeDao.insertKnowledgeList(SEED_MERCHANT_KNOWLEDGE)
            }
        }
    }
}
