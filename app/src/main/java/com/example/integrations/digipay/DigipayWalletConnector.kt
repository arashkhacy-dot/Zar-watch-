package com.example.integrations.digipay

import com.example.domain.model.MetalType
import java.math.BigDecimal

enum class DigipayConnectionStatus(val displayNameFa: String) {
    DISCONNECTED("قطع ارتباط"),
    CONNECTED_MOCK("متصل (محیط آزمایشی ایمن)"),
    AUTHENTICATING("در حال بررسی مجوز"),
    ERROR("خطای ارتباط")
}

data class DigipayBalances(
    val goldGrams: BigDecimal,
    val silverGrams: BigDecimal,
    val cashBalanceToman: BigDecimal,
    val lastSyncTimestamp: Long
)

data class DigipayTransaction(
    val id: String,
    val metal: MetalType,
    val type: String, // "BUY", "SELL"
    val grams: BigDecimal,
    val unitPriceToman: BigDecimal,
    val totalCostToman: BigDecimal,
    val feeToman: BigDecimal,
    val timestamp: Long,
    val trackingCode: String
)

data class DigipayTradeRequest(
    val metal: MetalType,
    val isBuy: Boolean,
    val grams: BigDecimal,
    val maxSlippagePercent: BigDecimal = BigDecimal("0.5")
)

data class DigipayTradeResult(
    val success: Boolean,
    val trackingCode: String,
    val executedGrams: BigDecimal,
    val executedPriceToman: BigDecimal,
    val message: String
)

interface DigipayWalletConnector {
    fun getConnectionStatus(): DigipayConnectionStatus
    suspend fun authenticate(): Boolean
    suspend fun fetchBalances(): DigipayBalances
    suspend fun fetchTransactions(): List<DigipayTransaction>
    suspend fun fetchBuyQuote(metal: MetalType): BigDecimal
    suspend fun fetchSellQuote(metal: MetalType): BigDecimal
    suspend fun requestTrade(order: DigipayTradeRequest): DigipayTradeResult
    fun disconnect()
}

class MockDigipayConnector : DigipayWalletConnector {
    private var status = DigipayConnectionStatus.DISCONNECTED

    private var goldGrams = BigDecimal("15.250000")
    private var silverGrams = BigDecimal("250.000000")
    private var cashToman = BigDecimal("45000000")

    override fun getConnectionStatus(): DigipayConnectionStatus = status

    override suspend fun authenticate(): Boolean {
        // Safe simulation without requesting passwords or OTP
        status = DigipayConnectionStatus.CONNECTED_MOCK
        return true
    }

    override suspend fun fetchBalances(): DigipayBalances {
        return DigipayBalances(
            goldGrams = goldGrams,
            silverGrams = silverGrams,
            cashBalanceToman = cashToman,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun fetchTransactions(): List<DigipayTransaction> {
        val now = System.currentTimeMillis()
        return listOf(
            DigipayTransaction(
                id = "DP-1001",
                metal = MetalType.GOLD,
                type = "BUY",
                grams = BigDecimal("10.000000"),
                unitPriceToman = BigDecimal("4650000"),
                totalCostToman = BigDecimal("46500000"),
                feeToman = BigDecimal("250000"),
                timestamp = now - 86400000L * 15,
                trackingCode = "TRK-98214"
            ),
            DigipayTransaction(
                id = "DP-1002",
                metal = MetalType.GOLD,
                type = "BUY",
                grams = BigDecimal("5.250000"),
                unitPriceToman = BigDecimal("4820000"),
                totalCostToman = BigDecimal("25305000"),
                feeToman = BigDecimal("120000"),
                timestamp = now - 86400000L * 3,
                trackingCode = "TRK-98771"
            ),
            DigipayTransaction(
                id = "DP-1003",
                metal = MetalType.SILVER,
                type = "BUY",
                grams = BigDecimal("250.000000"),
                unitPriceToman = BigDecimal("94500"),
                totalCostToman = BigDecimal("23625000"),
                feeToman = BigDecimal("118000"),
                timestamp = now - 86400000L * 7,
                trackingCode = "TRK-98440"
            )
        )
    }

    override suspend fun fetchBuyQuote(metal: MetalType): BigDecimal {
        return if (metal == MetalType.GOLD) BigDecimal("4920000") else BigDecimal("99800")
    }

    override suspend fun fetchSellQuote(metal: MetalType): BigDecimal {
        return if (metal == MetalType.GOLD) BigDecimal("4810000") else BigDecimal("96800")
    }

    override suspend fun requestTrade(order: DigipayTradeRequest): DigipayTradeResult {
        // Safe mock trade execution
        val tracking = "MOCK-TXN-${System.currentTimeMillis() % 100000}"
        val quote = if (order.isBuy) fetchBuyQuote(order.metal) else fetchSellQuote(order.metal)

        if (order.isBuy) {
            val cost = order.grams.multiply(quote)
            if (cashToman >= cost) {
                cashToman = cashToman.subtract(cost)
                if (order.metal == MetalType.GOLD) goldGrams = goldGrams.add(order.grams)
                else silverGrams = silverGrams.add(order.grams)
                return DigipayTradeResult(true, tracking, order.grams, quote, "سفارش آزمایشی با موفقیت در دیجی‌پی ثبت شد.")
            } else {
                return DigipayTradeResult(false, "", BigDecimal.ZERO, quote, "موجودی ریالی کیف پول دیجی‌پی کافی نیست.")
            }
        } else {
            val available = if (order.metal == MetalType.GOLD) goldGrams else silverGrams
            if (available >= order.grams) {
                if (order.metal == MetalType.GOLD) goldGrams = goldGrams.subtract(order.grams)
                else silverGrams = silverGrams.subtract(order.grams)
                val proceeds = order.grams.multiply(quote)
                cashToman = cashToman.add(proceeds)
                return DigipayTradeResult(true, tracking, order.grams, quote, "فروش آزمایشی طلا در کیف پول دیجی‌پی انجام شد.")
            } else {
                return DigipayTradeResult(false, "", BigDecimal.ZERO, quote, "موجودی گرمی در کیف پول دیجی‌پی کمتر از مقدار درخواستی است.")
            }
        }
    }

    override fun disconnect() {
        status = DigipayConnectionStatus.DISCONNECTED
    }
}
