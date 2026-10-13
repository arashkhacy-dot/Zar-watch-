package com.example.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PersianFormatters {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(PERSIAN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats a BigDecimal Toman monetary amount with thousands separators and optional currency unit.
     */
    fun formatToman(
        amount: BigDecimal,
        usePersianDigits: Boolean = true,
        includeUnit: Boolean = true
    ): String {
        val rounded = amount.setScale(0, RoundingMode.HALF_UP)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val df = DecimalFormat("#,##0", symbols)
        val formatted = df.format(rounded)
        val withDigits = if (usePersianDigits) toPersianDigits(formatted) else formatted
        return if (includeUnit) {
            if (usePersianDigits) "$withDigits تومان" else "$withDigits TMN"
        } else {
            withDigits
        }
    }

    /**
     * Formats weight in grams (up to 6 decimal places).
     */
    fun formatGrams(
        grams: BigDecimal,
        usePersianDigits: Boolean = true,
        maxDecimals: Int = 4,
        includeUnit: Boolean = true
    ): String {
        val pattern = if (maxDecimals > 0) "#,##0." + "#".repeat(maxDecimals) else "#,##0"
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat(pattern, symbols)
        val formatted = df.format(grams)
        val withDigits = if (usePersianDigits) toPersianDigits(formatted) else formatted
        return if (includeUnit) {
            if (usePersianDigits) "$withDigits گرم" else "$withDigits g"
        } else {
            withDigits
        }
    }

    /**
     * Formats a percentage (e.g. +12.45% or -3.20%).
     */
    fun formatPercent(
        percent: BigDecimal,
        usePersianDigits: Boolean = true,
        includeSign: Boolean = true
    ): String {
        val rounded = percent.setScale(2, RoundingMode.HALF_UP)
        val absVal = rounded.abs()
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = '.'
        }
        val df = DecimalFormat("0.00", symbols)
        val formatted = df.format(absVal)
        val withDigits = if (usePersianDigits) toPersianDigits(formatted) else formatted

        val sign = when {
            percent > BigDecimal.ZERO -> if (includeSign) "+" else ""
            percent < BigDecimal.ZERO -> "-"
            else -> ""
        }

        return "$sign$withDigits%"
    }

    /**
     * Converts a millisecond timestamp to Jalali (Solar Hijri) or Gregorian date string.
     */
    fun formatDate(
        timestamp: Long,
        useJalali: Boolean = true,
        usePersianDigits: Boolean = true,
        includeTime: Boolean = true
    ): String {
        if (timestamp <= 0L) return if (usePersianDigits) "نامشخص" else "N/A"

        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            timeInMillis = timestamp
        }

        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)

        val dateStr = if (useJalali) {
            val j = gregorianToJalali(gYear, gMonth, gDay)
            String.format(Locale.US, "%04d/%02d/%02d", j.year, j.month, j.day)
        } else {
            String.format(Locale.US, "%04d-%02d-%02d", gYear, gMonth, gDay)
        }

        val fullStr = if (includeTime) "$dateStr $timeStr" else dateStr
        return if (usePersianDigits) toPersianDigits(fullStr) else fullStr
    }

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    /**
     * Converts Gregorian Year/Month/Day to Jalali Year/Month/Day using standard arithmetic.
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0 until 11) {
            if (jDayNo >= jDaysInMonth[i]) {
                jDayNo -= jDaysInMonth[i]
                jm = i + 1
            } else {
                break
            }
        }
        val jd = jDayNo + 1

        return JalaliDate(jy, jm + 1, jd)
    }
}
