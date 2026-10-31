package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.AlertRule
import com.example.data.model.TradeAction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SignalsAndAlertsScreen(
    rules: List<AlertRule>,
    onToggleRule: (AlertRule) -> Unit,
    onDeleteRule: (AlertRule) -> Unit,
    onTriggerTestAlert: (TradeAction) -> Unit,
    onCreateRuleClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<TradeAction?>(null) }

    val filteredRules = remember(rules, selectedFilter) {
        if (selectedFilter == null) rules else rules.filter { it.tradeAction == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Create Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Buy & Sell Rules (${rules.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "24/7 Evaluated in real-time background loop",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onCreateRuleClick,
                    modifier = Modifier.testTag("btn_create_new_alert"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = DarkNavyBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ New Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 2. Instant Notification Test Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test 24/7 Alert Delivery System",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Instantly verify heads-up notifications, custom sounds, haptics & Telegram/Discord dispatch:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onTriggerTestAlert(TradeAction.BUY) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_test_buy_alert"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent.copy(alpha = 0.2f), contentColor = EmeraldAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🟢 Test BUY Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onTriggerTestAlert(TradeAction.SELL) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_test_sell_alert"),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed.copy(alpha = 0.2f), contentColor = CoralRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔴 Test SELL Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Filter Chips: All, BUY, SELL, Custom
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${rules.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SurfaceCardLight,
                        selectedLabelColor = CyanAccent
                    )
                )
                FilterChip(
                    selected = selectedFilter == TradeAction.BUY,
                    onClick = { selectedFilter = TradeAction.BUY },
                    label = { Text("🟢 Buy Rules", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldAccent.copy(alpha = 0.25f),
                        selectedLabelColor = EmeraldAccent
                    )
                )
                FilterChip(
                    selected = selectedFilter == TradeAction.SELL,
                    onClick = { selectedFilter = TradeAction.SELL },
                    label = { Text("🔴 Sell Rules", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoralRed.copy(alpha = 0.25f),
                        selectedLabelColor = CoralRed
                    )
                )
            }
        }

        // 4. Alert Rules List
        if (filteredRules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active alert rules found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ New Alert' above to create 24/7 Buy or Sell triggers",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredRules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onToggle = { onToggleRule(rule) },
                    onDelete = { onDeleteRule(rule) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun RuleCard(
    rule: AlertRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val actionColor = when (rule.tradeAction) {
        TradeAction.BUY -> EmeraldAccent
        TradeAction.SELL -> CoralRed
        TradeAction.ALERT -> CyanAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("rule_card_${rule.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Action Badge, Symbol, Enabled Switch
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
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${rule.tradeAction.emoji} ${rule.tradeAction.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("toggle_rule_${rule.id}"),
                    colors = SwitchDefaults.colors(checkedThumbColor = actionColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Condition details
            Text(
                text = "${rule.conditionType.displayName}: ${rule.formattedThreshold}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (rule.notes.isNotBlank()) {
                Text(
                    text = rule.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: Cooldown, Trigger count, Channels & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cooldown: ${rule.cooldownMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text("•", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "Fired: ${rule.triggerCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (rule.triggerCount > 0) AmberAlert else TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rule.notifyAndroid) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = "Push", tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (rule.notifyTelegram) {
                        Icon(Icons.Default.Send, contentDescription = "Telegram", tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (rule.notifyDiscord) {
                        Icon(Icons.Default.Forum, contentDescription = "Discord", tint = Color(0xFF5865F2), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_rule_${rule.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
