package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ColorEngine
import com.example.domain.model.HoldingSummary
import com.example.domain.model.MetalType
import com.example.domain.model.PortfolioSummary
import com.example.domain.model.PriceQuote
import java.math.BigDecimal

@Composable
fun DynamicPnlBanner(
    portfolio: PortfolioSummary,
    usePersianDigits: Boolean,
    useJalaliCalendar: Boolean,
    drivingPercent: Float,
    modifier: Modifier = Modifier,
    onRefreshClick: () -> Unit = {}
) {
    val targetBg = ColorEngine.calculateBackgroundColor(drivingPercent, portfolio.isStale)
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(600), label = "pnl_bg")
    val textColor = ColorEngine.getContrastingTextColor(animatedBg)
    val subTextColor = textColor.copy(alpha = 0.75f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & Stale indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ارزش کل پرتفوی سرمایه‌گذاری",
                    color = subTextColor,
                    style = MaterialTheme.typography.labelLarge
                )

                if (portfolio.isStale) {
                    Row(
                        modifier = Modifier
                            .background(Color(0x44F59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "داده قدیمی",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "قیمت با تاخیر",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = portfolio.valuationMode.displayNameFa,
                            color = subTextColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Total Valuation
            Text(
                text = PersianFormatters.formatToman(portfolio.totalValuationToman, usePersianDigits, true),
                color = textColor,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Unrealized P/L and %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سود/زیان تحقق‌نیافته",
                        color = subTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = PersianFormatters.formatToman(portfolio.unrealizedProfitToman, usePersianDigits, true),
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (portfolio.unrealizedProfitPercent >= BigDecimal.ZERO) Color(0x3310B981) else Color(0x33EF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (portfolio.unrealizedProfitPercent >= BigDecimal.ZERO) Color(0x6610B981) else Color(0x66EF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = PersianFormatters.formatPercent(portfolio.unrealizedProfitPercent, usePersianDigits, true),
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: Cost Basis & Realized Profit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x15000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "بهای تمام‌شده: " + PersianFormatters.formatToman(portfolio.totalCostBasisToman, usePersianDigits, true),
                    color = subTextColor,
                    fontSize = 11.sp
                )
                Text(
                    text = "سود تحقق‌یافته: " + PersianFormatters.formatToman(portfolio.realizedProfitToman, usePersianDigits, true),
                    color = subTextColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MetalHoldingCard(
    summary: HoldingSummary,
    usePersianDigits: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isGold = summary.metal == MetalType.GOLD
    val accentColor = if (isGold) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
    val pnlColor = ColorEngine.getAccentPnlColor(summary.unrealizedProfitPercent)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGold) "طلای ۱۸ عیار" else "نقره ۹۹۹",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = PersianFormatters.formatPercent(summary.unrealizedProfitPercent, usePersianDigits, true),
                    color = pnlColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remaining Grams and Valuation
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
                        text = PersianFormatters.formatGrams(summary.remainingGrams, usePersianDigits, 4, true),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ارزش روز دارایی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = PersianFormatters.formatToman(summary.estimatedGrossValueToman, usePersianDigits, true),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cost Basis & Average Price Per Gram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "میانگین خرید: " + PersianFormatters.formatToman(summary.averageCostPerGramToman, usePersianDigits, false) + " /گرم",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "سود/زیان: " + PersianFormatters.formatToman(summary.unrealizedProfitToman, usePersianDigits, true),
                    style = MaterialTheme.typography.bodySmall,
                    color = pnlColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MarketTickerRow(
    goldQuote: PriceQuote,
    silverQuote: PriceQuote,
    usePersianDigits: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Gold Ticker
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مظنه طلا ۱۸", fontSize = 11.sp, color = Color(0xFFFDE68A))
                    Text(
                        text = PersianFormatters.formatPercent(goldQuote.change24hPercent, usePersianDigits, true),
                        fontSize = 11.sp,
                        color = if (goldQuote.change24hPercent >= BigDecimal.ZERO) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PersianFormatters.formatToman(goldQuote.pricePerGramToman, usePersianDigits, true),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Silver Ticker
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مظنه نقره ۹۹۹", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                    Text(
                        text = PersianFormatters.formatPercent(silverQuote.change24hPercent, usePersianDigits, true),
                        fontSize = 11.sp,
                        color = if (silverQuote.change24hPercent >= BigDecimal.ZERO) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PersianFormatters.formatToman(silverQuote.pricePerGramToman, usePersianDigits, true),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
