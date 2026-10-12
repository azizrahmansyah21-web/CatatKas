package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

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
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            CategoryEntity(
                name = "Makanan & Minuman",
                type = TransactionType.EXPENSE,
                iconKey = "restaurant",
                colorHex = "#EF4444",
                isDefault = true,
                orderIndex = 1
            ),
            CategoryEntity(
                name = "Transportasi",
                type = TransactionType.EXPENSE,
                iconKey = "directions_car",
                colorHex = "#F59E0B",
                isDefault = true,
                orderIndex = 2
            ),
            CategoryEntity(
                name = "Belanja & Kebutuhan",
                type = TransactionType.EXPENSE,
                iconKey = "shopping_cart",
                colorHex = "#3B82F6",
                isDefault = true,
                orderIndex = 3
            ),
            CategoryEntity(
                name = "Tagihan & Utilitas",
                type = TransactionType.EXPENSE,
                iconKey = "receipt_long",
                colorHex = "#8B5CF6",
                isDefault = true,
                orderIndex = 4
            ),
            CategoryEntity(
                name = "Hiburan & Hobi",
                type = TransactionType.EXPENSE,
                iconKey = "movie",
                colorHex = "#EC4899",
                isDefault = true,
                orderIndex = 5
            ),
            CategoryEntity(
                name = "Kesehatan & Obat",
                type = TransactionType.EXPENSE,
                iconKey = "local_hospital",
                colorHex = "#10B981",
                isDefault = true,
                orderIndex = 6
            ),
            CategoryEntity(
                name = "Pendidikan & Buku",
                type = TransactionType.EXPENSE,
                iconKey = "school",
                colorHex = "#06B6D4",
                isDefault = true,
                orderIndex = 7
            ),
            CategoryEntity(
                name = "Tempat Tinggal",
                type = TransactionType.EXPENSE,
                iconKey = "home",
                colorHex = "#6366F1",
                isDefault = true,
                orderIndex = 8
            ),
            CategoryEntity(
                name = "Keluarga & Hadiah",
                type = TransactionType.EXPENSE,
                iconKey = "favorite",
                colorHex = "#F43F5E",
                isDefault = true,
                orderIndex = 9
            ),
            CategoryEntity(
                name = "Pengeluaran Lainnya",
                type = TransactionType.EXPENSE,
                iconKey = "more_horiz",
                colorHex = "#64748B",
                isDefault = true,
                orderIndex = 10
            ),

            // Income Categories
            CategoryEntity(
                name = "Gaji Pokok",
                type = TransactionType.INCOME,
                iconKey = "payments",
                colorHex = "#10B981",
                isDefault = true,
                orderIndex = 11
            ),
            CategoryEntity(
                name = "Bonus & Insentif",
                type = TransactionType.INCOME,
                iconKey = "savings",
                colorHex = "#059669",
                isDefault = true,
                orderIndex = 12
            ),
            CategoryEntity(
                name = "Bisnis & Penjualan",
                type = TransactionType.INCOME,
                iconKey = "storefront",
                colorHex = "#3B82F6",
                isDefault = true,
                orderIndex = 13
            ),
            CategoryEntity(
                name = "Investasi & Dividen",
                type = TransactionType.INCOME,
                iconKey = "trending_up",
                colorHex = "#8B5CF6",
                isDefault = true,
                orderIndex = 14
            ),
            CategoryEntity(
                name = "Pemasukan Lainnya",
                type = TransactionType.INCOME,
                iconKey = "account_balance_wallet",
                colorHex = "#64748B",
                isDefault = true,
                orderIndex = 15
            )
        )
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.categoryDao())
                }
            }
        }

        private suspend fun populateInitialData(categoryDao: CategoryDao) {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DEFAULT_CATEGORIES)
            }
        }
    }
}
