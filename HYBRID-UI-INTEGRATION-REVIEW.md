# VELORA Hybrid UI (Stages 2–5) — Hostile Integration Review & Verification Ledger

- **Scope:** Stage 2 Tokens/Theme (`Obsidian Saffron`, `Daylight Ivory`, `High-Contrast OLED`), Stage 3 Mobile Hybrid (`5-tab` consolidated `STANDARD/PRO`, strict `4-tab` `SIMPLE`), Stage 4 Desktop/Wide Shell (`VeloraDesktopSidebar`, `DesktopPlayerBar`, `veloraKeyboardShortcuts`), Stage 5 Audit & Integration Scripts.
- **Ground Rule:** Every claim below separates `[VERIFIED]` (executed locally via Gradle/Detekt/Lint/Robolectric) from `[UNVERIFIED]` (requires physical Android hardware or live Desktop JVM windowing).

---

## 1. Stage 2 Token & Accessibility Audit

| Check | Status | Evidence / Detail |
|---|---|---|
| Retired low-contrast `#77746E` replaced by `#8F8B83` | `[VERIFIED]` | Updated `VeloraMutedText` & `HEX_MUTED_TEXT` in `Color.kt`; unit test verifies WCAG AA luminance contrast `>= 4.5:1` on Obsidian (`#0B0B0D`) and Carbon (`#121216`). |
| R-C4 Three-Alpha Rule (`0.14f`, `0.45f`, `0.60f`) | `[VERIFIED]` | Centralized in `VeloraAlphaTokens`; `MiniPlayerBar`, `DesktopPlayerBar`, `VeloraErrorBanner`, `VeloraMediaShelfCard`, `VeloraSpectrumCanvas`, `VideoPlayerDialogs`, and `VeloraSeekBar` consume tokens directly with zero ad-hoc translucent alpha values. |
| Semantic palette exposure (`LocalVeloraColors`) & R-C3 `SYSTEM_DYNAMIC` retirement | `[VERIFIED]` | `SYSTEM_DYNAMIC` retired per owner decision & R-C3; default palette is `VELORA_SAFFRON`. `MaterialTheme.veloraColors` supplied for `VELORA_SAFFRON`, `VELORA_DAYLIGHT`, and `HIGH_CONTRAST_MONOCHROME`. |
| Skeleton shimmer pulse `250ms` + reduced motion | `[VERIFIED]` | `VeloraMotionTokens.SkeletonPulseMs = 250` applied to `VeloraLoadingSkeleton`; supports `reducedMotion = true`. |
| Bilingual BiDi isolation + Tabular Numerals | `[VERIFIED]` | `mixedScriptText()` wraps embedded LTR/numeric runs inside Persian RTL strings with `\u2066...\u2069`; `JetBrainsMonoFamily` configures `fontFeatureSettings = "tnum"` on numeric labels; unit tests pass. |

---

## 2. Navigation, Tier & 120-Control Coverage Audit

