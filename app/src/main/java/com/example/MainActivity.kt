package com.example

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
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
import com.example.ui.screens.LockScreen
import com.example.ui.screens.SettingsScreen
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

            val isDark = when (userPreferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CatatKasTheme(darkTheme = isDark) {
                MainAppContent(
                    viewModel = viewModel,
                    onPromptBiometric = { showBiometricPrompt() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If biometric or PIN is configured, lock app upon resuming
        viewModel.lockApp()
        if (viewModel.isAppLocked.value && viewModel.userPreferences.value.biometricEnabled) {
            showBiometricPrompt()
        }
    }

    override fun onPause() {
        super.onPause()
        biometricCancellationSignal?.cancel()
        biometricCancellationSignal = null
    }

    private fun showBiometricPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                biometricCancellationSignal?.cancel()
                val cancellationSignal = CancellationSignal()
                biometricCancellationSignal = cancellationSignal

                val executor = ContextCompat.getMainExecutor(this)
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(this)
                    .setTitle(getString(R.string.biometrik_prompt_title))
                    .setSubtitle(getString(R.string.biometrik_prompt_subtitle))
                    .setNegativeButton(getString(R.string.batal), executor) { _, _ -> }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.unlockApp()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // Handled gracefully, user can unlock using fallback PIN
                        }
                    }
                )
            } catch (_: Exception) {}
        }
    }
}

private data class BottomNavItem(
    val tab: NavTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onPromptBiometric: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                try {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Exception) {}
            }
        }
    }

    // Observe snackbar messages
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isTransactionSheetOpen by viewModel.isTransactionSheetOpen.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()

    val isCategorySheetOpen by viewModel.isCategorySheetOpen.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()

    if (isAppLocked) {
        LockScreen(
            hasBiometric = userPreferences.biometricEnabled,
            correctPin = userPreferences.pinCode,
            onBiometricClick = onPromptBiometric,
            onUnlocked = { viewModel.unlockApp() }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val tabs = listOf(
                        BottomNavItem(NavTab.TRANSACTIONS, "Transaksi", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
                        BottomNavItem(NavTab.STATISTICS, "Statistik", Icons.Filled.PieChart, Icons.Outlined.PieChart),
                        BottomNavItem(NavTab.CATEGORIES, "Kategori", Icons.Filled.Category, Icons.Outlined.Category),
                        BottomNavItem(NavTab.SETTINGS, "Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )

                    tabs.forEach { item ->
                        val isSelected = currentTab == item.tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                        )
                    }
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
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onCurrentMonth = {
                                viewModel.changeMonth(0)
                            },
                            onFilterChanged = { viewModel.setFilter(it) },
                            onSearchChanged = { viewModel.setSearchQuery(it) },
                            onItemClick = { viewModel.openEditTransaction(it) },
                            onItemDelete = { viewModel.deleteTransaction(it.transaction) },
                            onAddTransactionClick = { viewModel.openAddTransaction() }
                        )
                    }

                    NavTab.STATISTICS -> {
                        StatisticsScreen(
                            year = selectedYear,
                            month = selectedMonth,
                            summary = monthlySummary,
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onCurrentMonth = { viewModel.changeMonth(0) }
                        )
                    }

                    NavTab.CATEGORIES -> {
                        CategoriesScreen(
                            categories = categories,
                            onAddCategoryClick = { viewModel.openAddCategory() },
                            onEditCategoryClick = { viewModel.openEditCategory(it) },
                            onDeleteCategoryClick = { viewModel.deleteCategory(it) }
                        )
                    }

                    NavTab.SETTINGS -> {
                        SettingsScreen(
                            userPreferences = userPreferences,
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
}
