package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.local.TradePulseDatabase

class TradePulseApp : Application() {

    lateinit var database: TradePulseDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = TradePulseDatabase.getInstance(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: 24/7 Foreground Watcher Status Channel (Low importance, persistent)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "24/7 Watcher Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and heartbeat of the 24/7 trading watcher"
                setShowBadge(false)
            }

            // Channel 2: Urgent Trading Alerts Channel (High importance, heads-up with sound/vibration)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Trading Signal & Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent price thresholds, RSI, and MACD technical indicator triggers"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250, 150, 400)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "tradepulse_watcher_service"
        const val CHANNEL_ALERTS_ID = "tradepulse_trading_alerts"

        lateinit var instance: TradePulseApp
            private set
    }
}
