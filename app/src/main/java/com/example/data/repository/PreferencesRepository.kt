package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "catatkas_prefs")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val biometricEnabled: Boolean = false,
    val pinCode: String = "",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val lastBackupTimestamp: Long = 0L
)

class PreferencesRepository(private val context: Context) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val PIN_CODE_KEY = stringPreferencesKey("pin_code")
    private val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
    private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
    private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_timestamp")

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val themeString = preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (e: Exception) {
            ThemeMode.DARK
        }

        UserPreferences(
            themeMode = themeMode,
            biometricEnabled = preferences[BIOMETRIC_ENABLED_KEY] ?: false,
            pinCode = preferences[PIN_CODE_KEY] ?: "",
            reminderEnabled = preferences[REMINDER_ENABLED_KEY] ?: true,
            reminderHour = preferences[REMINDER_HOUR_KEY] ?: 20,
            reminderMinute = preferences[REMINDER_MINUTE_KEY] ?: 0,
            lastBackupTimestamp = preferences[LAST_BACKUP_KEY] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { it[PIN_CODE_KEY] = pin }
    }

    suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[REMINDER_ENABLED_KEY] = enabled
            it[REMINDER_HOUR_KEY] = hour
            it[REMINDER_MINUTE_KEY] = minute
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { it[LAST_BACKUP_KEY] = timestamp }
    }
}
