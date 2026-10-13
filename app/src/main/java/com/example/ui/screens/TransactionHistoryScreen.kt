package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.MetalType
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToAddTransaction: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    var selectedMetalFilter by remember { mutableStateOf<String?>("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTxs = transactions.filter { tx ->
        val metalMatch = selectedMetalFilter == "ALL" || tx.metal == selectedMetalFilter
        val searchMatch = searchQuery.isEmpty() ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.source.contains(searchQuery, ignoreCase = true)
        metalMatch && searchMatch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دفتر کل معاملات و تراکنش‌ها", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, contentDescription = "ثبت معامله")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("جستجو در یادداشت‌ها یا منبع معامله...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMetalFilter == "ALL",
                    onClick = { selectedMetalFilter = "ALL" },
                    label = { Text("همه") }
                )
                FilterChip(
                    selected = selectedMetalFilter == "GOLD",
                    onClick = { selectedMetalFilter = "GOLD" },
                    label = { Text("فقط طلا") }
                )
                FilterChip(
                    selected = selectedMetalFilter == "SILVER",
                    onClick = { selectedMetalFilter = "SILVER" },
                    label = { Text("فقط نقره") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ تراکنشی مطابق جستجو یافت نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTxs) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (tx.metal == "GOLD") "طلا" else "نقره"} ${tx.purity} • ${if (tx.type == "BUY") "خرید" else if (tx.type == "SELL") "فروش" else "تعدیل"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = {
                                        viewModel.deleteTransaction(
                                            tx.id,
                                            if (tx.metal == "GOLD") MetalType.GOLD else MetalType.SILVER
                                        )
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف معامله", tint = Color(0xFFEF4444))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "وزن: " + PersianFormatters.formatGrams(BigDecimal(tx.quantityGrams), prefs.usePersianDigits),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "مبلغ: " + PersianFormatters.formatToman(BigDecimal(tx.totalCostToman), prefs.usePersianDigits),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "قیمت واحد: " + PersianFormatters.formatToman(BigDecimal(tx.unitPriceToman), prefs.usePersianDigits) + " • منبع: ${tx.source}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = PersianFormatters.formatDate(tx.timestamp, prefs.useJalaliCalendar, prefs.usePersianDigits, true),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (tx.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "یادداشت: ${tx.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}
