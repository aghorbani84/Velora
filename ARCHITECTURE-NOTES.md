# VELORA — Architecture Notes (Phase 0 Baseline & Multiplatform Target Map)

**Repository:** `aghorbani84/Velora`  
**Base Commit:** `8d13bfb5e4466023615aa7f1c2568daacfd52efb`  
**Version:** `2.5.3` (`VERSION`)  

---

## 1. Explicit Platform Reality Declaration (Today vs. Target)

> **CRITICAL REALITY DECLARATION:**  
> Today, the repository is a **single-module Android-only application (`:app`)** built with:
> - **Kotlin:** `2.2.10` (via AGP `9.1.1` built-in Kotlin support)
> - **KSP:** `2.3.5`
> - **Jetpack Compose BOM:** `2024.09.00` (`androidx.compose.material3`, `androidx.navigation:navigation-compose:2.8.0`)
> - **Playback & DSP:** `androidx.media3:media3-exoplayer:1.5.1`, `media3-session:1.5.1`, `media3-ui:1.5.1`, `media3-exoplayer-hls:1.5.1`, `media3-exoplayer-dash:1.5.1`, plus a custom 64-bit IEEE-754 Direct-Form II Transposed 10-band Biquad & Goertzel spectrum `AudioProcessor` (`TenBandBiquadAudioProcessor.kt`).
> - **Local Persistence:** `androidx.room:room-runtime:2.7.0` (`VeloraDatabase` v4) & `androidx.datastore:datastore-preferences:1.1.1`.
> - **Cloud Sync & Identity:** Firebase BOM `34.11.0` (`firebase-firestore`, `firebase-auth`) + Jetpack `CredentialManager` (`1.5.0`).
> - **DI:** Koin `4.0.2` (`io.insert-koin:koin-android`, `koin-androidx-compose`).
> - **Image Loading:** Coil `2.7.0` (`io.coil-kt:coil-compose`).

There are currently **no** `commonMain`, `desktopMain`, `iosMain`, or `wasmJsMain` Gradle source sets in `settings.gradle.kts` (`include(":app")`). Every reference to multiplatform in this document and in `MIGRATION-NOTES.md` separates **what exists today in `:app`** from **the target Kotlin Multiplatform (`commonMain` / `androidMain` / `desktopMain`) boundary architecture**.

---

## 2. Module & Package Map (`:app/src/main/java/com/example/`)

