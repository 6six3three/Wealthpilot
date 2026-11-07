# CHALORYX Market Lab 4.0 — Android private-test build

Package: `com.chaloryx.marketlab.v4`. Installs alongside 2.1 and 3.0, preserving their local workspaces. Export an existing workspace backup, then import into 4.0.

## Guided mobile workflow
1. **Load public data:** tap the first green button. Frankfurter v2 provides central-bank/reference daily FX histories for EUR/USD, GBP/USD and USD/ZAR; Coinbase Exchange provides completed historical daily OHLCV for BTC/USD, ETH/USD and SOL/USD. No forecasts or invented prices.
2. **Inspect a signal:** only recent public-source observations qualify for the guided shortcut. Dates, source and stale/synthetic status are always disclosed. BULLISH / BEARISH are transparent trend rule flags, not guaranteed market direction.
3. **Practice virtually:** cash-funded, fee-aware paper trading only. No real-money execution.

Advanced tabs support indicators, long-only deterministic backtesting, risk sizing, CSV import, forward signal audit and JSON backup/restore. Gold and stocks require authorized provider access or independently verified CSV. Manual data fetch only; no background polling.

## Tests
- 24 built-in deterministic known-value checks (RSI, SMA, EMA, MACD, Bollinger, ATR, validation and risk-related invariants).
- 243 independent browser QA assertions were completed before this build: 5 viewports, 160 indicator comparisons with independent Python implementations, CSV validation, strategy tests, paper trading, synthetic safeguards and mocked public provider payloads.
- JavaScript parsed successfully with Node; no uncaught browser errors observed during this test suite.
- The test suite validates calculation logic and app behavior with fixtures; it cannot prove predictive accuracy, that external feeds always work, or native Android compatibility on a specific device.

## Security and release
Embedded restrictive Content Security Policy; HTTPS allowlist for data fetches; no broker API keys or real-money orders.
Twelve Data **optional** read-only key remains an in-memory field: never enter a trading/exchange account password or secret.
Build with `.github/workflows/build-chaloryx-android.yml`. Output is a debug APK for private device review, not a Play Store release, audited broker system or certified financial adviser. 
