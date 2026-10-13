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
import com.example.domain.model.MetalType
import com.example.domain.model.TransactionType
import com.example.ui.components.PersianFormatters
import com.example.ui.viewmodel.MainViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: MainViewModel,
    initialMetal: MetalType = MetalType.GOLD,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val goldQuote by viewModel.goldQuote.collectAsState()
    val silverQuote by viewModel.silverQuote.collectAsState()

    var selectedMetal by remember { mutableStateOf(initialMetal) }
    var selectedType by remember { mutableStateOf(TransactionType.BUY) }
    var purity by remember { mutableStateOf(if (initialMetal == MetalType.GOLD) "18K" else "999") }

    var gramsInput by remember { mutableStateOf("") }
    var unitPriceInput by remember {
        val defaultPrice = if (initialMetal == MetalType.GOLD) {
            goldQuote.pricePerGramToman.toPlainString()
        } else {
            silverQuote.pricePerGramToman.toPlainString()
        }
        mutableStateOf(defaultPrice)
    }
    var feeInput by remember { mutableStateOf("0") }
    var sourceInput by remember { mutableStateOf("دستی") }
    var notesInput by remember { mutableStateOf("") }

    val grams = try { BigDecimal(gramsInput.replace(",", ".")) } catch (e: Exception) { BigDecimal.ZERO }
    val unitPrice = try { BigDecimal(unitPriceInput.replace(",", "")) } catch (e: Exception) { BigDecimal.ZERO }
    val fee = try { BigDecimal(feeInput.replace(",", "")) } catch (e: Exception) { BigDecimal.ZERO }
    val totalCost = grams.multiply(unitPrice).add(fee).setScale(0, RoundingMode.HALF_UP)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت معامله جدید طلا و نقره", style = MaterialTheme.typography.titleMedium) },
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
            // Metal Selection
            Text("انتخاب نوع فلز", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMetal == MetalType.GOLD,
                    onClick = {
                        selectedMetal = MetalType.GOLD
                        purity = "18K"
                        unitPriceInput = goldQuote.pricePerGramToman.toPlainString()
                    },
                    label = { Text("طلای ۱۸ عیار") }
                )
                FilterChip(
                    selected = selectedMetal == MetalType.SILVER,
                    onClick = {
                        selectedMetal = MetalType.SILVER
                        purity = "999"
                        unitPriceInput = silverQuote.pricePerGramToman.toPlainString()
                    },
                    label = { Text("نقره ۹۹۹") }
                )
            }

            // Transaction Type Selection
            Text("نوع تراکنش", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == TransactionType.BUY,
                    onClick = { selectedType = TransactionType.BUY },
                    label = { Text("خرید") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.SELL,
                    onClick = { selectedType = TransactionType.SELL },
                    label = { Text("فروش") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.DEPOSIT,
                    onClick = { selectedType = TransactionType.DEPOSIT },
                    label = { Text("واریز دارایی") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.WITHDRAWAL,
                    onClick = { selectedType = TransactionType.WITHDRAWAL },
                    label = { Text("برداشت") }
                )
            }

            // Weight input
            OutlinedTextField(
                value = gramsInput,
                onValueChange = { gramsInput = it },
                label = { Text("وزن خریداری شده (گرم) - تا ۶ رقم اعشار") },
                placeholder = { Text("مثال: 5.425000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Unit Price input
            OutlinedTextField(
                value = unitPriceInput,
                onValueChange = { unitPriceInput = it },
                label = { Text("قیمت هر گرم (تومان)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Fee input
            OutlinedTextField(
                value = feeInput,
                onValueChange = { feeInput = it },
                label = { Text("کارمزد، اجرت یا هزینه جانبی (تومان)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Source input
            Text("منبع یا پلتفرم خرید", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("دستی", "دیجی‌پی", "طلافروشی/بازار", "صندوق سرمایه‌گذاری").forEach { src ->
                    FilterChip(
                        selected = sourceInput == src,
                        onClick = { sourceInput = src },
                        label = { Text(src) }
                    )
                }
            }

            // Notes input
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("یادداشت یا شماره فاکتور (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Calculation Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("خلاصه محاسبه مالی معامله", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("مبلغ خالص خرید:", PersianFormatters.formatToman(grams.multiply(unitPrice), prefs.usePersianDigits))
                    DetailRow("کارمزد و هزینه‌ها:", PersianFormatters.formatToman(fee, prefs.usePersianDigits))
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("مجموع بهای تمام‌شده پرداختی:", PersianFormatters.formatToman(totalCost, prefs.usePersianDigits))
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (grams > BigDecimal.ZERO && unitPrice > BigDecimal.ZERO) {
                        viewModel.addTransaction(
                            metal = selectedMetal,
                            purity = purity,
                            type = selectedType,
                            quantityGrams = grams,
                            unitPriceToman = unitPrice,
                            feeToman = fee,
                            source = sourceInput,
                            notes = notesInput
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = grams > BigDecimal.ZERO && unitPrice > BigDecimal.ZERO
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("ثبت معامله و محاسبه میانگین وزنی", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
