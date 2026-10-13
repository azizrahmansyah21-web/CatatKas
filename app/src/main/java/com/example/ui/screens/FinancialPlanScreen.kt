package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.DecisionPlanEntity
import com.example.data.model.PlanStatus
import com.example.data.model.PlanType
import com.example.data.model.WalletEntity
import com.example.engine.FinancialDecisionMetrics
import com.example.engine.Rule50_30_20Analysis
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import com.example.util.debouncedClickable

// Custom tokens for decision engine
private val NeedsCyan = Color(0xFF38BDF8)
private val WantsRose = Color(0xFFFB7185)
private val SavingsEmerald = Color(0xFF10B981)
private val AmberAccent = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialPlanScreen(
    decisionMetrics: FinancialDecisionMetrics?,
    decisionPlans: List<DecisionPlanEntity>,
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    onAddPlan: (title: String, price: Double, planType: PlanType, urgency: Int, categoryId: Long?, note: String) -> Unit,
    onRealizePlan: (DecisionPlanEntity, walletName: String) -> Unit,
    onDeletePlan: (DecisionPlanEntity) -> Unit,
    onUpdatePlanStatus: (DecisionPlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Pos 50/30/20, 1 = Kalkulator Keputusan, 2 = Wishlist Rencana
    var isAddPlanSheetOpen by remember { mutableStateOf(false) }
    var selectedPlanToRealize by remember { mutableStateOf<DecisionPlanEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("financial_plan_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Rencana Kas & Keputusan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Kendalikan keinginan, penuhi kebutuhan, dan capai tujuan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = { isAddPlanSheetOpen = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SavingsEmerald.copy(alpha = 0.15f))
                        .testTag("add_plan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catat Rencana",
                        tint = SavingsEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Tab Row
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color(0xFF1F2937),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Text(
                            text = "Pos 50/30/20",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSubTab == 0) SavingsEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            text = "Kalkulator Beli",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSubTab == 1) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = {
                        Text(
                            text = "Wishlist (${decisionPlans.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSubTab == 2) NeedsCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedSubTab) {
                0 -> Budget503020Tab(decisionMetrics = decisionMetrics)
                1 -> DecisionCalculatorTab(
                    categories = categories,
                    onSaveToWishlist = { title, price, planType, urgency, catId, note ->
                        onAddPlan(title, price, planType, urgency, catId, note)
                        selectedSubTab = 2 // pindah ke tab wishlist
                    }
                )
                2 -> WishlistTrackerTab(
                    plans = decisionPlans,
                    onRealizeClick = { selectedPlanToRealize = it },
                    onDeleteClick = onDeletePlan,
                    onAddNewPlanClick = { isAddPlanSheetOpen = true }
                )
            }
        }
    }

    // Modal Sheet Tambah Rencana Belanja Baru
    if (isAddPlanSheetOpen) {
        AddPlanBottomSheet(
            categories = categories,
            onDismiss = { isAddPlanSheetOpen = false },
            onSave = { title, price, planType, urgency, catId, note ->
                onAddPlan(title, price, planType, urgency, catId, note)
                isAddPlanSheetOpen = false
                selectedSubTab = 2
            }
        )
    }

    // Dialog Konfirmasi Realisasi Rencana ke Transaksi
    if (selectedPlanToRealize != null) {
        val plan = selectedPlanToRealize!!
        RealizePlanBottomSheet(
            plan = plan,
            wallets = wallets,
            onDismiss = { selectedPlanToRealize = null },
            onConfirmRealize = { walletName ->
                onRealizePlan(plan, walletName)
                selectedPlanToRealize = null
            }
        )
    }
}

// ==========================================
// 1. TAB: POS ANGGARAN 50/30/20
// ==========================================
@Composable
private fun Budget503020Tab(
    decisionMetrics: FinancialDecisionMetrics?
) {
    val analysis = decisionMetrics?.ruleAnalysis ?: Rule50_30_20Analysis(
        needsAmount = 0.0,
        needsPercentage = 0f,
        wantsAmount = 0.0,
        wantsPercentage = 0f,
        savingsAmount = 0.0,
        savingsPercentage = 0f,
        isHealthy = true,
        recommendation = "Mulai catat transaksi pengeluaran dan pemasukan untuk melihat analisis pos 50/30/20 secara otomatis."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SavingsEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SavingsEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Formula Rasio 50/30/20",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (analysis.isHealthy) SavingsEmerald.copy(alpha = 0.2f)
                                    else AmberAccent.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (analysis.isHealthy) "Ideal & Sehat" else "Perlu Penyesuaian",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (analysis.isHealthy) SavingsEmerald else AmberAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Combined Multi-color Progress Bar
                    val totalBase = (analysis.needsPercentage + analysis.wantsPercentage + analysis.savingsPercentage).coerceAtLeast(1f)
                    val needsFraction = (analysis.needsPercentage / totalBase).coerceIn(0f, 1f)
                    val wantsFraction = (analysis.wantsPercentage / totalBase).coerceIn(0f, 1f)
                    val savingsFraction = (analysis.savingsPercentage / totalBase).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF374151))
                    ) {
                        if (needsFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(needsFraction)
                                    .fillMaxSize()
                                    .background(NeedsCyan)
                            )
                        }
                        if (wantsFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(wantsFraction)
                                    .fillMaxSize()
                                    .background(WantsRose)
                            )
                        }
                        if (savingsFraction > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(savingsFraction)
                                    .fillMaxSize()
                                    .background(SavingsEmerald)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Advice text
                    Text(
                        text = analysis.recommendation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // Pos 1: Kebutuhan (50%)
        item {
            PosRuleCard(
                title = "Kebutuhan Pokok (Needs)",
                targetPercentage = "50%",
                actualPercentage = "${analysis.needsPercentage.toInt()}%",
                actualAmount = CurrencyFormatter.formatRupiah(analysis.needsAmount),
                color = NeedsCyan,
                description = "Makanan harian, listrik/air, tempat tinggal, transportasi wajib, dan kesehatan.",
                isExceeded = analysis.needsPercentage > 55f
            )
        }

        // Pos 2: Keinginan (30%)
        item {
            PosRuleCard(
                title = "Keinginan & Gaya Hidup (Wants)",
                targetPercentage = "30%",
                actualPercentage = "${analysis.wantsPercentage.toInt()}%",
                actualAmount = CurrencyFormatter.formatRupiah(analysis.wantsAmount),
                color = WantsRose,
                description = "Jajan kopi, kuliner akhir pekan, langganan hiburan, gadget, dan belanja hobi.",
                isExceeded = analysis.wantsPercentage > 35f
            )
        }

        // Pos 3: Tabungan & Investasi (20%)
        item {
            PosRuleCard(
                title = "Tabungan & Investasi (Savings)",
                targetPercentage = "20%",
                actualPercentage = "${analysis.savingsPercentage.toInt()}%",
                actualAmount = CurrencyFormatter.formatRupiah(analysis.savingsAmount),
                color = SavingsEmerald,
                description = "Alokasi dana darurat, tabungan masa depan, dan investasi aset produktif.",
                isExceeded = false
            )
        }
    }
}