```text
:app (com.example / applicationId: com.aistudio.fazora.audiosync)
├── MainActivity.kt                        # Edge-to-edge host, VIEW/SEND intent routing, Koin entry
├── VeloraApplication.kt                   # Application bootstrap & Koin startKoin(veloraAppModule)
├── di/
│   └── VeloraModule.kt                    # Koin module: Room DB (with MIGRATION_1_2..3_4), DataStore, Scanner, Media3, ViewModel
├── domain/                                # Pure Kotlin domain models, DSP math, & engine contracts (KMP-ready)
│   ├── engine/
│   │   ├── PlayerEngine.kt                # Platform-agnostic playback & 10-band EQ interface
│   │   └── MediaOrchestrator.kt           # Session restoration, queue coordination, history & EQ sync
│   └── model/
│       ├── Track.kt                       # Immutable Track, MediaType, PlaybackQueue (O(1) id lookup)
│       ├── EqModels.kt                    # 10-band ISO frequencies, RBJ cookbook biquad & Goertzel math
│       ├── PlaylistAndSettings.kt         # Playlist, AppSettings, ThemePaletteMode, WCAG/Oklab/APCA & folder allowlist
│       ├── FullscreenPolicy.kt            # Pure rules for landscape lock & Simple/Studio control visibility
│       ├── VeloraIntelligenceEngine.kt    # Lyrics LRC/SRT parser, Copilot, TimeMachine, URI/SSRF validator
│       ├── VeloraEcosystemEngines.kt      # HLS/DASH manifest validator, M3U8 parser, Tier-1 plugin registry
│       └── VeloraRadioAndDspSuite.kt      # Radio catalog, Studio DSP state, symmetric JSON backup/restore
├── orchestrator/
│   ├── IMediaOrchestrator.kt              # Pure Kotlin orchestrator contract (zero Android/Media3 types)
│   └── PlaybackState.kt                   # Immutable PlaybackState & OrchestratorState
├── data/                                  # Android platform implementations (target: androidMain)
│   ├── audio/
│   │   └── VeloraAudioDeviceManager.kt    # AudioManager route detection (Bluetooth, Wired, USB DAC, Speaker)
│   ├── dsp/
│   │   └── TenBandBiquadAudioProcessor.kt # Media3 AudioProcessor: 64-bit Biquad cascade + Goertzel analyzer
│   ├── firebase/
│   │   └── VeloraFirebaseRepository.kt    # Google Sign-In (CredentialManager) + Firestore cloud sync
│   ├── local/
│   │   ├── VeloraDatabase.kt              # Room DB v4 + TrackDao, PlaylistDao, EqPresetDao, PlaybackHistoryDao
│   │   └── VeloraEntities.kt              # Room entities & domain mappers
│   ├── playback/
│   │   ├── Media3PlayerEngine.kt          # ExoPlayer wrapper, URL validator gate, audio focus & noisy receiver
│   │   └── VeloraPlaybackService.kt       # MediaSessionService with URI validation on onAddMediaItems
│   ├── repository/
│   │   └── VeloraLibraryRepository.kt     # Single source of truth combining Room DAOs & MediaLibraryScanner
│   ├── scanner/
│   │   ├── MediaLibraryScanner.kt         # MediaStore + SAF DocumentFile + local folder incremental scanner
│   │   └── BundledMediaSynthesizer.kt     # Opt-in reference WAV synthesizer & demo sample generator
│   └── settings/
│       └── VeloraSettingsRepository.kt    # DataStore Preferences + timestamped corruption backup handler
└── ui/                                    # Compose UI layer (target: commonMain UI + androidMain adapters)
    ├── components/
    │   ├── SharedComponents.kt            # MiniPlayerBar, TrackRowItem, BiquadFrequencyResponseCanvas
    │   └── VeloraVideoSurface.kt          # Platform video surface boundary encapsulating Media3 PlayerView
    ├── navigation/
    │   └── NavGraph.kt                    # 8-route Compose NavHost (Library, Music, Radio, Artists, Video, Search, Player, Settings)
    ├── permissions/
    │   └── PermissionController.kt        # API-level aware media permission helper (API 33+ granular vs legacy)
    ├── screens/
    │   ├── VeloraShellScreen.kt           # Adaptive NavigationBar / NavigationRail shell + RTL CompositionLocal
    │   ├── LibraryScreen.kt               # Folder allowlist manager, playlists, M3U8 import/export, history
    │   ├── MusicScreen.kt                 # Audio library browser & quick shuffle/play actions
    │   ├── RadioScreen.kt                 # Live radio tuner + custom stream URL validator
    │   ├── ArtistScreen.kt                # Two-pane / list-detail artist discography browser
    │   ├── VideoPlayerScreen.kt           # Cinema player, fullscreen dialog, gesture HUD, subtitle overlay
    │   ├── SearchScreen.kt                # Debounced OmniSearch + offline Media Copilot command bar
    │   ├── PlayerAndEqScreen.kt           # Full player, 10-band EQ sliders, Biquad curve, LRC sync studio
    │   └── SettingsScreen.kt              # Audio route, battery saver, themes (Saffron/Daylight/OLED/Dynamic), backup
    ├── theme/
    │   ├── Color.kt                       # Obsidian, Saffron, Champagne Gold & Fibonacci/Phi tokens
    │   ├── Theme.kt                       # Dark Saffron, Daylight Light, OLED High-Contrast & Material You schemes
    │   └── Type.kt                        # Bundled PlusJakartaSans, SpaceGrotesk & JetBrainsMono typography
    └── viewmodel/
        ├── VeloraViewModel.kt             # StateFlow combine pipeline & user intent handlers
        ├── VeloraUiState.kt               # Immutable UI state snapshot
        ├── VeloraUiStateAssembler.kt      # Memoized library/artist/smart-playlist projection assembler
        └── VeloraRadioAndDspExtensions.kt # ViewModel extensions for DSP, radio, LRC & JSON backup
```

---

## 3. Target Multiplatform Boundary (`commonMain` vs. `androidMain` vs. `desktopMain`)

