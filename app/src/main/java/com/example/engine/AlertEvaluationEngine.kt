package com.example.engine

import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction

data class TriggerEvaluationResult(
    val rule: AlertRule,
    val pair: MarketPair,
    val action: TradeAction,
    val triggerMessage: String,
    val triggerPrice: Double
)

object AlertEvaluationEngine {

    fun evaluateRules(
        pairs: List<MarketPair>,
        rules: List<AlertRule>,
        currentTime: Long = System.currentTimeMillis()
    ): List<TriggerEvaluationResult> {
        val triggered = mutableListOf<TriggerEvaluationResult>()
        val pairMap = pairs.associateBy { it.symbol }

        for (rule in rules) {
            if (!rule.isReadyToTrigger(currentTime)) continue
            val pair = pairMap[rule.symbol] ?: continue

            val result = checkCondition(rule, pair)
            if (result != null) {
                triggered.add(result)
            }
        }

        return triggered
    }

    private fun checkCondition(rule: AlertRule, pair: MarketPair): TriggerEvaluationResult? {
        val price = pair.price
        val threshold = rule.targetValue

        return when (rule.conditionType) {
            AlertConditionType.BUY_TARGET -> {
                if (price <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "BUY TARGET REACHED! ${pair.symbol} dipped to ${pair.formattedPrice} (Target: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.DIP_BUY_5_PERCENT -> {
                if (price <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "5% DIP ENTRY OPPORTUNITY: ${pair.symbol} pulled back to ${pair.formattedPrice} (Buy Dip Target: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.DIP_BUY_10_PERCENT -> {
                if (price <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "10% DEEP VALUE DIP BUY: ${pair.symbol} reached heavy discount at ${pair.formattedPrice} (Target: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.SELL_TAKE_PROFIT -> {
                if (price >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "TAKE-PROFIT TARGET HIT! ${pair.symbol} surged to ${pair.formattedPrice} (Target: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PROFIT_TARGET_10_PERCENT -> {
                if (price >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "💰 +10% PROFIT MILESTONE REACHED! ${pair.symbol} hit target at ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PROFIT_TARGET_20_PERCENT -> {
                if (price >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "🚀 +20% HIGH PROFIT TARGET HIT! ${pair.symbol} rallied to ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PROFIT_TARGET_50_PERCENT -> {
                if (price >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "🔥 +50% SUPER PROFIT ROCKET! ${pair.symbol} soared to ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.SELL_STOP_LOSS -> {
                if (price <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "⚠️ STOP-LOSS TRIGGERED! ${pair.symbol} fell to ${pair.formattedPrice} (Stop: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.AUTO_BUY_STRATEGY -> {
                if (pair.rsi14 <= 32.0 || (pair.rsi14 < 40.0 && pair.macd > pair.macdSignal)) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "STRONG BUY SIGNAL: ${pair.symbol} RSI ${String.format("%.1f", pair.rsi14)} Oversold with positive MACD momentum at ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.AUTO_SELL_STRATEGY -> {
                if (pair.rsi14 >= 68.0 || (pair.rsi14 > 60.0 && pair.macd < pair.macdSignal)) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "SELL / TAKE-PROFIT SIGNAL: ${pair.symbol} RSI ${String.format("%.1f", pair.rsi14)} Overbought resistance at ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PRICE_ABOVE -> {
                if (price >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = rule.tradeAction,
                        triggerMessage = "${pair.symbol} Price surged above threshold: ${pair.formattedPrice} ≥ $threshold",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PRICE_BELOW -> {
                if (price <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = rule.tradeAction,
                        triggerMessage = "${pair.symbol} Price dropped below threshold: ${pair.formattedPrice} ≤ $threshold",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.RSI_OVERBOUGHT -> {
                if (pair.rsi14 >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "${pair.symbol} RSI Overbought: RSI-14 is ${String.format("%.1f", pair.rsi14)} (Threshold: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.RSI_OVERSOLD -> {
                if (pair.rsi14 <= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "${pair.symbol} RSI Oversold: RSI-14 is ${String.format("%.1f", pair.rsi14)} (Threshold: $threshold)",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.MACD_BULLISH_CROSS -> {
                if (pair.macd > pair.macdSignal) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "${pair.symbol} MACD Bullish Crossover! Line crossed above Signal line at ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.MACD_BEARISH_CROSS -> {
                if (pair.macd < pair.macdSignal) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "${pair.symbol} MACD Bearish Crossover! Line dropped below Signal line at ${pair.formattedPrice}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.VOLUME_SPIKE -> {
                if (pair.volume24h >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.ALERT,
                        triggerMessage = "${pair.symbol} Volume Surge Breakout! 24h Volume reached ${String.format("%,.0f", pair.volume24h)}",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PERCENT_PUMP_24H -> {
                if (pair.change24h >= threshold) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.BUY,
                        triggerMessage = "${pair.symbol} High Profit Pump (+${String.format("%.2f", pair.change24h)}%)! Exceeded +$threshold%",
                        triggerPrice = price
                    )
                } else null
            }

            AlertConditionType.PERCENT_DUMP_24H -> {
                if (pair.change24h <= -kotlin.math.abs(threshold)) {
                    TriggerEvaluationResult(
                        rule = rule,
                        pair = pair,
                        action = TradeAction.SELL,
                        triggerMessage = "${pair.symbol} Significant Drop (${String.format("%.2f", pair.change24h)}%)! Dropped past -$threshold%",
                        triggerPrice = price
                    )
                } else null
            }
        }
    }
}
