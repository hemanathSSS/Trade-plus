package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TradeAction
import com.example.ui.MainViewModel
import com.example.ui.components.CreateAlertDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TradePulseTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val marketData by viewModel.marketData.collectAsStateWithLifecycle()
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()
    val selectedSymbol by viewModel.selectedPairSymbol.collectAsStateWithLifecycle()
    val chartTimeframe by viewModel.chartTimeframe.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state for creating new alert
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogInitialSymbol by remember { mutableStateOf("BTC/USDT") }
    var dialogInitialAction by remember { mutableStateOf(TradeAction.BUY) }

    // Notification Permission Request for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    com.example.TradePulseApp.instance,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.userFeedback.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Custom BackHandler: if not on Watchlist (tab 0), return to tab 0
    BackHandler(enabled = currentTab != 0) {
        viewModel.setTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = SurfaceCard,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Watchlist") },
                    label = { Text("Watchlist") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldAccent,
                        selectedTextColor = EmeraldAccent,
                        indicatorColor = EmeraldAccent.copy(alpha = 0.2f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_watchlist")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts") },
                    label = { Text("Signals") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldAccent,
                        selectedTextColor = EmeraldAccent,
                        indicatorColor = EmeraldAccent.copy(alpha = 0.2f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_signals")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "Chart") },
                    label = { Text("Chart") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = CyanAccent.copy(alpha = 0.2f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_chart")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberAlert,
                        selectedTextColor = AmberAlert,
                        indicatorColor = AmberAlert.copy(alpha = 0.2f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_logs")
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanAccent,
                        selectedTextColor = CyanAccent,
                        indicatorColor = CyanAccent.copy(alpha = 0.2f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkNavyBg)
        ) {
            when (currentTab) {
                0 -> WatchlistScreen(
                    marketData = marketData,
                    serviceState = serviceState,
                    onToggleService = { viewModel.toggleService() },
                    onSelectPairForChart = { symbol ->
                        viewModel.selectPair(symbol)
                        viewModel.setTab(2)
                    },
                    onQuickAddAlert = { symbol, action ->
                        dialogInitialSymbol = symbol
                        dialogInitialAction = action
                        showCreateDialog = true
                    },
                    onAddCustomStock = { ticker, name, price ->
                        viewModel.addCustomStock(ticker, name, price)
                    }
                )
                1 -> SignalsAndAlertsScreen(
                    rules = allRules,
                    onToggleRule = { rule -> viewModel.toggleRule(rule) },
                    onDeleteRule = { rule -> viewModel.deleteRule(rule) },
                    onTriggerTestAlert = { action -> viewModel.triggerTestNotification(action) },
                    onCreateRuleClick = {
                        dialogInitialSymbol = selectedSymbol
                        dialogInitialAction = TradeAction.BUY
                        showCreateDialog = true
                    }
                )
                2 -> ChartScreen(
                    marketData = marketData,
                    selectedSymbol = selectedSymbol,
                    timeframe = chartTimeframe,
                    onSelectSymbol = { viewModel.selectPair(it) },
                    onSelectTimeframe = { viewModel.setTimeframe(it) },
                    onAddAlertClick = { symbol, action ->
                        dialogInitialSymbol = symbol
                        dialogInitialAction = action
                        showCreateDialog = true
                    }
                )
                3 -> LogsScreen(
                    logs = allLogs,
                    onClearLogs = { viewModel.clearAllLogs() }
                )
                4 -> SettingsScreen(
                    settings = settings,
                    onSaveSettings = { poll, tToken, tChat, dWeb, dUrl, snd, vib, autoBS ->
                        viewModel.saveWatcherSettings(poll, tToken, tChat, dWeb, dUrl, snd, vib, autoBS)
                    },
                    onTestTelegram = { token, chatId ->
                        viewModel.sendTestTelegram(token, chatId)
                    },
                    onTestDiscord = { webhookUrl ->
                        viewModel.sendTestDiscord(webhookUrl)
                    }
                )
            }
        }

        // Create Alert Dialog
        if (showCreateDialog) {
            CreateAlertDialog(
                initialSymbol = dialogInitialSymbol,
                initialAction = dialogInitialAction,
                availablePairs = marketData,
                onDismiss = { showCreateDialog = false },
                onSave = { symbol, cond, target, action, cooldown, push, tele, disc, sound, notes ->
                    viewModel.createRule(symbol, cond, target, action, cooldown, push, tele, disc, sound, notes)
                    showCreateDialog = false
                }
            )
        }
    }
}
