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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integrations.digipay.DigipayBalances
import com.example.integrations.digipay.DigipayConnectionStatus
import com.example.integrations.digipay.DigipayTransaction
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigipayIntegrationScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf(viewModel.digipayConnector.getConnectionStatus()) }
    var balances by remember { mutableStateOf<DigipayBalances?>(null) }
    var transactions by remember { mutableStateOf<List<DigipayTransaction>>(emptyList()) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("یکپارچه‌سازی کیف پول طلا و نقره دیجی‌پی", style = MaterialTheme.typography.titleMedium) },
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
            // Security Guarantee Banner
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
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("استاندارد امنیت اطلاعات مالی زرواچ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "زرواچ هیچ‌گونه رمز عبور، پیامک OTP یا کوکی خصوصی دیجی‌پی را درخواست یا ذخیره نمی‌کند. این ماژول برای اتصال ایمن بر اساس استانداردهای رسمی طراحی شده و در این نسخه آزمایشی به صورت ایمن اجرا می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Connection Status & Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow("وضعیت اتصال پلتفرم:", status.displayNameFa)

                    if (status == DigipayConnectionStatus.DISCONNECTED) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.digipayConnector.authenticate()
                                    status = viewModel.digipayConnector.getConnectionStatus()
                                    balances = viewModel.digipayConnector.fetchBalances()
                                    transactions = viewModel.digipayConnector.fetchTransactions()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Wallet, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("برقراری اتصال با محیط ایمن آزمایشی")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.digipayConnector.disconnect()
                                status = viewModel.digipayConnector.getConnectionStatus()
                                balances = null
                                transactions = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("قطع اتصال")
                        }
                    }
                }
            }

            // Balances display
            balances?.let { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("موجودی استعلام‌شده در کیف پول دیجی‌پی", style = MaterialTheme.typography.titleMedium)
                        DetailRow("موجودی طلای ۱۸ عیار:", PersianFormatters.formatGrams(b.goldGrams, prefs.usePersianDigits))
                        DetailRow("موجودی نقره ۹۹۹:", PersianFormatters.formatGrams(b.silverGrams, prefs.usePersianDigits))
                        DetailRow("موجودی ریالی کیف پول:", PersianFormatters.formatToman(b.cashBalanceToman, prefs.usePersianDigits))

                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.importDigipayHoldings()
                                syncMessage = "موجودی دیجی‌پی با موفقیت وارد دفتر کل زرواچ شد و میانگین وزنی محاسبه گردید."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("انتقال موجودی به سبد دارایی زرواچ")
                        }

                        syncMessage?.let { msg ->
                            Text(text = msg, color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Transaction list
            if (transactions.isNotEmpty()) {
                Text("تراکنش‌های دریافت‌شده از دیجی‌پی", style = MaterialTheme.typography.titleMedium)
                transactions.forEach { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "خرید ${PersianFormatters.formatGrams(tx.grams, prefs.usePersianDigits)} ${if (tx.metal.name == "GOLD") "طلا" else "نقره"}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = PersianFormatters.formatToman(tx.totalCostToman, prefs.usePersianDigits),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "کد پیگیری: ${tx.trackingCode} • قیمت واحد: " + PersianFormatters.formatToman(tx.unitPriceToman, prefs.usePersianDigits),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
