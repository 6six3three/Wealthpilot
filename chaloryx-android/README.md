# CHALORYX Market Lab 3.0 — Android private-test build

The Android package `com.chaloryx.marketlab.v3` installs *alongside* the verified 2.1 app, so the original debug build and its local data are not replaced. Back up the workspace in 2.1 and import the JSON backup into 3.0.

## Verified functionality to check
- Offline charts, technical indicators, deterministic backtests, risk sizing and local paper trading.
- Public historical data fetch: Frankfurter ECB daily FX reference rates (EUR/USD, GBP/USD, USD/ZAR) and Coinbase Exchange public daily OHLCV candles for BTC, ETH, SOL. Manual internet fetch only; APIs and Android network behavior are subject to real-device confirmation.
- Optional Twelve Data with your *own read-only key* for stocks/metals/other symbols, or manual OHLC CSV import; do not enter broker credentials or trading secrets.
- Prospective **forward signal audit** with immutable recorded decision metadata, 5/10 completed-bar horizon, outcome ledger, CSV export and baseline comparison.
- Historical retrospective rule audit for qualitative context; overlapping samples and tiny sample sizes are explicitly marked.
- CSV and JSON backups stored under Android Downloads/CHALORYX; the Android runtime has an Android-only export bridge.

## Build and privacy
GitHub Actions: `.github/workflows/build-chaloryx-android.yml`. Output is a debug APK, NOT a production/Play Store release.
No brokerage account connection, real-money execution, push alert service or background scheduler. Refresh only when app is open and user taps Fetch.
Avoid proprietary real-time quote claims. Reference exchange rates and exchange history are not guaranteed actionable broker bid/ask quotes.
