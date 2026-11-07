# Contributing to VELORA (*Beyond Playback.*)

Thank you for your interest in contributing to **VELORA**! We engineer VELORA to be a globally competitive, privacy-first multimedia platform with uncompromising code quality, acoustic precision, and visual elegance.

---

## 1. Core Engineering Principles

1. **Correctness Before Speed:** Every playback transition, DSP filter equation, and database migration must be deterministic and verified by automated tests.
2. **Clean Architecture Boundaries:**
   - `com.example.orchestrator`: `IMediaOrchestrator` contract and state models (`PlaybackState`, `MediaOrchestratorState`).
   - `com.example.domain`: Pure Kotlin business logic, DSP math (`TenBandBiquadAudioProcessor`), queue invariants (`PlaybackQueue`), and intelligence engines (`VeloraIntelligenceEngine`, `VeloraEcosystemEngines`).
   - `com.example.data`: Platform implementations (`Media3PlayerEngine`, `FazoraLibraryRepository` Room v3, `FazoraSettingsRepository` DataStore, `VeloraCloudRepository` Firestore).
   - `com.example.ui`: Jetpack Compose Material 3 screens (`Dark Saffron & Champagne Gold` theme) and `FazoraViewModel`.
3. **No Silent Failures:** Never swallow exceptions (`catch (_: Exception) {}`). Surface actionable state via `PlaybackStatus.ERROR` or `VeloraMediaEvent.ErrorRaised`.
4. **No Fake Functionality:** Every UI control must be wired to a real domain/data implementation and tagged with `Modifier.testTag("snake_case_tag")`.

---

## 2. Development Setup & Quality Gates

### Prerequisites
- **JDK:** 21 (Temurin recommended)
- **Android SDK:** `compileSdk = 36`, `minSdk = 26`
- **Build System:** Gradle 9.3.1 (Kotlin DSL)

### Mandatory Pre-PR Verification Commands
Before opening a Pull Request, ensure all three checks pass locally with zero warnings or errors:

```bash
# 1. Static Analysis (Detekt must report 0 issues)
./gradlew detekt

# 2. Fast JVM Unit Test Suite (DSP, Queue, OmniSearch, Copilot, Subtitle Export, Security)
./gradlew :app:testDebugUnitTest --tests com.example.ExampleUnitTest

# 3. Assemble Debug & Release APKs
./gradlew assembleDebug
```

---

## 3. Static Analysis Rules (`config/detekt/detekt.yml`)

- **Max Line Length:** `160` characters.
- **Function Thresholds:** Keep interfaces under `25` functions (`thresholdInInterfaces: 25`) and classes under `55` functions (`thresholdInClasses: 55`). Use focused helper files or extension functions for composite operations.
- **Naming Conventions:** Only `private` properties may use a leading underscore (`_backingState`). Internal or public properties must use `camelCase`.

---

## 4. Design System Guidelines (Dark Saffron & Champagne Gold)

- Use semantic tokens from `com.example.ui.theme.Color.kt` (`VeloraObsidian`, `VeloraSecondaryBg`, `VeloraElevated`, `VeloraCard`, `VeloraPrimarySaffron`, `VeloraChampagneGold`, `VeloraPrimaryText`).
- Maintain **WCAG 2.1 AA** contrast ($\ge 4.5:1$ for body text, $\ge 3.0:1$ for large text/icons).
- Ensure all interactive elements meet the **48.dp × 48.dp** minimum touch target requirement and include localized strings in both `res/values/strings.xml` (English) and `res/values-fa/strings.xml` (Persian).
