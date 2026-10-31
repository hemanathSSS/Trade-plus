package com.example.data.local

import androidx.room.*
import com.example.data.model.AlertLog
import com.example.data.model.AlertRule
import com.example.data.model.WatcherSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface TradePulseDao {

    // Alert Rules
    @Query("SELECT * FROM alert_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<AlertRule>>

    @Query("SELECT * FROM alert_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<AlertRule>

    @Query("SELECT * FROM alert_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): AlertRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRule): Long

    @Update
    suspend fun updateRule(rule: AlertRule)

    @Delete
    suspend fun deleteRule(rule: AlertRule)

    @Query("UPDATE alert_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE alert_rules SET lastTriggeredTime = :time, triggerCount = triggerCount + 1 WHERE id = :id")
    suspend fun updateRuleTriggered(id: Long, time: Long)

    // Alert Logs
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AlertLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlertLog): Long

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM alert_logs")
    fun getLogCount(): Flow<Int>

    // Watcher Settings
    @Query("SELECT * FROM watcher_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<WatcherSettings?>

    @Query("SELECT * FROM watcher_settings WHERE id = 1")
    suspend fun getSettings(): WatcherSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: WatcherSettings)

    @Query("UPDATE watcher_settings SET totalCyclesRun = totalCyclesRun + 1, lastHeartbeatTimestamp = :time WHERE id = 1")
    suspend fun incrementCycle(time: Long)

    @Query("UPDATE watcher_settings SET totalAlertsFired = totalAlertsFired + 1 WHERE id = 1")
    suspend fun incrementAlertsFired()
}
