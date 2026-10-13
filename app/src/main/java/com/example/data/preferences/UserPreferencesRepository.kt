package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ColorDriver
import com.example.domain.model.ValuationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zarwatch_settings")

data class UserPreferences(
    val usePersianDigits: Boolean = true,
    val useJalaliCalendar: Boolean = true,
    val colorDriver: ColorDriver = ColorDriver.COMBINED_PERCENT,
    val valuationMode: ValuationMode = ValuationMode.ESTIMATED_LIQUIDATION,
    val liquidationSpreadPercent: Float = 1.5f,
    val liquidationDeductionToman: Long = 15000L,
    val activeProviderType: String = "DEMO", // "DEMO", "MANUAL", "NETWORK"
    val manualGoldPriceToman: Long = 4850000L,
    val manualSilverPriceToman: Long = 98000L,
    val foregroundRefreshIntervalSeconds: Int = 30,
    val isBiometricEnabled: Boolean = false,
    val isDarkTheme: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USE_PERSIAN_DIGITS = booleanPreferencesKey("use_persian_digits")
        val USE_JALALI_CALENDAR = booleanPreferencesKey("use_jalali_calendar")
        val COLOR_DRIVER = stringPreferencesKey("color_driver")
        val VALUATION_MODE = stringPreferencesKey("valuation_mode")
        val LIQUIDATION_SPREAD = floatPreferencesKey("liquidation_spread")
        val LIQUIDATION_DEDUCTION = longPreferencesKey("liquidation_deduction")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val MANUAL_GOLD_PRICE = longPreferencesKey("manual_gold_price")
        val MANUAL_SILVER_PRICE = longPreferencesKey("manual_silver_price")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val colorDriverStr = prefs[PreferencesKeys.COLOR_DRIVER] ?: ColorDriver.COMBINED_PERCENT.name
        val valuationModeStr = prefs[PreferencesKeys.VALUATION_MODE] ?: ValuationMode.ESTIMATED_LIQUIDATION.name

        val colorDriver = try {
            ColorDriver.valueOf(colorDriverStr)
        } catch (e: Exception) {
            ColorDriver.COMBINED_PERCENT
        }

        val valuationMode = try {
            ValuationMode.valueOf(valuationModeStr)
        } catch (e: Exception) {
            ValuationMode.ESTIMATED_LIQUIDATION
        }

        UserPreferences(
            usePersianDigits = prefs[PreferencesKeys.USE_PERSIAN_DIGITS] ?: true,
            useJalaliCalendar = prefs[PreferencesKeys.USE_JALALI_CALENDAR] ?: true,
            colorDriver = colorDriver,
            valuationMode = valuationMode,
            liquidationSpreadPercent = prefs[PreferencesKeys.LIQUIDATION_SPREAD] ?: 1.5f,
            liquidationDeductionToman = prefs[PreferencesKeys.LIQUIDATION_DEDUCTION] ?: 15000L,
            activeProviderType = prefs[PreferencesKeys.ACTIVE_PROVIDER] ?: "DEMO",
            manualGoldPriceToman = prefs[PreferencesKeys.MANUAL_GOLD_PRICE] ?: 4850000L,
            manualSilverPriceToman = prefs[PreferencesKeys.MANUAL_SILVER_PRICE] ?: 98000L,
            foregroundRefreshIntervalSeconds = prefs[PreferencesKeys.REFRESH_INTERVAL] ?: 30,
            isBiometricEnabled = prefs[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
            isDarkTheme = prefs[PreferencesKeys.DARK_THEME] ?: true
        )
    }

    suspend fun updatePersianDigits(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_PERSIAN_DIGITS] = enabled }
    }

    suspend fun updateJalaliCalendar(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_JALALI_CALENDAR] = enabled }
    }

    suspend fun updateColorDriver(driver: ColorDriver) {
        context.dataStore.edit { it[PreferencesKeys.COLOR_DRIVER] = driver.name }
    }

    suspend fun updateValuationMode(mode: ValuationMode) {
        context.dataStore.edit { it[PreferencesKeys.VALUATION_MODE] = mode.name }
    }

    suspend fun updateLiquidationParameters(spread: Float, deduction: Long) {
        context.dataStore.edit {
            it[PreferencesKeys.LIQUIDATION_SPREAD] = spread
            it[PreferencesKeys.LIQUIDATION_DEDUCTION] = deduction
        }
    }

    suspend fun updateActiveProvider(provider: String) {
        context.dataStore.edit { it[PreferencesKeys.ACTIVE_PROVIDER] = provider }
    }

    suspend fun updateManualPrices(goldToman: Long, silverToman: Long) {
        context.dataStore.edit {
            it[PreferencesKeys.MANUAL_GOLD_PRICE] = goldToman
            it[PreferencesKeys.MANUAL_SILVER_PRICE] = silverToman
        }
    }

    suspend fun updateRefreshInterval(seconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.REFRESH_INTERVAL] = seconds }
    }

    suspend fun updateBiometric(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun updateDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_THEME] = isDark }
    }
}