| Subsystem / Responsibility | `commonMain` (Shared Pure Kotlin + Compose UI) | `androidMain` (Android Platform Actuals) | `desktopMain` (JVM Desktop Platform Actuals) |
| :--- | :--- | :--- | :--- |
| **Domain Models & Math** | `Track`, `PlaybackQueue`, `EqPreset`, `EqSpecification` (RBJ Biquad & Goertzel math), `VeloraDownloadValidator`, `VeloraGestureEngine`, `FullscreenPolicy`, `VeloraBackupRestoreEngine` | None (100% shared in `commonMain`) | None (100% shared in `commonMain`) |
| **Orchestration** | `IMediaOrchestrator`, `MediaOrchestrator`, `PlaybackState`, `OrchestratorState` | None (100% shared in `commonMain`) | None (100% shared in `commonMain`) |
| **Playback & 10-Band EQ Engine** | `expect interface PlayerEngine` | `Media3PlayerEngine` (`ExoPlayer` + `TenBandBiquadAudioProcessor` + `MediaSessionService`) | `DesktopVlcjPlayerEngine` (LibVLC / VLCJ native audio/video + PCM callback through shared `EqSpecification` 64-bit Biquad cascade) |
| **Video Surface Rendering** | `@Composable expect fun VeloraVideoSurface(...)` | `AndroidView` wrapping `androidx.media3.ui.PlayerView` (`VeloraVideoSurface.kt`) | Compose Desktop `Canvas` / `SwingPanel` backed by VLCJ `CallbackMediaPlayerComponent` DirectBuffer |
| **Local Database & Settings** | Room KMP `VeloraDatabase` (`androidx.room` KMP 2.7+) + `DataStore<Preferences>` | Android `Context` database file builder (`context.getDatabasePath("velora_media.db")`) | JVM user data directory builder (`~/.local/share/velora/velora_media.db`) |
| **Media Scanner** | `expect class MediaLibraryScanner` | `MediaStore` + SAF `DocumentFile` + `MediaMetadataRetriever` | `java.nio.file.Files.walk` + JAudioTagger / FFprobe metadata extractor |
| **Hardware Audio Route** | `expect class AudioRouteMonitor` | `VeloraAudioDeviceManager` (`AudioManager.getDevices`) | Java Sound `AudioSystem.getMixerInfo()` / PipeWire / CoreAudio device enumerator |

---

## 4. Discrepancies & Environment Constraints

1. **Prompt Reference Files Missing from Repo Root:**
   - The prompt references `RE-REPORT.md`, `kotlin-libraries-2026.md`, and `FIX-PROMPT.md` as external files alongside the prompt. Inspection of the repository root confirms those three files were not committed to git; however, their full 54-item specification (`F1`–`F23`, `N1`–`N22`, `B1`–`B10`) and library catalog were embedded directly in the user prompt history. All 54 items have been cross-checked against actual `file:line` locations in the repository and resolved in `FIX-LOG.md`.
2. **Library Version Catalog vs. Container Toolchain:**
   - The prompt's Phase 3 catalog lists future 2026 versions (`Kotlin 2.4.21`, `Compose Multiplatform 1.12.2`, `Coil 3.6.3`, `Vico 3.3.1`, `Koin 4.2.2`).
   - In the actual repository (`gradle/libs.versions.toml`), the project is locked to the verified compatible toolchain: **AGP `9.1.1`**, **Kotlin `2.2.10`**, **KSP `2.3.5`**, **Compose BOM `2024.09.00`**, **Room `2.7.0`**, **Media3 `1.5.1`**, **Coil `2.7.0`**, and **Koin `4.0.2`**. Upgrading Kotlin/AGP plugins in the build container breaks pre-cached Gradle dependencies and KSP alignment; therefore, the existing verified toolchain is preserved for compilation while the UI architecture and platform boundaries (`VeloraVideoSurface`, `IMediaOrchestrator`, `VeloraTheme`) are refactored to be multiplatform-ready.
3. **Headless Build Container vs. Physical Device / ADB:**
   - The execution environment is a headless Linux container without an attached physical Android device, hardware audio output, or `adb` daemon. All automated JVM, Robolectric (`VeloraPlaybackService`, Room migrations, `MediaOrchestrator`, `Media3PlayerEngine`, `VeloraLibraryRepository`), and security tests run and pass locally (`gradle detekt :app:testDebugUnitTest` + `compile_applet`), while scenarios requiring physical hardware (Bluetooth A2DP unplug, lock-screen hardware buttons, interactive PiP window dragging, OS phone call interruption) are explicitly documented in `QA-REPORT.md` under **"مواردی که نتوانستم تست کنم و چرا"**.
