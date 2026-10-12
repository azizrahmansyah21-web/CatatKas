package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.DailyCashflow
import com.example.data.model.MonthlySummary
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.repository.FinanceRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.receiver.ReminderScheduler
import com.example.util.DataBackupManager
import com.example.util.DateUtils
import com.example.util.ReportExporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavTab {
    TRANSACTIONS,
    STATISTICS,
    CATEGORIES,
    SETTINGS
}

class MainViewModel(
    private val financeRepository: FinanceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val now = Calendar.getInstance()

    private val _selectedYear = MutableStateFlow(now.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(now.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.TRANSACTIONS)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _activeFilter = MutableStateFlow<TransactionType?>(null)
    val activeFilter: StateFlow<TransactionType?> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Transaction Sheet state
    private val _isTransactionSheetOpen = MutableStateFlow(false)
    val isTransactionSheetOpen: StateFlow<Boolean> = _isTransactionSheetOpen.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionWithCategory?>(null)
    val editingTransaction: StateFlow<TransactionWithCategory?> = _editingTransaction.asStateFlow()

    // Category Sheet state
    private val _isCategorySheetOpen = MutableStateFlow(false)
    val isCategorySheetOpen: StateFlow<Boolean> = _isCategorySheetOpen.asStateFlow()

    private val _editingCategory = MutableStateFlow<CategoryEntity?>(null)
    val editingCategory: StateFlow<CategoryEntity?> = _editingCategory.asStateFlow()

    // Security Lock State
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // UI Events (Snackbar / Toast notifications)
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    init {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                if (prefs.biometricEnabled || prefs.pinCode.isNotBlank()) {
                    _isAppLocked.value = true
                }
            }
        }
    }

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        _selectedYear,
        _selectedMonth
    ) { year, month -> Pair(year, month) }
        .flatMapLatest { (year, month) ->
            financeRepository.getTransactionsForMonth(year, month)
        }
        .combine(_activeFilter) { list, filter ->
            if (filter == null) list else list.filter { it.transaction.type == filter }
        }
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.transaction.note.contains(query, ignoreCase = true) ||
                        (it.category?.name?.contains(query, ignoreCase = true) == true) ||
                        it.transaction.wallet.contains(query, ignoreCase = true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlySummary: StateFlow<MonthlySummary> = combine(
        _selectedYear,
        _selectedMonth
    ) { year, month -> Pair(year, month) }
        .flatMapLatest { (year, month) ->
            financeRepository.getMonthlySummary(year, month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlySummary(
                year = now.get(Calendar.YEAR),
                month = now.get(Calendar.MONTH),
                totalIncome = 0.0,
                totalExpense = 0.0,
                netBalance = 0.0,
                savingsRatePercentage = 0f,
                topSpendingCategory = null,
                categorySpendings = emptyList(),
                dailyCashflows = emptyList()
            )
        )

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, offset)
        }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
    }

    fun setFilter(type: TransactionType?) {
        _activeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddTransaction() {
        _editingTransaction.value = null
        _isTransactionSheetOpen.value = true
    }

    fun openEditTransaction(transactionWithCategory: TransactionWithCategory) {
        _editingTransaction.value = transactionWithCategory
        _isTransactionSheetOpen.value = true
    }

    fun closeTransactionSheet() {
        _isTransactionSheetOpen.value = false
        _editingTransaction.value = null
    }

    fun saveTransaction(
        id: Long = 0,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        wallet: String,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                financeRepository.insertTransaction(
                    TransactionEntity(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                )
                _uiMessage.emit("Transaksi berhasil dicatat 🎉")
            } else {
                financeRepository.updateTransaction(
                    TransactionEntity(
                        id = id,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                )
                _uiMessage.emit("Transaksi berhasil diperbarui")
            }
            closeTransactionSheet()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(transaction)
            _uiMessage.emit("Transaksi telah dihapus")
        }
    }

    fun openAddCategory() {
        _editingCategory.value = null
        _isCategorySheetOpen.value = true
    }

    fun openEditCategory(category: CategoryEntity) {
        _editingCategory.value = category
        _isCategorySheetOpen.value = true
    }

    fun closeCategorySheet() {
        _isCategorySheetOpen.value = false
        _editingCategory.value = null
    }

    fun saveCategory(
        id: Long = 0,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                financeRepository.insertCategory(
                    CategoryEntity(
                        name = name.trim(),
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isDefault = false
                    )
                )
                _uiMessage.emit("Kategori baru berhasil ditambahkan ✨")
            } else {
                financeRepository.updateCategory(
                    CategoryEntity(
                        id = id,
                        name = name.trim(),
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        isDefault = false
                    )
                )
                _uiMessage.emit("Kategori berhasil diperbarui")
            }
            closeCategorySheet()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            financeRepository.deleteCategory(category)
            _uiMessage.emit("Kategori '${category.name}' telah dihapus")
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        val prefs = userPreferences.value
        if (prefs.biometricEnabled || prefs.pinCode.isNotBlank()) {
            _isAppLocked.value = true
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
            val msg = if (enabled) "Proteksi biometrik diaktifkan 🔒" else "Proteksi biometrik dinonaktifkan"
            _uiMessage.emit(msg)
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            preferencesRepository.setPinCode(pin)
            val msg = if (pin.isNotBlank()) "PIN keamanan diperbarui" else "PIN dinonaktifkan"
            _uiMessage.emit(msg)
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            preferencesRepository.setReminderSettings(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleReminder(context, hour, minute)
                _uiMessage.emit("Pengingat harian disetel untuk ${String.format("%02d:%02d", hour, minute)} ⏰")
            } else {
                ReminderScheduler.cancelReminder(context)
                _uiMessage.emit("Pengingat harian dimatikan")
            }
        }
    }

    fun exportReport(context: Context, format: String) {
        viewModelScope.launch {
            val period = DateUtils.formatMonthYear(_selectedYear.value, _selectedMonth.value)
            val transactions = financeRepository.getTransactionsForMonthList(
                _selectedYear.value,
                _selectedMonth.value
            )

            if (transactions.isEmpty()) {
                _uiMessage.emit("Belum ada data transaksi di periode ini untuk diekspor")
                return@launch
            }

            if (format.equals("CSV", ignoreCase = true)) {
                val file = ReportExporter.exportToCsv(context, period, transactions)
                if (file != null) {
                    ReportExporter.shareExportFile(
                        context,
                        file,
                        "text/csv",
                        "Laporan Keuangan $period (CSV)"
                    )
                    _uiMessage.emit("Laporan CSV siap dibagikan 📊")
                } else {
                    _uiMessage.emit("Gagal membuat laporan CSV")
                }
            } else {
                val file = ReportExporter.exportToPdf(context, period, transactions)
                if (file != null) {
                    ReportExporter.shareExportFile(
                        context,
                        file,
                        "application/pdf",
                        "Laporan Keuangan $period (PDF)"
                    )
                    _uiMessage.emit("Laporan PDF siap dibagikan 📑")
                } else {
                    _uiMessage.emit("Gagal membuat laporan PDF")
                }
            }
        }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            val payload = financeRepository.exportBackupPayload()
            val file = DataBackupManager.writeBackupFile(context, payload)
            if (file != null) {
                preferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
                ReportExporter.shareExportFile(
                    context,
                    file,
                    "application/json",
                    "Cadangan Data CatatKas (JSON)"
                )
                _uiMessage.emit("Cadangan data berhasil dibuat! Simpan ke Google Drive / Cloud untuk sinkronisasi ☁️")
            } else {
                _uiMessage.emit("Gagal membuat file cadangan data")
            }
        }
    }

    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val payload = DataBackupManager.readBackupFromUri(context, uri)
            if (payload != null) {
                val success = financeRepository.restoreBackup(payload)
                if (success) {
                    preferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
                    _uiMessage.emit("Data berhasil dipulihkan & disinkronkan (${payload.transactions.size} transaksi) ✨")
                } else {
                    _uiMessage.emit("Gagal memulihkan database dari file")
                }
            } else {
                _uiMessage.emit("Format file cadangan tidak valid atau rusak")
            }
        }
    }
}

class MainViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(financeRepository, preferencesRepository) as T
    }
}
