# VELORA Design System Consistency Audit (Phase 3)

Audited using the **§0-A Design System Consistency Auditor** protocol and **§0-B Apple HIG Lens** against `/DESIGN-SYSTEM.md` (Phase 1) and the Phase 0 Feature Inventory (`/UI-AUDIT.md`).

---

## Screen 1: Library (`LibraryScreen.kt` + `LibraryDialogs.kt` + `MusicAndArtistScreens.kt`)

- **Verdict:** `PASS` — All cards, buttons, spacing, typography, empty/loading states, and tier gates strictly consume `MaterialTheme` and `PhiTokens` with zero hard-coded hex or `.dp` literals.
- **Audit Confidence:** `High` — Inspected full source AST of `LibraryScreen.kt`, `LibraryDialogs.kt`, and `MusicAndArtistScreens.kt` plus automated Detekt & Robolectric verification.
- **Scope Summary:**
  - Audited `LibraryDiscoveryCard`, `PlaylistsSectionCard`, `SmartPlaylistsAndAnalyticsSection`, `FilterAndSortSection`, and `PlaybackHistorySection`.
  - Audited `MusicScreen` and `ArtistScreen` (responsive `<600dp` Compact chips vs `>=600dp` two-pane master-detail).
  - Verified `VeloraEmptyState` and `VeloraLoadingSkeleton` integration.
  - Verified `ExperienceTier` progressive disclosure (`SIMPLE` hides advanced folder path rules and smart rule builder; `PRO` unlocks all).
  - Checked touch target sizes (`minimumInteractiveComponentSize()` = `48.dp`) and WCAG AA/AAA contrast ratios.
- **Findings (Severity Order):**
  - *None breaking or inconsistent after Phase 2 modular rebuild.* (During Phase 2 construction, legacy `10.dp`/`12.dp`/`14.dp`/`16.dp` corner radii in `LibraryAndMusicScreens.kt` were replaced with `PhiTokens.CornerRadius13` and `PhiTokens.CornerRadius21`).
- **Patterns Worth Keeping:**
  - Golden Ratio `0.618f` / `0.382f` primary/secondary CTA button weight split (`library_auto_scan_button` vs `scan_library_button`).
  - Responsive `BoxWithConstraints` master-detail layout in `ArtistScreen` using `PhiTokens.WideScreenBreakpoint` (`600.dp`).
  - Animated `VeloraLoadingSkeleton` feedback during active media scanning.
- **Questions Before Shipping:**
  1. Should `SIMPLE` tier users see read-only Smart Mixes on the Library tab or keep Smart Mixes exclusive to `STANDARD` and `PRO`? (Currently shown in `STANDARD` and `PRO` to keep `SIMPLE` ultra-clean).
  2. Should M3U8 export also offer Android system Share sheet in addition to in-app clipboard/file export?
  3. Should artist avatars support custom user-picked images in a future release?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale (`veloraTypography`)
  - [x] Spacing uses the design-system scale (`PhiTokens`)
  - [x] Components are used consistently (`VeloraSectionCard`, `TrackRowItem`, `VeloraPrimaryButton`)
  - [x] Icons are consistent in style and size (`PhiTokens.IconSize20`)
  - [x] Accessibility (contrast >= 4.5:1, touch targets >= 48dp, focus/semantics)
  - [x] Naming follows conventions (`snake_case` `testTag`s)

---

## Screen 2: Music Player / Now Playing (`NowPlayingScreen.kt` + `PlayerLyricsAndDialogs.kt`)

- **Verdict:** `PASS` — Now Playing stage, narrow seek bar, transport controls, lyrics viewer, and queue list adhere 100% to `DESIGN-SYSTEM.md` tokens and narrow-state recomposition rules.
- **Audit Confidence:** `High` — Verified source code, narrow `PlaybackPositionState` flow decoupling, and `veloraCinemaGestures` sharing.
- **Scope Summary:**
  - Audited `NowPlayingStageCard`, `SpectrumAnalyzerCanvas`, `VeloraSeekBar`, and `VeloraTransportControls`.
  - Audited `SynchronizedLyricsAndBookmarksSection` and `QueueSectionCard`.
  - Verified that 500ms playback ticks update only `PlaybackPositionState` (`VeloraSeekBar` + timecodes) without recomposing `VeloraUiState`.
  - Verified that `NowPlayingStageCard` uses the single shared `Modifier.veloraCinemaGestures`.
