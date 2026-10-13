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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.ValuationMode
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndBackupScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات و پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium) },
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
            // Language & Localization Card
            Text("قالب‌بندی زبان و اعداد", style = MaterialTheme.typography.labelLarge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("اعداد فارسی (۰۱۲۳۴۵۶۷۸۹)", style = MaterialTheme.typography.bodyMedium)
                            Text("نمایش ارقام قیمت و وزن با فونت فارسی وزیرمتن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.usePersianDigits,
                            onCheckedChange = { viewModel.updateDigitsPreference(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تقویم شمسی (جلالی)", style = MaterialTheme.typography.bodyMedium)
                            Text("نمایش تاریخ تراکنش‌ها بر اساس تقویم خورشیدی ایران", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.useJalaliCalendar,
                            onCheckedChange = { viewModel.updateJalaliPreference(it) }
                        )
                    }
                }
            }

            // Valuation Mode Selector
            Text("نحوه ارزش‌گذاری سبد دارایی", style = MaterialTheme.typography.labelLarge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValuationMode.values().forEach { mode ->
                        FilterChip(
                            selected = prefs.valuationMode == mode,
                            onClick = { viewModel.updateValuationMode(mode) },
                            label = { Text(mode.displayNameFa) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Backup & Restore Card
            Text("پشتیبان‌گیری محلی و بازیابی اطلاعات", style = MaterialTheme.typography.labelLarge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اطلاعات مالی شما کاملاً روی حافظه آفلاین دستگاه ذخیره می‌شوند و به هیچ سرور خارجی ارسال نمی‌گردند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportJsonText = viewModel.exportBackupJson()
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("خروجی JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("ورود فایل پشتیبان")
                        }
                    }
                }
            }

            // Danger Zone: Reset Data
            Text("مدیریت حافظه", style = MaterialTheme.typography.labelLarge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x15EF4444)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حذف کامل کلیه تراکنش‌ها و صفر کردن پرتفوی", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("پاکسازی کامل دیتابیس محلی")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("پشتیبان تراکنش‌های زرواچ") },
            text = {
                OutlinedTextField(
                    value = exportJsonText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showExportDialog = false }) { Text("بستن") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("بازیابی اطلاعات از فایل پشتیبان") },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    placeholder = { Text("متن فایل JSON پشتیبان را اینجا قرار دهید...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.importBackupJson(importJsonInput)
                            showImportDialog = false
                            importJsonInput = ""
                        }
                    }
                ) {
                    Text("بازیابی و محاسبه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("تایید حذف کامل اطلاعات") },
            text = { Text("آیا از حذف تمامی خریدها، فروش‌ها و تنظیمات مطمئن هستید؟ این عملیات غیرقابل بازگشت است.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("بله، پاکسازی شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("انصراف") }
            }
        )
    }
}
