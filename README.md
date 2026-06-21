# Forest Timer

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Weather](https://img.shields.io/badge/weather-Open--Meteo-00B4D8?style=flat-square)](https://open-meteo.com/)

**Turn focus into foliage.** Forest Timer takes the [Pomodoro technique](https://en.wikipedia.org/wiki/Pomodoro_Technique) and makes it grow — every session you stay locked in grows a tree, and every completed four-session cycle harvests it into your own procedurally-rendered garden. Built for **CP3406 / CP5307**, no excuses for half-finished focus sessions.

100% Kotlin. 100% Jetpack Compose. 100% hand-drawn `Canvas` art — not a single PNG in sight.

---

## Quickstart

1. Clone it, crack open **Android Studio**.
2. Let Gradle sync — versions are pinned in `gradle/libs.versions.toml`, no surprises.
3. Hit run. Emulator or real device, `minSdk = 24`, `targetSdk = 36`.
4. Want the sky to actually match the sky outside? Grant location access for live weather — see [Weather Integration](#5-weather-integration-real-time--manual).

---

## Architecture at a Glance

```
┌───────────────────────────────────────────────────────────────┐
│                          MainActivity                         │
└───────────────────────────────┬───────────────────────────────┘
                                │ hosts
                                ▼
┌───────────────────────────────────────────────────────────────┐
│               UtilityApp (Scaffold + bottom nav)              │
└─────────┬─────────────────────┬─────────────────────┬─────────┘
          │                     │                     │
          ▼                     ▼                     ▼
┌───────────────────┐  ┌────────────────┐  ┌────────────────────┐
│ ForestTimerScreen │  │  GardenScreen  │  │ SettingsFormScreen │
└─────────┬─────────┘  └────────┬───────┘  └──────────┬─────────┘
          │                     │                     │
          └──────────── observe StateFlow ────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                        ForestViewModel                        │ 
│                 state · timer logic · persist                 │ 
└───────┬─────────────────────┬─────────────────────────┬───────┘
        │                     │                         │           
        │                     │                         │
        ▼                     ▼                         ▼
┌───────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│    AppPrefs   │ │   WeatherRepository   │ │  LocationRepository   │
│ (SharedPrefs) │ │ (Retrofit/Open-Meteo) │ │ (FusedLocationClient) │
└───────────────┘ └───────────────────────┘ └───────────────────────┘


```

- **`ForestViewModel`** (`AndroidViewModel`) is the brain — every piece of state lives in a `MutableStateFlow`, exposed read-only. The UI just reacts via `collectAsState()`.
- **`AppPrefs`** wraps `SharedPreferences` with hand-rolled serialization. No Room, no Gson for local state — kept lean on purpose.
- **Repositories** keep the external world (network, GPS) firmly out of the ViewModel's hands. Weather and location are isolated, swappable, testable.

### Key Files

| File | Responsibility |
|------|----------------|
| `MainActivity.kt` | Entry point, theme wrapper, bottom-nav scaffold, save-on-stop |
| `ForestViewModel.kt` | Timer engine, Pomodoro cycle logic, stats, persistence, weather polling |
| `ForestScreens.kt` | The three screens: Focus, Garden, Settings |
| `TreePainter.kt` | Procedural `Canvas` rendering of trees + weather effects |
| `Models.kt` | Enums and data classes (`TreeSpecies`, `TreeStage`, `SessionType`, `AppSettings`, `GardenTree`, `WeatherCondition`) |
| `AppPrefs.kt` | `SharedPreferences` persistence + serialization |
| `ui/theme/Theme.kt` | Dynamic light/dark + accent-color theming |
| `network/`, `data/weather/`, `data/location/` | Open-Meteo API client and FusedLocation wrapper |
| `extensions/WeatherMapping.kt` | Maps WMO weather codes → `WeatherCondition` |

---

## Core Features & Implementation

### 1. Pomodoro Focus Timer

A countdown engine that cycles **Focus → Short Break → Focus → … → Long Break**, no babysitting required.

- **State** lives in `ForestViewModel`: `secondsRemaining`, `isTimerRunning`, `isTimerPaused`, `sessionType` (`FOCUS`, `SHORT_BREAK`, `LONG_BREAK`).
- **The tick loop** is a coroutine in `viewModelScope` that `delay(1000ms)`s its way down to zero, then fires `onSessionComplete()` (`startTimer()` in `ForestViewModel.kt`).
- **Controls** — Start, Pause/Resume, Skip, Cancel — wired straight into `ForestTimerScreen`. `cancelSession()` is the bail-out that doesn't grow your tree; `skipCurrentSession()` is the cheat code that completes it anyway.
- **Durations** are fully configurable and validated in Settings (focus sessions need ≥ 25 min to count toward tree growth — no gaming the system with 2-minute "focus").
- **Automation**: `autoStartBreak` / `autoStartFocus` toggles chain sessions back-to-back, hands-free.
- **Completion effects**: finish a session and get the default notification ringtone plus a one-shot vibration, each gated behind its own settings toggle.

### 2. Tree Growth & The 4-Session Cycle

The gamification engine room: your focus time literally grows a tree through five stages.

- A Pomodoro **cycle = 4 focus sessions**, tracked by `completedSessionsInCycle`.
- Each completed session bumps the tree through a `TreeStage` via `TreeStage.fromInt()`: `SEED → SPROUT → YOUNG_TREE → NEARLY_MATURE → MATURE`.
- Nail the 4th focus session (`onSessionComplete()` in `ForestViewModel.kt`) and the tree gets harvested into the garden as a `GardenTree` — species, completion date, total focus minutes, cycle index, all logged. Cycle resets, a fresh seed gets planted, and you've earned a **Long Break**. Anything short of that, it's just a **Short Break**.
- **Species selection** for each new seed is driven by `speciesMode` (`plantNewSeed()`):
  - `Random` — any of the five species, no bias.
  - `Manual` — you call the shots.
  - `Seasonal` — weighted by the current month using **Southern Hemisphere** seasons (`selectSeasonalSpecies()`) — Cherry Blossom dominates spring, Pine takes winter.

### 3. Procedural Tree & Weather Rendering

Every visual is drawn live on Compose `Canvas`. No PNGs, no vector assets, no cheating.

- **`TreeCanvas`** (`TreePainter.kt`) draws trunk, branches, and canopy uniquely for each of the five `TreeSpecies` across every `TreeStage`, plus a sky layer, ground/soil, and the empty-plot daisy.
- **Living animation**: `rememberInfiniteTransition` drives a gentle trunk **sway** — calm under sun, whipping wide and fast through Wind and Storm.
- **Weather overlays** ride the same canvas — a rayed sun for Sunny, drifting clouds for Cloudy/Storm, animated falling rain, and keyframed lightning for Storm.

### 4. Productivity Garden & Statistics

The Garden tab is half trophy case, half analytics dashboard.

- **`GardenScreen`** lays harvested trees out in a `LazyVerticalGrid` of plots. Tap a tree for the full story (species, date, minutes invested, cycle #); tap an empty plot for a nudge on how to earn one.
- **Stats**, computed live in `ForestViewModel`:
  - `totalMatureTreesCount` — your garden's bragging rights.
  - `totalFocusHours` — every focus minute, garden plus in-progress cycle.
  - `currentFocusStreak` — consecutive-day streak from completed-session dates (`calculateStreak()`).
  - `averageSessionsPerDay` — sessions ÷ days since your first session (`calculateAverageSessionsPerDay()`).
- **Daily goal**: a `LinearProgressIndicator` on the Focus screen tracks today's minutes against your configurable `dailyFocusGoalMinutes`.

### 5. Weather Integration (Real-time & Manual)

Weather drives the on-screen ambiance — set it manually, or let the sky decide.

- **Manual mode**: pick a `WeatherCondition` in Settings, applied instantly.
- **Real-time mode**: `ForestViewModel` spins up a polling coroutine that refreshes weather every **5 minutes** (`startWeatherPolling()`).
  1. `LocationRepository` grabs the current location via Play Services **FusedLocationProvider**.
  2. `WeatherRepository` hits the **Open-Meteo** `v1/forecast` endpoint through **Retrofit** (`OpenMeteoApi`), with a 5-minute in-memory cache guarded by a `Mutex`.
  3. The WMO `weathercode` maps to one of the app's five conditions via `mapWeatherCodeToCondition()`.
- **Permissions**: flipping to real-time triggers a runtime request for fine/coarse location (`SettingsFormScreen`). Denied? No drama — the app just holds the last known condition.
- Networking lives in `NetworkModule` (OkHttp logging interceptor, 15s timeouts, Gson converter).

### 6. Settings & Customization

`SettingsFormScreen` is one scrollable form. Change it, it applies — no Save button to forget.

- **Profile**: display name (shown as "Welcome back, …").
- **Appearance**: theme mode (System/Light/Dark) and six accent colors to pick from. `MainActivity` watches settings and re-wraps the UI in `CP3406_CP5603...Theme` with the chosen `darkTheme`/`accentColorName` (`Theme.kt`).
- **Timers**: focus / short break / long break durations with inline validation (focus ≥ 25, short break < long break).
- **Automation, sounds, vibration, daily goal, species mode, weather mode** — all covered above.
- Valid edits flow through `applySettingsIfValid()` → `viewModel.updateSettings()` → `AppPrefs.saveSettings()`.

### 7. Persistence & Background Resume

State survives restarts. Timers survive backgrounding. Nothing gets lost.

- **`AppPrefs`** persists settings, the garden (`species|date|minutes|cycle` rows joined by `;`), completed-session dates, cycle progress, active species, and the cycle counter — all in `SharedPreferences` with manual string serialization.
- **Timer resume**: background the app and `MainActivity.onStop()` saves a `SavedTimerState` (remaining seconds, an absolute target timestamp, session type, paused flag). Come back, and `restorePersistedTimer()` either:
  - restores a paused timer exactly as you left it, or
  - recomputes remaining time from the target timestamp and keeps ticking — and if that target already passed, the session completes as if it finished while you were gone.

---

## Tech Stack

- **Language**: Kotlin (2.0.21)
- **UI**: Jetpack Compose + Material 3 (Compose BOM)
- **Async**: Kotlin Coroutines + `StateFlow`
- **Architecture**: MVVM (`AndroidViewModel`)
- **Networking**: Retrofit + OkHttp + Gson — [Open-Meteo](https://open-meteo.com/) API
- **Location**: Google Play Services FusedLocationProvider
- **Persistence**: Android `SharedPreferences`
- **Graphics**: Compose `Canvas` (fully procedural, no image assets)
- **Build**: Gradle (Kotlin DSL) with a version catalog

---

## Permissions

| Permission | Why |
|------------|-----|
| `INTERNET` | Fetch live weather from Open-Meteo |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Locate the device for real-time weather |
| `VIBRATE` | Haptic feedback on session completion |

---

## License

Provided for educational use in CP3406 / CP5307. Feel free to modify and extend.