- **Findings (Severity Order):**
  - *None open.* (Old 1,635-line monolith split into modular files; tabular `JetBrainsMono` / `Vazirmatn` `labelMedium` used on `elapsed_time_label` and `total_duration_label`).
- **Patterns Worth Keeping:**
  - Narrow `PlaybackPositionState` parameter passed directly to `VeloraSeekBar`.
  - Tier-aware `VeloraTransportControls` (3 core buttons in `SIMPLE`, 5 buttons with Shuffle/Repeat in `STANDARD`/`PRO`).
  - Multi-script synchronized lyrics highlighting active cue with `primaryContainer` surface and `primary` border.
- **Questions Before Shipping:**
  1. Should album cover artwork be toggleable with the live Goertzel spectrum in `SIMPLE` mode?
  2. Should queue reordering support drag-and-drop handles in `STANDARD` mode?
  3. Should lyrics auto-scroll smoothly when the user manually scrolls the LazyColumn?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets >= 48dp, semantic labels on Play/Pause)
  - [x] Naming follows conventions

---

## Screen 3: Equalizer & Studio DSP (`EqualizerAndDspScreen.kt`)

- **Verdict:** `PASS` — 10-band Biquad equalizer and Studio DSP rack are 100% tokenized and strictly gated by `ExperienceTier.PRO`.
- **Audit Confidence:** `High` — Verified both in `NowPlayingScreen.kt` (`FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.TEN_BAND_EQ)`) and in `FullscreenPolicyTest.kt`.
- **Scope Summary:**
  - Audited `EqualizerAndStudioDspSection`, 10 ISO frequency sliders (`31Hz`–`16kHz`), auto-headroom readout, and custom preset chips.
  - Audited Studio DSP sliders (Preamp, Sub-Bass Boost, 3D Stereo Width), A-B Loop buttons, Crossfade chips, and Sleep Timer chips.
  - Verified that in `SIMPLE` and `STANDARD` tiers, `EqualizerAndStudioDspSection` is completely absent from the composition tree.
- **Findings (Severity Order):**
  - *None open.*
- **Patterns Worth Keeping:**
  - Real-time Biquad auto-headroom calculation displayed in the card subtitle (`Auto-Headroom: -X.X dB`).
  - Live cascaded Biquad frequency-response curve overlaid on `SpectrumAnalyzerCanvas`.
- **Questions Before Shipping:**
  1. Should custom EQ presets support per-audio-route auto-binding (e.g., Bluetooth vs USB DAC)?
  2. Should the 10-band sliders support vertical orientation on tablet/Expanded screens?
  3. Should A-B loop points display markers directly on the main `VeloraSeekBar` track?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets, slider semantics)
  - [x] Naming follows conventions

---

## Screen 4: Video Player (`VideoPlayerScreen.kt` + `VideoPlayerDialogs.kt`)

- **Verdict:** `PASS` — Hardware `SurfaceView` encapsulation, shared cinema gestures, fullscreen orientation policy, and 3-tier control gating all pass audit.
- **Audit Confidence:** `High` — Verified `VeloraVideoSurface` boundary, `FullscreenPolicy`, and `VideoPlayerScreen.kt`.
- **Scope Summary:**
  - Audited inline and fullscreen video viewports (`VeloraVideoSurface`).
  - Verified zero `ExoPlayer` references in `VideoPlayerScreen` parameters (uses `PlayerEngine?` adapter).
  - Verified single shared `Modifier.veloraCinemaGestures` used for both inline and fullscreen video as well as Music Player.
  - Verified `FullscreenPolicy.isVideoControlVisible(tier, group)` hides Speed, Aspect, Subtitles, Audio Track, and Bookmarks in `SIMPLE` and `STANDARD` tiers.
