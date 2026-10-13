package com.example.trading.bot

import com.example.domain.model.MetalType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class StrategyRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val metal: MetalType,
    val triggerType: StrategyTriggerType,
    val targetThresholdToman: BigDecimal,
    val orderGrams: BigDecimal,
    val isBuy: Boolean,
    val isEnabled: Boolean = true
)

enum class StrategyTriggerType(val displayNameFa: String) {
    PRICE_THRESHOLD("حد نصاب قیمت (Price Threshold)"),
    TAKE_PROFIT("کسب سود خودکار (Take Profit)"),
    STOP_LOSS("حد ضرر خودکار (Stop Loss)"),
    MA_CROSSOVER("تقاطع میانگین متحرک (MA Crossover)"),
    REBALANCE("تعدیل و توازن سبد دارایی (Rebalance)")
}

data class PaperOrder(
    val orderId: String = UUID.randomUUID().toString(),
    val metal: MetalType,
    val isBuy: Boolean,
    val grams: BigDecimal,
    val executedPriceToman: BigDecimal,
    val totalCostToman: BigDecimal,
    val strategyName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "EXECUTED"
)

data class PaperPortfolio(
    var cashToman: BigDecimal = BigDecimal("100000000"), // 100M TMN starting paper cash
    var goldGrams: BigDecimal = BigDecimal("10.000000"),
    var silverGrams: BigDecimal = BigDecimal("150.000000")
)

class RiskManager(
    val maxPositionGramsGold: BigDecimal = BigDecimal("100.0"),
    val maxPositionGramsSilver: BigDecimal = BigDecimal("2000.0"),
    val maxTradeSizeToman: BigDecimal = BigDecimal("50000000"), // 50M max per trade
    val maxDailyLossToman: BigDecimal = BigDecimal("15000000")
) {
    var emergencyKillSwitchActive: Boolean = false

    fun validateOrder(
        portfolio: PaperPortfolio,
        metal: MetalType,
        isBuy: Boolean,
        grams: BigDecimal,
        currentPriceToman: BigDecimal
    ): Pair<Boolean, String> {
        if (emergencyKillSwitchActive) {
            return Pair(false, "کلید قطع اضطراری (Kill Switch) فعال است. کلیه معاملات متوقف شده‌اند.")
        }

        val totalCost = grams.multiply(currentPriceToman)
        if (totalCost > maxTradeSizeToman) {
            return Pair(false, "حجم معامله بیش از سقف مجاز ریسک مدیریت سرمایه ($maxTradeSizeToman تومان) است.")
        }

        if (isBuy) {
            if (portfolio.cashToman < totalCost) {
                return Pair(false, "موجودی ریالی حساب آزمایشی برای این خرید کافی نیست.")
            }
            val maxAllowed = if (metal == MetalType.GOLD) maxPositionGramsGold else maxPositionGramsSilver
            val currentPos = if (metal == MetalType.GOLD) portfolio.goldGrams else portfolio.silverGrams
            if (currentPos.add(grams) > maxAllowed) {
                return Pair(false, "سفارش فراتر از سقف مجاز انباشت دارایی برای این فلز است.")
            }
        } else {
            val currentPos = if (metal == MetalType.GOLD) portfolio.goldGrams else portfolio.silverGrams
            if (currentPos < grams) {
                return Pair(false, "موجودی گرمی برای فروش کافی نیست.")
            }
        }

        return Pair(true, "تایید شد.")
    }
}

class AuditLogger {
    private val logs = mutableListOf<String>()

    fun log(action: String, details: String) {
        val entry = "[${System.currentTimeMillis()}] $action: $details"
        logs.add(0, entry)
        if (logs.size > 200) logs.removeAt(logs.size - 1)
    }

    fun getLogs(): List<String> = logs.toList()
}

class PaperTradingEngine(
    val riskManager: RiskManager = RiskManager(),
    val auditLogger: AuditLogger = AuditLogger()
) {
    val portfolio = PaperPortfolio()
    val orders = mutableListOf<PaperOrder>()
    val activeStrategies = mutableListOf<StrategyRule>()

    init {
        // Default demo strategies
        activeStrategies.add(
            StrategyRule(
                name = "کف‌خری طلای ۱۸ عیار",
                description = "خرید ۲ گرم طلا در صورت افت قیمت به کمتر از ۴،۷۵۰،۰۰۰ تومان",
                metal = MetalType.GOLD,
                triggerType = StrategyTriggerType.PRICE_THRESHOLD,
                targetThresholdToman = BigDecimal("4750000"),
                orderGrams = BigDecimal("2.000000"),
                isBuy = true
            )
        )
        activeStrategies.add(
            StrategyRule(
                name = "حد سود نقره ۹۹۹",
                description = "فروش ۵۰ گرم نقره در صورت عبور قیمت از ۱۰۵،۰۰۰ تومان",
                metal = MetalType.SILVER,
                triggerType = StrategyTriggerType.TAKE_PROFIT,
                targetThresholdToman = BigDecimal("105000"),
                orderGrams = BigDecimal("50.000000"),
                isBuy = false
            )
        )
    }

    fun executeOrder(
        metal: MetalType,
        isBuy: Boolean,
        grams: BigDecimal,
        currentPriceToman: BigDecimal,
        strategyName: String
    ): Pair<Boolean, String> {
        val (valid, message) = riskManager.validateOrder(portfolio, metal, isBuy, grams, currentPriceToman)
        if (!valid) {
            auditLogger.log("REJECTED_ORDER", "$strategyName: $message")
            return Pair(false, message)
        }

        val totalCost = grams.multiply(currentPriceToman).setScale(0, RoundingMode.HALF_UP)
        if (isBuy) {
            portfolio.cashToman = portfolio.cashToman.subtract(totalCost)
            if (metal == MetalType.GOLD) {
                portfolio.goldGrams = portfolio.goldGrams.add(grams)
            } else {
                portfolio.silverGrams = portfolio.silverGrams.add(grams)
            }
        } else {
            portfolio.cashToman = portfolio.cashToman.add(totalCost)
            if (metal == MetalType.GOLD) {
                portfolio.goldGrams = portfolio.goldGrams.subtract(grams)
            } else {
                portfolio.silverGrams = portfolio.silverGrams.subtract(grams)
            }
        }

        val order = PaperOrder(
            metal = metal,
            isBuy = isBuy,
            grams = grams,
            executedPriceToman = currentPriceToman,
            totalCostToman = totalCost,
            strategyName = strategyName
        )
        orders.add(0, order)
        auditLogger.log("EXECUTE_ORDER", "موفق: $strategyName -> $grams گرم ${metal.displayNameFa} به قیمت $currentPriceToman تومان")

        return Pair(true, "معامله آزمایشی با موفقیت در موتور پپر تریدینگ ثبت شد.")
    }
}
