package com.example.data.network

import com.example.data.model.AssetType
import com.example.data.model.CandleStick
import com.example.data.model.MarketPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object MarketApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    data class AssetInfo(
        val symbol: String, // e.g. "PLTR", "SOFI"
        val name: String,
        val assetType: AssetType,
        val isLowCost: Boolean,
        val volatility: String,
        val marketCap: String,
        var basePrice: Double,
        val binanceSymbol: String? = null
    )

    // Curated high-profit, low-cost growth stocks & crypto assets
    private val DEFAULT_ASSETS = mutableListOf(
        AssetInfo("SOFI", "SoFi Technologies", AssetType.STOCK, true, "HIGH", "$10.2B", 9.85),
        AssetInfo("PLTR", "Palantir Technologies", AssetType.STOCK, true, "ULTRA", "$92.4B", 42.80),
        AssetInfo("HOOD", "Robinhood Markets", AssetType.STOCK, true, "HIGH", "$21.5B", 24.35),
        AssetInfo("MARA", "MARA Holdings", AssetType.STOCK, true, "ULTRA", "$5.1B", 18.60),
        AssetInfo("NIO", "NIO Inc.", AssetType.STOCK, true, "ULTRA", "$11.2B", 5.42),
        AssetInfo("RIVN", "Rivian Automotive", AssetType.STOCK, true, "HIGH", "$10.8B", 10.95),
        AssetInfo("CLOV", "Clover Health", AssetType.STOCK, true, "ULTRA", "$1.7B", 3.28),
        AssetInfo("INTC", "Intel Corporation", AssetType.STOCK, true, "HIGH", "$96.1B", 22.40),
        AssetInfo("AMC", "AMC Entertainment", AssetType.STOCK, true, "ULTRA", "$1.4B", 4.35),
        AssetInfo("GME", "GameStop Corp.", AssetType.STOCK, true, "ULTRA", "$9.6B", 22.50),
        // Major Crypto for multi-asset flexibility
        AssetInfo("BTC/USDT", "Bitcoin", AssetType.CRYPTO, false, "HIGH", "$1.3T", 68500.0, "BTCUSDT"),
        AssetInfo("ETH/USDT", "Ethereum", AssetType.CRYPTO, false, "HIGH", "$420B", 3490.0, "ETHUSDT"),
        AssetInfo("SOL/USDT", "Solana", AssetType.CRYPTO, false, "ULTRA", "$85B", 182.5, "SOLUSDT")
    )

    // Dynamic user-added stocks
    private val customStocks = mutableListOf<AssetInfo>()

    // In-memory cache of market pairs
    private val marketCache = mutableMapOf<String, MarketPair>()

    fun addCustomStock(ticker: String, name: String = "", initialPrice: Double = 15.0): MarketPair {
        val cleanTicker = ticker.trim().uppercase()
        val existing = DEFAULT_ASSETS.find { it.symbol == cleanTicker } ?: customStocks.find { it.symbol == cleanTicker }
        if (existing != null) {
            return marketCache[cleanTicker] ?: createInitialPair(existing)
        }

        val newAsset = AssetInfo(
            symbol = cleanTicker,
            name = if (name.isNotBlank()) name else "$cleanTicker Stock",
            assetType = AssetType.STOCK,
            isLowCost = initialPrice <= 50.0,
            volatility = "HIGH",
            marketCap = "$5.0B",
            basePrice = initialPrice
        )
        customStocks.add(newAsset)
        val pair = createInitialPair(newAsset)
        marketCache[cleanTicker] = pair
        return pair
    }

    private fun createInitialPair(info: AssetInfo): MarketPair {
        val candles = generateMockCandles(info.basePrice)
        val (rsi, macd, signal, sma20, sma50) = computeIndicators(candles)
        return MarketPair(
            symbol = info.symbol,
            baseAsset = info.symbol.replace("/USDT", ""),
            quoteAsset = if (info.assetType == AssetType.CRYPTO) "USDT" else "USD",
            name = info.name,
            price = info.basePrice,
            change24h = Random.nextDouble(-2.5, 4.5),
            high24h = info.basePrice * 1.05,
            low24h = info.basePrice * 0.95,
            volume24h = Random.nextDouble(500000.0, 5000000.0),
            assetType = info.assetType,
            isLowCostHighProfit = info.isLowCost,
            volatilityRating = info.volatility,
            marketCapShort = info.marketCap,
            rsi14 = rsi,
            macd = macd,
            macdSignal = signal,
            sma20 = sma20,
            sma50 = sma50,
            recentCandles = candles,
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun fetchMarketData(): List<MarketPair> = withContext(Dispatchers.IO) {
        val allAssets = DEFAULT_ASSETS + customStocks
        val pairs = mutableListOf<MarketPair>()

        // Try live Binance for crypto tickers if online
        val binancePrices = mutableMapOf<String, JSONObject>()
        try {
            val url = "https://api.binance.com/api/v3/ticker/24hr"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val jsonArray = JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        binancePrices[obj.getString("symbol")] = obj
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        for (info in allAssets) {
            val cached = marketCache[info.symbol]
            var currentPrice = cached?.price ?: info.basePrice
            var change24h = cached?.change24h ?: Random.nextDouble(-3.0, 5.0)
            var vol = cached?.volume24h ?: Random.nextDouble(1000000.0, 10000000.0)

            // If Binance quote is available for this crypto asset
            if (info.binanceSymbol != null && binancePrices.containsKey(info.binanceSymbol)) {
                val obj = binancePrices[info.binanceSymbol]
                if (obj != null) {
                    currentPrice = obj.getString("lastPrice").toDoubleOrNull() ?: currentPrice
                    change24h = obj.getString("priceChangePercent").toDoubleOrNull() ?: change24h
                    vol = obj.getString("volume").toDoubleOrNull() ?: vol
                }
            } else {
                // Stock simulation: realistic high-beta stock intraday fluctuation
                val deltaPercent = when (info.volatility) {
                    "ULTRA" -> Random.nextDouble(-0.007, 0.0075)
                    else -> Random.nextDouble(-0.004, 0.0042)
                }
                currentPrice = (currentPrice * (1.0 + deltaPercent)).coerceAtLeast(0.50)
                change24h += Random.nextDouble(-0.06, 0.07)
                vol += Random.nextDouble(1000.0, 20000.0)
            }

            // Update candle tracking
            val candles = cached?.recentCandles?.takeIf { it.isNotEmpty() }
                ?: generateMockCandles(currentPrice)

            val updatedCandles = candles.toMutableList()
            if (updatedCandles.isNotEmpty()) {
                val last = updatedCandles.last()
                updatedCandles[updatedCandles.size - 1] = last.copy(
                    close = currentPrice,
                    high = maxOf(last.high, currentPrice),
                    low = minOf(last.low, currentPrice)
                )
            }

            val (rsi, macd, signal, sma20, sma50) = computeIndicators(updatedCandles)

            val pair = MarketPair(
                symbol = info.symbol,
                baseAsset = info.symbol.replace("/USDT", ""),
                quoteAsset = if (info.assetType == AssetType.CRYPTO) "USDT" else "USD",
                name = info.name,
                price = currentPrice,
                change24h = change24h,
                high24h = maxOf(cached?.high24h ?: (currentPrice * 1.04), currentPrice),
                low24h = minOf(cached?.low24h ?: (currentPrice * 0.96), currentPrice),
                volume24h = vol,
                assetType = info.assetType,
                isLowCostHighProfit = info.isLowCost,
                volatilityRating = info.volatility,
                marketCapShort = info.marketCap,
                rsi14 = rsi,
                macd = macd,
                macdSignal = signal,
                sma20 = sma20,
                sma50 = sma50,
                recentCandles = updatedCandles,
                lastUpdated = System.currentTimeMillis()
            )

            pairs.add(pair)
            marketCache[info.symbol] = pair
        }

        pairs
    }

    private fun generateMockCandles(currentPrice: Double, count: Int = 30): List<CandleStick> {
        val candles = mutableListOf<CandleStick>()
        var price = currentPrice * 0.92
        val now = System.currentTimeMillis()
        val stepMs = 3600 * 1000L // 1-hour candles

        for (i in 0 until count) {
            val open = price
            val change = (Random.nextDouble(-0.025, 0.027)) * open
            val close = open + change
            val high = maxOf(open, close) + Random.nextDouble(0.002, 0.015) * open
            val low = minOf(open, close) - Random.nextDouble(0.002, 0.015) * open
            val vol = Random.nextDouble(50000.0, 500000.0)
            candles.add(CandleStick(now - (count - i) * stepMs, open, high, low, close, vol))
            price = close
        }
        return candles
    }

    private fun computeIndicators(candles: List<CandleStick>): IndicatorResult {
        if (candles.size < 15) {
            val p = candles.lastOrNull()?.close ?: 10.0
            return IndicatorResult(50.0, 0.0, 0.0, p, p)
        }

        val closes = candles.map { it.close }
        val rsi = computeRSI(closes, 14)
        val (macd, signal) = computeMACD(closes)
        val sma20 = if (closes.size >= 20) closes.takeLast(20).average() else closes.average()
        val sma50 = if (closes.size >= 30) closes.takeLast(30).average() else closes.average()

        return IndicatorResult(rsi, macd, signal, sma20, sma50)
    }

    private fun computeRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size <= period) return 50.0
        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val diff = prices[i] - prices[i - 1]
            if (diff >= 0) gains += diff else losses -= diff
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until prices.size) {
            val diff = prices[i] - prices[i - 1]
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    private fun computeMACD(prices: List<Double>): Pair<Double, Double> {
        val ema12 = computeEMA(prices, 12)
        val ema26 = computeEMA(prices, 26)
        val macd = ema12 - ema26
        val signal = macd * 0.82
        return Pair(macd, signal)
    }

    private fun computeEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var ema = prices.first()
        for (i in 1 until prices.size) {
            ema = prices[i] * k + ema * (1 - k)
        }
        return ema
    }

    data class IndicatorResult(
        val rsi: Double,
        val macd: Double,
        val signal: Double,
        val sma20: Double,
        val sma50: Double
    )
}
