package com.example.domain.engine

import com.example.domain.model.HoldingSummary
import com.example.domain.model.MetalType
import com.example.domain.model.PortfolioSummary
import com.example.domain.model.ValuationMode
import java.math.BigDecimal
import java.math.RoundingMode

object FinancialCalculator {

    val SCALE_CURRENCY = 0
    val SCALE_PERCENT = 2
    val SCALE_WEIGHT = 6
    val SCALE_INTERNAL = 10

    // Standard Iranian Gold Conversion Constants
    val MESGHAL_IN_GRAMS: BigDecimal = BigDecimal("4.6083")
    val KARAT_18_PURITY: BigDecimal = BigDecimal("750")
    val KARAT_24_PURITY: BigDecimal = BigDecimal("999.9")
    val SILVER_999_PURITY: BigDecimal = BigDecimal("999")
    val TOMAN_TO_RIAL_RATIO: BigDecimal = BigDecimal("10")

    /**
     * Normalizes ounces to grams: 1 troy ounce = 31.1034768 grams.
     */
    fun ounceToGrams(ounces: BigDecimal): BigDecimal {
        val ounceRatio = BigDecimal("31.1034768")
        return ounces.multiply(ounceRatio).setScale(SCALE_WEIGHT, RoundingMode.HALF_UP)
    }

