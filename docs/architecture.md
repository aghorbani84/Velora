# VELORA System Architecture & Layer Contracts

**VELORA (*Beyond Playback*)** is an Android-first multimedia platform (`minSdk 26`, `targetSdk 36`) engineered around explicit unidirectional state flow and clean domain boundaries:

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ 1. PRESENTATION LAYER (com.example.ui)                                     │
│  • MainActivity (42 lines) -> VeloraTheme -> VeloraShellScreen             │
│  • Navigation: NavGraph.kt (VeloraDestination + VeloraNavGraph)            │
│  • Screens: VideoPlayerScreen, SearchScreen, SettingsScreen,               │
│             LibraryAndMusicScreens, PlayerAndEqScreen, RadioAndDsp         │
│  • State Holder: VeloraViewModel + VeloraUiStateAssembler (StateFlow)      │
└────────────────────────────────────┬───────────────────────────────────────┘
                                     │ Unidirectional Intents & StateFlow
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ 2. ORCHESTRATION LAYER (com.example.orchestrator & domain.engine)          │
│  • IMediaOrchestrator -> MediaOrchestrator                                 │
│  • Coordinates PlayerEngine, Room v3 History, DataStore Session Checkpoints│
│  • Emits StateFlow<PlaybackState>, StateFlow<MediaOrchestratorState>,      │
│    and lifecycle-safe SharedFlow<VeloraMediaEvent>                         │
└───────────────┬────────────────────────────┬───────────────────────────────┘
                │                            │
                ▼                            ▼
┌───────────────────────────────────────┐  ┌─────────────────────────────────┐
│ 3. DOMAIN LAYER (com.example.domain)  │  │ 4. DATA LAYER (com.example.data)│
│  • Zero Android framework imports     │  │  • Media3PlayerEngine +         │
│  • EqBandMath (10-band RBJ Biquad)    │  │    VeloraPlaybackService        │
│  • PlaybackQueue (Shuffle/Repeat)     │  │  • TenBandBiquadAudioProcessor  │
│  • VeloraOmniSearchEngine (Fuzzy)     │  │    (Direct Form II + Goertzel)  │
│  • VeloraSmartPlaylistEngine          │  │  • VeloraDatabase (Room v3)     │
│  • VeloraDownloadValidator (Sanitizer)│  │  • VeloraSettingsRepository     │
│  • VeloraGestureEngine (Double-tap/Sw)│  │    (DataStore + .corrupt.bak)   │
│  • VeloraPluginRegistry (Sandbox)     │  │  • VeloraFirebaseRepository     │
└───────────────────────────────────────┘  └─────────────────────────────────┘
```

---

## Layer Dependency Rules

1. **`domain` (`com.example.domain.*`):**
   - Pure Kotlin only. No `android.*` or `androidx.*` imports.
   - Contains deterministic mathematical and algorithmic engines (`EqBandMath`, `PlaybackQueue`, `VeloraOmniSearchEngine`, `VeloraSmartPlaylistEngine`, `VeloraLyricsAndSubtitleEngine`, `VeloraSubtitleExporter`, `VeloraDownloadValidator`, `VeloraGestureEngine`, `VeloraPluginRegistry`).
2. **`orchestrator` (`com.example.orchestrator.*`):**
   - Defines `IMediaOrchestrator` and `PlaybackState`, bridging UI intents to `PlayerEngine`, `VeloraLibraryRepository`, and `VeloraSettingsRepository`.
3. **`data` (`com.example.data.*`):**
   - Implements AndroidX Media3 playback (`Media3PlayerEngine`, `VeloraPlaybackService`), real-time PCM biquad filtering (`TenBandBiquadAudioProcessor`), Room SQLite v3 persistence (`VeloraDatabase`, `VeloraLibraryRepository`), atomic DataStore settings (`VeloraSettingsRepository`), and Google Sign-In / Firestore sync (`VeloraFirebaseRepository`).
4. **`ui` (`com.example.ui.*`):**
   - Stateless Jetpack Compose Material 3 screens consuming `StateFlow<VeloraUiState>` and dispatching user intents to `VeloraViewModel`.

---

## Architectural Decision Records (ADRs)

- **[ADR-0001](decisions/0001-desktop-cmp-libvlc-adr.md):** Compose Multiplatform Desktop Engine Evaluation (`libVLC` / `vlcj` vs `libmpv`).
- **[ADR-0002](decisions/0002-application-identity-and-namespace-retention-adr.md):** Application Identity (`applicationId`) & Namespace Retention Strategy for Zero-Data-Loss APK Upgrades and Firebase Compatibility.
