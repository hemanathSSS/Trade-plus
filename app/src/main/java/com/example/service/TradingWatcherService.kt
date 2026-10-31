package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.TradePulseApp
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertLog
import com.example.data.model.AlertRule
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction
import com.example.data.model.WatcherSettings
import com.example.data.network.MarketApiService
import com.example.data.network.WebhookDispatcher
import com.example.engine.AlertEvaluationEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TradingWatcherService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TradePulse:247WatcherLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopWatcher()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startForeground(SERVICE_NOTIFICATION_ID, buildForegroundNotification("Initializing 24/7 Market Watcher..."))
            startWatcher()
        }

        return START_STICKY
    }

    private fun startWatcher() {
        isRunning = true
        _serviceState.value = _serviceState.value.copy(
            isRunning = true,
            startTime = System.currentTimeMillis()
        )
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // Safe 24h wakelock

        serviceScope.launch {
            val dao = TradePulseApp.instance.database.tradePulseDao()
            var cycle = 0L

            while (isActive && isRunning) {
                val cycleStartTime = System.currentTimeMillis()
                try {
                    // Fetch live market data
                    val marketPairs = MarketApiService.fetchMarketData()
                    _latestMarketData.value = marketPairs

                    // Fetch active rules & settings from Room
                    val activeRules = dao.getActiveRules()
                    val settings = dao.getSettings() ?: WatcherSettings()

                    // Ensure built-in Auto Buy/Sell rules exist if enabled in settings
                    val effectiveRules = activeRules.toMutableList()
                    if (settings.autoBuySellStrategyEnabled) {
                        for (pair in marketPairs) {
                            if (effectiveRules.none { it.symbol == pair.symbol && it.conditionType == AlertConditionType.AUTO_BUY_STRATEGY }) {
                                effectiveRules.add(
                                    AlertRule(
                                        id = -100L,
                                        symbol = pair.symbol,
                                        conditionType = AlertConditionType.AUTO_BUY_STRATEGY,
                                        targetValue = 30.0,
                                        tradeAction = TradeAction.BUY,
                                        cooldownMinutes = 15,
                                        isEnabled = true,
                                        notifyAndroid = true,
                                        notifyTelegram = settings.telegramBotToken.isNotBlank(),
                                        notifyDiscord = settings.discordWebhookUrl.isNotBlank()
                                    )
                                )
                            }
                            if (effectiveRules.none { it.symbol == pair.symbol && it.conditionType == AlertConditionType.AUTO_SELL_STRATEGY }) {
                                effectiveRules.add(
                                    AlertRule(
                                        id = -200L,
                                        symbol = pair.symbol,
                                        conditionType = AlertConditionType.AUTO_SELL_STRATEGY,
                                        targetValue = 70.0,
                                        tradeAction = TradeAction.SELL,
                                        cooldownMinutes = 15,
                                        isEnabled = true,
                                        notifyAndroid = true,
                                        notifyTelegram = settings.telegramBotToken.isNotBlank(),
                                        notifyDiscord = settings.discordWebhookUrl.isNotBlank()
                                    )
                                )
                            }
                        }
                    }

                    // Evaluate rules
                    val triggers = AlertEvaluationEngine.evaluateRules(marketPairs, effectiveRules, cycleStartTime)

                    for (trigger in triggers) {
                        handleTriggeredAlert(trigger.rule, trigger.pair, trigger.action, trigger.triggerMessage, trigger.triggerPrice, settings)
                    }

                    // Update cycle count and heartbeat
                    cycle++
                    dao.incrementCycle(cycleStartTime)

                    // Ping dead-man switch if configured every ~10 cycles
                    if (cycle % 10 == 0L && settings.deadManSwitchUrl.isNotBlank()) {
                        WebhookDispatcher.pingDeadManSwitch(settings.deadManSwitchUrl)
                    }

                    // Update persistent notification summary
                    val topStock = marketPairs.find { it.symbol == "PLTR" } ?: marketPairs.find { it.symbol == "SOFI" } ?: marketPairs.firstOrNull()
                    val topText = topStock?.let { "${it.symbol} ${it.formattedPrice} (${if (it.change24h >= 0) "+" else ""}${String.format("%.1f", it.change24h)}%)" } ?: "Active"
                    val statusText = "$topText • Watching ${marketPairs.size} tickers • Cycle #$cycle"
                    updateForegroundNotification(statusText)

                    _serviceState.value = _serviceState.value.copy(
                        cycleCount = cycle,
                        lastCheckTimestamp = cycleStartTime,
                        activePairsCount = marketPairs.size,
                        activeRulesCount = activeRules.size,
                        latencyMs = System.currentTimeMillis() - cycleStartTime
                    )

                } catch (e: Exception) {
                    _serviceState.value = _serviceState.value.copy(
                        errorCount = _serviceState.value.errorCount + 1,
                        lastError = e.message ?: "Unknown error"
                    )
                }

                // Poll interval delay
                val pollDelay = (TradePulseApp.instance.database.tradePulseDao().getSettings()?.pollIntervalSeconds ?: 5) * 1000L
                val elapsed = System.currentTimeMillis() - cycleStartTime
                val remaining = maxOf(1000L, pollDelay - elapsed)
                delay(remaining)
            }
        }
    }

    private suspend fun handleTriggeredAlert(
        rule: AlertRule,
        pair: MarketPair,
        action: TradeAction,
        message: String,
        price: Double,
        settings: WatcherSettings
    ) {
        val dao = TradePulseApp.instance.database.tradePulseDao()
        val now = System.currentTimeMillis()

        if (rule.id > 0) {
            dao.updateRuleTriggered(rule.id, now)
        }
        dao.incrementAlertsFired()

        var sentTelegram = false
        var sentDiscord = false

        // Dispatch to Telegram
        if (rule.notifyTelegram && settings.telegramBotToken.isNotBlank() && settings.telegramChatId.isNotBlank()) {
            val res = WebhookDispatcher.sendTelegramAlert(
                settings.telegramBotToken,
                settings.telegramChatId,
                pair.symbol,
                action,
                message,
                price
            )
            sentTelegram = res.isSuccess
        }

        // Dispatch to Discord
        if (rule.notifyDiscord && settings.discordWebhookUrl.isNotBlank()) {
            val res = WebhookDispatcher.sendDiscordAlert(
                settings.discordWebhookUrl,
                pair.symbol,
                action,
                message,
                price
            )
            sentDiscord = res.isSuccess
        }

        // Insert into Room Log
        val log = AlertLog(
            ruleId = rule.id,
            symbol = pair.symbol,
            tradeAction = action,
            conditionType = rule.conditionType,
            triggerMessage = message,
            priceAtTrigger = price,
            timestamp = now,
            sentToAndroid = rule.notifyAndroid,
            sentToTelegram = sentTelegram,
            sentToDiscord = sentDiscord
        )
        dao.insertLog(log)

        // Show Heads-Up Android Notification
        if (rule.notifyAndroid) {
            fireAndroidAlertNotification(pair.symbol, action, message, price, settings.isSoundEnabled, settings.isVibrationEnabled)
        }
    }

    private fun fireAndroidAlertNotification(
        symbol: String,
        action: TradeAction,
        message: String,
        price: Double,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${action.emoji} ${action.label}: $symbol"
        val formattedPrice = if (price >= 1000) String.format("$%,.2f", price) else String.format("$%.4f", price)

        val builder = NotificationCompat.Builder(this, TradePulseApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$formattedPrice - $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$formattedPrice\n$message\nTradePulse 24/7 Watcher"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (vibrationEnabled) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 150, 400), -1)
            }
        }

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TradingWatcherService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TradePulseApp.CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("TradePulse 24/7 Engine Active")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pause 24/7", stopPendingIntent)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_NOTIFICATION_ID, buildForegroundNotification(contentText))
    }

    private fun stopWatcher() {
        isRunning = false
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        _serviceState.value = _serviceState.value.copy(isRunning = false)
    }

    override fun onDestroy() {
        stopWatcher()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.tradepulse.START"
        const val ACTION_STOP = "com.example.tradepulse.STOP"
        private const val SERVICE_NOTIFICATION_ID = 247

        data class ServiceState(
            val isRunning: Boolean = false,
            val startTime: Long = 0L,
            val lastCheckTimestamp: Long = 0L,
            val cycleCount: Long = 0L,
            val activePairsCount: Int = 0,
            val activeRulesCount: Int = 0,
            val latencyMs: Long = 0L,
            val errorCount: Long = 0L,
            val lastError: String = ""
        )

        private val _serviceState = MutableStateFlow(ServiceState())
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

        private val _latestMarketData = MutableStateFlow<List<MarketPair>>(emptyList())
        val latestMarketData: StateFlow<List<MarketPair>> = _latestMarketData.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, TradingWatcherService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TradingWatcherService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
