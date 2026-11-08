# Contributing to VELORA (*Beyond Playback.*)

Thank you for your interest in contributing to **VELORA**! We engineer VELORA to be a globally competitive, privacy-first multimedia platform with uncompromising code quality, acoustic precision, and visual elegance.

---

## 1. Core Engineering Principles & Module Map

1. **Correctness Before Speed:** Every playback transition, DSP filter equation, and database migration must be deterministic and verified by automated tests.
2. **Clean Architecture Boundaries (`ui → orchestrator → domain → data`):**
   - `com.example.ui`:
     - `navigation/NavGraph.kt`: Type-safe destinations (`VeloraDestination`) and `VeloraNavGraph`.
     - `permissions/PermissionController.kt`: Centralized API 26–36 media & notification permission resolver.
     - `screens/`: Modularized Jetpack Compose screens (`VeloraShellScreen.kt`, `VideoPlayerScreen.kt`, `SearchScreen.kt`, `SettingsScreen.kt`, `LibraryAndMusicScreens.kt`, `PlayerAndEqScreen.kt`, `RadioAndDspComponents.kt`, `Phase2Components.kt`).
     - `viewmodel/`: `VeloraViewModel.kt` (<450 lines) and `VeloraUiStateAssembler.kt`.
     - `theme/`: Dark Saffron & Champagne Gold tokens (`Color.kt`), Material 3 dynamic palette engine (`Theme.kt`), and bundled Google Fonts (`Type.kt`).
   - `com.example.orchestrator`: `IMediaOrchestrator` contract and state models (`PlaybackState`, `MediaOrchestratorState`).
   - `com.example.domain`: Pure Kotlin business logic, DSP math (`EqBandMath`), queue invariants (`PlaybackQueue`), gesture physics (`VeloraGestureEngine`), and intelligence engines (`VeloraIntelligenceEngine`, `VeloraEcosystemEngines`, `VeloraPhase2Suite`).
   - `com.example.data`: Platform implementations (`Media3PlayerEngine`, `TenBandBiquadAudioProcessor`, `VeloraLibraryRepository` Room v3, `VeloraSettingsRepository` DataStore, `VeloraFirebaseRepository` Firestore).
3. **No Silent Failures:** Never swallow exceptions (`catch (_: Exception) {}`). Surface actionable state via `PlaybackStatus.ERROR` or `VeloraMediaEvent.ErrorRaised`.
4. **No Fake Functionality:** Every UI control must be wired to a real domain/data implementation and tagged with `Modifier.testTag("snake_case_tag")`.

---

## 2. Development Setup, Testing Strategy & Quality Gates

### Prerequisites
- **JDK:** 21 (Temurin recommended)
- **Android SDK:** `compileSdk = 36` (`minorApiLevel = 1`), `minSdk = 26`, `targetSdk = 36`
- **Build System:** Gradle 9.3.1 (Kotlin DSL)
- **Version Source of Truth:** Root `VERSION` file (`2.3.0`) + `gradle.properties` (`velora.versionCode=6`).

### Automated Test Suites
- `DspBiquadMathTest`: Property-based verification of all 10 ISO biquad bands (`31.25 Hz`–`16 kHz`), $Q = \sqrt{2}$, `0 dB` unity pass-through, and Schur-Cohn pole stability across sample rates `44.1 kHz`–`192 kHz`.
- `DownloadValidatorSecurityTest`: Stream URL sanitization against IPv4/IPv6 loopback (`127.0.0.1`, `[::1]`, `fc00::/7`, `fe80::/10`), RFC-1918 private networks, IDN homograph spoofing, `.m3u8`/`.mpd` manifest poisoning, and Tier-1 plugin sandbox capabilities.
- `ExampleUnitTest`: Queue invariants, OmniSearch Levenshtein fuzzy ranking, Subtitle SRT/WebVTT export, Media Copilot, Gesture Engine, and Backup/Restore JSON/M3U serialization.
- `ExampleRobolectricTest`: Android JVM Robolectric tests for Room v3 persistence, DataStore `.corrupt.bak` forensic recovery, and `MediaOrchestrator` state transitions.

### Mandatory Pre-PR Verification Commands
Before opening a Pull Request, ensure all checks pass locally with zero warnings or errors:

```bash
# 1. Static Analysis (Detekt must report 0 issues)
./gradlew detekt

# 2. Fast JVM Unit Test Suites (DSP, Security, Queue, OmniSearch, Copilot, Subtitle Export)
./gradlew :app:testDebugUnitTest --tests com.example.ExampleUnitTest --tests com.example.DspBiquadMathTest --tests com.example.DownloadValidatorSecurityTest

# 3. Assemble Debug & R8-Minified Release APKs
./gradlew assembleDebug assembleRelease
```

---

## 3. Ratcheting Static Analysis Rules (`config/detekt/detekt.yml`)

- **Max Issues:** `0` (`maxIssues: 0`).
- **Max Line Length:** `160` characters.
- **Large Class Threshold:** `500` lines (`LargeClass: 500`).
- **Function Thresholds:** Keep files under `35` functions (`thresholdInFiles: 35`), interfaces under `25` functions (`thresholdInInterfaces: 25`), and classes under `52` functions (`thresholdInClasses: 52`).
- **Android Lint:** `abortOnError = true` in `app/build.gradle.kts`.

---

## 4. Design System & i18n Guidelines (Dark Saffron & Champagne Gold)

- Use semantic tokens from `com.example.ui.theme.Color.kt` (`VeloraObsidian`, `VeloraSecondaryBg`, `VeloraElevated`, `VeloraCard`, `VeloraPrimarySaffron`, `VeloraChampagneGold`, `VeloraPrimaryText`).
- Maintain **WCAG 2.1 AA** contrast ($\ge 4.5:1$ for body text, $\ge 3.0:1$ for large text/icons).
- Ensure all interactive elements meet the **48.dp × 48.dp** minimum touch target requirement and include localized strings in both `res/values/strings.xml` (English) and `res/values-fa/strings.xml` (Persian).