- **Findings (Severity Order):**
  - *None open.*
- **Patterns Worth Keeping:**
  - Orientation lock policy (`FullscreenPolicy.shouldLockLandscape(nativeRatio)`) that locks landscape videos (`> 1.0f`) to sensor-landscape while preserving portrait orientation for vertical/square clips (`<= 1.0f`).
  - Controls lock toggle (`video_lock_controls_button` / `video_unlock_controls_button`) that prevents accidental touch seeks during playback.
- **Questions Before Shipping:**
  1. Should double-tap seek interval (`10s`) be configurable (`5s` / `10s` / `15s`) in Settings?
  2. Should Picture-in-Picture automatically trigger when pressing Home while a video is playing?
  3. Should external `.srt`/`.vtt` file picker be exposed directly inside `VideoSubtitlesDialog`?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets >= 48dp, lock button semantics)
  - [x] Naming follows conventions

---

## Screen 5: OmniSearch & Media Copilot (`SearchScreen.kt`)

- **Verdict:** `PASS` — Clean debounced search bar, semantic chips, Copilot bar, and designed `VeloraEmptyState` for zero-result queries.
- **Audit Confidence:** `High` — Inspected `SearchScreen.kt` (219 lines) and ViewModel 250ms debounce flow.
- **Scope Summary:**
  - Audited `omnisearch_query_input`, clear button, and `SEMANTIC_SUGGESTION_CHIPS`.
  - Audited `VELORA MEDIA COPILOT` natural-language command bar (`copilot_command_input`, `copilot_execute_button`).
  - Audited `VeloraEmptyState` when `uiState.searchResults.isEmpty()`.
- **Findings (Severity Order):**
  - *None open.*
- **Patterns Worth Keeping:**
  - 250ms coroutine `debounce` on non-empty queries with instant `0ms` reset when cleared.
  - One-tap "Clear Query" CTA inside `VeloraEmptyState` when a search yields no matches.
- **Questions Before Shipping:**
  1. Should recent search queries be optionally persisted locally with a one-tap clear history button?
  2. Should OmniSearch group results under collapsible category headers when >20 items match?
  3. Should Copilot commands show autocomplete suggestions as the user types?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets, clear button description)
  - [x] Naming follows conventions

---

## Screen 6: Live Radio Tuner (`RadioScreen.kt`)

- **Verdict:** `PASS` — Clean tokenized station cards, genre filter chips, and custom HTTPS stream dialog.
- **Audit Confidence:** `High` — Inspected `RadioScreen.kt` (194 lines).
- **Scope Summary:**
  - Audited genre filter row (`radio_genre_chip_<genre>`), station cards (`radio_station_card_<id>`), and `AddCustomRadioDialog`.
  - Verified Golden Ratio `0.618f` / `0.382f` split between "Tune Live" primary CTA and "Queue" secondary CTA.
- **Findings (Severity Order):**
  - *None open.*
- **Patterns Worth Keeping:**
  - Clear codec and bitrate eyebrow metadata (`Ambient • 320 kbps • AAC-LC`) on each station card.
  - SSRF/HTTPS validation on custom stream URLs via the underlying playback engine.
- **Questions Before Shipping:**
  1. Should live ICY stream title metadata updates be logged into Playback History?
  2. Should users be able to delete or edit custom radio stations from the card menu?
  3. Should favorite radio stations appear on the main Library tab?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets)
  - [x] Naming follows conventions

---

## Screen 7: Settings & App Shell (`SettingsScreen.kt` + `VeloraShellScreen.kt`)

