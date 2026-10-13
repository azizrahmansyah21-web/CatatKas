package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.CategorySpending
import com.example.data.model.DailyCashflow
import com.example.data.model.DecisionPlanEntity
import com.example.data.model.MonthlySummary
import com.example.data.model.PlanStatus
import com.example.data.model.PlanType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.WalletEntity
import com.example.data.repository.FinanceRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.engine.ExcelCsvStatementParser
import com.example.engine.FinancialDecisionMetrics
import com.example.engine.ReceiptOcrScanner
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
    FINANCIAL_PLAN,
    PROFILE
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

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

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

    val allCategories: StateFlow<List<CategoryEntity>> = financeRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allWallets: StateFlow<List<WalletEntity>> = financeRepository.allWallets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyTransactions: StateFlow<List<TransactionWithCategory>> =
        combine(_selectedYear, _selectedMonth, _activeFilter, _selectedCategoryId, _searchQuery) { year, month, filter, catId, query ->
            MonthlyQueryParam(year, month, filter, catId, query)
        }.flatMapLatest { param ->
            financeRepository.getTransactionsForMonth(param.year, param.month)
                .combine(_activeFilter) { list, filter ->
                    if (filter != null) list.filter { it.transaction.type == filter } else list
                }
                .combine(_selectedCategoryId) { list, catId ->
                    if (catId != null) list.filter { it.transaction.categoryId == catId } else list
                }
                .combine(_searchQuery) { list, query ->
                    if (query.isBlank()) {
                        list
                    } else {
                        val q = query.trim().lowercase()
                        list.filter {
                            it.transaction.note.lowercase().contains(q) ||
                                    (it.category?.name?.lowercase()?.contains(q) ?: false)
                        }
                    }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlySummary: StateFlow<MonthlySummary> =
        combine(_selectedYear, _selectedMonth) { year, month -> Pair(year, month) }
            .flatMapLatest { (year, month) ->
                financeRepository.getMonthlySummary(year, month)
            }.stateIn(
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

    private val _decisionMetrics = MutableStateFlow<FinancialDecisionMetrics?>(null)
    val decisionMetrics: StateFlow<FinancialDecisionMetrics?> = _decisionMetrics.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferences
                .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())
                .value
            if (prefs.biometricEnabled || prefs.pinCode.isNotEmpty()) {
                _isAppLocked.value = true
            }
        }
        refreshDecisionMetrics()
    }

    fun refreshDecisionMetrics() {
        viewModelScope.launch {
            try {
                val metrics = financeRepository.getDecisionMetricsForMonth(_selectedYear.value, _selectedMonth.value)
                _decisionMetrics.value = metrics
            } catch (_: Exception) {}
        }
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, _selectedYear.value)
        cal.set(Calendar.MONTH, _selectedMonth.value)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        if (offset == 0) {
            val nowCal = Calendar.getInstance()
            _selectedYear.value = nowCal.get(Calendar.YEAR)
            _selectedMonth.value = nowCal.get(Calendar.MONTH)
        } else {
            cal.add(Calendar.MONTH, offset)
            _selectedYear.value = cal.get(Calendar.YEAR)
            _selectedMonth.value = cal.get(Calendar.MONTH)
        }
        refreshDecisionMetrics()
    }

    fun setNavTab(tab: NavTab) {
        _currentTab.value = tab
        if (tab == NavTab.STATISTICS) {
            refreshDecisionMetrics()
        }
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

    fun openEditTransaction(item: TransactionWithCategory) {
        _editingTransaction.value = item
        _isTransactionSheetOpen.value = true
    }

    fun closeTransactionSheet() {
        _isTransactionSheetOpen.value = false
        _editingTransaction.value = null
    }

    fun saveTransaction(
        id: Long,
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
                _uiMessage.emit("Transaksi berhasil disimpan")
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
            refreshDecisionMetrics()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(transaction)
            _uiMessage.emit("Transaksi dihapus")
            refreshDecisionMetrics()
        }
    }

    // Smart Import Excel / CSV Handler
    fun importExcelCsvStatement(context: Context, uri: Uri, passwordAttempt: String?) {
        viewModelScope.launch {
            _uiMessage.emit("Memproses berkas e-statement...")
            val result = ExcelCsvStatementParser.parseStatementFile(context, uri, passwordAttempt)
            if (!result.success) {
                _uiMessage.emit(result.errorMessage ?: "Gagal memproses e-statement")
                return@launch
            }

            val categories = allCategories.value
            val defaultExpenseCat = categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id ?: 1L
            val defaultIncomeCat = categories.firstOrNull { it.type == TransactionType.INCOME }?.id ?: 5L

            var importedCount = 0
            for (rec in result.records) {
                val catId = if (rec.isExpense) defaultExpenseCat else defaultIncomeCat
                val entity = TransactionEntity(
                    amount = rec.amount,
                    type = if (rec.isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                    categoryId = catId,
                    wallet = rec.suspectedWallet,
                    dateMillis = rec.dateMillis,
                    note = rec.description
                )
                financeRepository.insertTransaction(entity)
                importedCount++
            }

            _uiMessage.emit("Berhasil mengimpor $importedCount transaksi dari berkas e-statement!")
            refreshDecisionMetrics()
        }
    }

    // Receipt OCR Scanning Handler
    fun processReceiptScanFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = ReceiptOcrScanner.scanFromBitmap(bitmap)
            val categories = allCategories.value
            val matchedCat = categories.firstOrNull { it.name.contains("Makan", ignoreCase = true) }
                ?: categories.firstOrNull { it.type == result.type }

            val entity = TransactionEntity(
                amount = result.amount,
                type = result.type,
                categoryId = matchedCat?.id ?: 1L,
                wallet = result.paymentSource,
                dateMillis = result.dateMillis,
                note = "${result.merchantName} (${result.note})"
            )
            financeRepository.insertTransaction(entity)
            _uiMessage.emit("Berhasil memindai bukti bayar: ${result.merchantName} - Rp ${result.amount.toInt()}")
            refreshDecisionMetrics()
        }
    }

    fun processReceiptScanFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = ReceiptOcrScanner.scanFromUri(context, uri)
            val categories = allCategories.value
            val matchedCat = categories.firstOrNull { it.type == result.type }

            val entity = TransactionEntity(
                amount = result.amount,
                type = result.type,
                categoryId = matchedCat?.id ?: 1L,
                wallet = result.paymentSource,
                dateMillis = result.dateMillis,
                note = "${result.merchantName} (${result.note})"
            )
            financeRepository.insertTransaction(entity)
            _uiMessage.emit("Berhasil memindai screenshot bukti bayar: Rp ${result.amount.toInt()}")
            refreshDecisionMetrics()
        }
    }

    // Category CRUD
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

    fun saveCategory(id: Long, name: String, type: TransactionType, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            if (id == 0L) {
                financeRepository.insertCategory(
                    CategoryEntity(
                        name = name,
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex
                    )
                )
                _uiMessage.emit("Kategori baru ditambahkan")
            } else {
                financeRepository.updateCategory(
                    CategoryEntity(
                        id = id,
                        name = name,
                        type = type,
                        iconKey = iconKey,
                        colorHex = colorHex
                    )
                )
                _uiMessage.emit("Kategori diperbarui")
            }
            closeCategorySheet()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            financeRepository.deleteCategory(category)
            _uiMessage.emit("Kategori dihapus")
        }
    }

    // Preferences & Settings
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
            _uiMessage.emit(if (enabled) "Biometrik diaktifkan" else "Biometrik dimatikan")
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            preferencesRepository.setPinCode(pin)
            _uiMessage.emit(if (pin.isEmpty()) "PIN dihapus" else "PIN disimpan")
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int, context: Context) {
        viewModelScope.launch {
            preferencesRepository.setReminderSettings(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleReminder(context, hour, minute)
                _uiMessage.emit("Pengingat harian diatur ke pukul %02d:%02d".format(hour, minute))
            } else {
                ReminderScheduler.cancelReminder(context)
                _uiMessage.emit("Pengingat harian dinonaktifkan")
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun exportReport(context: Context, format: String) {
        viewModelScope.launch {
            val transactions = financeRepository.allTransactions
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .value
            val period = DateUtils.formatMonthYear(_selectedYear.value, _selectedMonth.value)
            if (format.equals("CSV", ignoreCase = true)) {
                val file = ReportExporter.exportToCsv(context, period, transactions)
                if (file != null) {
                    ReportExporter.shareExportFile(context, file, "text/csv", "Laporan Keuangan CSV")
                }
            } else {
                val file = ReportExporter.exportToPdf(context, period, transactions)
                if (file != null) {
                    ReportExporter.shareExportFile(context, file, "application/pdf", "Laporan Keuangan PDF")
                }
            }
            _uiMessage.emit("Laporan  berhasil diekspor")
        }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            try {
                val payload = financeRepository.exportBackupPayload()
                val file = DataBackupManager.writeBackupFile(context, payload)
                _uiMessage.emit(if (file != null) "Cadangan data berhasil dibuat" else "Gagal membuat cadangan")
            } catch (e: Exception) {
                _uiMessage.emit("Gagal membuat cadangan: ${e.localizedMessage}")
            }
        }
    }

    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val payload = DataBackupManager.readBackupFromUri(context, uri)
                if (payload != null) {
                    val success = financeRepository.restoreBackup(payload)
                    _uiMessage.emit(if (success) "Berhasil memulihkan cadangan transaksi" else "Gagal memulihkan cadangan")
                    refreshDecisionMetrics()
                } else {
                    _uiMessage.emit("Format cadangan tidak valid")
                }
            } catch (e: Exception) {
                _uiMessage.emit("Gagal memulihkan: ${e.localizedMessage}")
            }
        }
    }

    // --- Decision Planning & Wishlist Methods ---
    val allDecisionPlans: StateFlow<List<DecisionPlanEntity>> = financeRepository.allDecisionPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addDecisionPlan(
        title: String,
        estimatedPrice: Double,
        planType: PlanType,
        urgencyLevel: Int,
        categoryId: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val score = calculateDecisionScore(estimatedPrice, planType, urgencyLevel)
            val plan = DecisionPlanEntity(
                title = title,
                estimatedPrice = estimatedPrice,
                planType = planType,
                urgencyLevel = urgencyLevel,
                categoryId = categoryId,
                note = note,
                decisionScore = score,
                coolingOffHours = if (planType == PlanType.WANTS) 72 else 24
            )
            financeRepository.insertDecisionPlan(plan)
            _uiMessage.emit("Rencana '$title' dicatat ke Rencana Kas")
        }
    }

    fun updateDecisionPlan(plan: DecisionPlanEntity) {
        viewModelScope.launch {
            financeRepository.updateDecisionPlan(plan)
            _uiMessage.emit("Rencana diperbarui")
        }
    }

    fun deleteDecisionPlan(plan: DecisionPlanEntity) {
        viewModelScope.launch {
            financeRepository.deleteDecisionPlan(plan)
            _uiMessage.emit("Rencana dihapus")
        }
    }

    fun realizeDecisionPlan(plan: DecisionPlanEntity, walletName: String = "Mandiri") {
        viewModelScope.launch {
            financeRepository.realizeDecisionPlan(plan, walletName)
            _uiMessage.emit("'${plan.title}' direalisasikan menjadi transaksi pengeluaran!")
            refreshDecisionMetrics()
        }
    }

    private fun calculateDecisionScore(price: Double, planType: PlanType, urgency: Int): Int {
        var baseScore = if (planType == PlanType.NEEDS) 85 else 55
        baseScore += (urgency - 3) * 8
        if (price > 500_000.0) baseScore -= 10
        if (price > 2_000_000.0) baseScore -= 15
        return baseScore.coerceIn(10, 99)
    }

    private data class MonthlyQueryParam(
        val year: Int,
        val month: Int,
        val filter: TransactionType?,
        val catId: Long?,
        val query: String
    )
}

class MainViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(financeRepository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
