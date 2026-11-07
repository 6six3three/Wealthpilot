# CHALORYX Market Lab 4.1 — Internet diagnostics and second exchange

Version 4.1 is a **paper-trading and market-research** Android application. It is not connected to a real-money brokerage and does not make guaranteed predictions.

## Mobile workflow
1. On the Home screen, tap **Check internet and market-data connections**. The connection doctor tests a lightweight Frankfurter FX response, Coinbase Exchange candles, and Kraken backup candles. It reports exact success/failure per service.
2. Tap **① Load available public data**. CHALORYX fetches historical FX reference data (Frankfurter), BTC/ETH/SOL daily OHLCV (Coinbase) and falls back to **Kraken** if Coinbase fails. Every series shows an observation date and original source.
3. Tap **② Inspect a signal** to see indicators, or **③ Practice virtually** for risk-free demonstration trades.
4. Save a workspace backup from Settings. Data remain available offline after import/fetch.

Important: FX quotes are *reference closing observations*; crypto candles exclude incomplete current UTC day and are not real-time executable prices. Stock and gold coverage require independently verified CSV or a legitimately obtained read-only historical market-data key. API availability, coverage and CORS restrictions can change.

## Technical and testing
- Unique application ID `com.chaloryx.marketlab.v41`: installs alongside older tested versions without overwriting localStorage.
- Historical data fetched manually only (no background subscription, promises, or push alerts).
- Kraken backup uses `https://api.kraken.com/0/public/OHLC?pair=XBTUSD|ETHUSD|SOLUSD&interval=1440`; verifies timestamp, OHLC ranges, positive close, minimum 60 completed bars.
- Connectivity doctor checks *response shape* without relying on invented data.
- Restrictive CSP includes only directly used data sources; removed keyless CryptoCompare flow because it may require an API key.
- Tested via Node JS syntax check and Chromium/Playwright browser checks at 360/390/768 px: 72 internal math diagnostics, mocked FX + Kraken provider histories, Coinbase failure/recovery and six-symbol guided refresh. Those tests verify logic and fallback, **not live data uptime, profitability or 100% accuracy**.

This build is a debug APK for user testing, not signed production-ready brokerage software. No real trades and no broker credentials.
