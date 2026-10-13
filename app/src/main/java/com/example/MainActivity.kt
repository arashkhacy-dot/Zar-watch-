package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MetalType
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.AlertsAndTargetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DigipayIntegrationScreen
import com.example.ui.screens.HistoricalChartsScreen
import com.example.ui.screens.MetalDetailsScreen
import com.example.ui.screens.PaperTradingScreen
import com.example.ui.screens.PriceProviderConfigScreen
import com.example.ui.screens.ReconciliationScreen
import com.example.ui.screens.SettingsAndBackupScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.screens.WidgetConfigScreen
import com.example.ui.theme.ZarWatchTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class Screen(val titleFa: String, val icon: ImageVector) {
    DASHBOARD("پیشخوان", Icons.Default.Dashboard),
    GOLD_DETAILS("جزئیات طلا ۱۸", Icons.Default.Circle),
    SILVER_DETAILS("جزئیات نقره ۹۹۹", Icons.Default.Circle),
    ADD_TRANSACTION("ثبت معامله", Icons.Default.AddCircle),
    RECONCILIATION("تطبیق موجودی", Icons.Default.Autorenew),
    TRANSACTIONS("دفتر کل معاملات", Icons.Default.ReceiptLong),
    CHARTS("نمودار تحلیلی", Icons.Default.Assessment),
    ALERTS("هشدارها و تارگت‌ها", Icons.Default.Notifications),
    WIDGET_CONFIG("پیکربندی ویجت", Icons.Default.Widgets),
    PRICE_PROVIDERS("منابع استخراج قیمت", Icons.Default.Tune),
    DIGIPAY("کیف پول دیجی‌پی", Icons.Default.Wallet),
    PAPER_TRADING("ربات ترید آزمایشی", Icons.Default.SmartToy),
    SETTINGS("تنظیمات و پشتیبان", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by viewModel.userPreferences.collectAsState()
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ZarWatchTheme(darkTheme = prefs.isDarkTheme) {
                // Persian-first RTL Interface Layout
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

                    // BackHandler to navigate back to dashboard
                    if (currentScreen != Screen.DASHBOARD) {
                        BackHandler {
                            currentScreen = Screen.DASHBOARD
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "زرواچ AI (ZarWatch)",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "سامانه مانیتورینگ طلا، نقره و پرتفوی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Screen.values().forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationDrawerItem(
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.titleFa,
                                                tint = if (screen == Screen.GOLD_DETAILS) Color(0xFFF59E0B)
                                                else if (screen == Screen.SILVER_DETAILS) Color(0xFFCBD5E1)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = { Text(screen.titleFa, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        selected = isSelected,
                                        onClick = {
                                            currentScreen = screen
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                if (currentScreen == Screen.DASHBOARD) {
                                    TopAppBar(
                                        title = { Text("زرواچ AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, contentDescription = "منو")
                                            }
                                        }
                                    )
                                }
                            },
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "پیشخوان") },
                                        label = { Text("پیشخوان") },
                                        selected = currentScreen == Screen.DASHBOARD,
                                        onClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Assessment, contentDescription = "نمودار") },
                                        label = { Text("نمودار") },
                                        selected = currentScreen == Screen.CHARTS,
                                        onClick = { currentScreen = Screen.CHARTS }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "معاملات") },
                                        label = { Text("معاملات") },
                                        selected = currentScreen == Screen.TRANSACTIONS,
                                        onClick = { currentScreen = Screen.TRANSACTIONS }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                                        label = { Text("تنظیمات") },
                                        selected = currentScreen == Screen.SETTINGS,
                                        onClick = { currentScreen = Screen.SETTINGS }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    Screen.DASHBOARD -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToAddTransaction = { currentScreen = Screen.ADD_TRANSACTION },
                                        onNavigateToReconcile = { currentScreen = Screen.RECONCILIATION },
                                        onNavigateToGoldDetails = { currentScreen = Screen.GOLD_DETAILS },
                                        onNavigateToSilverDetails = { currentScreen = Screen.SILVER_DETAILS },
                                        onNavigateToCharts = { currentScreen = Screen.CHARTS },
                                        onNavigateToDigipay = { currentScreen = Screen.DIGIPAY },
                                        onNavigateToHistory = { currentScreen = Screen.TRANSACTIONS }
                                    )
                                    Screen.GOLD_DETAILS -> MetalDetailsScreen(
                                        metal = MetalType.GOLD,
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD },
                                        onNavigateToAddTransaction = { currentScreen = Screen.ADD_TRANSACTION }
                                    )
                                    Screen.SILVER_DETAILS -> MetalDetailsScreen(
                                        metal = MetalType.SILVER,
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD },
                                        onNavigateToAddTransaction = { currentScreen = Screen.ADD_TRANSACTION }
                                    )
                                    Screen.ADD_TRANSACTION -> AddEditTransactionScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.RECONCILIATION -> ReconciliationScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.TRANSACTIONS -> TransactionHistoryScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD },
                                        onNavigateToAddTransaction = { currentScreen = Screen.ADD_TRANSACTION }
                                    )
                                    Screen.CHARTS -> HistoricalChartsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.ALERTS -> AlertsAndTargetsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.WIDGET_CONFIG -> WidgetConfigScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.PRICE_PROVIDERS -> PriceProviderConfigScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.DIGIPAY -> DigipayIntegrationScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.PAPER_TRADING -> PaperTradingScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    Screen.SETTINGS -> SettingsAndBackupScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
