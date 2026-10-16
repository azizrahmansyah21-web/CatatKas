package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ThemeMode
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CategoryBottomSheet
import com.example.ui.screens.FinancialPlanScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TransactionBottomSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.CatatKasTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.ui.viewmodel.NavTab

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as CatatKasApplication
        MainViewModelFactory(app.financeRepository, app.preferencesRepository)
    }

    private var biometricCancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

            val isDarkTheme = when (userPreferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CatatKasTheme(darkTheme = isDarkTheme) {
                val isLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                if (isLocked) {
                    LockScreen(
                        hasBiometric = userPreferences.biometricEnabled,
                        correctPin = userPreferences.pinCode,
                        onBiometricClick = { triggerBiometricPrompt() },
                        onUnlocked = { viewModel.unlockApp() }
                    )
                } else {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }

    private fun triggerBiometricPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val prompt = android.hardware.biometrics.BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.biometrik_prompt_title))
                .setSubtitle(getString(R.string.biometrik_prompt_subtitle))
                .setNegativeButton("Gunakan PIN", mainExecutor) { _, _ -> }
                .build()

            biometricCancellationSignal = CancellationSignal()
            prompt.authenticate(
                biometricCancellationSignal!!,
                mainExecutor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.unlockApp()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricCancellationSignal?.cancel()
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val decisionPlans by viewModel.allDecisionPlans.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val decisionMetrics by viewModel.decisionMetrics.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val importBatches by viewModel.allImportBatches.collectAsStateWithLifecycle()

    val isTransactionSheetOpen by viewModel.isTransactionSheetOpen.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val isCategorySheetOpen by viewModel.isCategorySheetOpen.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher for direct camera capture preview
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processReceiptScanFromBitmap(bitmap)
        }
    }

    // Permission launcher for Android 13+ notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // gracefully handled
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.TRANSACTIONS,
                    onClick = { viewModel.setNavTab(NavTab.TRANSACTIONS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.TRANSACTIONS)
                                Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Transaksi"
                        )
                    },
                    label = {
                        Text(
                            text = "Transaksi",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == NavTab.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_transactions")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.STATISTICS,
                    onClick = { viewModel.setNavTab(NavTab.STATISTICS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.STATISTICS)
                                Icons.Filled.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "Statistik"
                        )
                    },
                    label = {
                        Text(
                            text = "Statistik",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == NavTab.STATISTICS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_statistics")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.FINANCIAL_PLAN,
                    onClick = { viewModel.setNavTab(NavTab.FINANCIAL_PLAN) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.FINANCIAL_PLAN)
                                Icons.Filled.Psychology else Icons.Outlined.Psychology,
                            contentDescription = "Rencana Kas"
                        )
                    },
                    label = {
                        Text(
                            text = "Rencana Kas",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == NavTab.FINANCIAL_PLAN) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_financial_plan")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.PROFILE,
                    onClick = { viewModel.setNavTab(NavTab.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.PROFILE)
                                Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profil"
                        )
                    },
                    label = {
                        Text(
                            text = "Profil",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == NavTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_profile")
                )
            }
        },
        floatingActionButton = {
            // Show FAB on Transactions tab
            AnimatedVisibility(
                visible = currentTab == NavTab.TRANSACTIONS,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { viewModel.openAddTransaction() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catat Transaksi",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        year = selectedYear,
                        month = selectedMonth,
                        monthlySummary = monthlySummary,
                        transactions = monthlyTransactions,
                        activeFilter = activeFilter,
                        searchQuery = searchQuery,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onPreviousMonth = { viewModel.changeMonth(-1) },
                        onNextMonth = { viewModel.changeMonth(1) },
                        onCurrentMonth = { viewModel.changeMonth(0) },
                        onFilterChanged = { viewModel.setFilter(it) },
                        onCategorySelected = { viewModel.setSelectedCategory(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onItemClick = { viewModel.openEditTransaction(it) },
                        onItemDelete = { viewModel.deleteTransaction(it.transaction) },
                        onAddTransactionClick = { viewModel.openAddTransaction() },
                        onManageCategoriesClick = { viewModel.openAddCategory() }
                    )
                }
                NavTab.STATISTICS -> {
                    StatisticsScreen(
                        year = selectedYear,
                        month = selectedMonth,
                        summary = monthlySummary,
                        decisionMetrics = decisionMetrics,
                        onPreviousMonth = { viewModel.changeMonth(-1) },
                        onNextMonth = { viewModel.changeMonth(1) },
                        onCurrentMonth = { viewModel.changeMonth(0) },
                        onImportStatementSelected = { uri, password ->
                            viewModel.importExcelCsvStatement(context, uri, password)
                        },
                        onScanReceiptPhoto = {
                            cameraPreviewLauncher.launch(null)
                        },
                        onScanReceiptGallerySelected = { uri ->
                            viewModel.processReceiptScanFromUri(context, uri)
                        }
                    )
                }
                NavTab.FINANCIAL_PLAN -> {
                    FinancialPlanScreen(
                        decisionMetrics = decisionMetrics,
                        decisionPlans = decisionPlans,
                        categories = categories,
                        wallets = wallets,
                        onAddPlan = { title, price, planType, urgency, catId, note ->
                            viewModel.addDecisionPlan(title, price, planType, urgency, catId, note)
                        },
                        onRealizePlan = { plan, walletName ->
                            viewModel.realizeDecisionPlan(plan, walletName)
                        },
                        onDeletePlan = { plan ->
                            viewModel.deleteDecisionPlan(plan)
                        },
                        onUpdatePlanStatus = { plan ->
                            viewModel.updateDecisionPlan(plan)
                        }
                    )
                }
                NavTab.PROFILE -> {
                    ProfileScreen(
                        userPreferences = userPreferences,
                        wallets = wallets,
                        importBatches = importBatches,
                        onDeleteImportBatch = { batch -> viewModel.deleteImportBatch(batch) },
                        onThemeChanged = { viewModel.setThemeMode(it) },
                        onBiometricToggled = { viewModel.setBiometricEnabled(it) },
                        onPinChanged = { viewModel.setPinCode(it) },
                        onReminderChanged = { enabled, h, m ->
                            viewModel.setReminder(enabled, h, m, context)
                        },
                        onExportReport = { format -> viewModel.exportReport(context, format) },
                        onBackupClick = { viewModel.backupData(context) },
                        onRestoreSelected = { uri -> viewModel.restoreData(context, uri) }
                    )
                }
            }
        }

        // Bottom Sheets
        TransactionBottomSheet(
            isOpen = isTransactionSheetOpen,
            onDismiss = { viewModel.closeTransactionSheet() },
            categories = categories,
            editingItem = editingTransaction,
            onSave = { id, amount, type, catId, wallet, dateMillis, note ->
                viewModel.saveTransaction(id, amount, type, catId, wallet, dateMillis, note)
            },
            onAddNewCategory = {
                viewModel.closeTransactionSheet()
                viewModel.openAddCategory()
            }
        )

        CategoryBottomSheet(
            isOpen = isCategorySheetOpen,
            onDismiss = { viewModel.closeCategorySheet() },
            editingCategory = editingCategory,
            onSave = { id, name, type, iconKey, colorHex ->
                viewModel.saveCategory(id, name, type, iconKey, colorHex)
            }
        )
    }
}
