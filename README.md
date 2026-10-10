# Anemona Clicker (`id.anemona.clicker`)

An Android auto-clicker that fires at a **wall-clock time you set** (not a countdown), using an
AccessibilityService for the taps and a system overlay for the controls.

## Features

- **Time-based start.** Set e.g. `14:30:00.000` and the clicker waits for the clock to reach it,
  not for N seconds to elapse.
- **NTP clock sync.** Optional SNTP query corrects for clock drift; the overlay shows the last
  measured network RTT.
- **Multiple touch points.** Add/remove points on the overlay; each is draggable and carries its
  own repeat count, tap interval, hold duration and post-tap delay (set from app defaults).
- **Floating panel.** Draggable anywhere.
  - *Minimized:* clock with milliseconds + network RTT.
  - *Expanded:* icon-only buttons — add point, remove point, start, stop.
- **Resizable overlay.** Panel scale 80%–200% in app settings.
- **MIUI X UI.** Built with the [miuix](https://github.com/miuix-kotlin-multiplatform/miuix)
  (HyperOS-style) Compose library.

## Building

No local build is required — CI is the build gate.

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

Requirements: JDK 17, Gradle 9.8.0 (wrapper included), Android SDK with `compileSdk 35`.

## GitHub Actions

`.github/workflows/build.yml` runs on push to `main`/`master`, on PRs, and on manual dispatch
with a **build type input** (`debug` or `release`).

- Builds the requested variant.
- On failure, attaches the **build log and all module reports** (lint, test results, heapdumps)
  as an artifact so the error is readable from the run page.
- The **release APK is published to GitHub Releases** (`softprops/action-gh-release`) with
  auto-generated notes.

## Permissions

| Permission | Why |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Dispatches the taps. Reads no screen content. |
| `SYSTEM_ALERT_WINDOW` | Draws the floating panel and point markers. |
| `INTERNET` | NTP queries for clock sync. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_ACCESSIBILITY` | Keeps the overlay alive while clicking. |
| `POST_NOTIFICATIONS` | The persistent "running" notification. |

## Project layout

```
app/src/main/java/id/anemona/clicker/
  ui/            MainActivity, miuix settings screen, time-of-day parsing
  clicker/       AccessibilityService + timing engine
  ntp/           SNTP client + ClockSync (offset/RTT)
  overlay/       OverlayService (window), OverlayContent (Compose panel), PointMarkers
  store/         SharedPreferences-backed settings + touch-point model
```
