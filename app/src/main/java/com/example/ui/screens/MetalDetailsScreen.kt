package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ColorEngine
import com.example.domain.model.MetalType
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalDetailsScreen(
    metal: MetalType,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToAddTransaction: () -> Unit
) {
    val portfolio by viewModel.portfolioSummary.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()
    val allTxs by viewModel.allTransactions.collectAsState()

    val summary = if (metal == MetalType.GOLD) portfolio.goldSummary else portfolio.silverSummary
    val txs = allTxs.filter { it.metal == metal.name }
    val accentColor = if (metal == MetalType.GOLD) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
    val pnlColor = ColorEngine.getAccentPnlColor(summary.unrealizedProfitPercent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (metal == MetalType.GOLD) "جزئیات سبد طلای ۱۸ عیار" else "جزئیات سبد نقره ۹۹۹",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن معامله", tint = accentColor)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Valuation Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ارزش روز دارایی",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (summary.unrealizedProfitPercent >= BigDecimal.ZERO) Color(0x3310B981) else Color(0x33EF4444),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = PersianFormatters.formatPercent(summary.unrealizedProfitPercent, prefs.usePersianDigits, true),
                                    color = pnlColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = PersianFormatters.formatToman(summary.estimatedGrossValueToman, prefs.usePersianDigits, true),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "موجودی دارایی",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = PersianFormatters.formatGrams(summary.remainingGrams, prefs.usePersianDigits, 4, true),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "سود/زیان تحقق‌نیافته",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = PersianFormatters.formatToman(summary.unrealizedProfitToman, prefs.usePersianDigits, true),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = pnlColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Metric details grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow("مظنه روز بازار (هر گرم)", PersianFormatters.formatToman(summary.currentPriceToman, prefs.usePersianDigits))
                        DetailRow("میانگین قیمت خرید شما", PersianFormatters.formatToman(summary.averageCostPerGramToman, prefs.usePersianDigits) + " /گرم")
                        DetailRow("کل بهای تمام‌شده خرید", PersianFormatters.formatToman(summary.totalCostBasisToman, prefs.usePersianDigits))
                        DetailRow("ارزش خالص نقدشوندگی (تخمینی)", PersianFormatters.formatToman(summary.estimatedLiquidationValueToman, prefs.usePersianDigits))
                        DetailRow("کل سود تحقق‌یافته از فروش‌ها", PersianFormatters.formatToman(summary.realizedProfitToman, prefs.usePersianDigits))
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تاریخچه تراکنش‌های این فلز",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("ثبت معامله جدید")
                    }
                }
            }

            if (txs.isEmpty()) {
                item {
                    Text(
                        text = "هیچ معامله‌ای برای این فلز ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(txs) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (tx.type == "BUY") "خرید" else if (tx.type == "SELL") "فروش" else "تعدیل"} ${PersianFormatters.formatGrams(BigDecimal(tx.quantityGrams), prefs.usePersianDigits)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "قیمت گرمی: " + PersianFormatters.formatToman(BigDecimal(tx.unitPriceToman), prefs.usePersianDigits) + " • منبع: ${tx.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tx.notes.isNotEmpty()) {
                                    Text(
                                        text = tx.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = PersianFormatters.formatDate(tx.timestamp, prefs.useJalaliCalendar, prefs.usePersianDigits, true),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(onClick = { viewModel.deleteTransaction(tx.id, metal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف معامله", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
