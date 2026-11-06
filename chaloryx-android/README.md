# CHALORYX Market Lab — Android application

This is an installable Android WebView application project built from the verified CHALORYX Market Lab 2.0 offline-first HTML interface. Package ID: `com.chaloryx.marketlab`.

## Build
GitHub Actions workflow: `.github/workflows/build-chaloryx-android.yml` (at repository root).
Debug APK output: `chaloryx-android/app/build/outputs/apk/debug/app-debug.apk`.

## Features
- Market research, imported OHLC data, technical indicators, backtests and paper-trading portfolio.
- CSV/JSON imports through Android's system file picker.
- Native CSV/JSON exports to Downloads/CHALORYX.
- Custom app icon, local workspace storage and optional HTTP(S) market-data fetches.

## Limits
No live order execution, broker login, or production account integrations. This is a debug APK for private testing, not a signed Play Store release. Do not enter broker passwords or secret keys.
Export a backup from the earlier browser version and import it into the APK; separate app origins do not share localStorage.
