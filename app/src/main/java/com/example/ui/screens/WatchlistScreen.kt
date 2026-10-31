package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.AssetType
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction
import com.example.service.TradingWatcherService
import com.example.ui.theme.*

@Composable
fun WatchlistScreen(
    marketData: List<MarketPair>,
    serviceState: TradingWatcherService.Companion.ServiceState,
    onToggleService: () -> Unit,
    onSelectPairForChart: (String) -> Unit,
    onQuickAddAlert: (String, TradeAction) -> Unit,
    onAddCustomStock: (String, String, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("STOCKS") } // "STOCKS", "ALL", "CRYPTO"
    var showAddStockDialog by remember { mutableStateOf(false) }

    val filteredList = remember(marketData, selectedCategory) {
        when (selectedCategory) {
            "STOCKS" -> marketData.filter { it.assetType == AssetType.STOCK }
            "CRYPTO" -> marketData.filter { it.assetType == AssetType.CRYPTO }
            else -> marketData
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 24/7 Watcher Engine Status Card
        item {
            EngineStatusCard(
                serviceState = serviceState,
                onToggleService = onToggleService
            )
        }

        // 2. Buy & Sell Signal Opportunity Banner (if any pair is in oversold/overbought zone)
        item {
            val buyOpportunities = marketData.filter { it.rsi14 <= 35.0 || (it.rsi14 < 42.0 && it.macd > it.macdSignal) }
            val sellOpportunities = marketData.filter { it.rsi14 >= 65.0 || (it.rsi14 > 58.0 && it.macd < it.macdSignal) }

            if (buyOpportunities.isNotEmpty() || sellOpportunities.isNotEmpty()) {
                SignalsOpportunityBanner(
                    buyOpportunities = buyOpportunities,
                    sellOpportunities = sellOpportunities,
                    onQuickAddAlert = onQuickAddAlert
                )
            }
        }

        // 3. Category Filter Tabs & Add Custom Stock Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedCategory == "STOCKS",
                        onClick = { selectedCategory = "STOCKS" },
                        label = { Text("🔥 Low-Cost Stocks", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldAccent.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldAccent
                        )
                    )
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("All Assets", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurfaceCardLight,
                            selectedLabelColor = CyanAccent
                        )
                    )
                    FilterChip(
                        selected = selectedCategory == "CRYPTO",
                        onClick = { selectedCategory = "CRYPTO" },
                        label = { Text("Crypto", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurfaceCardLight,
                            selectedLabelColor = AmberAlert
                        )
                    )
                }

                IconButton(
                    onClick = { showAddStockDialog = true },
                    modifier = Modifier.testTag("btn_open_add_stock")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Stock", tint = CyanAccent)
                }
            }
        }

        // 4. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == "STOCKS") "Low-Cost Growth Stocks (${filteredList.size})" else "Active Tickers (${filteredList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Continuous 24/7 Scan",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 5. Market Pairs List
        items(filteredList, key = { it.symbol }) { pair ->
            StockPairCard(
                pair = pair,
                onSelectChart = { onSelectPairForChart(pair.symbol) },
                onAddBuy = { onQuickAddAlert(pair.symbol, TradeAction.BUY) },
                onAddSell = { onQuickAddAlert(pair.symbol, TradeAction.SELL) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showAddStockDialog) {
        AddCustomStockDialog(
            onDismiss = { showAddStockDialog = false },
            onAdd = { ticker, name, price ->
                onAddCustomStock(ticker, name, price)
                showAddStockDialog = false
            }
        )
    }
}

@Composable
private fun EngineStatusCard(
    serviceState: TradingWatcherService.Companion.ServiceState,
    onToggleService: () -> Unit
) {
    val isRunning = serviceState.isRunning

    // Infinite pulse animation for 24/7 live heartbeat
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isRunning) EmeraldAccent.copy(alpha = 0.5f) else CoralRed.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) EmeraldAccent.copy(alpha = pulseAlpha) else CoralRed
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "24/7 WATCHER ONLINE" else "WATCHER PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) EmeraldAccent else CoralRed,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onToggleService,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("toggle_service_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) CoralRed.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f),
                        contentColor = if (isRunning) CoralRed else EmeraldAccent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "Pause" else "Start 24/7",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats grid: Cycles, Active Rules, Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Cycles Run", value = "#${serviceState.cycleCount}")
                StatItem(label = "Stocks Monitored", value = "${serviceState.activePairsCount} Tickers")
                StatItem(label = "Active Rules", value = "${serviceState.activeRulesCount} Active")
                StatItem(label = "Feed Latency", value = "${serviceState.latencyMs}ms")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SignalsOpportunityBanner(
    buyOpportunities: List<MarketPair>,
    sellOpportunities: List<MarketPair>,
    onQuickAddAlert: (String, TradeAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardLight)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = AmberAlert,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "High-Profit Signal Opportunities",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberAlert
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            buyOpportunities.take(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🟢", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${pair.symbol} RSI ${String.format("%.0f", pair.rsi14)} (Value Dip Entry)",
                            fontSize = 12.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "+ BUY DIP ALERT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        modifier = Modifier
                            .clickable { onQuickAddAlert(pair.symbol, TradeAction.BUY) }
                            .padding(4.dp)
                    )
                }
            }

            sellOpportunities.take(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔴", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${pair.symbol} RSI ${String.format("%.0f", pair.rsi14)} (Take Profit Peak)",
                            fontSize = 12.sp,
                            color = CoralRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "+ TAKE PROFIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed,
                        modifier = Modifier
                            .clickable { onQuickAddAlert(pair.symbol, TradeAction.SELL) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockPairCard(
    pair: MarketPair,
    onSelectChart: () -> Unit,
    onAddBuy: () -> Unit,
    onAddSell: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectChart() }
            .testTag("pair_card_${pair.baseAsset}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Symbol, Volatility, Price, 24h Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pair.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (pair.volatilityRating == "ULTRA") AmberAlert.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${pair.volatilityRating} BETA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pair.volatilityRating == "ULTRA") AmberAlert else CyanAccent
                            )
                        }
                    }
                    Text(
                        text = "${pair.name} • Cap: ${pair.marketCapShort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = pair.formattedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    val isPositive = pair.change24h >= 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isPositive) CandleGreen.copy(alpha = 0.2f) else CandleRed.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", pair.change24h)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) CandleGreen else CandleRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profit Projection Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCardLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Dip Buy Target (-5%): ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = String.format("$%.2f", pair.targetDip5),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "+20% Profit Target: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = String.format("$%.2f", pair.targetGain20),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberAlert,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicators row: RSI, MACD, Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rsiColor = when {
                    pair.rsi14 <= 30.0 -> EmeraldAccent
                    pair.rsi14 >= 70.0 -> CoralRed
                    else -> TextSecondary
                }
                IndicatorPill(label = "RSI", value = String.format("%.1f", pair.rsi14), color = rsiColor)
                IndicatorPill(
                    label = "MACD",
                    value = if (pair.macd >= pair.macdSignal) "BULLISH" else "BEARISH",
                    color = if (pair.macd >= pair.macdSignal) EmeraldAccent else CoralRed
                )
                IndicatorPill(label = "24h Vol", value = String.format("%,.0fK", pair.volume24h / 1000.0), color = CyanAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: + Buy Dip Target, + Take Profit, Chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddBuy,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_buy_${pair.baseAsset}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldAccent.copy(alpha = 0.5f))),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🟢 + Buy Dip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddSell,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_sell_${pair.baseAsset}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CoralRed.copy(alpha = 0.5f))),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🔴 + Take Profit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSelectChart,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_chart_${pair.baseAsset}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardLight, contentColor = CyanAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = "Chart", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun IndicatorPill(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardLight)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AddCustomStockDialog(
    onDismiss: () -> Unit,
    onAdd: (ticker: String, name: String, price: Double) -> Unit
) {
    var tickerText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("15.00") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Track New Stock Ticker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Add any low-cost, high-profit stock to 24/7 background monitor",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tickerText,
                    onValueChange = { tickerText = it.uppercase() },
                    label = { Text("Stock Ticker (e.g. NVDA, TSLA, RKLB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Company Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Current Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (tickerText.isNotBlank()) {
                                onAdd(tickerText, nameText, priceText.toDoubleOrNull() ?: 15.0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = DarkNavyBg)
                    ) {
                        Text("Add to 24/7 Watcher", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
