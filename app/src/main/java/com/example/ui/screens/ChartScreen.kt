package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleStick
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChartScreen(
    marketData: List<MarketPair>,
    selectedSymbol: String,
    timeframe: String,
    onSelectSymbol: (String) -> Unit,
    onSelectTimeframe: (String) -> Unit,
    onAddAlertClick: (String, TradeAction) -> Unit
) {
    val currentPair = marketData.find { it.symbol == selectedSymbol } ?: marketData.firstOrNull()
    val candles = currentPair?.recentCandles ?: emptyList()
    var selectedCandleIndex by remember(candles) { mutableStateOf<Int?>(null) }

    val activeCandle = selectedCandleIndex?.let { idx ->
        if (idx in candles.indices) candles[idx] else null
    } ?: candles.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Symbol Selector Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(marketData) { pair ->
                val isSelected = pair.symbol == selectedSymbol
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent.copy(alpha = 0.25f) else SurfaceCard)
                        .clickable { onSelectSymbol(pair.symbol) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("chart_chip_${pair.baseAsset}")
                ) {
                    Text(
                        text = pair.baseAsset,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyanAccent else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Pair Title, Price, Timeframe selector
        if (currentPair != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${currentPair.symbol} • ${currentPair.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentPair.formattedPrice,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isPos = currentPair.change24h >= 0
                        Text(
                            text = "${if (isPos) "+" else ""}${String.format("%.2f", currentPair.change24h)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPos) CandleGreen else CandleRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Quick Add Alert button
                Button(
                    onClick = { onAddAlertClick(currentPair.symbol, TradeAction.BUY) },
                    modifier = Modifier.testTag("chart_set_alert_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = DarkNavyBg),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeframe toggles: 1m, 5m, 15m, 1h, 1D
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("1m", "5m", "15m", "1h", "1D").forEach { tf ->
                    val isSel = timeframe == tf
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) SurfaceCardLight else SurfaceCard)
                            .clickable { onSelectTimeframe(tf) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) CyanAccent else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Candle Inspector Bar (Open, High, Low, Close)
            if (activeCandle != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CandleMetric("O", String.format("%.2f", activeCandle.open))
                    CandleMetric("H", String.format("%.2f", activeCandle.high))
                    CandleMetric("L", String.format("%.2f", activeCandle.low))
                    CandleMetric("C", String.format("%.2f", activeCandle.close), if (activeCandle.isBullish) CandleGreen else CandleRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Candlestick Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                CandlestickChartCanvas(
                    candles = candles,
                    onCandleSelected = { selectedCandleIndex = it }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. RSI Technical Indicator Panel
            Text(
                text = "RSI (14 Period): ${String.format("%.1f", currentPair.rsi14)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    currentPair.rsi14 <= 30 -> EmeraldAccent
                    currentPair.rsi14 >= 70 -> CoralRed
                    else -> CyanAccent
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                RsiChartCanvas(candles = candles, rsiValue = currentPair.rsi14)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. MACD Indicator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MACD (12, 26, 9) Signal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MACD: ${String.format("%.2f", currentPair.macd)}", color = CyanAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("Signal: ${String.format("%.2f", currentPair.macdSignal)}", color = AmberAlert, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        val isBullish = currentPair.macd >= currentPair.macdSignal
                        Text(
                            text = if (isBullish) "🟢 Bullish Momentum" else "🔴 Bearish Momentum",
                            color = if (isBullish) EmeraldAccent else CoralRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun CandleMetric(label: String, value: String, color: Color = TextPrimary) {
    Row {
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

@Composable
private fun CandlestickChartCanvas(
    candles: List<CandleStick>,
    onCandleSelected: (Int?) -> Unit
) {
    if (candles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading chart candles...", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    val minPrice = candles.minOfOrNull { it.low } ?: 0.0
    val maxPrice = candles.maxOfOrNull { it.high } ?: 1.0
    val priceRange = max(1.0, maxPrice - minPrice)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(candles) {
                detectTapGestures(
                    onPress = { offset ->
                        val candleWidth = size.width / candles.size
                        val index = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                        onCandleSelected(index)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val candleCount = candles.size
        val candleSlotWidth = width / candleCount
        val candleBodyWidth = max(2f, candleSlotWidth * 0.65f)

        // Draw horizontal price grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = SurfaceBorder.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw Candles
        candles.forEachIndexed { index, candle ->
            val centerX = index * candleSlotWidth + candleSlotWidth / 2f

            val highY = (height - ((candle.high - minPrice) / priceRange * height)).toFloat()
            val lowY = (height - ((candle.low - minPrice) / priceRange * height)).toFloat()
            val openY = (height - ((candle.open - minPrice) / priceRange * height)).toFloat()
            val closeY = (height - ((candle.close - minPrice) / priceRange * height)).toFloat()

            val candleColor = if (candle.isBullish) CandleGreen else CandleRed

            // Draw wick line
            drawLine(
                color = candleColor,
                start = Offset(centerX, highY),
                end = Offset(centerX, lowY),
                strokeWidth = 2f
            )

            // Draw candle body rectangle
            val topBodyY = min(openY, closeY)
            val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))

            drawRect(
                color = candleColor,
                topLeft = Offset(centerX - candleBodyWidth / 2f, topBodyY),
                size = Size(candleBodyWidth, bodyHeight)
            )
        }

        // Draw current price line
        val lastPrice = candles.last().close
        val lastPriceY = (height - ((lastPrice - minPrice) / priceRange * height)).toFloat()
        drawLine(
            color = CyanAccent,
            start = Offset(0f, lastPriceY),
            end = Offset(width, lastPriceY),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}

@Composable
private fun RsiChartCanvas(candles: List<CandleStick>, rsiValue: Double) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Overbought line (70)
        val y70 = height * (1f - 0.7f)
        drawLine(
            color = CoralRed.copy(alpha = 0.5f),
            start = Offset(0f, y70),
            end = Offset(width, y70),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Oversold line (30)
        val y30 = height * (1f - 0.3f)
        drawLine(
            color = EmeraldAccent.copy(alpha = 0.5f),
            start = Offset(0f, y30),
            end = Offset(width, y30),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Mid line (50)
        val y50 = height * 0.5f
        drawLine(
            color = SurfaceBorder,
            start = Offset(0f, y50),
            end = Offset(width, y50),
            strokeWidth = 0.8f
        )

        // Draw simulated RSI curve based on candle progression
        if (candles.size > 2) {
            val path = Path()
            val step = width / (candles.size - 1)
            var currentRsi = 50.0

            candles.forEachIndexed { i, candle ->
                val delta = (candle.close - candle.open) / candle.open * 200.0
                currentRsi = (currentRsi + delta).coerceIn(15.0, 85.0)
                if (i == candles.size - 1) {
                    currentRsi = rsiValue.coerceIn(5.0, 95.0)
                }

                val x = i * step
                val y = (height * (1.0 - (currentRsi / 100.0))).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = CyanAccent,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