- **Verdict:** `PASS` — Instant 3-tier `ExperienceTier` switcher, 4 WCAG-verified color schemes, bilingual English LTR / Persian RTL (`Vazirmatn`) support, Backup/Restore, and Cloud Sync all pass audit.
- **Audit Confidence:** `High` — Inspected `SettingsScreen.kt`, `VeloraShellScreen.kt`, and verified with Robolectric tests.
- **Scope Summary:**
  - Audited 3-tier selector (`tier_option_SIMPLE`, `tier_option_STANDARD`, `tier_option_PRO`) in `SettingsScreen` and quick cycle pill (`shell_mode_toggle_button`) in `VeloraTopHeaderBar`.
  - Audited `ThemePaletteMode` selector (`SYSTEM_DYNAMIC`, `VELORA_SAFFRON`, `VELORA_DAYLIGHT`, `HIGH_CONTRAST_MONOCHROME`).
  - Audited Persian (`fa`) RTL layout direction (`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`) and `Vazirmatn` font binding.
  - Audited JSON Backup/Restore, demo sample purge, and Firebase Google Sign-In card.
- **Findings (Severity Order):**
  - *None open.*
- **Patterns Worth Keeping:**
  - Instant reactive tier switching from both the top shell bar and Settings without activity restart.
  - Automatic `LayoutDirection.Rtl` + `Vazirmatn` font switching when `appLanguageCode == "fa"`.
- **Questions Before Shipping:**
  1. Should `STANDARD` be the default tier on fresh install for tablet devices while keeping `SIMPLE` as the default on phones?
  2. Should JSON backup export also integrate with Android `CreateDocument` SAF picker?
  3. Should theme mode include an automatic sunrise/sunset schedule between `VELORA_DAYLIGHT` and `VELORA_SAFFRON`?
- **Checklist:**
  - [x] Colors use tokens, not hard-coded values
  - [x] Typography uses the design-system scale (`Vazirmatn` + `SpaceGrotesk`/`Inter`/`JetBrainsMono`)
  - [x] Spacing uses the design-system scale
  - [x] Components are used consistently
  - [x] Icons are consistent in style and size
  - [x] Accessibility (contrast, touch targets, RTL layout direction)
  - [x] Naming follows conventions

---

## Phase 0 Feature Inventory Verification (Zero Features Lost)

- [x] Local media auto-scan, manual rescan, SAF file import, SAF folder import, safe folder allowlist (`LibraryScreen.kt`)
- [x] Playlists create, play, shuffle, delete, add track, M3U8 export & import (`LibraryScreen.kt`, `LibraryDialogs.kt`)
- [x] Smart playlists, Listening Stats recap, Custom Smart Rule Builder (`LibraryDialogs.kt`)
- [x] Playback History (Media Time Machine) resume & clear (`LibraryDialogs.kt`)
- [x] Music collection quick filter, Hi-Res filter, Play All, Shuffle All (`MusicAndArtistScreens.kt`)
- [x] Artists adaptive master-detail browser (`MusicAndArtistScreens.kt`)
- [x] Now Playing 10-band Goertzel spectrum, live Biquad transfer curve, narrow-state seek bar, transport controls, exact ms seek, metadata tag editor (`NowPlayingScreen.kt`, `PlayerLyricsAndDialogs.kt`)
- [x] Synchronized multi-script lyrics, ±250ms offset, Time Machine bookmarks, interactive LRC timestamp studio (`PlayerLyricsAndDialogs.kt`)
- [x] 10-Band Biquad Equalizer with presets + Studio DSP Rack (Preamp, Sub-Bass, 3D Stereo Width, Crossfade, A-B Loop, Sleep Timer) (`EqualizerAndDspScreen.kt`)
- [x] Video Player hardware SurfaceView, shared gestures, controls lock, fullscreen with landscape lock policy, PiP, speed, aspect ratio, subtitles & style picker, audio track selector (`VideoPlayerScreen.kt`, `VideoPlayerDialogs.kt`)
- [x] OmniSearch with 250ms debounce, semantic chips, and Media Copilot natural-language command bar (`SearchScreen.kt`)
- [x] Live Radio tuner with genre filter and custom HTTPS stream dialog (`RadioScreen.kt`)
- [x] Settings with instant 3-Tier selector (`SIMPLE`/`STANDARD`/`PRO`), 4 themes, Persian RTL / English LTR, audio route, demo sample toggle/purge, JSON backup/restore, Firebase Cloud Sync, and declarative plugins (`SettingsScreen.kt`)
