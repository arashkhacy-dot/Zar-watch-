package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChartMetric
import com.example.domain.model.MetalType
import com.example.domain.model.TimeFrame
import com.example.ui.components.PersianFormatters
import com.example.ui.components.ZarWatchCanvasChart
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalChartsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val observations by viewModel.chartObservations.collectAsState()

    var selectedMetric by remember { mutableStateOf(ChartMetric.GOLD_PRICE) }
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.WEEK) }

    val metal = if (selectedMetric == ChartMetric.SILVER_PRICE) MetalType.SILVER else MetalType.GOLD

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نمودارهای تحلیلی و پایش تاریخچه", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metric selector chips
            Text("شاخص مورد نظر", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    ChartMetric.GOLD_PRICE,
                    ChartMetric.SILVER_PRICE,
                    ChartMetric.PORTFOLIO_VALUATION
                ).forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = {
                            selectedMetric = metric
                            viewModel.loadHistoricalChart(
                                if (metric == ChartMetric.SILVER_PRICE) MetalType.SILVER else MetalType.GOLD,
                                selectedTimeFrame
                            )
                        },
                        label = { Text(metric.displayNameFa) }
                    )
                }
            }

            // Timeframe selector chips
            Text("بازه زمانی", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeFrame.values().forEach { tf ->
                    FilterChip(
                        selected = selectedTimeFrame == tf,
                        onClick = {
                            selectedTimeFrame = tf
                            viewModel.loadHistoricalChart(metal, tf)
                        },
                        label = { Text(tf.displayNameFa) }
                    )
                }
            }

            // Interactive Canvas Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ZarWatchCanvasChart(
                        observations = observations,
                        usePersianDigits = prefs.usePersianDigits,
                        useJalaliCalendar = prefs.useJalaliCalendar,
                        lineColor = if (selectedMetric == ChartMetric.SILVER_PRICE) Color(0xFFCBD5E1) else Color(0xFF10B981)
                    )
                }
            }

            // Chart Statistics Card
            if (observations.isNotEmpty()) {
                val prices = observations.map { it.priceToman }
                val minP = prices.minOrNull() ?: BigDecimal.ZERO
                val maxP = prices.maxOrNull() ?: BigDecimal.ZERO
                val firstP = prices.firstOrNull() ?: BigDecimal.ZERO
                val lastP = prices.lastOrNull() ?: BigDecimal.ZERO
                val changeP = lastP.subtract(firstP)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("آمار نوسان در این بازه", style = MaterialTheme.typography.titleMedium)
                        DetailRow("بالاترین قیمت ثبت‌شده:", PersianFormatters.formatToman(maxP, prefs.usePersianDigits))
                        DetailRow("پایین‌ترین قیمت ثبت‌شده:", PersianFormatters.formatToman(minP, prefs.usePersianDigits))
                        DetailRow("تغییر قیمت کل بازه:", PersianFormatters.formatToman(changeP, prefs.usePersianDigits))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
