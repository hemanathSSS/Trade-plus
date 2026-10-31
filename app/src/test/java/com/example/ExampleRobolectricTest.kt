package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlertConditionType
import com.example.data.model.AlertRule
import com.example.data.model.AssetType
import com.example.data.model.MarketPair
import com.example.data.model.TradeAction
import com.example.engine.AlertEvaluationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TradePulse 24/7", appName)
    }

    @Test
    fun `alert engine triggers low-cost stock dip buy target`() {
        val pairs = listOf(
            MarketPair(
                symbol = "SOFI",
                baseAsset = "SOFI",
                quoteAsset = "USD",
                name = "SoFi Technologies",
                price = 9.20,
                change24h = -5.4,
                high24h = 9.90,
                low24h = 9.15,
                volume24h = 12000000.0,
                assetType = AssetType.STOCK,
                isLowCostHighProfit = true
            )
        )
        val rules = listOf(
            AlertRule(
                id = 1,
                symbol = "SOFI",
                conditionType = AlertConditionType.DIP_BUY_5_PERCENT,
                targetValue = 9.35,
                tradeAction = TradeAction.BUY
            )
        )

        val triggers = AlertEvaluationEngine.evaluateRules(pairs, rules)
        assertEquals(1, triggers.size)
        assertEquals(TradeAction.BUY, triggers[0].action)
        assertTrue(triggers[0].triggerMessage.contains("5% DIP ENTRY OPPORTUNITY"))
    }

    @Test
    fun `alert engine triggers high-profit milestone on stock rally`() {
        val pairs = listOf(
            MarketPair(
                symbol = "PLTR",
                baseAsset = "PLTR",
                quoteAsset = "USD",
                name = "Palantir Technologies",
                price = 52.00,
                change24h = 12.8,
                high24h = 52.50,
                low24h = 46.00,
                volume24h = 35000000.0,
                assetType = AssetType.STOCK,
                isLowCostHighProfit = true
            )
        )
        val rules = listOf(
            AlertRule(
                id = 2,
                symbol = "PLTR",
                conditionType = AlertConditionType.PROFIT_TARGET_20_PERCENT,
                targetValue = 51.36,
                tradeAction = TradeAction.SELL
            )
        )

        val triggers = AlertEvaluationEngine.evaluateRules(pairs, rules)
        assertEquals(1, triggers.size)
        assertEquals(TradeAction.SELL, triggers[0].action)
        assertTrue(triggers[0].triggerMessage.contains("+20% HIGH PROFIT TARGET HIT"))
    }
}
