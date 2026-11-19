# VELORA Hybrid UI (Stages 2–5) — Hostile Integration Review & Verification Ledger

- **Scope:** Stage 2 Tokens/Theme (`Obsidian Saffron`, `Daylight Ivory`, `High-Contrast OLED`), Stage 3 Mobile Hybrid (`5-tab` consolidated `STANDARD/PRO`, strict `4-tab` `SIMPLE`), Stage 4 Desktop/Wide Shell (`VeloraDesktopSidebar`, `DesktopPlayerBar`, `veloraKeyboardShortcuts`), Stage 5 Audit & Integration Scripts.
- **Ground Rule:** Every claim below separates `[VERIFIED]` (executed locally via Gradle/Detekt/Lint/Robolectric) from `[UNVERIFIED]` (requires physical Android hardware or live Desktop JVM windowing).

---

## 1. Stage 2 Token & Accessibility Audit

| Check | Status | Evidence / Detail |
|---|---|---|
| Retired low-contrast `#77746E` replaced by `#8F8B83` | `[VERIFIED]` | Updated `VeloraMutedText` & `HEX_MUTED_TEXT` in `Color.kt`; unit test verifies WCAG AA luminance contrast `>= 4.5:1` on Obsidian (`#0B0B0D`) and Carbon (`#121216`). |
| R-C4 Three-Alpha Rule (`0.14f`, `0.45f`, `0.60f`) | `[VERIFIED]` | Centralized in `VeloraAlphaTokens`; `MiniPlayerBar`, `DesktopPlayerBar`, `VeloraErrorBanner`, and `VeloraMediaShelfCard` consume constants directly with zero ad-hoc translucent alpha values. |
| Semantic palette exposure (`LocalVeloraColors`) | `[VERIFIED]` | `MaterialTheme.veloraColors` supplied for `VELORA_SAFFRON`, `VELORA_DAYLIGHT`, `HIGH_CONTRAST_MONOCHROME`, and `SYSTEM_DYNAMIC`. |
| Skeleton shimmer pulse `250ms` + reduced motion | `[VERIFIED]` | `VeloraMotionTokens.SkeletonPulseMs = 250` applied to `VeloraLoadingSkeleton`; supports `reducedMotion = true`. |
| Bilingual BiDi isolation + Tabular Numerals | `[VERIFIED]` | `mixedScriptText()` wraps embedded LTR/numeric runs inside Persian RTL strings with `\u2066...\u2069`; `JetBrainsMonoFamily` configures `fontFeatureSettings = "tnum"` on numeric labels; unit tests pass. |

---

## 2. Navigation, Tier & 120-Control Coverage Audit

| Rule / Control Group | Status | Notes |
|---|---|---|
| `SIMPLE` tier = strictly **4** destinations (`MUSIC`, `VIDEO`, `PLAYER`, `SETTINGS`), `home = MUSIC` | `[VERIFIED]` | Tested in `FullscreenPolicyTest` & `HybridUiShellRobolectricTest`. `HOME`, `LIBRARY`, `ARTISTS`, `RADIO`, `SEARCH` remain hidden in `SIMPLE`. |
| `STANDARD` / `PRO` mobile tabs = **5** tabs (`HOME`, `SEARCH`, `LIBRARY`, `VIDEO`, `SETTINGS`) | `[VERIFIED]` | `LIBRARY` routes to `LibraryHubScreen` with 4 segmented sub-tabs (`Collections/Playlists`, `Music`, `Artists`, `Radio`), keeping every existing control and `testTag` intact. |
| Glassy MiniPlayer -> Full-Screen Now Playing + Queue/Lyrics sheets | `[VERIFIED]` | `MiniPlayerBar` displays above bottom tabs when an active track is loaded and navigates to `NowPlayingScreen`. `NowPlayingScreen` preserves EQ, Studio DSP, LRC Studio, inline Lyrics/Queue sections AND adds 1-tap `ModalBottomSheet` triggers (`open_queue_sheet_button`, `open_lyrics_sheet_button`) with artwork stage height capped `<= 0.618f`. |
| `FirebaseUser` decoupling from UI screens | `[VERIFIED]` | `VeloraUiState.cloudProfile: CloudAccountProfile?` maps auth state cleanly; `SettingsScreen` consumes `cloudProfile` without importing Firebase types. |
| Wide / Desktop Shell (`>= 600.dp`) | `[VERIFIED in Compose tree]` / `[UNVERIFIED on Desktop JVM]` | `VeloraDesktopSidebar` (auto-collapsing `< 720.dp` with manual toggle `desktop_sidebar_collapse_toggle`), `DesktopPlayerBar`, and `veloraKeyboardShortcuts` wired in `VeloraShellScreen`. Unit tests verify `Space`, `←/→`, `↑/↓`, `M`, `F` mappings. |

