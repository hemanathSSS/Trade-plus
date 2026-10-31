# Low-Cost, High-Profit Stock 24/7 Watcher & Push Notifications

An automated background monitoring service and real-time push notification system designed for low-cost, high-profit growth stocks (such as high-volatility momentum equities, breakout candidates, and growth leaders like SOFI, PLTR, HOOD, MARA, CLOV, and tech runners). The system continuously evaluates technical thresholds in the background and delivers instant heads-up Android notifications, custom sounds, and optional webhook dispatches when buy entry or sell take-profit targets are triggered.

## User Review & Critical Decisions

> [!IMPORTANT]
> The monitoring engine is tailored for **low-cost, high-profit stocks**—equities priced under $50 with high beta, significant trading volume, and large percentage profit potential.

- **Confirmed Stock Focus**: Low-cost, high-upside growth stocks (e.g., SOFI, PLTR, MARA, HOOD, NIO, RIVN, INTC, CLOV) with full support for user-added custom stock tickers.
- **Notification Protocol**: Android Foreground Service with continuous background tick evaluation, delivering high-priority Heads-Up system notifications with distinct audible sound chimes and haptic vibrations.
- **Profit Strategy Modes**:
  1. **Low-Cost Dip Entry (BUY)**: Alerts when a target stock pulls back into key oversold support (e.g., RSI < 32 or 5–10% dip from 24h highs).
  2. **High-Profit Target (SELL / Take-Profit)**: Alerts when the stock hits predetermined profit milestones (+10%, +20%, +50%) or enters overbought exhaustion (RSI > 70 with MACD divergence).
  3. **Stop-Loss Protection**: Alerts immediately if price breaches safety floors to preserve capital.

---

## 1. Overview & Core Concept

### What It Does
The system transforms the Android device into a 24/7 autonomous trading sentinel. Even when the phone is locked or the app is closed, a persistent Android Foreground Service continuously monitors live stock quotes, evaluates technical indicators, and triggers instant alerts the exact second a buy dip or profit target is hit.

### Target Audience & Persona
Retail traders, swing traders, and active investors focusing on high-beta, low-dollar stocks who cannot stare at charts 24/7 but need immediate alerts to seize fast intraday or multi-day profit swings.

### Key Value
- **Zero Missed Entries/Exits**: Uninterrupted background operation with wake-lock support.
- **Built-in Profit Multiplier Presets**: Quick 1-tap alerts configured for +10%, +20%, and +50% profit targets.
- **Complete Transparency**: Full local audit history of all triggered buy and sell signals with timestamped prices.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LOW-COST HIGH-PROFIT STOCKS                     │
├────────────────────────────────────────────────────────────────────────┤
│ 1. WATCHLIST                                                           │
│    • Live feed of low-cost stocks ($5 - $45 range)                     │
│    • Real-time price flashes, 24h % change, Volume Surge badge         │
│    • 1-Tap Quick Action: [🟢 + Buy Dip Target]  [🔴 + Take Profit]      │
│                                                                        │
│ 2. CREATE ALERT MODAL                                                  │
│    • Preset buttons: "Quick 10% Profit Target", "Oversold Bounce Buy", │
│      "Breakout Surge", "Trailing Stop"                                 │
│    • Automatic price target math (e.g. Current $18.40 -> +15% = $21.16)│
│    • Cooldown debounce setting (5m, 15m, 30m, 1h)                      │
│                                                                        │
│ 3. 24/7 BACKGROUND TRIGGER                                             │
│    • Foreground Service detects threshold match                        │
│    • Android Heads-Up Notification pops up with custom chime & haptic  │
│    • Webhook dispatch to Telegram & Discord (optional)                 │
│                                                                        │
│ 4. AUDIT & PROFIT TRACKER                                              │
│    • In-app log records entry/exit prices, time, and potential return  │
└────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Theme**: Bloomberg terminal dark aesthetic with vibrant financial neon accents.
- **Background**: Deep Midnight (`#080D1A`) with elevated Slate cards (`#0F172A`).
- **Signal Accents**:
  - `🟢 Emerald Neon (#00FFA3)`: Buy entry opportunities, bullish crossovers, positive profit margins.
  - `🔴 Coral Crimson (#FF5252)`: Take-profit exits, overbought warnings, stop-loss protection.
  - `⚡ Electric Cyan (#00E5FF)`: Price trends, volume spikes, and technical indicator lines.
  - `🟡 Amber Gold (#FFD600)`: High-priority urgency badges and alert notifications.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Android Foreground Service vs. WorkManager Periodic Work**
  - *Chosen Approach*: Android Foreground Service with persistent status bar notification and WakeLock.
  - *Why*: WorkManager has a minimum 15-minute polling interval, which is far too slow for volatile high-profit stocks where 5% moves happen in minutes. A Foreground Service runs continuously at 3s–10s intervals for real-time responsiveness.
  - *Trade-Off*: Requires a subtle, low-importance status notification in the notification drawer while running.

