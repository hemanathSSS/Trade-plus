package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.TradePulseApp
import com.example.data.model.*
import com.example.data.network.MarketApiService
import com.example.data.network.WebhookDispatcher
import com.example.service.TradingWatcherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TradePulseApp.instance.database.tradePulseDao()

    // Screen navigation
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Selected pair for chart
    private val _selectedPairSymbol = MutableStateFlow("BTC/USDT")
    val selectedPairSymbol: StateFlow<String> = _selectedPairSymbol.asStateFlow()

    // Selected chart timeframe
    private val _chartTimeframe = MutableStateFlow("1h")
    val chartTimeframe: StateFlow<String> = _chartTimeframe.asStateFlow()

    // Active rules from Room
    val allRules: StateFlow<List<AlertRule>> = dao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Fired logs from Room
    val allLogs: StateFlow<List<AlertLog>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings from Room
    val settings: StateFlow<WatcherSettings> = dao.getSettingsFlow()
        .map { it ?: WatcherSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WatcherSettings())

    // Service state & market data
    val serviceState: StateFlow<TradingWatcherService.Companion.ServiceState> = TradingWatcherService.serviceState
    val liveMarketData: StateFlow<List<MarketPair>> = TradingWatcherService.latestMarketData

    // Local fallback market data if service is off
    private val _localMarketData = MutableStateFlow<List<MarketPair>>(emptyList())
    val marketData: StateFlow<List<MarketPair>> = combine(liveMarketData, _localMarketData) { live, local ->
        if (live.isNotEmpty()) live else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast/Snackbar event channel
    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    init {
        // Seed default starter rules if empty
        viewModelScope.launch {
            val existing = dao.getAllRules().first()
            if (existing.isEmpty()) {
                seedInitialAlertRules()
            }
            refreshMarketDataOnce()
            // Auto start service if preferred
            val currentSettings = dao.getSettings()
            if (currentSettings == null) {
                dao.saveSettings(WatcherSettings(isServiceRunning = true))
                TradingWatcherService.startService(getApplication())
            } else if (currentSettings.isServiceRunning) {
                TradingWatcherService.startService(getApplication())
            }
        }
    }

    private suspend fun seedInitialAlertRules() {
        // Pre-configure high-profit and dip-buy alert rules for low-cost stocks
        val starterRules = listOf(
            AlertRule(
                symbol = "SOFI",
                conditionType = AlertConditionType.DIP_BUY_5_PERCENT,
                targetValue = 9.35,
                tradeAction = TradeAction.BUY,
                entryReferencePrice = 9.85,
                cooldownMinutes = 15,
                isEnabled = true,
                notes = "Dip entry target for high-growth fintech"
            ),
            AlertRule(
                symbol = "SOFI",
                conditionType = AlertConditionType.PROFIT_TARGET_20_PERCENT,
                targetValue = 11.82,
                tradeAction = TradeAction.SELL,
                entryReferencePrice = 9.85,
                cooldownMinutes = 15,
                isEnabled = true,
                notes = "+20% High-profit take-profit target"
            ),
            AlertRule(
                symbol = "PLTR",
                conditionType = AlertConditionType.BUY_TARGET,
                targetValue = 39.50,
                tradeAction = TradeAction.BUY,
                entryReferencePrice = 42.80,
                cooldownMinutes = 15,
                isEnabled = true,
                notes = "Palantir pullback support buy zone"
            ),
            AlertRule(
                symbol = "PLTR",
                conditionType = AlertConditionType.PROFIT_TARGET_20_PERCENT,
                targetValue = 51.36,
                tradeAction = TradeAction.SELL,
                entryReferencePrice = 42.80,
                cooldownMinutes = 15,
                isEnabled = true,
                notes = "+20% High-profit target breakout"
            ),
            AlertRule(
                symbol = "MARA",
                conditionType = AlertConditionType.PROFIT_TARGET_50_PERCENT,
                targetValue = 27.90,
                tradeAction = TradeAction.SELL,
                entryReferencePrice = 18.60,
                cooldownMinutes = 30,
                isEnabled = true,
                notes = "+50% Super-profit momentum exit"
            ),
            AlertRule(
                symbol = "HOOD",
                conditionType = AlertConditionType.AUTO_BUY_STRATEGY,
                targetValue = 30.0,
                tradeAction = TradeAction.BUY,
                entryReferencePrice = 24.35,
                cooldownMinutes = 15,
                isEnabled = true,
                notes = "Robinhood oversold bounce detector"
            )
        )
        for (r in starterRules) {
            dao.insertRule(r)
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun selectPair(symbol: String) {
        _selectedPairSymbol.value = symbol
    }

    fun setTimeframe(tf: String) {
        _chartTimeframe.value = tf
    }

    fun addCustomStock(ticker: String, name: String = "", initialPrice: Double = 15.0) {
        viewModelScope.launch {
            val newPair = MarketApiService.addCustomStock(ticker, name, initialPrice)
            refreshMarketDataOnce()
            _selectedPairSymbol.value = newPair.symbol
            _userFeedback.emit("Added ${newPair.symbol} to 24/7 Watcher")
        }
    }

    fun toggleService() {
        val isCurrentlyRunning = serviceState.value.isRunning
        viewModelScope.launch {
            val currentSettings = dao.getSettings() ?: WatcherSettings()
            if (isCurrentlyRunning) {
                TradingWatcherService.stopService(getApplication())
                dao.saveSettings(currentSettings.copy(isServiceRunning = false))
                _userFeedback.emit("24/7 Watcher Service paused")
            } else {
                TradingWatcherService.startService(getApplication())
                dao.saveSettings(currentSettings.copy(isServiceRunning = true))
                _userFeedback.emit("24/7 Watcher Service started in background")
            }
        }
    }

    fun refreshMarketDataOnce() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = MarketApiService.fetchMarketData()
                _localMarketData.value = data
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun createRule(
        symbol: String,
        conditionType: AlertConditionType,
        targetValue: Double,
        tradeAction: TradeAction,
        cooldownMinutes: Int,
        notifyAndroid: Boolean,
        notifyTelegram: Boolean,
        notifyDiscord: Boolean,
        playSound: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val rule = AlertRule(
                symbol = symbol,
                conditionType = conditionType,
                targetValue = targetValue,
                tradeAction = tradeAction,
                cooldownMinutes = cooldownMinutes,
                isEnabled = true,
                notifyAndroid = notifyAndroid,
                notifyTelegram = notifyTelegram,
                notifyDiscord = notifyDiscord,
                playSound = playSound,
                notes = notes
            )
            dao.insertRule(rule)
            _userFeedback.emit("${tradeAction.emoji} Alert created for $symbol")
        }
    }

    fun toggleRule(rule: AlertRule) {
        viewModelScope.launch {
            dao.setRuleEnabled(rule.id, !rule.isEnabled)
        }
    }

    fun deleteRule(rule: AlertRule) {
        viewModelScope.launch {
            dao.deleteRule(rule)
            _userFeedback.emit("Alert rule deleted")
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            dao.clearAllLogs()
            _userFeedback.emit("Alert history cleared")
        }
    }

    fun saveWatcherSettings(
        pollIntervalSeconds: Int,
        telegramToken: String,
        telegramChatId: String,
        discordWebhook: String,
        deadManUrl: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        autoBuySellEnabled: Boolean
    ) {
        viewModelScope.launch {
            val current = dao.getSettings() ?: WatcherSettings()
            val updated = current.copy(
                pollIntervalSeconds = pollIntervalSeconds,
                telegramBotToken = telegramToken.trim(),
                telegramChatId = telegramChatId.trim(),
                discordWebhookUrl = discordWebhook.trim(),
                deadManSwitchUrl = deadManUrl.trim(),
                isSoundEnabled = soundEnabled,
                isVibrationEnabled = vibrationEnabled,
                autoBuySellStrategyEnabled = autoBuySellEnabled
            )
            dao.saveSettings(updated)
            _userFeedback.emit("Settings saved successfully")
        }
    }

    fun triggerTestNotification(action: TradeAction, symbol: String = "PLTR") {
        viewModelScope.launch {
            val currentSettings = dao.getSettings() ?: WatcherSettings()
            val price = marketData.value.find { it.symbol == symbol }?.price ?: 42.80
            val condition = if (action == TradeAction.BUY) AlertConditionType.DIP_BUY_5_PERCENT else AlertConditionType.PROFIT_TARGET_20_PERCENT
            val msg = if (action == TradeAction.BUY) {
                "TEST BUY SIGNAL: $symbol dip entry triggered at ${String.format("$%.2f", price)}!"
            } else {
                "TEST SELL SIGNAL: $symbol +20% high-profit target hit at ${String.format("$%.2f", price)}!"
            }

            var sentTelegram = false
            var sentDiscord = false

            if (currentSettings.telegramBotToken.isNotBlank() && currentSettings.telegramChatId.isNotBlank()) {
                val res = WebhookDispatcher.sendTelegramAlert(
                    currentSettings.telegramBotToken,
                    currentSettings.telegramChatId,
                    symbol,
                    action,
                    msg,
                    price
                )
                sentTelegram = res.isSuccess
            }

            if (currentSettings.discordWebhookUrl.isNotBlank()) {
                val res = WebhookDispatcher.sendDiscordAlert(
                    currentSettings.discordWebhookUrl,
                    symbol,
                    action,
                    msg,
                    price
                )
                sentDiscord = res.isSuccess
            }

            val log = AlertLog(
                ruleId = 0,
                symbol = symbol,
                tradeAction = action,
                conditionType = condition,
                triggerMessage = msg,
                priceAtTrigger = price,
                sentToAndroid = true,
                sentToTelegram = sentTelegram,
                sentToDiscord = sentDiscord
            )
            dao.insertLog(log)

            // Trigger local foreground service or direct Android notification
            val intent = android.content.Intent(getApplication(), TradingWatcherService::class.java).apply {
                // If service is running, it will dispatch, or we can trigger notification via Service method
            }
            // Fire local notification directly via helper
            sendLocalAlertNotification(symbol, action, msg, price, currentSettings.isSoundEnabled, currentSettings.isVibrationEnabled)

            _userFeedback.emit("⚡ Test ${action.label} sent! Check notification shade & webhooks.")
        }
    }

    fun sendTestTelegram(token: String, chatId: String) {
        viewModelScope.launch {
            _userFeedback.emit("Sending test message to Telegram...")
            val res = WebhookDispatcher.sendTelegramAlert(
                token,
                chatId,
                "BTC/USDT",
                TradeAction.BUY,
                "24/7 Watcher Telegram Integration Test: Connection Successful!",
                68000.0
            )
            if (res.isSuccess) {
                _userFeedback.emit("✅ Telegram message sent successfully!")
            } else {
                _userFeedback.emit("❌ Telegram failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun sendTestDiscord(webhookUrl: String) {
        viewModelScope.launch {
            _userFeedback.emit("Sending test message to Discord...")
            val res = WebhookDispatcher.sendDiscordAlert(
                webhookUrl,
                "BTC/USDT",
                TradeAction.SELL,
                "24/7 Watcher Discord Integration Test: Connection Successful!",
                68000.0
            )
            if (res.isSuccess) {
                _userFeedback.emit("✅ Discord message sent successfully!")
            } else {
                _userFeedback.emit("❌ Discord failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    private fun sendLocalAlertNotification(
        symbol: String,
        action: TradeAction,
        message: String,
        price: Double,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        val context = getApplication<Application>()
        val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val formattedPrice = if (price >= 1000) String.format("$%,.2f", price) else String.format("$%.4f", price)
        val title = "${action.emoji} ${action.label}: $symbol"

        val builder = androidx.core.app.NotificationCompat.Builder(context, TradePulseApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$formattedPrice - $message")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("$formattedPrice\n$message\nTradePulse 24/7 Automated Alert"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (soundEnabled) {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
        }

        if (vibrationEnabled) {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 400), -1)
            }
        }

        manager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
