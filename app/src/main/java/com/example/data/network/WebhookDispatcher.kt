package com.example.data.network

import android.util.Log
import com.example.data.model.TradeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebhookDispatcher {

    private const val TAG = "WebhookDispatcher"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendTelegramAlert(
        token: String,
        chatId: String,
        symbol: String,
        action: TradeAction,
        message: String,
        price: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram token or Chat ID is blank"))
        }

        try {
            val formattedPrice = if (price >= 1000) String.format("$%,.2f", price) else String.format("$%.4f", price)
            val header = when (action) {
                TradeAction.BUY -> "🟢 *[TRADE PULSE - BUY SIGNAL]*"
                TradeAction.SELL -> "🔴 *[TRADE PULSE - SELL SIGNAL]*"
                TradeAction.ALERT -> "⚡ *[TRADE PULSE - PRICE ALERT]*"
            }

            val text = """
                $header
                *Pair:* `$symbol`
                *Price:* `$formattedPrice`
                *Trigger:* $message
                *Time:* `${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}`
                
                _24/7 TradePulse Automated Engine_
            """.trimIndent()

            val json = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("text", text)
                put("parse_mode", "Markdown")
            }

            val url = "https://api.telegram.org/bot${token.trim()}/sendMessage"
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Telegram alert delivered successfully")
            } else {
                Log.e(TAG, "Telegram error: $body")
                Result.failure(Exception("Telegram error ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Telegram alert", e)
            Result.failure(e)
        }
    }

    suspend fun sendDiscordAlert(
        webhookUrl: String,
        symbol: String,
        action: TradeAction,
        message: String,
        price: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Discord webhook URL is blank"))
        }

        try {
            val formattedPrice = if (price >= 1000) String.format("$%,.2f", price) else String.format("$%.4f", price)
            val color = when (action) {
                TradeAction.BUY -> 0x00FF88 // Neon Green
                TradeAction.SELL -> 0xFF4444 // Red
                TradeAction.ALERT -> 0x00E5FF // Cyan
            }

            val embed = JSONObject().apply {
                put("title", "${action.emoji} ${action.label}: $symbol")
                put("description", message)
                put("color", color)
                val fields = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "Market Price")
                        put("value", formattedPrice)
                        put("inline", true)
                    })
                    put(JSONObject().apply {
                        put("name", "Symbol")
                        put("value", symbol)
                        put("inline", true)
                    })
                }
                put("fields", fields)
                put("footer", JSONObject().apply {
                    put("text", "TradePulse 24/7 Monitoring Engine")
                })
            }

            val payload = JSONObject().apply {
                put("content", "**TradePulse 24/7 Alert Triggered**")
                put("embeds", JSONArray().apply { put(embed) })
            }

            val request = Request.Builder()
                .url(webhookUrl.trim())
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 204) {
                Result.success("Discord alert sent successfully")
            } else {
                val errorBody = response.body?.string() ?: ""
                Result.failure(Exception("Discord error ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Discord alert", e)
            Result.failure(e)
        }
    }

    suspend fun pingDeadManSwitch(url: String) = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext
        try {
            val request = Request.Builder().url(url.trim()).build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Healthcheck ping failed: ${e.message}")
        }
    }
}