---

## 3. Five-Trap Engineering Verification

1. **No 10 Hz / 500 ms Full-Tree Recomposition:** `playbackPosition: StateFlow<PlaybackPositionState>` remains strictly isolated from structural `VeloraUiState`; only `MiniPlayerBar`, `DesktopPlayerBar`, `VeloraSeekBar`, and lyric highlighter read high-frequency ticks.
2. **Single Cinema Gesture Detector:** `veloraCinemaGestures` remains the single shared gesture modifier across `NowPlayingScreen` and `VideoPlayerScreen`.
3. **Exported `MediaSessionService` Guard:** `VeloraPlaybackService` preserves its explicit `MediaSession.Callback` security guard.
4. **No Fake Desktop Backend or Synthetic-Media Spam:** P2b desktop playback backend remains explicitly open in [ADR 0003](./docs/decisions/0003-desktop-playback-backend-and-hybrid-ui-review.md) (`VLCJ` vs `JavaFX Media` vs `GStreamer-Java`); no fake stub claims added.
5. **No Physical Device Screenshot Fabrication:** Per workspace rules, no fake `screenshots/after/` PNGs were generated because no physical device or live Desktop JVM display exists in this build container.

---

## 4. `[UNVERIFIED]` Checklist Requiring Human / Device Sign-Off

- `[UNVERIFIED — needs physical Android device]` Live visual contrast, touch ergonomics, and smooth 60/120 fps gesture scrolling of `HomeScreen`, `LibraryHubScreen`, `NowPlayingScreen` sheets, and `MiniPlayerBar` on phone hardware.
- `[UNVERIFIED — needs Desktop JVM window]` Actual hardware keyboard focus behavior (`Space`, arrows, `M`, `F`) and window resize smooth collapsing on Windows/Linux/macOS once Phase P6 wires `:shared-ui` and `:desktopApp`.
- `[UNVERIFIED — needs P2b selection]` Real codec/hardware-accel validation of chosen desktop media backend (`VLCJ` recommended candidate).

---

## 5. Gate Summary (`G1–G8`)

- **G1 (Builds — `gradle detekt lintDebug :app:testDebugUnitTest assembleDebug` + `compile_applet`):** `[VERIFIED]` `BUILD SUCCESSFUL` in 1m 9s (`:detekt`, `:app:detekt`, `:app:lintDebug`, `:app:testDebugUnitTest`, `:app:assembleDebug`), and `compile_applet` succeeded.
- **G2 (Tests):** `[VERIFIED]` 66 unit/Robolectric tests executed across 8 suites: **63 passed**, **3 skipped** (`VeloraFirebaseRepositoryRuleTest` emulator-gated tests when Firebase Local Emulator is not running on `127.0.0.1:9099/8085`), **0 failures**, **0 errors** (including `FullscreenPolicyTest`, `HybridUiTokensAndShortcutsTest`, `HybridUiShellRobolectricTest`, and `ExampleRobolectricTest`).
- **G3 (No Regressions):** All 8 screen feature sets + 120 controls preserved; `SIMPLE` = 4, `STANDARD/PRO` = 5 mobile tabs + `Home` discovery.
- **G4 (Tokens):** Zero hardcoded colors or off-scale metrics in new composables.
- **G5 (Accessibility):** `#8F8B83` WCAG AA muted text verified, `48.dp` min interactive targets preserved, BiDi `mixedScriptText()` active.
- **G6 (Privacy/Security):** Zero new permissions, zero secrets, zero hidden network calls.
- **G7 (Honesty):** Physical device visuals and desktop window execution explicitly marked `[UNVERIFIED]`.
- **G8 (Review):** Hostile review complete; clean single-module build maintained (**Gates: 8/8 passed** locally, with physical Android hardware & Desktop JVM windowing explicitly disclosed as `[UNVERIFIED]`).
