package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.ZarWatchDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.ColorEngine
import com.example.domain.engine.FinancialCalculator
import com.example.domain.model.ColorDriver
import com.example.domain.model.MetalType
import com.example.domain.model.ValuationMode
import com.example.ui.components.PersianFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal

object WidgetHelper {

    const val ACTION_MANUAL_REFRESH = "com.example.widget.ACTION_MANUAL_REFRESH"

    suspend fun updateAllWidgets(context: Context) = withContext(Dispatchers.IO) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@withContext

        val smallIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ZarWatchWidgetSmall::class.java))
        val mediumIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ZarWatchWidgetMedium::class.java))
        val largeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ZarWatchWidgetLarge::class.java))

        if (smallIds.isEmpty() && mediumIds.isEmpty() && largeIds.isEmpty()) return@withContext

        val db = ZarWatchDatabase.getInstance(context)
        val prefsRepo = UserPreferencesRepository(context)
        val prefs = prefsRepo.userPreferencesFlow.first()

        // Read holdings
        val goldHolding = db.holdingDao().getHoldingSync(MetalType.GOLD.name)
        val silverHolding = db.holdingDao().getHoldingSync(MetalType.SILVER.name)

        val goldGrams = goldHolding?.totalGrams?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val goldCost = goldHolding?.totalCostBasisToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val goldRealized = goldHolding?.realizedProfitToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO

        val silverGrams = silverHolding?.totalGrams?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val silverCost = silverHolding?.totalCostBasisToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        val silverRealized = silverHolding?.realizedProfitToman?.let { BigDecimal(it) } ?: BigDecimal.ZERO

        // Read latest prices
        val goldObs = db.priceObservationDao().getLatestObservation(MetalType.GOLD.name)
        val silverObs = db.priceObservationDao().getLatestObservation(MetalType.SILVER.name)

        val goldPrice = goldObs?.priceToman?.let { BigDecimal(it) } ?: BigDecimal(prefs.manualGoldPriceToman)
        val silverPrice = silverObs?.priceToman?.let { BigDecimal(it) } ?: BigDecimal(prefs.manualSilverPriceToman)

        val goldSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.GOLD,
            remainingGrams = goldGrams,
            totalCostBasisToman = goldCost,
            currentMarketPriceToman = goldPrice,
            realizedProfitToman = goldRealized,
            valuationMode = prefs.valuationMode
        )

        val silverSummary = FinancialCalculator.calculateHoldingSummary(
            metal = MetalType.SILVER,
            remainingGrams = silverGrams,
            totalCostBasisToman = silverCost,
            currentMarketPriceToman = silverPrice,
            realizedProfitToman = silverRealized,
            valuationMode = prefs.valuationMode
        )

        val portfolio = FinancialCalculator.calculatePortfolioSummary(
            goldSummary = goldSummary,
            silverSummary = silverSummary,
            valuationMode = prefs.valuationMode
        )

        val drivingPercent = ColorEngine.getDrivingValue(portfolio, prefs.colorDriver)
        val bgArgb = ColorEngine.calculateWidgetBackgroundColorArgb(drivingPercent)

        // Pending Intent to launch App
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pending Intent to manual refresh
        val refreshIntent = Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = ACTION_MANUAL_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pnlPercentStr = PersianFormatters.formatPercent(portfolio.unrealizedProfitPercent, prefs.usePersianDigits)
        val pnlTomanStr = PersianFormatters.formatToman(portfolio.unrealizedProfitToman, prefs.usePersianDigits)
        val totalValuationStr = PersianFormatters.formatToman(portfolio.totalValuationToman, prefs.usePersianDigits)
        val timeStr = PersianFormatters.formatDate(System.currentTimeMillis(), prefs.useJalaliCalendar, prefs.usePersianDigits, true)

        // Update Small
        for (id in smallIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_small).apply {
                setInt(R.id.widget_small_root, "setBackgroundColor", bgArgb)
                setTextViewText(R.id.tv_widget_small_pnl_percent, pnlPercentStr)
                setTextViewText(R.id.tv_widget_small_pnl_toman, pnlTomanStr)
                setTextViewText(R.id.tv_widget_small_time, "بروزرسانی: $timeStr")
                setOnClickPendingIntent(R.id.widget_small_root, launchPendingIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }

        // Update Medium
        for (id in mediumIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_medium).apply {
                setInt(R.id.widget_medium_root, "setBackgroundColor", bgArgb)
                setTextViewText(R.id.tv_widget_med_valuation, totalValuationStr)
                setTextViewText(R.id.tv_widget_med_pnl_percent, pnlPercentStr)
                setTextViewText(R.id.tv_widget_med_pnl_toman, "سود/زیان: $pnlTomanStr")
                setTextViewText(
                    R.id.tv_widget_med_gold_pnl,
                    "طلا: " + PersianFormatters.formatPercent(goldSummary.unrealizedProfitPercent, prefs.usePersianDigits)
                )
                setTextViewText(
                    R.id.tv_widget_med_silver_pnl,
                    "نقره: " + PersianFormatters.formatPercent(silverSummary.unrealizedProfitPercent, prefs.usePersianDigits)
                )
                setTextViewText(R.id.tv_widget_med_time, timeStr)
                setOnClickPendingIntent(R.id.widget_medium_root, launchPendingIntent)
                setOnClickPendingIntent(R.id.btn_widget_med_refresh, refreshPendingIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }

        // Update Large
        for (id in largeIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_large).apply {
                setInt(R.id.widget_large_root, "setBackgroundColor", bgArgb)
                setTextViewText(R.id.tv_widget_large_valuation, totalValuationStr)
                setTextViewText(R.id.tv_widget_large_pnl_percent, pnlPercentStr)
                setTextViewText(R.id.tv_widget_large_pnl_toman, "(سود/زیان: $pnlTomanStr)")
                setTextViewText(
                    R.id.tv_widget_large_gold_holdings,
                    PersianFormatters.formatGrams(goldSummary.remainingGrams, prefs.usePersianDigits)
                )
                setTextViewText(
                    R.id.tv_widget_large_gold_price,
                    "مظنه: " + PersianFormatters.formatToman(goldPrice, prefs.usePersianDigits)
                )
                setTextViewText(
                    R.id.tv_widget_large_silver_holdings,
                    PersianFormatters.formatGrams(silverSummary.remainingGrams, prefs.usePersianDigits)
                )
                setTextViewText(
                    R.id.tv_widget_large_silver_price,
                    "مظنه: " + PersianFormatters.formatToman(silverPrice, prefs.usePersianDigits)
                )
                setTextViewText(R.id.tv_widget_large_source, "منبع: ${goldObs?.source ?: "نرخ مرجع زرواچ"}")
                setTextViewText(R.id.tv_widget_large_time, "آخرین بروزرسانی: $timeStr")
                setOnClickPendingIntent(R.id.widget_large_root, launchPendingIntent)
                setOnClickPendingIntent(R.id.btn_widget_large_refresh, refreshPendingIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
