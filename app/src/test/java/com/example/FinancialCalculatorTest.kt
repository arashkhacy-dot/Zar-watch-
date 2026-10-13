package com.example

import androidx.compose.ui.graphics.Color
import com.example.domain.engine.ColorEngine
import com.example.domain.engine.FinancialCalculator
import com.example.domain.model.ColorDriver
import com.example.domain.model.MetalType
import com.example.domain.model.ValuationMode
import com.example.trading.bot.PaperPortfolio
import com.example.trading.bot.RiskManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinancialCalculatorTest {

    @Test
    fun testCurrencyAndPurityConversions() {
        // Rial to Toman
        val rials = BigDecimal("50000000")
        val toman = FinancialCalculator.rialToToman(rials)
        assertEquals(BigDecimal("5000000"), toman)

        // Mesghal (705 purity) to 18K (750 purity)
        val mesghalPrice = BigDecimal("21000000") // 21M Toman per mesghal
        val gram18KPrice = FinancialCalculator.mesghalPriceToGram18K(mesghalPrice)
        // 21,000,000 / 4.6083 * (750/705) ≈ 4,847,778
        assertTrue(gram18KPrice > BigDecimal("4800000"))
        assertTrue(gram18KPrice < BigDecimal("4900000"))

        // Ounce to Grams
        val ounces = BigDecimal("1.0")
        val grams = FinancialCalculator.ounceToGrams(ounces)
        assertEquals(BigDecimal("31.103477"), grams)
    }

    @Test
    fun testLiquidationValuation() {
        val marketPrice = BigDecimal("5000000") // 5,000,000 Toman/g
        val spread = BigDecimal("0.02") // 2%
        val deduction = BigDecimal("20000") // 20,000 Toman/g

        val netPrice = FinancialCalculator.calculateLiquidationPricePerGram(marketPrice, spread, deduction)
        // 5,000,000 * 0.98 - 20,000 = 4,900,000 - 20,000 = 4,880,000
        assertEquals(BigDecimal("4880000"), netPrice)
    }

    @Test
    fun testWeightedAverageCostBasisAndUnrealizedProfit() {
        // Purchase 1: 10g at 4,000,000 Toman = 40,000,000
        // Purchase 2: 5g at 5,000,000 Toman = 25,000,000
        // Total Grams: 15g, Total Cost: 65,000,000, Average Cost = 4,333,333
        val remainingGrams = BigDecimal("15.000000")
        val totalCost = BigDecimal("65000000")
        val currentMarketPrice = BigDecimal("5000000")

        val summary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.GOLD,
            remainingGrams = remainingGrams,
            totalCostBasisToman = totalCost,
            currentMarketPriceToman = currentMarketPrice,
            realizedProfitToman = BigDecimal.ZERO,
            valuationMode = ValuationMode.INDICATIVE
        )

        // Estimated Gross Value = 15 * 5,000,000 = 75,000,000
        assertEquals(BigDecimal("75000000"), summary.estimatedGrossValueToman)
        // Unrealized Profit = 75,000,000 - 65,000,000 = 10,000,000
        assertEquals(BigDecimal("10000000"), summary.unrealizedProfitToman)
        // Return % = (10,000,000 / 65,000,000) * 100 ≈ +15.38%
        assertEquals(BigDecimal("15.38"), summary.unrealizedProfitPercent)
    }

    @Test
    fun testZeroHoldingsAndZeroCostBasis() {
        val summary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.GOLD,
            remainingGrams = BigDecimal.ZERO,
            totalCostBasisToman = BigDecimal.ZERO,
            currentMarketPriceToman = BigDecimal("4800000"),
            realizedProfitToman = BigDecimal.ZERO,
            valuationMode = ValuationMode.ESTIMATED_LIQUIDATION
        )

        assertEquals(BigDecimal.ZERO, summary.estimatedGrossValueToman)
        assertEquals(BigDecimal.ZERO, summary.unrealizedProfitToman)
        assertEquals(BigDecimal.ZERO, summary.unrealizedProfitPercent)
        assertEquals(BigDecimal.ZERO, summary.averageCostPerGramToman)
    }

    @Test
    fun testCombinedPortfolioSummarySummingMoneyNotGrams() {
        val goldSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.GOLD,
            remainingGrams = BigDecimal("10.000000"),
            totalCostBasisToman = BigDecimal("40000000"),
            currentMarketPriceToman = BigDecimal("4800000"),
            realizedProfitToman = BigDecimal("2000000"),
            valuationMode = ValuationMode.INDICATIVE
        )

        val silverSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.SILVER,
            remainingGrams = BigDecimal("100.000000"),
            totalCostBasisToman = BigDecimal("9000000"),
            currentMarketPriceToman = BigDecimal("100000"),
            realizedProfitToman = BigDecimal("500000"),
            valuationMode = ValuationMode.INDICATIVE
        )

        val portfolio = FinancialCalculator.calculatePortfolioSummary(
            goldSummary = goldSummary,
            silverSummary = silverSummary,
            valuationMode = ValuationMode.INDICATIVE
        )

        // Gold Valuation: 48M, Silver Valuation: 10M -> Total: 58M
        assertEquals(BigDecimal("58000000"), portfolio.totalValuationToman)
        // Gold Cost: 40M, Silver Cost: 9M -> Total: 49M
        assertEquals(BigDecimal("49000000"), portfolio.totalCostBasisToman)
        // Unrealized P/L: 58M - 49M = 9M
        assertEquals(BigDecimal("9000000"), portfolio.unrealizedProfitToman)
        // Total Realized Profit: 2M + 0.5M = 2.5M
        assertEquals(BigDecimal("2500000"), portfolio.realizedProfitToman)
    }

    @Test
    fun testColorEngineThresholds() {
        val neutral = ColorEngine.calculateBackgroundColor(0f)
        val p1 = ColorEngine.calculateBackgroundColor(1f)
        val p10 = ColorEngine.calculateBackgroundColor(10f)
        val p25 = ColorEngine.calculateBackgroundColor(25f)

        val n1 = ColorEngine.calculateBackgroundColor(-1f)
        val n10 = ColorEngine.calculateBackgroundColor(-10f)

        // Positive gradients should have green tint
        assertTrue(p10.green > neutral.green)
        // Negative gradients should have red tint
        assertTrue(n10.red > neutral.red)

        // WCAG Text Contrast Check
        val textOnDark = ColorEngine.getContrastingTextColor(neutral)
        assertEquals(Color(0xFFF8FAFC), textOnDark)
    }

    @Test
    fun testRiskManagerOrderValidation() {
        val riskManager = RiskManager()
        val portfolio = PaperPortfolio(
            cashToman = BigDecimal("30000000"),
            goldGrams = BigDecimal("5.0"),
            silverGrams = BigDecimal("100.0")
        )

        // Attempting to buy with insufficient cash
        val (res1, msg1) = riskManager.validateOrder(
            portfolio = portfolio,
            metal = MetalType.GOLD,
            isBuy = true,
            grams = BigDecimal("10.0"),
            currentPriceToman = BigDecimal("5000000") // 50M cost > 30M cash
        )
        assertFalse(res1)

        // Emergency Kill Switch
        riskManager.emergencyKillSwitchActive = true
        val (res2, msg2) = riskManager.validateOrder(
            portfolio = portfolio,
            metal = MetalType.GOLD,
            isBuy = true,
            grams = BigDecimal("1.0"),
            currentPriceToman = BigDecimal("5000000")
        )
        assertFalse(res2)
        assertTrue(msg2.contains("Kill Switch"))
    }
}
