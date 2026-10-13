package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PriceAlertEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.ZarWatchDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.PortfolioRepository
import com.example.domain.engine.FinancialCalculator
import com.example.domain.model.ColorDriver
import com.example.domain.model.MetalType
import com.example.domain.model.PortfolioSummary
import com.example.domain.model.PriceObservation
import com.example.domain.model.PriceQuote
import com.example.domain.model.TimeFrame
import com.example.domain.model.TransactionType
import com.example.domain.model.ValuationMode
import com.example.integrations.digipay.MockDigipayConnector
import com.example.sync.PriceSyncCoordinator
import com.example.trading.bot.PaperTradingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val db = ZarWatchDatabase.getInstance(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val portfolioRepository = PortfolioRepository(db.transactionDao(), db.holdingDao())
    val priceSyncCoordinator = PriceSyncCoordinator(
        context = application,
        observationDao = db.priceObservationDao(),
        alertDao = db.priceAlertDao(),
        preferencesRepository = preferencesRepository
    )

    val digipayConnector = MockDigipayConnector()
    val paperTradingEngine = PaperTradingEngine()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val goldQuote: StateFlow<PriceQuote> = priceSyncCoordinator.goldQuote
    val silverQuote: StateFlow<PriceQuote> = priceSyncCoordinator.silverQuote

    val allTransactions: StateFlow<List<TransactionEntity>> = portfolioRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<PriceAlertEntity>> = db.priceAlertDao().getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolioSummary: StateFlow<PortfolioSummary> = combine(
        portfolioRepository.getHolding(MetalType.GOLD),
        portfolioRepository.getHolding(MetalType.SILVER),
        goldQuote,
        silverQuote,
        userPreferences
    ) { goldHolding, silverHolding, gQuote, sQuote, prefs ->
        val goldGrams = goldHolding?.totalGrams?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val goldCost = goldHolding?.totalCostBasisToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val goldRealized = goldHolding?.realizedProfitToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO

        val silverGrams = silverHolding?.totalGrams?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val silverCost = silverHolding?.totalCostBasisToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val silverRealized = silverHolding?.realizedProfitToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO

        val spread = BigDecimal(prefs.liquidationSpreadPercent.toDouble() / 100.0)
        val deduction = BigDecimal(prefs.liquidationDeductionToman)

        val goldSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.GOLD,
            remainingGrams = goldGrams,
            totalCostBasisToman = goldCost,
            currentMarketPriceToman = gQuote.pricePerGramToman,
            realizedProfitToman = goldRealized,
            valuationMode = prefs.valuationMode,
            spreadPercent = spread,
            deductionPerGramToman = deduction
        )

        val silverSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.SILVER,
            remainingGrams = silverGrams,
            totalCostBasisToman = silverCost,
            currentMarketPriceToman = sQuote.pricePerGramToman,
            realizedProfitToman = silverRealized,
            valuationMode = prefs.valuationMode,
            spreadPercent = spread,
            deductionPerGramToman = deduction
        )

        val isStale = (System.currentTimeMillis() - gQuote.fetchTimestamp) > 300_000L

        FinancialCalculator.calculatePortfolioSummary(
            goldSummary = goldSummary,
            silverSummary = silverSummary,
            valuationMode = prefs.valuationMode,
            isStale = isStale
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        FinancialCalculator.calculatePortfolioSummary(
            FinancialCalculator.calculateHoldingSummary(
                MetalType.GOLD, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("4885000"), BigDecimal.ZERO, ValuationMode.ESTIMATED_LIQUIDATION
            ),
            FinancialCalculator.calculateHoldingSummary(
                MetalType.SILVER, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal("98400"), BigDecimal.ZERO, ValuationMode.ESTIMATED_LIQUIDATION
            ),
            ValuationMode.ESTIMATED_LIQUIDATION
        )
    )

    private val _chartObservations = MutableStateFlow<List<PriceObservation>>(emptyList())
    val chartObservations: StateFlow<List<PriceObservation>> = _chartObservations.asStateFlow()

    init {
        priceSyncCoordinator.startForegroundPolling(30)
        loadHistoricalChart(MetalType.GOLD, TimeFrame.WEEK)
    }

    override fun onCleared() {
        super.onCleared()
        priceSyncCoordinator.stopForegroundPolling()
    }

    fun manualRefresh() {
        viewModelScope.launch {
            priceSyncCoordinator.syncPrices()
        }
    }

    fun addTransaction(
        metal: MetalType,
        purity: String,
        type: TransactionType,
        quantityGrams: BigDecimal,
        unitPriceToman: BigDecimal,
        feeToman: BigDecimal,
        source: String,
        notes: String
    ) {
        viewModelScope.launch {
            portfolioRepository.addTransaction(
                metal = metal,
                purity = purity,
                type = type,
                quantityGrams = quantityGrams,
                unitPriceToman = unitPriceToman,
                feeToman = feeToman,
                source = source,
                notes = notes
            )
        }
    }

    fun deleteTransaction(id: Long, metal: MetalType) {
        viewModelScope.launch {
            portfolioRepository.deleteTransaction(id, metal)
        }
    }

    fun reconcile(metal: MetalType, actualGrams: BigDecimal, unitPrice: BigDecimal, reason: String) {
        viewModelScope.launch {
            portfolioRepository.reconcileHoldings(metal, actualGrams, unitPrice, reason)
        }
    }

    fun loadHistoricalChart(metal: MetalType, timeFrame: TimeFrame) {
        viewModelScope.launch {
            val since = System.currentTimeMillis() - (timeFrame.days.toLong() * 86400000L)
            val dbObs = db.priceObservationDao().getObservationsSync(metal.name, since)
            if (dbObs.size >= 5) {
                _chartObservations.value = dbObs.map {
                    PriceObservation(it.timestamp, BigDecimal(it.priceToman))
                }
            } else {
                // progressive demo points
                val demoObs = com.example.data.providers.DemoPriceProvider()
                    .fetchHistoricalPrices(metal, timeFrame)
                _chartObservations.value = demoObs
            }
        }
    }

    fun addPriceAlert(type: String, threshold: String) {
        viewModelScope.launch {
            db.priceAlertDao().insert(
                PriceAlertEntity(
                    targetType = type,
                    thresholdValue = threshold,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            db.priceAlertDao().delete(alert)
        }
    }

    fun updateDigitsPreference(usePersian: Boolean) {
        viewModelScope.launch { preferencesRepository.updatePersianDigits(usePersian) }
    }

    fun updateJalaliPreference(useJalali: Boolean) {
        viewModelScope.launch { preferencesRepository.updateJalaliCalendar(useJalali) }
    }

    fun updateValuationMode(mode: ValuationMode) {
        viewModelScope.launch { preferencesRepository.updateValuationMode(mode) }
    }

    fun updateColorDriver(driver: ColorDriver) {
        viewModelScope.launch { preferencesRepository.updateColorDriver(driver) }
    }

    fun updateActiveProvider(provider: String) {
        viewModelScope.launch { preferencesRepository.updateActiveProvider(provider) }
    }

    fun updateManualPrices(goldToman: Long, silverToman: Long) {
        viewModelScope.launch { preferencesRepository.updateManualPrices(goldToman, silverToman) }
    }

    fun importDigipayHoldings() {
        viewModelScope.launch {
            digipayConnector.authenticate()
            val balances = digipayConnector.fetchBalances()
            val gQuote = goldQuote.value.pricePerGramToman
            val sQuote = silverQuote.value.pricePerGramToman

            if (balances.goldGrams > BigDecimal.ZERO) {
                portfolioRepository.reconcileHoldings(
                    metal = MetalType.GOLD,
                    actualRemainingGrams = balances.goldGrams,
                    currentUnitPriceToman = gQuote,
                    reason = "همگام‌سازی و انتقال موجودی از کیف پول طلای دیجی‌پی"
                )
            }
            if (balances.silverGrams > BigDecimal.ZERO) {
                portfolioRepository.reconcileHoldings(
                    metal = MetalType.SILVER,
                    actualRemainingGrams = balances.silverGrams,
                    currentUnitPriceToman = sQuote,
                    reason = "همگام‌سازی و انتقال موجودی از کیف پول نقره دیجی‌پی"
                )
            }
        }
    }

    suspend fun exportBackupJson(): String {
        val txs = db.transactionDao().getAllTransactionsAscSync()
        val jsonRoot = JSONObject()
        jsonRoot.put("version", 1)
        jsonRoot.put("exportedAt", System.currentTimeMillis())

        val txArray = JSONArray()
        for (tx in txs) {
            val obj = JSONObject().apply {
                put("metal", tx.metal)
                put("purity", tx.purity)
                put("type", tx.type)
                put("quantityGrams", tx.quantityGrams)
                put("unitPriceToman", tx.unitPriceToman)
                put("totalCostToman", tx.totalCostToman)
                put("feeToman", tx.feeToman)
                put("source", tx.source)
                put("notes", tx.notes)
                put("timestamp", tx.timestamp)
            }
            txArray.put(obj)
        }
        jsonRoot.put("transactions", txArray)
        return jsonRoot.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val item = txArray.getJSONObject(i)
                val metal = MetalType.valueOf(item.getString("metal"))
                portfolioRepository.addTransaction(
                    metal = metal,
                    purity = item.optString("purity", metal.defaultPurity),
                    type = TransactionType.valueOf(item.getString("type")),
                    quantityGrams = BigDecimal(item.getString("quantityGrams")),
                    unitPriceToman = BigDecimal(item.getString("unitPriceToman")),
                    feeToman = BigDecimal(item.optString("feeToman", "0")),
                    source = item.optString("source", "BACKUP_IMPORT"),
                    notes = item.optString("notes", ""),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            portfolioRepository.clearAllData()
        }
    }
}