@Composable
private fun PosRuleCard(
    title: String,
    targetPercentage: String,
    actualPercentage: String,
    actualAmount: String,
    color: Color,
    description: String,
    isExceeded: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actualPercentage,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    )
                    Text(
                        text = " / Target $targetPercentage",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = actualAmount,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            )

            if (isExceeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpenseRed.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Melebihi batas anjuran. Evaluasi pengeluaran kategori ini.",
                        fontSize = 11.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. TAB: KALKULATOR KEPUTUSAN BELI
// ==========================================
@Composable
private fun DecisionCalculatorTab(
    categories: List<CategoryEntity>,
    onSaveToWishlist: (title: String, price: Double, planType: PlanType, urgency: Int, categoryId: Long?, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var planType by remember { mutableStateOf(PlanType.WANTS) }
    var urgencyLevel by remember { mutableIntStateOf(3) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    val price = priceText.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0

    // Kalkulasi Jam Kerja (Asumsi gaji rata-rata Rp 6.000.000 / 160 jam kerja = Rp 37.500 per jam)
    val hourlyIncomeRate = 37_500.0
    val equivalentHours = if (price > 0) (price / hourlyIncomeRate) else 0.0

    // Kalkulasi Skor Keputusan Cerdas (1-100)
    val decisionScore = remember(price, planType, urgencyLevel) {
        var score = if (planType == PlanType.NEEDS) 85 else 55
        score += (urgencyLevel - 3) * 8
        if (price > 500_000.0) score -= 10
        if (price > 2_000_000.0) score -= 15
        score.coerceIn(10, 99)
    }

    val recommendation = when {
        decisionScore >= 75 -> "Rekomendasi Beli: Kebutuhan penting dan memiliki urgensi tinggi untuk menunjang aktivitasmu."
        decisionScore >= 50 -> "Pertimbangkan Jeda 72 Jam: Terapkan aturan cooling-off 3 hari untuk memastikan barang ini bukan keinginan impulsif sesaat."
        else -> "Tunda atau Hindari: Skor belanja rendah. Sebaiknya alokasikan dana ini ke tabungan atau dana daruratmu."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Simulasi Pembelian 'Beli Sekarang vs Nanti'",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Nama Barang
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Barang / Rencana Pembelian") },
                        placeholder = { Text("Contoh: Sepatu Olahraga Baru, Kopi Kafe...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Estimasi Harga
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Estimasi Harga (Rp)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle Kebutuhan vs Keinginan
                    Text(
                        text = "Klasifikasi Belanja:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { planType = PlanType.NEEDS },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (planType == PlanType.NEEDS) NeedsCyan.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (planType == PlanType.NEEDS) NeedsCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            )
                        ) {
                            Text(
                                text = "Kebutuhan (Needs)",
                                color = if (planType == PlanType.NEEDS) NeedsCyan else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { planType = PlanType.WANTS },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (planType == PlanType.WANTS) WantsRose.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (planType == PlanType.WANTS) WantsRose else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            )
                        ) {
                            Text(
                                text = "Keinginan (Wants)",
                                color = if (planType == PlanType.WANTS) WantsRose else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Urgensi Belanja (1-5 Bintang)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tingkat Urgensi / Kepentingan: $urgencyLevel / 5",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Slider(
                        value = urgencyLevel.toFloat(),
                        onValueChange = { urgencyLevel = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberAccent,
                            activeTrackColor = AmberAccent
                        )
                    )
                }
            }
        }

        // Hasil Evaluasi Cerdas
        if (price > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Skor Kelayakan Belanja",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            decisionScore >= 75 -> SavingsEmerald.copy(alpha = 0.2f)
                                            decisionScore >= 50 -> AmberAccent.copy(alpha = 0.2f)
                                            else -> ExpenseRed.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$decisionScore / 100",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        decisionScore >= 75 -> SavingsEmerald
                                        decisionScore >= 50 -> AmberAccent
                                        else -> ExpenseRed
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ekivalen Jam Kerja
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF111827))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Harga barang ini setara dengan ${String.format("%.1f", equivalentHours)} jam waktu kerjamu.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSaveToWishlist(title, price, planType, urgencyLevel, selectedCategoryId, note)
                                }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SavingsEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan ke Catatan Wishlist & Evaluasi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. TAB: WISHLIST TRACKER & CATATAN EVALUASI
// ==========================================
@Composable
private fun WishlistTrackerTab(
    plans: List<DecisionPlanEntity>,
    onRealizeClick: (DecisionPlanEntity) -> Unit,
    onDeleteClick: (DecisionPlanEntity) -> Unit,
    onAddNewPlanClick: () -> Unit
) {
    if (plans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateView(
                title = "Belum Ada Catatan Rencana",
                subtitle = "Catat barang yang ingin dibeli di sini untuk dievaluasi kelayakan belanjanya sebelum checkout.",
                actionButtonText = "Catat Rencana Belanja Baru",
                onActionClick = onAddNewPlanClick
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plans, key = { it.id }) { plan ->
                WishlistItemCard(
                    plan = plan,
                    onRealize = { onRealizeClick(plan) },
                    onDelete = { onDeleteClick(plan) }
                )
            }
        }
    }
}

@Composable
private fun WishlistItemCard(
    plan: DecisionPlanEntity,
    onRealize: () -> Unit,
    onDelete: () -> Unit
) {
    val isRealized = plan.status == PlanStatus.REALIZED

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (plan.planType == PlanType.NEEDS) NeedsCyan.copy(alpha = 0.2f)
                            else WantsRose.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (plan.planType == PlanType.NEEDS) "Kebutuhan" else "Keinginan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.planType == PlanType.NEEDS) NeedsCyan else WantsRose
                    )
                }

                // Decision score badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Skor: ${plan.decisionScore}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            plan.decisionScore >= 75 -> SavingsEmerald
                            plan.decisionScore >= 50 -> AmberAccent
                            else -> ExpenseRed
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = CurrencyFormatter.formatRupiah(plan.estimatedPrice),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SavingsEmerald
                )
            )

            if (plan.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.note,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (!isRealized) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRealize,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SavingsEmerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCartCheckout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Realisasikan Jadi Transaksi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SavingsEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SavingsEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sudah Terealisasi Menjadi Transaksi Pengeluaran",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SavingsEmerald
                    )
                }
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET: TAMBAH RENCANA BARU
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlanBottomSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, price: Double, planType: PlanType, urgency: Int, categoryId: Long?, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var planType by remember { mutableStateOf(PlanType.WANTS) }
    var urgencyLevel by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }

    val price = priceText.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1F2937)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Catat Rencana / Wishlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Rencana Barang") },
                placeholder = { Text("Contoh: Mesin Kopi, Baju Kerja...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                label = { Text("Estimasi Harga (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = planType == PlanType.NEEDS,
                    onClick = { planType = PlanType.NEEDS },
                    label = { Text("Kebutuhan (50%)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeedsCyan.copy(alpha = 0.25f),
                        selectedLabelColor = NeedsCyan
                    )
                )
                FilterChip(
                    selected = planType == PlanType.WANTS,
                    onClick = { planType = PlanType.WANTS },
                    label = { Text("Keinginan (30%)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WantsRose.copy(alpha = 0.25f),
                        selectedLabelColor = WantsRose
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && price > 0) {
                        onSave(title, price, planType, urgencyLevel, null, note)
                    }
                },
                enabled = title.isNotBlank() && price > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SavingsEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Rencana", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// BOTTOM SHEET: REALISASI RENCANA
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealizePlanBottomSheet(
    plan: DecisionPlanEntity,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirmRealize: (walletName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedWallet by remember {
        mutableStateOf(wallets.firstOrNull()?.name ?: "Mandiri")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1F2937)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Realisasikan Jadi Transaksi",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Anda akan mencatat pengeluaran '${plan.title}' sebesar ${CurrencyFormatter.formatRupiah(plan.estimatedPrice)}.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pilih Sumber Dompet:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets) { wallet ->
                    FilterChip(
                        selected = selectedWallet == wallet.name,
                        onClick = { selectedWallet = wallet.name },
                        label = { Text(wallet.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SavingsEmerald.copy(alpha = 0.25f),
                            selectedLabelColor = SavingsEmerald
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onConfirmRealize(selectedWallet) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SavingsEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konfirmasi Pengeluaran", fontWeight = FontWeight.Bold)
            }
        }
    }
}