- **Decision 2: Stock Market Data Feed & Resilience**
  - *Chosen Approach*: Multi-tier live stock quote feed integrating Yahoo Finance / Finnhub public stock APIs with high-speed ticker fallback and simulated volatility generator for off-market hours or offline states.
  - *Why*: Guarantees the user can test, simulate, and receive alerts 24/7, even during weekend and overnight trading sessions.

- **Decision 3: Room Database for Rules and Signal History**
  - *Chosen Approach*: On-device SQLite with Android Room.
  - *Why*: Instant local access, offline capability, zero cloud server subscription fees, and complete data privacy for the user's trading rules.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                             ANDROID SYSTEM                                 │
│                                                                            │
│  ┌─────────────────────────────────┐   ┌────────────────────────────────┐  │
│  │   UI Layer (Jetpack Compose)    │   │  TradingWatcherService (24/7)  │  │
│  │ • WatchlistScreen (Low-Cost)    │   │ • Foreground Service           │  │
│  │ • SignalsAndAlertsScreen        │◄──┼─• Background Ticker Loop (3-10s│  │
│  │ • TechnicalChartScreen          │   │ • WakeLock & Heartbeat Monitor │  │
│  │ • LogHistoryScreen              │   └──────────────┬─────────────────┘  │
│  │ • SettingsScreen                │                  │                    │
│  └────────────────┬────────────────┘                  ▼                    │
│                   │                    ┌────────────────────────────────┐  │
│                   │                    │    AlertEvaluationEngine       │  │
│                   │                    │ • BUY_TARGET (Price ≤ Threshold│  │
│                   ▼                    │ • SELL_TAKE_PROFIT (≥ Target)  │  │
│  ┌─────────────────────────────────┐   │ • AUTO_BUY_STRATEGY (RSI < 30) │  │
│  │       MainViewModel             │   │ • AUTO_SELL_STRATEGY (RSI > 70)│  │
│  │ • StateFlows (Market, Rules)    │   └──────────────┬─────────────────┘  │
│  │ • Alert Creation & Test Action  │                  │                    │
│  └────────────────┬────────────────┘                  ▼                    │
│                   │                    ┌────────────────────────────────┐  │
│                   ▼                    │     Multi-Channel Dispatch     │  │
│  ┌─────────────────────────────────┐   │ • Android NotificationManager  │  │
│  │    Room Database (Local Cache)  │   │   (Heads-up, sound & haptic)   │  │
│  │ • alert_rules (Buy & Sell rules)│◄──┼─• Room AlertLogEntity          │  │
│  │ • alert_logs (History & PnL)    │   │ • Telegram Bot API             │  │
│  │ • watcher_settings              │   │ • Discord Channel Webhook      │  │
│  └─────────────────────────────────┘   └────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Stock Tickers Included in Low-Cost High-Profit Watchlist
1. **SOFI** (SoFi Technologies) — High volume, low-dollar fintech ($7 - $15 range).
2. **PLTR** (Palantir Technologies) — High growth, massive retail momentum ($20 - $45 range).
3. **HOOD** (Robinhood Markets) — High beta crypto & equity broker ($15 - $30 range).
4. **MARA** (MARA Holdings) — Ultra-volatile high-profit swings ($14 - $28 range).
5. **NIO** (NIO Inc.) — Low dollar EV volatility runner ($4 - $8 range).
6. **RIVN** (Rivian Automotive) — High beta growth stock ($10 - $18 range).
7. **CLOV** (Clover Health) — Low-cost value breakout candidate ($2 - $5 range).
8. **INTC** (Intel Corp) — Turnaround swing trade ($20 - $35 range).

---

## 5. Implementation Phases & Deliverables

1. **Stock Market Data Layer**: Expand `MarketApiService` to incorporate low-cost, high-profit US stock symbols with real-time volatility, OHLC candle bars, and volume calculations.
2. **High-Profit Alert Strategy Engine**: Add automatic profit target calculators (+10%, +20%, +50% gain triggers) and dip-buy support checks to `AlertEvaluationEngine`.
3. **Enhanced Watchlist & Quick Actions**: Add a dedicated "Low-Cost High-Profit Stocks" category toggle in `WatchlistScreen` with 1-tap "Buy Dip" and "Take Profit" modals.
4. **Interactive Charting**: Support candlestick charts and RSI/MACD technical indicators for all low-cost stock tickers.
5. **Heads-Up Notifications & Webhook Test**: Verify continuous background service execution, Android notification sound/vibration delivery, and test alert buttons.
