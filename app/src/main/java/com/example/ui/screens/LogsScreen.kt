package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertLog
import com.example.data.model.TradeAction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    logs: List<AlertLog>,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Alert Audit Logs (${logs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time records of all 24/7 triggered signals",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("btn_clear_logs")
                    ) {
                        Text("Clear All", color = CoralRed, fontSize = 13.sp)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No alerts have fired yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Signals detected by the 24/7 watcher will be logged here",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                LogItemCard(log = log, dateFormat = dateFormat)
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun LogItemCard(log: AlertLog, dateFormat: SimpleDateFormat) {
    val actionColor = when (log.tradeAction) {
        TradeAction.BUY -> EmeraldAccent
        TradeAction.SELL -> CoralRed
        TradeAction.ALERT -> CyanAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("log_item_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Action Badge, Symbol, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(actionColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${log.tradeAction.emoji} ${log.tradeAction.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.symbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trigger message & price
            Text(
                text = log.triggerMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Price at Trigger: ${log.formattedPrice}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (log.sentToAndroid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                            Text(" Push", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    if (log.sentToTelegram) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                            Text(" Telegram", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    if (log.sentToDiscord) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(14.dp))
                            Text(" Discord", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
