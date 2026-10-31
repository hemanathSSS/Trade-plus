package com.example.data.model

enum class AssetType {
    STOCK,
    CRYPTO
}

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
}

data class MarketPair(
    val symbol: String, // e.g. "SOFI", "PLTR", "BTC/USDT"
    val baseAsset: String, // e.g. "SOFI", "PLTR"
    val quoteAsset: String = "USD", // e.g. "USD", "USDT"
    val name: String, // e.g. "SoFi Technologies"
    val price: Double,
    val change24h: Double, // in percentage, e.g. +4.2%
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val assetType: AssetType = AssetType.STOCK,
    val isLowCostHighProfit: Boolean = true,
    val volatilityRating: String = "HIGH", // HIGH, ULTRA
    val marketCapShort: String = "$12.4B",
    val rsi14: Double = 50.0,
    val macd: Double = 0.0,
    val macdSignal: Double = 0.0,
    val sma20: Double = price,
    val sma50: Double = price,
    val recentCandles: List<CandleStick> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isBullish24h: Boolean get() = change24h >= 0
    val formattedPrice: String get() = when {
        price >= 1000.0 -> String.format("$%,.2f", price)
        price >= 1.0 -> String.format("$%.2f", price)
        price >= 0.01 -> String.format("$%.4f", price)
        else -> String.format("$%.6f", price)
    }

    // Quick profit target calculations for low-cost stocks
    val targetGain10: Double get() = price * 1.10
    val targetGain20: Double get() = price * 1.20
    val targetGain50: Double get() = price * 1.50
    val targetDip5: Double get() = price * 0.95
    val targetDip10: Double get() = price * 0.90
}
