package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySpending
import com.example.data.model.MonthlySummary
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthSelector
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CategoryIcons
import com.example.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun StatisticsScreen(
    year: Int,
    month: Int,
    summary: MonthlySummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasData = summary.totalIncome > 0 || summary.totalExpense > 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("statistics_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Month Selector Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                MonthSelector(
                    year = year,
                    month = month,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onCurrentMonth = onCurrentMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!hasData) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                EmptyStateView(
                    title = "Belum Ada Data Statistik",
                    subtitle = "Mulai catat transaksi pengeluaran dan pemasukan untuk melihat grafik dan analisis keuangan bulanan di sini.",
                    icon = Icons.Default.PieChart
                )
            }
        } else {
            // Financial Health Insight Card
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    FinancialHealthCard(summary = summary)
                }
            }

            // Daily Spending Trend Bar Chart
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    DailySpendingChartCard(summary = summary)
                }
            }

            // Expense Breakdown Header
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rincian Kategori Pengeluaran",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.categorySpendings.size} Kategori",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Category Spendings List
            if (summary.categorySpendings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada catatan pengeluaran di bulan ini.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(
                    items = summary.categorySpendings,
                    key = { it.category.id }
                ) { spending ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        CategorySpendingItem(spending = spending)
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialHealthCard(summary: MonthlySummary) {
    val isNetPositive = summary.netBalance >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = if (isNetPositive) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Analisis Arus Kas",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message text
            val message = when {
                summary.totalIncome == 0.0 && summary.totalExpense > 0.0 ->
                    "Bulan ini kamu belum mencatat pemasukan. Coba catat sumber pemasukanmu agar analisis lebih akurat! 💡"
                isNetPositive && summary.savingsRatePercentage >= 30f ->
                    "Luar biasa! Kamu berhasil menghemat ${summary.savingsRatePercentage.toInt()}% dari total pemasukanmu bulan ini. Pertahankan gaya hidup hematmu! 🌿"
                isNetPositive ->
                    "Arus kas positif! Kamu menghemat ${CurrencyFormatter.formatRupiah(summary.netBalance)}. Pertimbangkan menyisihkan dana darurat. ✨"
                else ->
                    "Perhatian: Pengeluaranmu melebihi pemasukan bulan ini sebesar ${CurrencyFormatter.formatRupiah(-summary.netBalance)}. Cek pos pengeluaran terbesar di bawah! ⚠️"
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (summary.totalIncome > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val ratio = (summary.totalExpense / summary.totalIncome).coerceIn(0.0, 1.0).toFloat()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rasio Pengeluaran: ${(ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Tersisa: ${((1f - ratio) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (ratio > 0.8f) ExpenseRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
fun DailySpendingChartCard(summary: MonthlySummary) {
    val dailyList = summary.dailyCashflows
    val maxDailyExpense = remember(dailyList) {
        val max = dailyList.maxOfOrNull { it.expense } ?: 1.0
        if (max <= 0) 1.0 else max
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tren Pengeluaran Harian",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Maks: ${CurrencyFormatter.formatRupiah(maxDailyExpense)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = dailyList.size
                    if (count == 0) return@Canvas

                    val barSpacing = 3.dp.toPx()
                    val totalSpacing = barSpacing * (count - 1)
                    val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())

                    // Baseline
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )

                    dailyList.forEachIndexed { index, cashflow ->
                        val x = index * (barWidth + barSpacing)
                        val heightFraction = (cashflow.expense / maxDailyExpense).toFloat().coerceIn(0.04f, 1f)
                        val barHeight = size.height * heightFraction

                        val barColor = if (cashflow.expense > 0) {
                            if (cashflow.expense >= maxDailyExpense * 0.8) ExpenseRed
                            else primaryColor
                        } else {
                            outlineColor.copy(alpha = 0.3f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tgl 1",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = "Tgl 15",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = "Tgl ${dailyList.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
fun CategorySpendingItem(spending: CategorySpending) {
    val cat = spending.category
    val catColor = CategoryIcons.parseColor(cat.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    iconKey = cat.iconKey,
                    colorHex = cat.colorHex,
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${spending.transactionCount} transaksi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(spending.totalAmount),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${String.format("%.1f", spending.percentage)}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = catColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proportional Progress Bar
            LinearProgressIndicator(
                progress = { (spending.percentage / 100f).coerceIn(0.01f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
