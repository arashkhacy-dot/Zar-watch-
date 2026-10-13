package com.example.domain.model

import java.math.BigDecimal

enum class MetalType(val displayNameFa: String, val displayNameEn: String, val defaultPurity: String) {
    GOLD("طلا", "Gold", "18K"),
    SILVER("نقره", "Silver", "999")
}

enum class TransactionType(val displayNameFa: String, val displayNameEn: String) {
    BUY("خرید", "Buy"),
    SELL("فروش", "Sell"),
    DEPOSIT("واریز دارایی", "Deposit"),
    WITHDRAWAL("برداشت دارایی", "Withdrawal"),
    RECONCILIATION("تعدیل و تطبیق", "Reconciliation")
}

enum class ValuationMode(val displayNameFa: String, val displayNameEn: String) {
    INDICATIVE("مظنه اسمی بازار", "Indicative Market"),
    ESTIMATED_LIQUIDATION("ارزش خالص فروش (با کسر کارمزد و اسپرد)", "Estimated Liquidation"),
    PLATFORM_QUOTE("مظنه خرید قطعی پلتفرم", "Platform Quote")
}

enum class ProviderStatus(val displayNameFa: String, val displayNameEn: String) {
    LIVE("زنده", "Live"),
    DELAYED("با تاخیر", "Delayed"),
    ESTIMATED("تخمینی", "Estimated"),
    MANUAL("دستی", "Manual"),
    UNAVAILABLE("غیرفعال", "Unavailable")
}

enum class ColorDriver(val displayNameFa: String, val displayNameEn: String) {
    COMBINED_PERCENT("درصد کل پرتفوی", "Combined Portfolio %"),
    GOLD_PERCENT("درصد سود/زیان طلا", "Gold %"),
    SILVER_PERCENT("درصد سود/زیان نقره", "Silver %"),
    ABSOLUTE_MONETARY("مبلغ کل سود/زیان (تومان)", "Absolute Monetary P/L")
}

enum class TimeFrame(val displayNameFa: String, val displayNameEn: String, val days: Int) {
    DAY("امروز", "Today", 1),
    WEEK("۷ روز", "7D", 7),
    MONTH("۳۰ روز", "30D", 30),
    YEAR("یک سال", "1Y", 365),
    ALL("همه", "All", 1825)
}

enum class ChartMetric(val displayNameFa: String, val displayNameEn: String) {
    GOLD_PRICE("قیمت طلای ۱۸ عیار", "Gold Price"),
    SILVER_PRICE("قیمت نقره ۹۹۹", "Silver Price"),
    PORTFOLIO_VALUATION("ارزش کل پرتفوی", "Portfolio Value"),
    UNREALIZED_PNL("سود/زیان تحقق‌نیافته", "Unrealized P/L"),
    RETURN_PERCENTAGE("درصد بازدهی کل", "Return %")
}

data class PriceQuote(
    val metal: MetalType,
    val pricePerGramToman: BigDecimal,
    val change24hPercent: BigDecimal = BigDecimal.ZERO,
    val high24hToman: BigDecimal = BigDecimal.ZERO,
    val low24hToman: BigDecimal = BigDecimal.ZERO,
    val sourceName: String,
    val status: ProviderStatus,
    val quoteTimestamp: Long,
    val fetchTimestamp: Long
)

data class HoldingSummary(
    val metal: MetalType,
    val remainingGrams: BigDecimal,
    val totalCostBasisToman: BigDecimal,
    val averageCostPerGramToman: BigDecimal,
    val currentPriceToman: BigDecimal,
    val estimatedGrossValueToman: BigDecimal,
    val estimatedLiquidationValueToman: BigDecimal,
    val unrealizedProfitToman: BigDecimal,
    val unrealizedProfitPercent: BigDecimal,
    val realizedProfitToman: BigDecimal,
    val lastUpdatedTimestamp: Long
)

data class PortfolioSummary(
    val totalValuationToman: BigDecimal,
    val totalCostBasisToman: BigDecimal,
    val unrealizedProfitToman: BigDecimal,
    val unrealizedProfitPercent: BigDecimal,
    val realizedProfitToman: BigDecimal,
    val goldSummary: HoldingSummary,
    val silverSummary: HoldingSummary,
    val valuationMode: ValuationMode,
    val lastUpdatedTimestamp: Long,
    val isStale: Boolean = false
)

data class PriceObservation(
    val timestamp: Long,
    val priceToman: BigDecimal
)
