package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WatcherSettings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: WatcherSettings,
    onSaveSettings: (
        pollInterval: Int,
        telegramToken: String,
        telegramChatId: String,
        discordWebhook: String,
        deadManUrl: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        autoBuySellEnabled: Boolean
    ) -> Unit,
    onTestTelegram: (String, String) -> Unit,
    onTestDiscord: (String) -> Unit
) {
    var pollInterval by remember(settings) { mutableIntStateOf(settings.pollIntervalSeconds) }
    var telegramToken by remember(settings) { mutableStateOf(settings.telegramBotToken) }
    var telegramChatId by remember(settings) { mutableStateOf(settings.telegramChatId) }
    var discordWebhook by remember(settings) { mutableStateOf(settings.discordWebhookUrl) }
    var deadManUrl by remember(settings) { mutableStateOf(settings.deadManSwitchUrl) }
    var soundEnabled by remember(settings) { mutableStateOf(settings.isSoundEnabled) }
    var vibrationEnabled by remember(settings) { mutableStateOf(settings.isVibrationEnabled) }
    var autoBuySellEnabled by remember(settings) { mutableStateOf(settings.autoBuySellStrategyEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "24/7 Engine & Webhooks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Configure automated alert dispatchers and background loop",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Section 1: 24/7 Watcher Background Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background Watcher Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tick Scan Frequency",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10, 30).forEach { sec ->
                        val isSel = pollInterval == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyanAccent.copy(alpha = 0.25f) else SurfaceCardLight)
                                .clickable { pollInterval = sec }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s",
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) CyanAccent else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Buy/Sell AI Strategy detection toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Buy/Sell Strategy Scan", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("24/7 auto-detection of RSI < 30 Oversold Buy and RSI > 70 Sell zones across all pairs", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoBuySellEnabled,
                        onCheckedChange = { autoBuySellEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Audio & Vibration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alert Sound Chime", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Vibration Pattern", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldAccent)
                    )
                }
            }
        }

        // Section 2: Telegram Bot Integration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Telegram Instant Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                }
                Text(
                    text = "Receive instant push messages on your phone or PC via your Telegram Bot",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { telegramToken = it },
                    label = { Text("Telegram Bot Token (from @BotFather)") },
                    placeholder = { Text("e.g. 123456789:ABCdefGhIJKlmNoPQR") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_telegram_token"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = { telegramChatId = it },
                    label = { Text("Your Chat ID (or Channel ID)") },
                    placeholder = { Text("e.g. 987654321") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_telegram_chat_id"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onTestTelegram(telegramToken, telegramChatId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_test_telegram"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldAccent.copy(alpha = 0.5f)))
                ) {
                    Text("Send Test Telegram Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 3: Discord Webhook Integration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Discord Channel Webhook",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5865F2)
                    )
                }
                Text(
                    text = "Post styled embeds into your Discord trading channels automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = discordWebhook,
                    onValueChange = { discordWebhook = it },
                    label = { Text("Discord Webhook URL") },
                    placeholder = { Text("https://discord.com/api/webhooks/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_discord_webhook"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5865F2),
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onTestDiscord(discordWebhook) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("btn_test_discord"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5865F2)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5865F2).copy(alpha = 0.5f)))
                ) {
                    Text("Send Test Discord Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 4: Dead-Man's Switch / Uptime URL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "24/7 Dead-Man's Switch (Healthcheck)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberAlert
                    )
                }
                Text(
                    text = "TradePulse pings this URL every 10 cycles. If your device ever shuts down, Healthchecks.io or UptimeRobot will notify you immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = deadManUrl,
                    onValueChange = { deadManUrl = it },
                    label = { Text("Healthcheck Ping URL (Optional)") },
                    placeholder = { Text("https://hc-ping.com/your-uuid-here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dead_man_url"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAlert,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                onSaveSettings(
                    pollInterval,
                    telegramToken,
                    telegramChatId,
                    discordWebhook,
                    deadManUrl,
                    soundEnabled,
                    vibrationEnabled,
                    autoBuySellEnabled
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_settings_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = DarkNavyBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save All Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
