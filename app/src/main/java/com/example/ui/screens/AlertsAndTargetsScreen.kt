package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsAndTargetsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val alerts by viewModel.allAlerts.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var alertType by remember { mutableStateOf("GOLD_PRICE") }
    var thresholdInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیم هشدارها و تارگت‌های قیمت", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن هشدار")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
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
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "سامانه پایش هوشمند نوسانات زرواچ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "با رسیدن قیمت یا بازدهی به سقف یا کف تعیین‌شده، اعلان فوری به شما ارسال می‌گردد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (alerts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "هیچ هشداری هنوز تعریف نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("تعریف اولین تارگت قیمت")
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(alerts) { alert ->
                        val typeLabel = when (alert.targetType) {
                            "GOLD_PRICE" -> "قیمت طلای ۱۸ عیار"
                            "SILVER_PRICE" -> "قیمت نقره ۹۹۹"
                            "PROFIT_PERCENT" -> "درصد سود پرتفوی"
                            else -> "درصد زیان پرتفوی"
                        }
                        val unitLabel = if (alert.targetType.contains("PRICE")) "تومان" else "%"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "حد آستانه: ${if (prefs.usePersianDigits) PersianFormatters.toPersianDigits(alert.thresholdValue) else alert.thresholdValue} $unitLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (alert.isTriggered) "فعال‌شده و اطلاع‌رسانی انجام شد" else "در حال رصد لحظه‌ای",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (alert.isTriggered) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(onClick = { viewModel.deletePriceAlert(alert) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف هشدار", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("تعریف هشدار قیمت جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نوع شاخص جهت پایش:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = alertType == "GOLD_PRICE",
                            onClick = { alertType = "GOLD_PRICE" },
                            label = { Text("طلا ۱۸") }
                        )
                        FilterChip(
                            selected = alertType == "SILVER_PRICE",
                            onClick = { alertType = "SILVER_PRICE" },
                            label = { Text("نقره ۹۹۹") }
                        )
                        FilterChip(
                            selected = alertType == "PROFIT_PERCENT",
                            onClick = { alertType = "PROFIT_PERCENT" },
                            label = { Text("تارگت سود %") }
                        )
                    }

                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it },
                        label = { Text("مقدار حد نصاب (تومان یا درصد)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (thresholdInput.isNotEmpty()) {
                            viewModel.addPriceAlert(alertType, thresholdInput)
                            showAddDialog = false
                            thresholdInput = ""
                        }
                    }
                ) {
                    Text("ذخیره هشدار")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
