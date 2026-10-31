package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long,
    val symbol: String,
    val tradeAction: TradeAction,
    val conditionType: AlertConditionType,
    val triggerMessage: String,
    val priceAtTrigger: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val sentToAndroid: Boolean = true,
    val sentToTelegram: Boolean = false,
    val sentToDiscord: Boolean = false
) {
    val formattedPrice: String get() = when {
        priceAtTrigger >= 1000.0 -> String.format("$%,.2f", priceAtTrigger)
        priceAtTrigger >= 1.0 -> String.format("$%.3f", priceAtTrigger)
        else -> String.format("$%.6f", priceAtTrigger)
    }
}

@Entity(tableName = "watcher_settings")
data class WatcherSettings(
    @PrimaryKey
    val id: Int = 1,
    val isServiceRunning: Boolean = false,
    val pollIntervalSeconds: Int = 5, // 3, 5, 10, 15, 30
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val discordWebhookUrl: String = "",
    val deadManSwitchUrl: String = "",
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val autoBuySellStrategyEnabled: Boolean = true, // 24/7 Auto detect Buy/Sell signals
    val autoStartOnBoot: Boolean = true,
    val lastHeartbeatTimestamp: Long = 0L,
    val totalCyclesRun: Long = 0L,
    val totalAlertsFired: Long = 0L
)
