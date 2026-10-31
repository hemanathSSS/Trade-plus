package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AlertConditionType
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlertDialog(
    initialSymbol: String,
    initialAction: TradeAction = TradeAction.BUY,
    availablePairs: List<MarketPair>,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf(initialSymbol) }
    var selectedAction by remember { mutableStateOf(initialAction) }

    val currentPair = availablePairs.find { it.symbol == selectedSymbol } ?: availablePairs.firstOrNull()
    val currentPrice = currentPair?.price ?: 15.0

    var selectedCondition by remember(selectedAction) {
        mutableStateOf(
            when (selectedAction) {
                TradeAction.BUY -> AlertConditionType.BUY_TARGET
                TradeAction.SELL -> AlertConditionType.SELL_TAKE_PROFIT
                TradeAction.ALERT -> AlertConditionType.PRICE_ABOVE
            }
        )
    }

    var targetValueText by remember(selectedCondition, currentPrice) {
        val defaultVal = when (selectedCondition) {
            AlertConditionType.BUY_TARGET -> currentPrice * 0.95
            AlertConditionType.DIP_BUY_5_PERCENT -> currentPrice * 0.95
            AlertConditionType.DIP_BUY_10_PERCENT -> currentPrice * 0.90
            AlertConditionType.SELL_TAKE_PROFIT -> currentPrice * 1.15
            AlertConditionType.PROFIT_TARGET_10_PERCENT -> currentPrice * 1.10
            AlertConditionType.PROFIT_TARGET_20_PERCENT -> currentPrice * 1.20
            AlertConditionType.PROFIT_TARGET_50_PERCENT -> currentPrice * 1.50
            AlertConditionType.SELL_STOP_LOSS -> currentPrice * 0.93
            AlertConditionType.RSI_OVERSOLD -> 30.0
            AlertConditionType.RSI_OVERBOUGHT -> 70.0
            AlertConditionType.PERCENT_PUMP_24H -> 5.0
            AlertConditionType.PERCENT_DUMP_24H -> 5.0
            AlertConditionType.VOLUME_SPIKE -> 100000.0
            else -> currentPrice
        }
        mutableStateOf(if (defaultVal >= 1000) String.format("%.2f", defaultVal) else String.format("%.2f", defaultVal))
    }

    var cooldownMinutes by remember { mutableIntStateOf(15) }
    var notifyAndroid by remember { mutableStateOf(true) }
    var notifyTelegram by remember { mutableStateOf(true) }
    var notifyDiscord by remember { mutableStateOf(false) }
    var playSound by remember { mutableStateOf(true) }
    var notesText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "New High-Profit Stock Alert",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Continuous 24/7 background sentinel on device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_alert_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trade Action Selector: BUY, SELL, CUSTOM ALERT
                Text(
                    text = "Signal Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TradeAction.values().forEach { action ->
                        val isSelected = selectedAction == action
                        val actionColor = when (action) {
                            TradeAction.BUY -> EmeraldAccent
                            TradeAction.SELL -> CoralRed
                            TradeAction.ALERT -> CyanAccent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) actionColor.copy(alpha = 0.25f) else SurfaceCardLight
                                )
                                .clickable {
                                    selectedAction = action
                                    selectedCondition = when (action) {
                                        TradeAction.BUY -> AlertConditionType.BUY_TARGET
                                        TradeAction.SELL -> AlertConditionType.PROFIT_TARGET_20_PERCENT
                                        TradeAction.ALERT -> AlertConditionType.PRICE_ABOVE
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(action.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = action.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) actionColor else TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stock Pair Selector
                Text(
                    text = "Select Stock Ticker",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availablePairs) { pair ->
                        val isSel = selectedSymbol == pair.symbol
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyanAccent.copy(alpha = 0.25f) else SurfaceCardLight)
                                .clickable {
                                    selectedSymbol = pair.symbol
                                    // Update target text automatically
                                    val newBase = pair.price
                                    val calc = when (selectedCondition) {
                                        AlertConditionType.PROFIT_TARGET_10_PERCENT -> newBase * 1.10
                                        AlertConditionType.PROFIT_TARGET_20_PERCENT -> newBase * 1.20
                                        AlertConditionType.PROFIT_TARGET_50_PERCENT -> newBase * 1.50
                                        AlertConditionType.DIP_BUY_5_PERCENT -> newBase * 0.95
                                        AlertConditionType.DIP_BUY_10_PERCENT -> newBase * 0.90
                                        else -> newBase
                                    }
                                    targetValueText = String.format("%.2f", calc)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pair.baseAsset,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) CyanAccent else TextPrimary
                                )
                                Text(
                                    text = pair.formattedPrice,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Profit / Dip Target Presets
                Text(
                    text = if (selectedAction == TradeAction.SELL) "High-Profit Multiplier Presets" else "Dip-Buy Entry Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberAlert,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedAction == TradeAction.SELL) {
                        PresetChip(
                            label = "+10% ($${String.format("%.2f", currentPrice * 1.10)})",
                            onClick = {
                                selectedCondition = AlertConditionType.PROFIT_TARGET_10_PERCENT
                                targetValueText = String.format("%.2f", currentPrice * 1.10)
                            }
                        )
                        PresetChip(
                            label = "+20% ($${String.format("%.2f", currentPrice * 1.20)})",
                            onClick = {
                                selectedCondition = AlertConditionType.PROFIT_TARGET_20_PERCENT
                                targetValueText = String.format("%.2f", currentPrice * 1.20)
                            }
                        )
                        PresetChip(
                            label = "+50% ($${String.format("%.2f", currentPrice * 1.50)})",
                            onClick = {
                                selectedCondition = AlertConditionType.PROFIT_TARGET_50_PERCENT
                                targetValueText = String.format("%.2f", currentPrice * 1.50)
                            }
                        )
                    } else {
                        PresetChip(
                            label = "-5% Dip ($${String.format("%.2f", currentPrice * 0.95)})",
                            onClick = {
                                selectedCondition = AlertConditionType.DIP_BUY_5_PERCENT
                                targetValueText = String.format("%.2f", currentPrice * 0.95)
                            }
                        )
                        PresetChip(
                            label = "-10% Deep ($${String.format("%.2f", currentPrice * 0.90)})",
                            onClick = {
                                selectedCondition = AlertConditionType.DIP_BUY_10_PERCENT
                                targetValueText = String.format("%.2f", currentPrice * 0.90)
                            }
                        )
                        PresetChip(
                            label = "Auto RSI < 30",
                            onClick = {
                                selectedCondition = AlertConditionType.AUTO_BUY_STRATEGY
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Condition Type selector
                Text(
                    text = "Trigger Rule Condition",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val relevantConditions = when (selectedAction) {
                    TradeAction.BUY -> listOf(
                        AlertConditionType.DIP_BUY_5_PERCENT,
                        AlertConditionType.DIP_BUY_10_PERCENT,
                        AlertConditionType.BUY_TARGET,
                        AlertConditionType.AUTO_BUY_STRATEGY,
                        AlertConditionType.RSI_OVERSOLD,
                        AlertConditionType.MACD_BULLISH_CROSS
                    )
                    TradeAction.SELL -> listOf(
                        AlertConditionType.PROFIT_TARGET_20_PERCENT,
                        AlertConditionType.PROFIT_TARGET_10_PERCENT,
                        AlertConditionType.PROFIT_TARGET_50_PERCENT,
                        AlertConditionType.SELL_TAKE_PROFIT,
                        AlertConditionType.SELL_STOP_LOSS,
                        AlertConditionType.AUTO_SELL_STRATEGY,
                        AlertConditionType.RSI_OVERBOUGHT
                    )
                    TradeAction.ALERT -> listOf(
                        AlertConditionType.PRICE_ABOVE,
                        AlertConditionType.PRICE_BELOW,
                        AlertConditionType.VOLUME_SPIKE,
                        AlertConditionType.PERCENT_PUMP_24H,
                        AlertConditionType.PERCENT_DUMP_24H
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    relevantConditions.forEach { cond ->
                        val isSelected = selectedCondition == cond
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else SurfaceCardLight)
                                .clickable {
                                    selectedCondition = cond
                                    val calc = when (cond) {
                                        AlertConditionType.PROFIT_TARGET_10_PERCENT -> currentPrice * 1.10
                                        AlertConditionType.PROFIT_TARGET_20_PERCENT -> currentPrice * 1.20
                                        AlertConditionType.PROFIT_TARGET_50_PERCENT -> currentPrice * 1.50
                                        AlertConditionType.DIP_BUY_5_PERCENT -> currentPrice * 0.95
                                        AlertConditionType.DIP_BUY_10_PERCENT -> currentPrice * 0.90
                                        else -> targetValueText.toDoubleOrNull() ?: currentPrice
                                    }
                                    targetValueText = String.format("%.2f", calc)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedCondition = cond
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = cond.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                                Text(
                                    text = "Trigger rule: ${cond.shortDesc}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Value input
                val needsTargetInput = selectedCondition != AlertConditionType.AUTO_BUY_STRATEGY &&
                        selectedCondition != AlertConditionType.AUTO_SELL_STRATEGY &&
                        selectedCondition != AlertConditionType.MACD_BULLISH_CROSS &&
                        selectedCondition != AlertConditionType.MACD_BEARISH_CROSS

                if (needsTargetInput) {
                    Text(
                        text = "Target Price Threshold ($)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = { targetValueText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_target_value"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text("Target Threshold Price ($)") },
                        supportingText = {
                            Text(
                                "Current $selectedSymbol price is ${currentPair?.formattedPrice ?: "$0"}",
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Cooldown Selector
                Text(
                    text = "Notification Cooldown (Anti-Spam)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        val isSel = cooldownMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) EmeraldAccent.copy(alpha = 0.2f) else SurfaceCardLight)
                                .clickable { cooldownMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) EmeraldAccent else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification Channels
                Text(
                    text = "Notification Dispatch Channels",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Android Heads-Up Push", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = notifyAndroid,
                        onCheckedChange = { notifyAndroid = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Telegram Instant Bot Push", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = notifyTelegram,
                        onCheckedChange = { notifyTelegram = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discord Webhook Embed", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = notifyDiscord,
                        onCheckedChange = { notifyDiscord = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audible Chime & Vibration", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = playSound,
                        onCheckedChange = { playSound = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save Action Button
                Button(
                    onClick = {
                        val parsedVal = targetValueText.toDoubleOrNull() ?: currentPrice
                        onSave(
                            selectedSymbol,
                            selectedCondition,
                            parsedVal,
                            selectedAction,
                            cooldownMinutes,
                            notifyAndroid,
                            notifyTelegram,
                            notifyDiscord,
                            playSound,
                            notesText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_alert_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedAction) {
                            TradeAction.BUY -> EmeraldAccent
                            TradeAction.SELL -> CoralRed
                            TradeAction.ALERT -> CyanAccent
                        },
                        contentColor = DarkNavyBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Activate 24/7 Watcher for $selectedSymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardLight)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AmberAlert
        )
    }
}
