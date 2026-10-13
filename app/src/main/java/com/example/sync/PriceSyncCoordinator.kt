package com.example.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.local.PriceAlertDao
import com.example.data.local.PriceAlertEntity
import com.example.data.local.PriceObservationDao
import com.example.data.local.PriceObservationEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.providers.DemoPriceProvider
import com.example.data.providers.IranianMarketNetworkProvider
import com.example.data.providers.ManualPriceProvider
import com.example.data.providers.MarketPriceProvider
import com.example.domain.model.MetalType
import com.example.domain.model.PriceQuote
import com.example.domain.model.ProviderStatus
import com.example.widget.WidgetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class PriceSyncCoordinator(
    private val context: Context,
    private val observationDao: PriceObservationDao,
    private val alertDao: PriceAlertDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val manualProvider = ManualPriceProvider()
    private val demoProvider = DemoPriceProvider()
    private val networkProvider = IranianMarketNetworkProvider(demoProvider)

    private var activeProvider: MarketPriceProvider = demoProvider

    private val _goldQuote = MutableStateFlow(
        PriceQuote(
            metal = MetalType.GOLD,
            pricePerGramToman = BigDecimal("4885000"),
            change24hPercent = BigDecimal("1.42"),
            high24hToman = BigDecimal("4920000"),
            low24hToman = BigDecimal("4840000"),
            sourceName = "در حال بارگذاری...",
            status = ProviderStatus.ESTIMATED,
            quoteTimestamp = System.currentTimeMillis(),
            fetchTimestamp = System.currentTimeMillis()
        )
    )
    val goldQuote: StateFlow<PriceQuote> = _goldQuote.asStateFlow()

    private val _silverQuote = MutableStateFlow(
        PriceQuote(
            metal = MetalType.SILVER,
            pricePerGramToman = BigDecimal("98400"),
            change24hPercent = BigDecimal("-0.65"),
            high24hToman = BigDecimal("99500"),
            low24hToman = BigDecimal("97800"),
            sourceName = "در حال بارگذاری...",
            status = ProviderStatus.ESTIMATED,
            quoteTimestamp = System.currentTimeMillis(),
            fetchTimestamp = System.currentTimeMillis()
        )
    )
    val silverQuote: StateFlow<PriceQuote> = _silverQuote.asStateFlow()

    private var pollingJob: Job? = null

    init {
        createNotificationChannel()
        // Listen to preferences for provider type and manual prices
        scope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                manualProvider.updateManualPrices(
                    BigDecimal(prefs.manualGoldPriceToman),
                    BigDecimal(prefs.manualSilverPriceToman)
                )
                activeProvider = when (prefs.activeProviderType) {
                    "MANUAL" -> manualProvider
                    "NETWORK" -> networkProvider
                    else -> demoProvider
                }
            }
        }
    }

    fun startForegroundPolling(intervalSeconds: Int = 30) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                syncPrices()
                delay(intervalSeconds * 1000L)
            }
        }
    }

    fun stopForegroundPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun syncPrices() {
        try {
            val gold = activeProvider.fetchGoldPrice()
            val silver = activeProvider.fetchSilverPrice()

            _goldQuote.value = gold
            _silverQuote.value = silver

            // Record observation
            observationDao.insert(
                PriceObservationEntity(
                    metal = MetalType.GOLD.name,
                    priceToman = gold.pricePerGramToman.toPlainString(),
                    source = gold.sourceName,
                    timestamp = gold.fetchTimestamp
                )
            )
            observationDao.insert(
                PriceObservationEntity(
                    metal = MetalType.SILVER.name,
                    priceToman = silver.pricePerGramToman.toPlainString(),
                    source = silver.sourceName,
                    timestamp = silver.fetchTimestamp
                )
            )

            // Check alerts
            checkAlerts(gold.pricePerGramToman, silver.pricePerGramToman)

            // Update home-screen widgets
            WidgetHelper.updateAllWidgets(context)
        } catch (e: Exception) {
            // Keep previous quotes
        }
    }

    private suspend fun checkAlerts(goldPrice: BigDecimal, silverPrice: BigDecimal) {
        val activeAlerts = alertDao.getActiveAlertsSync()
        for (alert in activeAlerts) {
            val threshold = try { BigDecimal(alert.thresholdValue) } catch (e: Exception) { null } ?: continue
            var triggered = false
            var message = ""

            when (alert.targetType) {
                "GOLD_PRICE" -> {
                    if (goldPrice >= threshold) {
                        triggered = true
                        message = "قیمت طلای ۱۸ عیار به حد نصاب ${alert.thresholdValue} تومان رسید!"
                    }
                }
                "SILVER_PRICE" -> {
                    if (silverPrice >= threshold) {
                        triggered = true
                        message = "قیمت نقره ۹۹۹ به حد نصاب ${alert.thresholdValue} تومان رسید!"
                    }
                }
            }

            if (triggered && !alert.isTriggered) {
                sendNotification("هشدار قیمت زرواچ (ZarWatch Alert)", message)
                alertDao.update(alert.copy(isTriggered = true))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZarWatch Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدارهای نوسان قیمت و سود/زیان زرواچ"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, text: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            // ignore if notification permission denied
        }
    }

    companion object {
        const val CHANNEL_ID = "zarwatch_price_alerts"
    }
}
