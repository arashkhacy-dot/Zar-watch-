package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metal: String, // "GOLD", "SILVER"
    val purity: String, // "18K", "24K", "999"
    val type: String, // "BUY", "SELL", "DEPOSIT", "WITHDRAWAL", "RECONCILIATION"
    val quantityGrams: String,
    val unitPriceToman: String,
    val totalCostToman: String,
    val feeToman: String,
    val source: String,
    val notes: String = "",
    val timestamp: Long,
    val remainingGrams: String,
    val realizedProfitToman: String = "0"
)

@Entity(tableName = "asset_holdings")
data class AssetHoldingEntity(
    @PrimaryKey
    val metal: String, // "GOLD", "SILVER"
    val totalGrams: String,
    val totalCostBasisToman: String,
    val averageCostPerGramToman: String,
    val realizedProfitToman: String = "0",
    val lastUpdated: Long
)

@Entity(tableName = "price_observations")
data class PriceObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metal: String,
    val priceToman: String,
    val source: String,
    val timestamp: Long
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetType: String, // "GOLD_PRICE", "SILVER_PRICE", "PROFIT_PERCENT", "LOSS_PERCENT"
    val thresholdValue: String,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "paper_trades")
data class PaperTradeOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metal: String,
    val type: String, // "BUY", "SELL"
    val strategyName: String,
    val quantityGrams: String,
    val priceToman: String,
    val status: String, // "FILLED", "CANCELLED", "PENDING"
    val timestamp: Long,
    val notes: String = ""
)
