package com.example.domain.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.domain.model.ColorDriver
import com.example.domain.model.PortfolioSummary
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ColorEngine {

    // Thresholds (%): 0, 1, 5, 10, 20
    private val NEUTRAL_COLOR = Color(0xFF1E293B) // Dark Charcoal Slate
    private val STALE_COLOR = Color(0xFF334155) // Dim Muted Slate

    // Positive Gradient Stops (Subtle -> Saturated Emerald)
    private val GREEN_P1 = Color(0xFF132F27) // +1%
    private val GREEN_P5 = Color(0xFF0D4A36) // +5%
    private val GREEN_P10 = Color(0xFF047857) // +10%
    private val GREEN_P20 = Color(0xFF059669) // +20% or higher

    // Negative Gradient Stops (Subtle -> Saturated Crimson)
    private val RED_N1 = Color(0xFF36181C) // -1%
    private val RED_N5 = Color(0xFF5C1D24) // -5%
    private val RED_N10 = Color(0xFF881337) // -10%
    private val RED_N20 = Color(0xFFDC2626) // -20% or lower

    /**
     * Determines the driving percentage or normalized value based on selected ColorDriver.
     */
    fun getDrivingValue(portfolio: PortfolioSummary, driver: ColorDriver): Float {
        return when (driver) {
            ColorDriver.COMBINED_PERCENT -> portfolio.unrealizedProfitPercent.toFloat()
            ColorDriver.GOLD_PERCENT -> portfolio.goldSummary.unrealizedProfitPercent.toFloat()
            ColorDriver.SILVER_PERCENT -> portfolio.silverSummary.unrealizedProfitPercent.toFloat()
            ColorDriver.ABSOLUTE_MONETARY -> {
                // Normalize monetary P/L: e.g. 100M Toman maps to 10%
                val pnl = portfolio.unrealizedProfitToman.toFloat()
                (pnl / 10_000_000f).coerceIn(-25f, 25f)
            }
        }
    }

    /**
     * Calculates the background color using continuous interpolation across defined profit/loss thresholds.
     */
    fun calculateBackgroundColor(
        percent: Float,
        isStale: Boolean = false
    ): Color {
        if (isStale) return STALE_COLOR

        val clamped = percent.coerceIn(-25f, 25f)

        return when {
            clamped == 0f -> NEUTRAL_COLOR
            clamped > 0f -> interpolatePositive(clamped)
            else -> interpolateNegative(abs(clamped))
        }
    }

    private fun interpolatePositive(p: Float): Color {
        return when {
            p <= 1f -> lerpColor(NEUTRAL_COLOR, GREEN_P1, p / 1f)
            p <= 5f -> lerpColor(GREEN_P1, GREEN_P5, (p - 1f) / 4f)
            p <= 10f -> lerpColor(GREEN_P5, GREEN_P10, (p - 5f) / 5f)
            p <= 20f -> lerpColor(GREEN_P10, GREEN_P20, (p - 10f) / 10f)
            else -> GREEN_P20
        }
    }

    private fun interpolateNegative(n: Float): Color {
        return when {
            n <= 1f -> lerpColor(NEUTRAL_COLOR, RED_N1, n / 1f)
            n <= 5f -> lerpColor(RED_N1, RED_N5, (n - 1f) / 4f)
            n <= 10f -> lerpColor(RED_N5, RED_N10, (n - 5f) / 5f)
            n <= 20f -> lerpColor(RED_N10, RED_N20, (n - 10f) / 10f)
            else -> RED_N20
        }
    }

    private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (end.red - start.red) * f,
            green = start.green + (end.green - start.green) * f,
            blue = start.blue + (end.blue - start.blue) * f,
            alpha = 1.0f
        )
    }

    /**
     * Returns a WCAG compliant text color for the given background color.
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        // Luminance calculation: 0.2126 * R + 0.7152 * G + 0.0722 * B
        val luminance = 0.2126f * backgroundColor.red + 0.7152f * backgroundColor.green + 0.0722f * backgroundColor.blue
        return if (luminance > 0.5f) {
            Color(0xFF0F172A) // Dark slate for bright backgrounds
        } else {
            Color(0xFFF8FAFC) // Crisp white/slate-50 for dark backgrounds
        }
    }

    /**
     * Helper for RemoteViews Android widget: returns integer ARGB.
     */
    fun calculateWidgetBackgroundColorArgb(percent: Float, isStale: Boolean = false): Int {
        return calculateBackgroundColor(percent, isStale).toArgb()
    }

    /**
     * Helper for status text and pill badges: crisp bright green, red, or neutral.
     */
    fun getAccentPnlColor(percent: BigDecimal): Color {
        return when {
            percent > BigDecimal.ZERO -> Color(0xFF10B981) // Emerald Green
            percent < BigDecimal.ZERO -> Color(0xFFEF4444) // Crimson Red
            else -> Color(0xFF94A3B8) // Neutral Slate
        }
    }
}
