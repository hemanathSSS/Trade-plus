package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TradeAction(val label: String, val emoji: String) {
    BUY("BUY SIGNAL", "🟢"),
    SELL("SELL SIGNAL", "🔴"),
    ALERT("PRICE ALERT", "⚡")
}

enum class AlertConditionType(
    val displayName: String,
    val shortDesc: String,
    val defaultAction: TradeAction
) {
    BUY_TARGET("Buy Entry Target Price", "Buy Target ≤", TradeAction.BUY),
    SELL_TAKE_PROFIT("Sell / Take-Profit Target", "Take-Profit ≥", TradeAction.SELL),
    SELL_STOP_LOSS("Sell / Stop-Loss Safety", "Stop-Loss ≤", TradeAction.SELL),
    PROFIT_TARGET_10_PERCENT("10% Profit Milestone", "+10% Profit ≥", TradeAction.SELL),
    PROFIT_TARGET_20_PERCENT("20% High-Profit Milestone", "+20% Profit ≥", TradeAction.SELL),
    PROFIT_TARGET_50_PERCENT("50% Super-Profit Milestone", "+50% Profit ≥", TradeAction.SELL),
    DIP_BUY_5_PERCENT("5% Dip Buy Opportunity", "5% Dip ≤", TradeAction.BUY),
    DIP_BUY_10_PERCENT("10% Deep Value Dip Buy", "10% Dip ≤", TradeAction.BUY),
    AUTO_BUY_STRATEGY("Auto Buy (RSI < 30 + MACD Bullish)", "Auto Buy Signal", TradeAction.BUY),
    AUTO_SELL_STRATEGY("Auto Sell (RSI > 70 + MACD Bearish)", "Auto Sell Signal", TradeAction.SELL),
    PRICE_ABOVE("Price Rises Above", "Price ≥", TradeAction.ALERT),
    PRICE_BELOW("Price Drops Below", "Price ≤", TradeAction.ALERT),
    RSI_OVERBOUGHT("RSI Overbought (Sell Warning)", "RSI ≥", TradeAction.SELL),
    RSI_OVERSOLD("RSI Oversold (Buy Opportunity)", "RSI ≤", TradeAction.BUY),
    MACD_BULLISH_CROSS("MACD Bullish Crossover", "MACD > Signal", TradeAction.BUY),
    MACD_BEARISH_CROSS("MACD Bearish Crossover", "MACD < Signal", TradeAction.SELL),
    VOLUME_SPIKE("Volume Surge Spike", "Vol Spike ≥", TradeAction.ALERT),
    PERCENT_PUMP_24H("24h Price Pump (+X%)", "24h Gain ≥", TradeAction.ALERT),
    PERCENT_DUMP_24H("24h Price Dump (-X%)", "24h Loss ≥", TradeAction.ALERT)
}

@Entity(tableName = "alert_rules")
data class AlertRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String, // e.g. "PLTR", "SOFI", "BTC/USDT"
    val conditionType: AlertConditionType,
    val targetValue: Double, // Target price or indicator threshold
    val tradeAction: TradeAction = conditionType.defaultAction,
    val entryReferencePrice: Double = 0.0, // Initial reference price for profit percentage calculation
    val cooldownMinutes: Int = 15, // minimum cooldown between triggers
    val isEnabled: Boolean = true,
    val notifyAndroid: Boolean = true,
    val notifyTelegram: Boolean = true,
    val notifyDiscord: Boolean = false,
    val playSound: Boolean = true,
    val lastTriggeredTime: Long = 0L,
    val triggerCount: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isReadyToTrigger(currentTime: Long): Boolean {
        if (!isEnabled) return false
        val cooldownMillis = cooldownMinutes * 60 * 1000L
        return (currentTime - lastTriggeredTime) >= cooldownMillis
    }

    val formattedThreshold: String get() = when (conditionType) {
        AlertConditionType.BUY_TARGET,
        AlertConditionType.SELL_TAKE_PROFIT,
        AlertConditionType.SELL_STOP_LOSS,
        AlertConditionType.PRICE_ABOVE,
        AlertConditionType.PRICE_BELOW -> {
            if (targetValue >= 1000) String.format("$%,.2f", targetValue) else String.format("$%.2f", targetValue)
        }
        AlertConditionType.PROFIT_TARGET_10_PERCENT -> "+10% (Target: ${String.format("$%.2f", targetValue)})"
        AlertConditionType.PROFIT_TARGET_20_PERCENT -> "+20% (Target: ${String.format("$%.2f", targetValue)})"
        AlertConditionType.PROFIT_TARGET_50_PERCENT -> "+50% (Target: ${String.format("$%.2f", targetValue)})"
        AlertConditionType.DIP_BUY_5_PERCENT -> "-5% Dip (Target: ${String.format("$%.2f", targetValue)})"
        AlertConditionType.DIP_BUY_10_PERCENT -> "-10% Deep Dip (Target: ${String.format("$%.2f", targetValue)})"
        AlertConditionType.RSI_OVERBOUGHT,
        AlertConditionType.RSI_OVERSOLD -> String.format("%.0f", targetValue)
        AlertConditionType.PERCENT_PUMP_24H -> "+${String.format("%.1f", targetValue)}%"
        AlertConditionType.PERCENT_DUMP_24H -> "-${String.format("%.1f", kotlin.math.abs(targetValue))}%"
        AlertConditionType.VOLUME_SPIKE -> "${String.format("%.0f", targetValue)}% Spike"
        AlertConditionType.AUTO_BUY_STRATEGY -> "RSI < 30 & MACD +"
        AlertConditionType.AUTO_SELL_STRATEGY -> "RSI > 70 & MACD -"
        AlertConditionType.MACD_BULLISH_CROSS,
        AlertConditionType.MACD_BEARISH_CROSS -> "MACD Cross"
    }
}
