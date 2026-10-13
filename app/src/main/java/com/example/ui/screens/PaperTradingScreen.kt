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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import com.example.domain.model.MetalType
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val goldQuote by viewModel.goldQuote.collectAsState()
    val bot = viewModel.paperTradingEngine

    var killSwitchActive by remember { mutableStateOf(bot.riskManager.emergencyKillSwitchActive) }
    var orderResultMsg by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ربات شبیه‌ساز معاملات (Paper Trading)", style = MaterialTheme.typography.titleMedium) },
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
            // Safety & Kill Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("کلید قطع اضطراری (Kill Switch)", fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = killSwitchActive,
                            onCheckedChange = {
                                killSwitchActive = it
                                bot.riskManager.emergencyKillSwitchActive = it
                            }
                        )
                    }
                    Text(
                        text = if (killSwitchActive)
                            "کلیه فعالیت‌های استراتژی‌ها و ثبت معاملات شبیه‌ساز فوراً متوقف شده است."
                        else
                            "سیستم مدیریت ریسک فعال است (سقف هر سفارش: ۵۰ میلیون تومان، سقف پوزیشن طلا: ۱۰۰ گرم).",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (killSwitchActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Paper Portfolio Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("موجودی دارایی در حساب آزمایشی (مجازی)", style = MaterialTheme.typography.titleMedium)
                    DetailRow("موجودی ریالی نقد:", PersianFormatters.formatToman(bot.portfolio.cashToman, prefs.usePersianDigits))
                    DetailRow("طلای ۱۸ عیار:", PersianFormatters.formatGrams(bot.portfolio.goldGrams, prefs.usePersianDigits))
                    DetailRow("نقره ۹۹۹:", PersianFormatters.formatGrams(bot.portfolio.silverGrams, prefs.usePersianDigits))
                }
            }

            // Active Strategies
            Text("استراتژی‌های الگوریتمی آماده", style = MaterialTheme.typography.titleMedium)
            bot.activeStrategies.forEach { st ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = st.name, fontWeight = FontWeight.Bold)
                            Text(text = st.triggerType.displayNameFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(text = st.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = {
                                val (success, msg) = bot.executeOrder(
                                    metal = st.metal,
                                    isBuy = st.isBuy,
                                    grams = st.orderGrams,
                                    currentPriceToman = goldQuote.pricePerGramToman,
                                    strategyName = st.name
                                )
                                orderResultMsg = msg
                                refreshTrigger++
                            },
                            enabled = !killSwitchActive,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("اجرای آزمایشی این شرط الگوریتمی")
                        }
                    }
                }
            }

            orderResultMsg?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Audit Logs
            Text("دفتر ثبت حسابرسی معاملات (Audit Log)", style = MaterialTheme.typography.titleMedium)
            val logs = bot.auditLogger.getLogs()
            if (logs.isEmpty()) {
                Text("هیچ لاگی هنوز ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                logs.take(5).forEach { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = log, modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
