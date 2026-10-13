package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.domain.engine.ColorEngine
import com.example.domain.model.MetalType
import com.example.ui.components.DynamicPnlBanner
import com.example.ui.components.MarketTickerRow
import com.example.ui.components.MetalHoldingCard
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToReconcile: () -> Unit,
    onNavigateToGoldDetails: () -> Unit,
    onNavigateToSilverDetails: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onNavigateToDigipay: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolioSummary.collectAsState()
    val goldQuote by viewModel.goldQuote.collectAsState()
    val silverQuote by viewModel.silverQuote.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val drivingPercent = ColorEngine.getDrivingValue(portfolio, prefs.colorDriver)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "زرواچ • مانیتور طلا و نقره",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = PersianFormatters.formatDate(portfolio.lastUpdatedTimestamp, prefs.useJalaliCalendar, prefs.usePersianDigits, true),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.manualRefresh() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بروزرسانی مظنه",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Dynamic P/L Banner
        item {
            DynamicPnlBanner(
                portfolio = portfolio,
                usePersianDigits = prefs.usePersianDigits,
                useJalaliCalendar = prefs.useJalaliCalendar,
                drivingPercent = drivingPercent,
                onRefreshClick = { viewModel.manualRefresh() }
            )
        }

        // Live Market Tickers
        item {
            MarketTickerRow(
                goldQuote = goldQuote,
                silverQuote = silverQuote,
                usePersianDigits = prefs.usePersianDigits
            )
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "ثبت معامله",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAddTransaction
                )
                QuickActionButton(
                    icon = Icons.Default.Autorenew,
                    label = "تطبیق موجودی",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToReconcile
                )
                QuickActionButton(
                    icon = Icons.Default.Assessment,
                    label = "نمودار تحلیلی",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCharts
                )
                QuickActionButton(
                    icon = Icons.Default.Wallet,
                    label = "دیجی‌پی",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDigipay
                )
            }
        }

        // Holdings Section Title
        item {
            Text(
                text = "کیف‌های دارایی شما",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Gold Holding Card
        item {
            MetalHoldingCard(
                summary = portfolio.goldSummary,
                usePersianDigits = prefs.usePersianDigits,
                onClick = onNavigateToGoldDetails
            )
        }

        // Silver Holding Card
        item {
            MetalHoldingCard(
                summary = portfolio.silverSummary,
                usePersianDigits = prefs.usePersianDigits,
                onClick = onNavigateToSilverDetails
            )
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین تراکنش‌ها و خریدها",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onNavigateToHistory)
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هنوز خریدی در پرتفوی ثبت نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = onNavigateToAddTransaction) {
                            Text("ثبت اولین خرید طلا یا نقره")
                        }
                    }
                }
            }
        } else {
            items(transactions.take(4)) { tx ->
                TransactionRowItem(
                    tx = tx,
                    usePersianDigits = prefs.usePersianDigits,
                    useJalali = prefs.useJalaliCalendar
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    tx: TransactionEntity,
    usePersianDigits: Boolean,
    useJalali: Boolean,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val isBuy = tx.type == "BUY" || tx.type == "DEPOSIT"
    val isGold = tx.metal == "GOLD"
    val metalLabel = if (isGold) "طلای ۱۸ عیار" else "نقره ۹۹۹"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isBuy) Color(0x2210B981) else Color(0x22EF4444),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBuy) Icons.Default.Add else Icons.Default.SwapHoriz,
                        contentDescription = tx.type,
                        tint = if (isBuy) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "$metalLabel (${if (isBuy) "خرید" else "فروش"})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = PersianFormatters.formatDate(tx.timestamp, useJalali, usePersianDigits, true),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianFormatters.formatGrams(BigDecimal(tx.quantityGrams), usePersianDigits),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = PersianFormatters.formatToman(BigDecimal(tx.totalCostToman), usePersianDigits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
