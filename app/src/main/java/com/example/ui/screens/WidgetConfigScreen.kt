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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.ColorDriver
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val portfolio by viewModel.portfolioSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پیکربندی ویجت‌های صفحه اصلی", style = MaterialTheme.typography.titleMedium) },
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
            // Widget Features Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "ویجت‌های تعاملی با تغییر رنگ لحظه‌ای",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "زرواچ دارای ۳ سایز ویجت بومی اندروید (کوچک ۲×۲، متوسط ۴×۲، و بزرگ ۴×۴) است که پس‌زمینه آن‌ها مستقیماً با سود و زیان شما تغییر رنگ می‌دهد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Driver Selector
            Text("مبنای رنگ‌بندی داینامیک ویجت", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDriver.values().forEach { driver ->
                    FilterChip(
                        selected = prefs.colorDriver == driver,
                        onClick = { viewModel.updateColorDriver(driver) },
                        label = { Text(driver.displayNameFa) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Background Scheduling Policy Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سیاست بروزرسانی پس‌زمینه (انطباق با باتری اندروید):", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "• برای حفظ دوام باتری، بروزرسانی دوره‌ای پس‌زمینه طبق استاندارد رسمی سیستم‌عامل با WorkManager در فواصل زمانی استاندارد (حداقل ۱۵ دقیقه) انجام می‌گیرد.\n• در زمان باز بودن برنامه (پیش‌زمینه)، بروزرسانی سریع‌تر (هر ۳۰ ثانیه) فعال است.\n• با زدن دکمه همگام‌سازی در ویجت، بروزرسانی بلافاصله انجام می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Manual Refresh Trigger
            Button(
                onClick = { viewModel.manualRefresh() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("همگام‌سازی فوری ویجت‌ها با قیمت‌های جدید")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