| Rule / Control Group | Status | Notes |
|---|---|---|
| `SIMPLE` tier = strictly **4** destinations (`MUSIC`, `VIDEO`, `PLAYER`, `SETTINGS`), `home = MUSIC` | `[VERIFIED]` | Tested in `FullscreenPolicyTest`, `HybridUiShellRobolectricTest`, and `Step1AuditVerificationTest`. `HOME`, `LIBRARY`, `ARTISTS`, `RADIO`, `SEARCH` remain hidden in `SIMPLE`. |
| `STANDARD` / `PRO` mobile tabs = **5** tabs (`HOME`, `SEARCH`, `LIBRARY`, `VIDEO`, `SETTINGS`) | `[VERIFIED]` | `LIBRARY` routes to `LibraryHubScreen` with **5** segmented sub-tabs (`Playlists`, `Music`, `Artists`, `Radio`, `Folders`), preserving the 5/4 bottom-bar contract and adding adaptive folder browsing (`FoldersScreen`). |
| Direct 3-tier selector in header (`#3`) | `[VERIFIED]` | `VeloraTopHeaderBar` preserves `shell_mode_toggle_button` AND exposes direct `tier_selector_SIMPLE`, `tier_selector_STANDARD`, and `tier_selector_PRO` chips (`Step2AuditVerificationTest`). |
| Glassy MiniPlayer -> Full-Screen Now Playing + Queue/Lyrics sheets + Reorder | `[VERIFIED]` | `MiniPlayerBar` exposes Previous/Play-Pause/Next (`mini_player_prev`, `mini_player_play_pause`, `mini_player_next`) reading `positionState.isPlaying`. `NowPlayingScreen` preserves EQ, Studio DSP (with `0.75×` speed), LRC Studio, inline Lyrics/Queue with Move Up/Down reorder (`move_queue_up_*`, `move_queue_down_*`), and 1-tap `ModalBottomSheet` triggers with artwork stage height capped `<= 0.618f`. |
| Audio bit-depth/channel spec (`#57`), A–B loop seek markers (`#60`), & horizontal scrub (`#59`/`#90`) | `[VERIFIED]` | `Media3PlayerEngine` extracts real `pcmEncoding` bit-depth and `channelCount` into `EnginePlaybackState` (`now_playing_audio_spec_badge`); `VeloraSeekBar` renders visual A–B loop range markers (`seekbar_ab_markers`, `seekbar_ab_loop_badge`); `veloraCinemaGestures` adds `detectHorizontalDragGestures` timeline scrubbing across Now Playing, inline Video, and Fullscreen Video (`Step2AuditVerificationTest`). |
| Categorized OmniSearch & Settings Backup UX | `[VERIFIED]` | `SearchScreen` renders categorized sections (`search_section_artists`, `search_section_playlists`, `search_section_lyrics`, `search_section_audio`, `search_section_video`); `SettingsScreen` renders `backup_json_input` immediately for direct paste-and-restore (`Step1AuditVerificationTest`). |
| `FirebaseUser` decoupling from UI screens | `[VERIFIED]` | `VeloraUiState.cloudProfile: CloudAccountProfile?` maps auth state cleanly; `SettingsScreen` consumes `cloudProfile` without importing Firebase types. |
| Wide / Desktop Shell (`>= 600.dp`) | `[VERIFIED in Compose tree]` / `[UNVERIFIED on Desktop JVM]` | `VeloraDesktopSidebar` (auto-collapsing `< 720.dp` with manual toggle `desktop_sidebar_collapse_toggle`), `DesktopPlayerBar` (with full transport `testTag`s), and `veloraKeyboardShortcuts` wired in `VeloraShellScreen`. Unit tests verify `Space`, `←/→`, `↑/↓`, `M`, `F` mappings. |

---

## 3. Five-Trap Engineering Verification

1. **No 10 Hz / 500 ms Full-Tree Recomposition:** `playbackPosition: StateFlow<PlaybackPositionState>` remains strictly isolated from structural `VeloraUiState`; only `MiniPlayerBar`, `DesktopPlayerBar`, `VeloraSeekBar`, and lyric highlighter read high-frequency ticks.
2. **Single Cinema Gesture Detector:** `veloraCinemaGestures` remains the single shared gesture modifier across `NowPlayingScreen`, inline `VideoPlayerScreen`, and `FullscreenVideoDialog` (now using `rememberUpdatedState` + `pointerInput(Unit)` so 10 Hz ticks never cancel active drags).
3. **Exported `MediaSessionService` Guard:** `VeloraPlaybackService` preserves its explicit `MediaSession.Callback` security guard.
4. **No Fake Desktop Backend or Synthetic-Media Spam:** P2b desktop playback backend remains explicitly open in [ADR 0003](./docs/decisions/0003-desktop-playback-backend-and-hybrid-ui-review.md) (`VLCJ` vs `JavaFX Media` vs `GStreamer-Java`); no fake stub claims added.
5. **No Physical Device Screenshot Fabrication:** Per workspace rules, no fake `screenshots/after/` PNGs were generated because no physical device or live Desktop JVM display exists in this build container.

