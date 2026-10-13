package com.example.data.repository

import com.example.data.local.AssetHoldingEntity
import com.example.data.local.HoldingDao
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.domain.engine.FinancialCalculator
import com.example.domain.model.MetalType
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class PortfolioRepository(
    private val transactionDao: TransactionDao,
    private val holdingDao: HoldingDao
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByMetal(metal: MetalType): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMetal(metal.name)

    fun getAllHoldings(): Flow<List<AssetHoldingEntity>> = holdingDao.getAllHoldings()

    fun getHolding(metal: MetalType): Flow<AssetHoldingEntity?> = holdingDao.getHolding(metal.name)

    suspend fun addTransaction(
        metal: MetalType,
        purity: String,
        type: TransactionType,
        quantityGrams: BigDecimal,
        unitPriceToman: BigDecimal,
        feeToman: BigDecimal = BigDecimal.ZERO,
        source: String = "MANUAL",
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val totalCost = quantityGrams.multiply(unitPriceToman).add(feeToman)
            .setScale(0, RoundingMode.HALF_UP)

        val entity = TransactionEntity(
            metal = metal.name,
            purity = purity,
            type = type.name,
            quantityGrams = quantityGrams.setScale(6, RoundingMode.HALF_UP).toPlainString(),
            unitPriceToman = unitPriceToman.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            totalCostToman = totalCost.toPlainString(),
            feeToman = feeToman.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            source = source,
            notes = notes,
            timestamp = timestamp,
            remainingGrams = quantityGrams.setScale(6, RoundingMode.HALF_UP).toPlainString(),
            realizedProfitToman = "0"
        )

        val insertedId = transactionDao.insert(entity)
        recalculateHoldingsForMetal(metal)
        insertedId
    }

    suspend fun deleteTransaction(id: Long, metal: MetalType) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
        recalculateHoldingsForMetal(metal)
    }

    suspend fun reconcileHoldings(
        metal: MetalType,
        actualRemainingGrams: BigDecimal,
        currentUnitPriceToman: BigDecimal,
        reason: String
    ): Long = withContext(Dispatchers.IO) {
        val currentHolding = holdingDao.getHoldingSync(metal.name)
        val currentGrams = currentHolding?.totalGrams?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val diffGrams = actualRemainingGrams.subtract(currentGrams)

        if (diffGrams.compareTo(BigDecimal.ZERO) == 0) {
            return@withContext -1L
        }

        val totalCost = diffGrams.abs().multiply(currentUnitPriceToman).setScale(0, RoundingMode.HALF_UP)

        val reconciliationEntity = TransactionEntity(
            metal = metal.name,
            purity = metal.defaultPurity,
            type = TransactionType.RECONCILIATION.name,
            quantityGrams = diffGrams.abs().setScale(6, RoundingMode.HALF_UP).toPlainString(),
            unitPriceToman = currentUnitPriceToman.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            totalCostToman = totalCost.toPlainString(),
            feeToman = "0",
            source = "RECONCILIATION",
            notes = "تعدیل موجودی: $reason (تغییر از ${currentGrams.toPlainString()} به ${actualRemainingGrams.toPlainString()} گرم)",
            timestamp = System.currentTimeMillis(),
            remainingGrams = diffGrams.abs().setScale(6, RoundingMode.HALF_UP).toPlainString(),
            realizedProfitToman = "0"
        )

        val id = transactionDao.insert(reconciliationEntity)
        recalculateHoldingsForMetal(metal)
        id
    }

    suspend fun recalculateHoldingsForMetal(metal: MetalType) = withContext(Dispatchers.IO) {
        val txs = transactionDao.getTransactionsByMetalAscSync(metal.name)

        var runningGrams = BigDecimal.ZERO
        var runningCostBasis = BigDecimal.ZERO
        var totalRealizedProfit = BigDecimal.ZERO

        for (tx in txs) {
            val qty = BigDecimal(tx.quantityGrams)
            val unitPrice = BigDecimal(tx.unitPriceToman)
            val fee = BigDecimal(tx.feeToman)
            val txType = try {
                TransactionType.valueOf(tx.type)
            } catch (e: Exception) {
                TransactionType.BUY
            }

            when (txType) {
                TransactionType.BUY, TransactionType.DEPOSIT -> {
                    val purchaseCost = qty.multiply(unitPrice).add(fee)
                    runningCostBasis = runningCostBasis.add(purchaseCost)
                    runningGrams = runningGrams.add(qty)
                }
                TransactionType.SELL, TransactionType.WITHDRAWAL -> {
                    if (runningGrams > BigDecimal.ZERO) {
                        val soldGrams = qty.min(runningGrams)
                        val avgCostAtSale = runningCostBasis.divide(runningGrams, 10, RoundingMode.HALF_EVEN)
                        val costOfSold = soldGrams.multiply(avgCostAtSale)

                        val grossRevenue = soldGrams.multiply(unitPrice)
                        val netRevenue = grossRevenue.subtract(fee)
                        val realized = netRevenue.subtract(costOfSold)

                        totalRealizedProfit = totalRealizedProfit.add(realized)
                        runningGrams = runningGrams.subtract(soldGrams)
                        runningCostBasis = runningCostBasis.subtract(costOfSold).max(BigDecimal.ZERO)
                    }
                }
                TransactionType.RECONCILIATION -> {
                    // Check if notes indicate positive or negative correction
                    val isPositive = tx.notes.contains("افزایش") || !tx.notes.contains("-")
                    if (isPositive) {
                        val cost = qty.multiply(unitPrice)
                        runningCostBasis = runningCostBasis.add(cost)
                        runningGrams = runningGrams.add(qty)
                    } else {
                        if (runningGrams > BigDecimal.ZERO) {
                            val reducedGrams = qty.min(runningGrams)
                            val avgCost = runningCostBasis.divide(runningGrams, 10, RoundingMode.HALF_EVEN)
                            val costOfReduced = reducedGrams.multiply(avgCost)
                            runningGrams = runningGrams.subtract(reducedGrams)
                            runningCostBasis = runningCostBasis.subtract(costOfReduced).max(BigDecimal.ZERO)
                        }
                    }
                }
            }
        }

        val avgCostPerGram = if (runningGrams > BigDecimal.ZERO) {
            runningCostBasis.divide(runningGrams, 0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val holdingEntity = AssetHoldingEntity(
            metal = metal.name,
            totalGrams = runningGrams.setScale(6, RoundingMode.HALF_UP).toPlainString(),
            totalCostBasisToman = runningCostBasis.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            averageCostPerGramToman = avgCostPerGram.toPlainString(),
            realizedProfitToman = totalRealizedProfit.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            lastUpdated = System.currentTimeMillis()
        )

        holdingDao.upsertHolding(holdingEntity)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
        holdingDao.clearAll()
    }
}
