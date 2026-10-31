package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertLog
import com.example.data.model.AlertRule
import com.example.data.model.TradeAction
import com.example.data.model.WatcherSettings

class Converters {
    @TypeConverter
    fun fromConditionType(value: AlertConditionType): String {
        return value.name
    }

    @TypeConverter
    fun toConditionType(value: String): AlertConditionType {
        return try {
            AlertConditionType.valueOf(value)
        } catch (e: Exception) {
            AlertConditionType.BUY_TARGET
        }
    }

    @TypeConverter
    fun fromTradeAction(value: TradeAction): String {
        return value.name
    }

    @TypeConverter
    fun toTradeAction(value: String): TradeAction {
        return try {
            TradeAction.valueOf(value)
        } catch (e: Exception) {
            TradeAction.ALERT
        }
    }
}

@Database(
    entities = [AlertRule::class, AlertLog::class, WatcherSettings::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TradePulseDatabase : RoomDatabase() {

    abstract fun tradePulseDao(): TradePulseDao

    companion object {
        @Volatile
        private var INSTANCE: TradePulseDatabase? = null

        fun getInstance(context: Context): TradePulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TradePulseDatabase::class.java,
                    "tradepulse_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
