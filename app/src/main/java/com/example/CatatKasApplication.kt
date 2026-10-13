package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.data.repository.PreferencesRepository
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CatatKasApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val financeRepository by lazy { FinanceRepository(database.categoryDao(), database.transactionDao(), database.walletDao(), database.cashBreakdownDao(), database.merchantKnowledgeDao()) }
    val preferencesRepository by lazy { PreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                val prefs = preferencesRepository.userPreferences.first()
                if (prefs.reminderEnabled) {
                    ReminderScheduler.scheduleReminder(
                        context = this@CatatKasApplication,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
