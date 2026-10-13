# ZarWatch AI (زرواچ) — Native Android Gold & Silver Portfolio Monitor

ZarWatch AI is a production-oriented, Persian-first native Android application engineered to monitor Iranian gold and silver markets (18K & 24K Gold, 999 Silver), track actual investment positions and acquisition costs, compute exact real-time unrealized and realized profit/loss using strict `BigDecimal` arithmetic, and display portfolio status through dynamic, color-changing Android home-screen widgets.

---

## 🏛 Architecture Overview

ZarWatch AI is built strictly according to Modern Android Architecture (Clean Architecture / MVVM), offline-first principles, and Persian RTL design:

```
app/src/main/java/com/example/
├── data/
│   ├── local/                     # Room SQLite Database, DAOs, and Entities
│   │   ├── Entities.kt            # Transactions, Holdings, Price Observations, Alerts, Paper Trades
│   │   ├── Daos.kt                # Reactive Flow queries & transaction operations
│   │   └── ZarWatchDatabase.kt    # Room database singleton
│   ├── preferences/               # Jetpack DataStore Preferences
│   │   └── UserPreferencesRepository.kt
│   ├── providers/                 # Market Price Providers (Provider-independent architecture)
│   │   ├── MarketPriceProvider.kt # Core Provider interface
│   │   └── Providers.kt           # Manual, Demo Live Feed, Network Aggregator with fallback
│   └── repository/                # Portfolio Repository
│       └── PortfolioRepository.kt # Weighted average cost basis & audit reconciliation engine
├── domain/
│   ├── engine/                    # Core Financial and Color Engines
│   │   ├── FinancialCalculator.kt # Strict BigDecimal financial formulas & purity conversions
│   │   └── ColorEngine.kt         # Continuous P/L gradient interpolation & WCAG contrast
│   └── model/                     # Domain Data Models (MetalType, TransactionType, ValuationMode)
│       └── Models.kt
├── integrations/
│   └── digipay/                   # Future Digipay Wallet Integration Module
│       └── DigipayWalletConnector.kt # Mock connector, sandbox balances, import/export
├── trading/
│   └── bot/                       # Future Trading Bot & Paper Trading Engine
│       └── TradingBot.kt          # Strategies (Threshold, TakeProfit, StopLoss), RiskManager, Kill Switch
├── sync/
│   └── PriceSyncCoordinator.kt    # Foreground polling, price observation logging & notification dispatch
├── widget/                        # Native Android Home-Screen Widgets (AppWidgetProvider)
│   ├── ZarWatchWidgetSmall.kt     # 2x2 Widget
│   ├── ZarWatchWidgetMedium.kt    # 4x2 Widget
│   ├── ZarWatchWidgetLarge.kt     # 4x4 Widget
│   ├── WidgetHelper.kt            # RemoteViews builder & dynamic background color binding
│   └── WidgetUpdateWorker.kt      # WorkManager battery-compliant background sync
└── ui/
    ├── components/                # Reusable UI & Persian formatting
    │   ├── PersianFormatters.kt   # Toman currency, Grams, Persian digits, Jalali (Shamsi) calendar
    │   ├── FinancialCards.kt      # DynamicPnlBanner, MetalHoldingCard, MarketTickerRow
    │   └── ZarWatchCanvasChart.kt # Touch-interactive Compose Canvas financial chart
    ├── screens/                   # 13 Dedicated Screens
    │   ├── DashboardScreen.kt
    │   ├── MetalDetailsScreen.kt  # Gold & Silver detail views
    │   ├── AddEditTransactionScreen.kt
    │   ├── ReconciliationScreen.kt # Non-destructive audit reconciliation workflow
    │   ├── TransactionHistoryScreen.kt
    │   ├── HistoricalChartsScreen.kt
    │   ├── AlertsAndTargetsScreen.kt
    │   ├── WidgetConfigScreen.kt
    │   ├── PriceProviderConfigScreen.kt
    │   ├── DigipayIntegrationScreen.kt
    │   ├── PaperTradingScreen.kt
    │   └── SettingsAndBackupScreen.kt
    ├── theme/                     # Terminal-inspired Dark & Light Color Schemes and Vazirmatn Typography
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── MainActivity.kt            # RTL Scaffold, Modal Drawer, Bottom Navigation, and BackHandler
```

---

## ⚡ Key Highlights

### 1. Strict Financial Precision (`BigDecimal`)
- **Zero Float/Double Arithmetic**: All monetary amounts, gram weights, and return percentages use `BigDecimal` with explicit scale and rounding modes (`HALF_EVEN` / `HALF_UP`).
- **Weighted-Average Cost Basis (WACB)**: Recalculates cost basis on new purchases and partial sales, isolating realized P/L from unrealized P/L.
- **Audit Reconciliation**: Allows adjusting holdings to match physical safe counts by generating a distinct `RECONCILIATION` transaction instead of mutating historical purchases.

### 2. Dynamic Profit/Loss Color Engine
- Continuous smooth color gradient between configurable thresholds:
  - `0%`: Neutral Dark Slate (`#1E293B`)
  - `+1%`: Subtle Emerald
  - `+5%`: Medium Emerald
  - `+10%`: Strong Emerald
  - `+20%+`: Saturated Emerald (`#059669`)
  - `-1%`: Subtle Crimson
  - `-5%`: Medium Crimson
  - `-10%`: Strong Crimson
  - `-20%-`: Saturated Crimson (`#DC2626`)
- Color driver configurable by: Combined Portfolio %, Gold %, Silver %, or Absolute Monetary Toman P/L.
- Automatically calculates WCAG-compliant contrasting typography.

### 3. Native Android Home-Screen Widgets
- Standard Android `AppWidgetProvider` with `RemoteViews` layouts for:
  - **Small (2x2)**: Total P/L, return %, dynamic background, and refresh timestamp.
  - **Medium (4x2)**: Total valuation, total P/L, separate Gold & Silver P/L, refresh button.
  - **Large (4x4)**: Comprehensive valuation, Gold & Silver holdings with prices, source attribution, and fast-launch shortcut.
- WorkManager background sync respects Android Doze mode and battery restrictions (15-min periodic minimum interval).

### 4. Persian-First Localization
- Bundled Google Font **Vazirmatn** (`app/src/main/res/font/vazirmatn.ttf`).
- Switchable Persian numerals (۰۱۲۳۴۵۶۷۸۹) and English numerals.
- Accurate Jalali (Solar Hijri / Shamsi) calendar conversion.
- Iranian Toman (TMN) currency normalization and 1 Mesghal (4.6083g, 705/1000) to 18K (750/1000) conversions.

### 5. Digipay & Algorithmic Trading Extension Points
- **Digipay Module**: Safe sandbox connector interface (`DigipayWalletConnector`), balances preview, transaction import, and clear privacy guarantees (no OTPs or passwords stored).
- **Paper Trading Engine**: Virtual sandbox with Risk Manager (position limits, maximum order size, emergency Kill Switch), automated strategies (Price Threshold, Take-Profit, Stop-Loss, MA Crossover, Rebalancer), and complete audit logging.

---

## 🛠 Build & Installation Instructions

### Building in Google AI Studio / Terminal:
To compile the Android project and assemble the APK:
```bash
gradle assembleDebug
```

To run all unit tests:
```bash
gradle :app:testDebugUnitTest
```

### Opening in Android Studio:
1. Open Android Studio (Ladybug or newer).
2. Select **Open** and point to the project root directory.
3. Allow Gradle sync to complete using Gradle 9.1+ and JDK 17 / 21.
4. Run on an Android device or emulator running API 26 or higher.