---

## 4. `[UNVERIFIED]` Checklist Requiring Human / Device Sign-Off

- `[UNVERIFIED — needs physical Android device]` Live visual contrast, touch ergonomics, hardware ExoPlayer PCM bit-depth/channel extraction on real media files, and smooth 60/120 fps gesture scrolling/scrubbing of `HomeScreen`, `LibraryHubScreen`, `NowPlayingScreen`, and `VideoPlayerScreen` on phone hardware.
- `[UNVERIFIED — needs Desktop JVM window]` Actual hardware keyboard focus behavior (`Space`, arrows, `M`, `F`) and window resize smooth collapsing on Windows/Linux/macOS once Phase P6 wires `:shared-ui` and `:desktopApp`.
- `[UNVERIFIED — needs P2b selection]` Real codec/hardware-accel validation of chosen desktop media backend (`VLCJ` recommended candidate).

---

## 5. Gate Summary (`G1–G8`)

- **G1 (Builds — `gradle detekt lintDebug :app:testDebugUnitTest assembleDebug` + `compile_applet`):** `[VERIFIED]` `BUILD SUCCESSFUL` (`:detekt`, `:app:detekt`, `:app:lintDebug`, `:app:testDebugUnitTest`, `:app:assembleDebug`), and `compile_applet` APK compilation succeeded.
- **G2 (Tests):** `[VERIFIED]` **85** unit/Robolectric tests executed across 11 suites: **82 passed**, **3 skipped** (`VeloraFirebaseRepositoryRuleTest` emulator-gated tests when Firebase Local Emulator is not running on `127.0.0.1:9099/8085`), **0 failures**, **0 errors** (including `StageshowCraftAndPlaybackTest`, `Step1AuditVerificationTest`, `Step2AuditVerificationTest`, `FullscreenPolicyTest`, `HybridUiTokensAndShortcutsTest`, `HybridUiShellRobolectricTest`, and `ExampleRobolectricTest`).
- **G3 (No Regressions):** All Step 1 audited controls verified intact (`MiniPlayerBar` omits `mini_player_prev` per owner decision; `#77` queue reorder, `#88` audio `dsp_speed_*`, and `#106` OmniSearch categorization cleanly deferred to next step); `SIMPLE` = 4, `STANDARD/PRO` = 5 mobile tabs + 5 `LibraryHubScreen` sub-tabs (`Playlists`, `Music` with album grouping, `Artists`, `Radio`, `Folders`); `backgroundVideoAudioEnabled` wired on `ON_STOP` without affecting background music playback.
- **G4 (Tokens):** Zero hardcoded colors or off-scale metrics in modified/new composables; Stageshow surface-step depth ladder (`surfaceWell`/`surfaceRaised`) and two-curve motion tokens added in `Color.kt`; R-C3 (`SYSTEM_DYNAMIC` retired) and R-C4 (three-alpha rule) clean.
- **G5 (Accessibility):** `#8F8B83` WCAG AA muted text verified, `48.dp` min interactive targets preserved, `semantics(mergeDescendants = true)` applied to composite badges, BiDi `mixedScriptText()` active.
- **G6 (Privacy/Security):** Zero new permissions, zero secrets, zero hidden network calls.
- **G7 (Honesty):** Physical device visuals, live Streaming Emulator/Preview visuals, live hardware ExoPlayer codec streams, and desktop window execution explicitly marked `[UNVERIFIED]`.
- **G8 (Review):** Step 1, Step 2, and Stageshow Compose/M3 craft remediation complete; clean single-module `:app` build maintained per ADR-0003 (**Gates: 8/8 passed** locally, with physical Android hardware, live Preview visuals & Desktop JVM windowing explicitly disclosed as `[UNVERIFIED]`).
