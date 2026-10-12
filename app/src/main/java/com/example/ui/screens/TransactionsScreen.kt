package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlySummary
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.ui.components.BalanceOverviewCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MonthSelector
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.DateUtils
import com.example.util.debouncedClickable

@Composable
fun TransactionsScreen(
    year: Int,
    month: Int,
    monthlySummary: MonthlySummary,
    transactions: List<TransactionWithCategory>,
    activeFilter: TransactionType?,
    searchQuery: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onFilterChanged: (TransactionType?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onItemClick: (TransactionWithCategory) -> Unit,
    onItemDelete: (TransactionWithCategory) -> Unit,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Group transactions by date
    val groupedTransactions = remember(transactions) {
        transactions.groupBy {
            DateUtils.formatDateHeader(it.transaction.dateMillis)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen_list"),
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

        // Summary Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                BalanceOverviewCard(
                    totalIncome = monthlySummary.totalIncome,
                    totalExpense = monthlySummary.totalExpense,
                    netBalance = monthlySummary.netBalance
                )
            }
        }

        // Search & Filter Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Filter Chips (Semua, Pengeluaran, Pemasukan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = activeFilter == null,
                            onClick = { onFilterChanged(null) },
                            label = { Text("Semua", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_all")
                        )

                        FilterChip(
                            selected = activeFilter == TransactionType.EXPENSE,
                            onClick = {
                                onFilterChanged(if (activeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                            },
                            label = { Text("Pengeluaran", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                                selectedLabelColor = ExpenseRed
                            ),
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_expense")
                        )

                        FilterChip(
                            selected = activeFilter == TransactionType.INCOME,
                            onClick = {
                                onFilterChanged(if (activeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
                            },
                            label = { Text("Pemasukan", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                                selectedLabelColor = IncomeGreen
                            ),
                            shape = CircleShape,
                            modifier = Modifier.testTag("filter_income")
                        )
                    }

                    // Search Toggle Button
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) onSearchChanged("")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Cari Transaksi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(visible = isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = { Text("Cari catatan, dompet, atau kategori...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("search_query_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // Empty State or Grouped Transactions
        if (transactions.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                EmptyStateView(
                    title = if (searchQuery.isNotBlank()) "Tidak ditemukan transaksi"
                    else "Belum ada transaksi di bulan ini",
                    subtitle = if (searchQuery.isNotBlank()) "Coba kata kunci pencarian lain atau bersihkan filter."
                    else "Keuanganmu masih kosong. Ketuk tombol di bawah untuk mencatat pengeluaran atau pemasukan pertamamu!",
                    actionButtonText = if (searchQuery.isBlank()) "Catat Transaksi Baru" else null,
                    onActionClick = onAddTransactionClick
                )
            }
        } else {
            groupedTransactions.forEach { (dateHeader, itemsInDate) ->
                // Date Header
                item(key = "header_$dateHeader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "${itemsInDate.size} transaksi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                // Items in date
                items(
                    items = itemsInDate,
                    key = { it.transaction.id }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        TransactionItemCard(
                            transactionWithCategory = item,
                            onClick = { onItemClick(item) },
                            onDelete = { onItemDelete(item) }
                        )
                    }
                }
            }
        }
    }
}