    /**
     * Converts Iranian Mesghal price to 18-karat gram price:
     * 1 Mesghal (705 purity) / 4.6083 * (750 / 705)
     */
    fun mesghalPriceToGram18K(mesghalPriceToman: BigDecimal): BigDecimal {
        if (mesghalPriceToman <= BigDecimal.ZERO) return BigDecimal.ZERO
        val purityRatio = BigDecimal("750").divide(BigDecimal("705"), SCALE_INTERNAL, RoundingMode.HALF_EVEN)
        val perGram705 = mesghalPriceToman.divide(MESGHAL_IN_GRAMS, SCALE_INTERNAL, RoundingMode.HALF_EVEN)
        return perGram705.multiply(purityRatio).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)
    }

    /**
     * Converts Rial to Toman (divides by 10).
     */
    fun rialToToman(rialAmount: BigDecimal): BigDecimal {
        return rialAmount.divide(TOMAN_TO_RIAL_RATIO, SCALE_CURRENCY, RoundingMode.HALF_UP)
    }

    /**
     * Calculates estimated liquidation selling price per gram given a market price,
     * percentage spread (e.g., 0.015 for 1.5%), and fixed deduction per gram in Toman.
     */
    fun calculateLiquidationPricePerGram(
        marketPricePerGram: BigDecimal,
        spreadPercent: BigDecimal = BigDecimal("0.015"), // 1.5% typical spread
        deductionPerGramToman: BigDecimal = BigDecimal("15000") // typical melt/transaction deduction
    ): BigDecimal {
        if (marketPricePerGram <= BigDecimal.ZERO) return BigDecimal.ZERO
        val discountMultiplier = BigDecimal.ONE.subtract(spreadPercent)
        val discounted = marketPricePerGram.multiply(discountMultiplier)
        val netPrice = discounted.subtract(deductionPerGramToman)
        return netPrice.max(BigDecimal.ZERO).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)
    }

    /**
     * Calculates holding valuation and unrealized profit/loss.
     */
    fun calculateHoldingSummary(
        metal: MetalType,
        remainingGrams: BigDecimal,
        totalCostBasisToman: BigDecimal,
        currentMarketPriceToman: BigDecimal,
        realizedProfitToman: BigDecimal,
        valuationMode: ValuationMode,
        spreadPercent: BigDecimal = BigDecimal("0.015"),
        deductionPerGramToman: BigDecimal = BigDecimal("15000"),
        platformSellingQuoteToman: BigDecimal? = null,
        lastUpdated: Long = System.currentTimeMillis()
    ): HoldingSummary {
        val safeGrams = remainingGrams.max(BigDecimal.ZERO)
        val safeCostBasis = totalCostBasisToman.max(BigDecimal.ZERO)

        val averageCostPerGram = if (safeGrams > BigDecimal.ZERO) {
            safeCostBasis.divide(safeGrams, SCALE_CURRENCY, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val estimatedGrossValue = safeGrams.multiply(currentMarketPriceToman)
            .setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)

        val estimatedLiquidationValue = when (valuationMode) {
            ValuationMode.INDICATIVE -> estimatedGrossValue
            ValuationMode.ESTIMATED_LIQUIDATION -> {
                val netPricePerGram = calculateLiquidationPricePerGram(
                    currentMarketPriceToman,
                    spreadPercent,
                    deductionPerGramToman
                )
                safeGrams.multiply(netPricePerGram).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)
            }
            ValuationMode.PLATFORM_QUOTE -> {
                val quote = platformSellingQuoteToman ?: currentMarketPriceToman
                safeGrams.multiply(quote).setScale(SCALE_CURRENCY, RoundingMode.HALF_UP)
            }
        }

        val valuationToCompare = if (valuationMode == ValuationMode.ESTIMATED_LIQUIDATION) {
            estimatedLiquidationValue
        } else {
            estimatedGrossValue
        }

        val unrealizedProfit = if (safeGrams > BigDecimal.ZERO) {
            valuationToCompare.subtract(safeCostBasis)
        } else {
            BigDecimal.ZERO
        }

        val unrealizedProfitPercent = if (safeCostBasis > BigDecimal.ZERO && safeGrams > BigDecimal.ZERO) {
            unrealizedProfit.multiply(BigDecimal("100"))
                .divide(safeCostBasis, SCALE_PERCENT, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return HoldingSummary(
            metal = metal,
            remainingGrams = safeGrams.setScale(SCALE_WEIGHT, RoundingMode.HALF_UP),
            totalCostBasisToman = safeCostBasis.setScale(SCALE_CURRENCY, RoundingMode.HALF_UP),
            averageCostPerGramToman = averageCostPerGram,
            currentPriceToman = currentMarketPriceToman.setScale(SCALE_CURRENCY, RoundingMode.HALF_UP),
            estimatedGrossValueToman = estimatedGrossValue,
            estimatedLiquidationValueToman = estimatedLiquidationValue,
            unrealizedProfitToman = unrealizedProfit.setScale(SCALE_CURRENCY, RoundingMode.HALF_UP),
            unrealizedProfitPercent = unrealizedProfitPercent,
            realizedProfitToman = realizedProfitToman.setScale(SCALE_CURRENCY, RoundingMode.HALF_UP),
            lastUpdatedTimestamp = lastUpdated
        )
    }

    /**
     * Calculates combined portfolio summary by summing monetary values (not grams).
     */
    fun calculatePortfolioSummary(
        goldSummary: HoldingSummary,
        silverSummary: HoldingSummary,
        valuationMode: ValuationMode,
        isStale: Boolean = false,
        lastUpdated: Long = System.currentTimeMillis()
    ): PortfolioSummary {
        val totalValuation = if (valuationMode == ValuationMode.ESTIMATED_LIQUIDATION) {
            goldSummary.estimatedLiquidationValueToman.add(silverSummary.estimatedLiquidationValueToman)
        } else {
            goldSummary.estimatedGrossValueToman.add(silverSummary.estimatedGrossValueToman)
        }

        val totalCostBasis = goldSummary.totalCostBasisToman.add(silverSummary.totalCostBasisToman)
        val unrealizedProfit = totalValuation.subtract(totalCostBasis)
        val realizedProfit = goldSummary.realizedProfitToman.add(silverSummary.realizedProfitToman)

        val unrealizedPercent = if (totalCostBasis > BigDecimal.ZERO) {
            unrealizedProfit.multiply(BigDecimal("100"))
                .divide(totalCostBasis, SCALE_PERCENT, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return PortfolioSummary(
            totalValuationToman = totalValuation,
            totalCostBasisToman = totalCostBasis,
            unrealizedProfitToman = unrealizedProfit,
            unrealizedProfitPercent = unrealizedPercent,
            realizedProfitToman = realizedProfit,
            goldSummary = goldSummary,
            silverSummary = silverSummary,
            valuationMode = valuationMode,
            lastUpdatedTimestamp = lastUpdated,
            isStale = isStale
        )
    }
}
