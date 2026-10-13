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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceProviderConfigScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val goldQuote by viewModel.goldQuote.collectAsState()
    val silverQuote by viewModel.silverQuote.collectAsState()

    var manualGoldPrice by remember(prefs.manualGoldPriceToman) {
        mutableStateOf(prefs.manualGoldPriceToman.toString())
    }
    var manualSilverPrice by remember(prefs.manualSilverPriceToman) {
        mutableStateOf(prefs.manualSilverPriceToman.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات و انتخاب منابع مظنه قیمت", style = MaterialTheme.typography.titleMedium) },
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
            // Active Provider Selector
            Text("منبع فعال استخراج قیمت‌ها", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = prefs.activeProviderType == "DEMO",
                    onClick = { viewModel.updateActiveProvider("DEMO") },
                    label = { Text("شبیه‌ساز زنده بازار (Demo)") }
                )
                FilterChip(
                    selected = prefs.activeProviderType == "MANUAL",
                    onClick = { viewModel.updateActiveProvider("MANUAL") },
                    label = { Text("ورود دستی (Manual)") }
                )
                FilterChip(
                    selected = prefs.activeProviderType == "NETWORK",
                    onClick = { viewModel.updateActiveProvider("NETWORK") },
                    label = { Text("پایشگر شبکه (Network)") }
                )
            }

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("وضعیت جاری منبع دریافت داده:", style = MaterialTheme.typography.titleMedium)
                    DetailRow("منبع طلا:", goldQuote.sourceName)
                    DetailRow("وضعیت طلا:", goldQuote.status.displayNameFa)
                    DetailRow("زمان استخراج طلا:", PersianFormatters.formatDate(goldQuote.fetchTimestamp, prefs.useJalaliCalendar, prefs.usePersianDigits, true))
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("منبع نقره:", silverQuote.sourceName)
                    DetailRow("وضعیت نقره:", silverQuote.status.displayNameFa)
                }
            }

            // Manual price inputs if MANUAL mode is active
            Text("نرخ‌های مرجع ورود دستی (تومان)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = manualGoldPrice,
                onValueChange = { manualGoldPrice = it },
                label = { Text("قیمت هر گرم طلای ۱۸ عیار (تومان)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = manualSilverPrice,
                onValueChange = { manualSilverPrice = it },
                label = { Text("قیمت هر گرم نقره ۹۹۹ (تومان)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val g = manualGoldPrice.toLongOrNull() ?: 4850000L
                    val s = manualSilverPrice.toLongOrNull() ?: 98000L
                    viewModel.updateManualPrices(g, s)
                    viewModel.manualRefresh()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("ذخیره نرخ‌های دستی")
            }

            // Normalization notes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("فرمول‌های استاندارد نرمال‌سازی در زرواچ:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("• هر مثقال طلا معادل ۴.۶۰۸۳ گرم طلای ۱۷ عیار (عیار ۷۰۵) است که در زرواچ با فرمول رسمی به طلای ۱۸ عیار (۷۵۰) تبدیل می‌شود.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• کلیه مبالغ به تومان ذخیره و نمایش داده می‌شوند (هر تومان = ۱۰ ریال).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
