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
import androidx.compose.material.icons.filled.Autorenew
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.MetalType
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val portfolio by viewModel.portfolioSummary.collectAsState()
    val goldQuote by viewModel.goldQuote.collectAsState()
    val silverQuote by viewModel.silverQuote.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    var selectedMetal by remember { mutableStateOf(MetalType.GOLD) }
    val currentHolding = if (selectedMetal == MetalType.GOLD) portfolio.goldSummary else portfolio.silverSummary
    val currentPrice = if (selectedMetal == MetalType.GOLD) goldQuote.pricePerGramToman else silverQuote.pricePerGramToman

    var actualGramsInput by remember(selectedMetal) {
        mutableStateOf(currentHolding.remainingGrams.toPlainString())
    }
    var reasonInput by remember { mutableStateOf("تطبیق فیزیکی دارایی") }

    val actualGrams = try {
        BigDecimal(actualGramsInput.replace(",", "."))
    } catch (e: Exception) {
        currentHolding.remainingGrams
    }

    val discrepancyGrams = actualGrams.subtract(currentHolding.remainingGrams)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تطبیق و تعدیل موجودی دارایی", style = MaterialTheme.typography.titleMedium) },
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
            // Explanatory Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "اصول تطبیق دارایی در زرواچ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "به جای دستکاری غیرمجاز تراکنش‌های گذشته، زرواچ اختلاف موجودی فیزیکی و دفتری را با یک سند تعدیل (Reconciliation) رسمی ثبت می‌کند تا حسابرسی میانگین وزنی دست‌نخورده و دقیق باقی بماند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metal Selection
            Text("انتخاب فلز جهت تطبیق", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMetal == MetalType.GOLD,
                    onClick = {
                        selectedMetal = MetalType.GOLD
                        actualGramsInput = portfolio.goldSummary.remainingGrams.toPlainString()
                    },
                    label = { Text("طلای ۱۸ عیار") }
                )
                FilterChip(
                    selected = selectedMetal == MetalType.SILVER,
                    onClick = {
                        selectedMetal = MetalType.SILVER
                        actualGramsInput = portfolio.silverSummary.remainingGrams.toPlainString()
                    },
                    label = { Text("نقره ۹۹۹") }
                )
            }

            // Current State vs Discrepancy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow("موجودی دفتری فعلی:", PersianFormatters.formatGrams(currentHolding.remainingGrams, prefs.usePersianDigits))
                    DetailRow(
                        "اختلاف موجودی (تعدیل):",
                        "${if (discrepancyGrams >= BigDecimal.ZERO) "+" else ""}${PersianFormatters.formatGrams(discrepancyGrams, prefs.usePersianDigits)}"
                    )
                    DetailRow("مظنه روز مرجع برای تعدیل:", PersianFormatters.formatToman(currentPrice, prefs.usePersianDigits))
                }
            }

            // Input Actual Current Grams
            OutlinedTextField(
                value = actualGramsInput,
                onValueChange = { actualGramsInput = it },
                label = { Text("موجودی واقعی فعلی در کیف یا گاوصندوق (گرم)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Reason for adjustment
            OutlinedTextField(
                value = reasonInput,
                onValueChange = { reasonInput = it },
                label = { Text("علت تعدیل موجودی (یادداشت حسابرسی)") },
                placeholder = { Text("مثال: وزن‌کشی مجدد طلا یا خطای ورود قبلی") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Submit Button
            Button(
                onClick = {
                    viewModel.reconcile(
                        metal = selectedMetal,
                        actualGrams = actualGrams,
                        unitPrice = currentPrice,
                        reason = reasonInput
                    )
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = discrepancyGrams.compareTo(BigDecimal.ZERO) != 0
            ) {
                Icon(Icons.Default.Autorenew, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("ثبت سند تطبیق و بازتنظیم میانگین وزنی", fontWeight = FontWeight.Bold)
            }
        }
    }
}
