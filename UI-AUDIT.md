# Velora UI Audit — Phase 0

> **Scope:** audit of the CURRENT UI on the local clone. No code changed, no commit, no Gradle run.
> **HEAD:** `19e70bc` — `refactor(settings): introduce experience tiers` (latest main, fetched 2026-11-18)
> **Branch:** `feature/ui-rebuild-phase0-1`
> **Repo:** `aghorbani84/Velora` — single-module Android app, Kotlin + Jetpack Compose + Media3
> **Date of audit:** 2026-11-18 (Asia/Tehran)
> **Method:** full read of `NavGraph.kt`, all 12 screen files, all 3 component files, theme files, `VeloraViewModel.kt`, `VeloraUiStateAssembler.kt`, `FullscreenPolicy.kt`; line counts via `wc -l`; callers verified via `grep`. No emulator, no device, no Gradle run, no real media files — see §7.
>
> **Note on a pre-existing file:** a `UI-AUDIT.md` was already present at the clone root, committed
> in HEAD `19e70bc`. It described a different snapshot (files that do not exist at this HEAD:
> `LibraryAndMusicScreens.kt`, `Phase2Components.kt`, `PlayerAndEqScreen.kt`,
> `RadioAndDspComponents.kt`; "500 ms ticks"; a `veloraMediaGestures` modifier with horizontal-drag
> scrub; a `bufferedPercentage` field in `PlaybackPositionState`; a
> `settings_dynamic_color_switch` that does not exist). It has been replaced by this audit,
> verified line-by-line against HEAD `19e70bc`.

## 0. Headline numbers

| Metric | Value |
|---|---|
| Nav destinations (routes) | 8 |
| Screen files (under `ui/screens/`) | 12 (8 route screens + shell + 3 dialog/section files) |
| Component files (under `ui/components/`) | 3 |
| Total UI-layer lines (`ui/` incl. viewmodel/navigation/theme) | 7,622 (`wc -l`) |
| Distinct user-facing controls/actions catalogued (§2) | **120** |
| Shared components (§2.10) | 12 |
| `testTag` occurrences (screens + components) | 125 |

---

## 1. Screen list — routes, files, actions

All routes are defined in `app/src/main/java/com/example/ui/navigation/NavGraph.kt:53-72`
(`VeloraDestination` enum). Start destination is `MUSIC` (`NavGraph.kt:118`,
`DEFAULT_START_ROUTE = "music"`); the shell overrides it per tier (§1.1). Navigation is
single-top with state restore (`navigateSingleTopTo`, `NavGraph.kt:99-107`).

| # | Screen (composable) | Route | File (lines) | Tier visibility |
|---|---|---|---|---|
| 1 | `VeloraShellScreen` (app shell, not a route) | — (hosts `NavHost`) | `ui/screens/VeloraShellScreen.kt` (423) | Always |
| 2 | `LibraryScreen` | `library` | `ui/screens/LibraryScreen.kt` (473) + `LibraryDialogs.kt` (361) | Hidden in SIMPLE (removed from nav) |
| 3 | `MusicScreen` | `music` | `ui/screens/MusicAndArtistScreens.kt` (370, shared with Artists) | Always; **home in SIMPLE** |
| 4 | `ArtistScreen` | `artists` | `ui/screens/MusicAndArtistScreens.kt` (370) | Hidden in SIMPLE |
| 5 | `RadioScreen` | `radio` | `ui/screens/RadioScreen.kt` (213) | Hidden in SIMPLE |
| 6 | `VideoPlayerScreen` | `video` | `ui/screens/VideoPlayerScreen.kt` (337) + `VideoPlayerDialogs.kt` (458) | Always |
| 7 | `SearchScreen` | `search` | `ui/screens/SearchScreen.kt` (232) | Hidden in SIMPLE |
| 8 | `PlayerAndEqScreen` | `player` | `ui/screens/NowPlayingScreen.kt` (460) + `EqualizerAndDspScreen.kt` (285) + `PlayerLyricsAndDialogs.kt` (460) | Always |
| 9 | `SettingsScreen` | `settings` | `ui/screens/SettingsScreen.kt` (381) | Always |

Notes on navigation behaviour (`VeloraShellScreen.kt`):
- `homeDestination` is `MUSIC` in SIMPLE, `LIBRARY` otherwise (`VeloraShellScreen.kt:124-129`).
- In SIMPLE only 4 destinations are shown: Music, Video, Player, Settings
  (`VeloraShellScreen.kt:131-143`). If the tier switches to SIMPLE while the user is on a
  hidden destination, the shell force-navigates to Music (`VeloraShellScreen.kt:145-152`).
- Back press does **not** pop the back stack: it navigates to the home destination
  (`VeloraShellScreen.kt:154-156`).
- Wide screens (≥ `PhiTokens.WideScreenBreakpoint`) get a side `NavigationRail` instead of the
  bottom `NavigationBar` (`VeloraShellScreen.kt:192-224`).
- Startup auto-scan: on first composition, if `autoScanOnStartup` is set, permissions are
  checked and `autoScanDeviceMedia()` runs (`VeloraShellScreen.kt:99-117`).
- Language is applied app-wide by forcing `LocalLayoutDirection` to RTL for `fa`, LTR otherwise
  (`VeloraShellScreen.kt:160-161`).

### 1.1 Product requirements — where each one lives today

| Requirement (rebuild prompt §4) | Current implementation |
|---|---|
| (1) SIMPLE mode truly simple | 3-tier gating via `FullscreenPolicy` + per-screen `tier != SIMPLE` / `tier == PRO` checks (§2). Mostly real, with the dead-code exceptions in §5. |
| (2) Easy video+music playback with all pro settings | Music: `PlayerAndEqScreen`; Video: `VideoPlayerScreen`; pro settings behind PRO tier (§2.6, §2.7). |
| (3) Video fullscreen | `FullscreenVideoDialog` — a full-size Compose `Dialog`, not a separate Activity/destination (`VideoPlayerDialogs.kt:189-333`). Hides system bars, locks orientation (see issue §5.5). |
| (4) Background playback for music AND video | `VeloraPlaybackService` (`MediaSessionService`, `data/playback/VeloraPlaybackService.kt:17`), started from the engine (`Media3PlayerEngine.kt:277-283`). A `backgroundVideoAudioEnabled` setting exists but has **no consumer in the engine/service** — see §5.8 / §6. |

---

## 2. Per-screen feature / control list (reference for "no feature is dropped")

Numbered items are the countable controls. Shared-component actions (e.g. the track row's
favorite/queue/playlist actions) are counted once per screen where they surface.

### 2.1 Shell — `VeloraShellScreen` (11)

1. Bottom `NavigationBar` (narrow) / side `NavigationRail` (wide) with tier-filtered destinations.
2. `MiniPlayerBar` (always above the nav bar when a track is loaded): progress bar, title,
   artist + elapsed time, play/pause, skip-next, tap opens Player route
   (`VeloraSharedComponents.kt:691+`).
3. Top header: `VELORA` wordmark + current tier label.
4. Tier cycle button (SIMPLE → STANDARD → PRO → SIMPLE) in the header
   (`VeloraShellScreen.kt:316-342`).
5. Language toggle button EN / فا in the header (`VeloraShellScreen.kt:344-370`).
6. Track-count badge in the header.
7. Status banner with dismiss button (`VeloraShellScreen.kt:374-402`).
8. Startup permission request + auto-scan flow.
9. Back-handler → home destination (no back-stack pop).
10. SIMPLE redirect: leaving a hidden destination force-navigates to Music.
11. RTL/LTR layout-direction switch for Persian/English.

### 2.2 Library — `LibraryScreen` + `LibraryDialogs` (17)

Discovery card (`LibraryScreen.kt:238+`):
1. Auto-scan storage (requests media + notification permissions first).
2. Rescan / manual scan.
3. Import file picker (audio/video).
4. Import folder picker (document tree).
5. **[PRO]** Folder-path text field + Add button.
6. **[PRO]** Create custom (sandbox) folder button.
7. **[PRO]** Remove-folder chips for each allowed folder URI.
8. Scan-progress loading skeleton while `scanProgress.isScanning`.
9. **[non-SIMPLE]** Playlists section: create playlist (name field + Create), play playlist,
   shuffle playlist, delete playlist.
10. **[non-SIMPLE]** M3U8 dialog: export each playlist to M3U8 text, import M3U8 (name + content fields).
11. **[non-SIMPLE]** Smart playlists & analytics: listening-stats recap card (total plays, top
    artist, Hi-Res count), play-smart-mix buttons.
12. **[PRO]** Smart-rule builder dialog: rule name, genre/artist keyword, min sample rate,
    favorites-only checkbox, Apply.
13. Filter-mode chips (all `TrackFilterMode` entries).
14. Sort-order chips (all `TrackSortOrder` entries).
15. Track list (`TrackRowItem` per track): tap to play, favorite toggle, add-to-queue,
    add-to-playlist dropdown menu; active track highlighted.
16. Empty state ("library is empty") with scan action.
17. **[non-SIMPLE]** Playback-history section: resume-from-position cards, clear-history button.

### 2.3 Music — `MusicScreen` (7)

1. Play All (with track count).
2. **[non-SIMPLE]** Shuffle All.
3. **[non-SIMPLE]** Quick-filter text field (title/artist/album) with clear button.
4. **[non-SIMPLE]** Hi-Res (≥48 kHz) filter toggle.
5. Auto-scan device music (permission flow).
6. Track list (`TrackRowItem`, same 4 actions as §2.2.15).
7. Empty state with scan action.

### 2.4 Artists — `ArtistScreen` (4)

1. Artist selector chips — horizontal row on narrow screens, side column on wide screens
   (breakpoint `PhiTokens.WideScreenBreakpoint`).
2. Play Artist.
3. Shuffle Artist.
4. Selected artist's track list (`TrackRowItem`, same 4 actions).

### 2.5 Radio — `RadioScreen` (5)

1. Genre filter chips (derived from station categories + "All").
2. Tune Live (play station).
3. Add station to queue.
4. Add custom station dialog: name, HTTPS stream URL, genre; Add/Cancel.
5. Empty state for an empty genre (no action button — only text).

### 2.6 Video — `VideoPlayerScreen` + `VideoPlayerDialogs` (25)

Inline player (`VideoPlayerScreen.kt:179+`):
1. Video surface (`VeloraVideoSurface`, Media3 `PlayerView`, controller disabled).
2. Subtitle overlay (shown when subtitles enabled and a line is active).
3. Lock / unlock controls button (top-end of the surface).
4. Tap surface toggles control visibility; double-tap left/right seeks ∓/±, double-tap center
   toggles play/pause; vertical drag on left/right half adjusts brightness/volume
   (all via shared `veloraCinemaGestures`, §2.10.11).
5. Gesture HUD label (shows e.g. seek/volume/brightness feedback text).
6. Seek bar with elapsed/total time (`VeloraSeekBar`).
7. Play / Pause button.
8. Fullscreen button (opens `FullscreenVideoDialog`).
9. PiP button (`enterPictureInPictureMode`, fixed 16:9 aspect — `NavGraph.kt:200-210`).
10. Scan / pick-video button (permission flow).
11. **[PRO]** Playback-speed chips: 0.5×, 1.0×, 1.25×, 1.5×, 2.0×.
12. **[PRO]** Aspect-mode chips (all `VideoAspectMode` entries: FIT, FILL, GOLDEN_PHI, SIXTEEN_NINE).
13. **[PRO]** Subtitles button → dialog: enable switch, font-scale picker, color-theme picker
    (4 themes: Saffron-on-dark, Yellow high-contrast, Cyan studio, White-on-dark).
14. **[PRO]** Audio-track button → dialog: pick embedded audio track (language + label) or
    default-stream notice.
15. **[PRO]** Bookmark button ("Cinema Scene" bookmark at current position).
16. Error banner with Retry action (retry = toggle play/pause).
17. Video track list (`TrackRowItem`, same 4 actions; tapping a video also navigates to Video).
18. Empty state with scan action.

Fullscreen dialog (`VideoPlayerDialogs.kt:189-333`):
19. Full-screen `Dialog` (platform default width off), system bars hidden, orientation lock
    (see §5.5).
20. Exit-fullscreen button (hidden while controls are locked).
21. Lock/unlock toggle (top bar, always visible).
22. Fullscreen seek bar.
23. Fullscreen gesture layer (same `veloraCinemaGestures`: tap, double-tap seek, vertical
    drag volume/brightness) + HUD label.
24. Fullscreen subtitle overlay.
25. Dialog dismiss (back / outside) exits fullscreen; system bars + orientation restored on dispose.

### 2.7 Player & EQ — `PlayerAndEqScreen` + `EqualizerAndDspScreen` + `PlayerLyricsAndDialogs` (29)

Now-playing stage (`NowPlayingScreen.kt:206+`):
1. Error banner with Dismiss action.
2. Spectrum analyzer canvas (live `spectrumBandsDb` + EQ curve; visualizer modes).
3. **[PRO]** Visualizer-mode selector chips (all `VisualizerMode` entries).
4. Cinema gestures on the spectrum canvas (double-tap seek, vertical-drag volume; brightness
   and tap-toggle are wired to no-ops — see §5.4).
5. Gesture HUD label.
6. Seek bar with elapsed/total time.
7. Transport controls: play/pause (large round button), previous, next; **[non-SIMPLE]** shuffle
   and repeat-cycle (OFF → ALL → ONE) (`VeloraSharedComponents.kt:444+`).
8. Mute toggle + volume slider.
9. **[non-SIMPLE]** Stop button.
10. **[non-SIMPLE]** Exact-seek dialog ("Seek ms"): accepts milliseconds or `mm:ss`.
11. **[non-SIMPLE]** Favorite toggle (header action).
12. **[non-SIMPLE]** Metadata tag editor dialog: edit title / artist / album, Save Tags.

EQ + Studio DSP — PRO only (`EqualizerAndDspScreen.kt:52+`, gated by
`MusicControlGroup.TEN_BAND_EQ`):
13. EQ enable/disable switch.
14. Flat reset button.
15. Save-custom-preset dialog (name field).
16. Preset chips (built-in + custom) with delete button on custom presets.
17. Ten band sliders (centre frequencies from `EqSpecification`, ±dB with live value labels,
    disabled when EQ off) + auto-headroom readout.
18. Preamp gain slider (−12…+12 dB).
19. Sub-bass boost slider (0…100%).
20. 3D stereo-width slider (0…200%).
21. A-B loop: Set A, Set B, Clear A-B.
22. Crossfade chips: 0 / 3 / 6 / 12 s.
23. Sleep-timer chips: Off / 15 / 30 / 60 min.
24. Non-PRO upsell card ("Switch to Studio Pro" button → sets tier PRO).

Lyrics & queue (`PlayerLyricsAndDialogs.kt:52+`):
25. **[STANDARD+]** Synchronized lyrics viewer: active line highlighted, tap a line to seek,
    Persian translation shown when available.
26. Lyrics offset −250 ms / +250 ms buttons.
27. **[PRO]** LRC Timestamp Studio dialog: add new line at current position, stamp first 4
    lines at current position, export LRC.
28. **[PRO]** Time-machine bookmarks: add bookmark at current position, bookmark chips
    (tap to seek).
29. **[STANDARD+]** Playback queue: tap a queue item to play it, remove-from-queue button per item.

### 2.8 Search — `SearchScreen` (6)

1. Omnisearch query field (title/artist/album/format) with clear button.
2. Semantic shortcut chips: Favorites, Hi-Res, Unfinished, Night Drive (set the query).
3. **[non-SIMPLE]** "Valid songs only" switch (skip short clips).
4. **[non-SIMPLE]** Media Copilot: natural-language command field + Run button
   (e.g. "Play favorites", "Resume last session", "Find Hi-Res tracks", "Export SRT").
5. Search-result list (`TrackRowItem`, same 4 actions).
6. Empty state (no results) with Clear Query action when a query is present.
   Search is debounced 250 ms in the ViewModel (`VeloraViewModel.kt:110-114`).

### 2.9 Settings — `SettingsScreen` (16)

1. Experience-tier picker: SIMPLE / STANDARD / PRO buttons (instant switch, no restart).
2. Language picker: English (LTR) / فارسی (RTL).
3. Theme-palette picker (all `ThemePaletteMode` entries).
4. Adaptive-audio-mode picker (all `AdaptiveAudioMode` entries).
5. Auto-scan-on-startup switch.
6. Valid-songs-only-in-search switch.
7. Background-video-audio switch.
8. Low-battery DSP-saver switch.
9. Include-demo-samples switch.
10. Scan Device Now (permission flow).
11. Purge demo samples.
12. **[PRO]** Video-controls auto-hide timeout slider (1500…15000 ms).
13. Google sign-in / sign-out (Firebase; shows signed-in identity).
14. Export offline backup (JSON) — result shown in an editable text field.
15. Restore offline backup from pasted JSON.
16. **[PRO]** Declarative-plugin toggles (one switch per registered plugin).

**Count check:** 11 (shell) + 17 (library) + 7 (music) + 4 (artists) + 5 (radio) + 25 (video)
+ 29 (player) + 6 (search) + 16 (settings) = **120**. (Library §2.2 lists 17 numbered items;
the four product-requirement rows in §1.1 are not controls. The §0 headline "124" in earlier
drafts double-counted the library M3U8/smart-rule sub-actions; the audited total is **120**
distinct user-facing controls/actions.)

### 2.10 Shared components — `ui/components/` (12)

`VeloraSharedComponents.kt` (784 lines):
1. `VeloraPrimaryButton` / 2. `VeloraSecondaryButton` (selected state, test tag).
3. `VeloraSectionCard` (eyebrow / title / subtitle / optional header action).
4. `VeloraEmptyState` (icon, title, subtitle, optional action).
5. `VeloraLoadingSkeleton` (3-row placeholder with label).
6. `VeloraErrorBanner` (message + action).
7. `VeloraSeekBar` (drag-local state, commits seek on release, clamps via `PlaybackQueue`).
8. `VeloraTransportControls` (see §2.7.7).
9. `TrackRowItem` (icon tile, title/artist/album, format + sample-rate + duration badges,
   favorite, add-to-queue, add-to-playlist dropdown).
10. `MiniPlayerBar` (see §2.1.2; renders nothing when no track is loaded).

`VeloraVideoSurface.kt` (140 lines):
11. `VeloraVideoSurface` (Media3 `PlayerView` in `AndroidView`, aspect resize modes) and the
    shared gesture modifier `veloraCinemaGestures` (tap / double-tap / vertical-drag, §2.6.4).

`VeloraSpectrumCanvas.kt` (156 lines):
12. `SpectrumAnalyzerCanvas` (spectrum bars + EQ curve overlay, visualizer modes).

Theme (`ui/theme/`, 398 lines total): `Color.kt` (palettes + `PhiTokens` spacing/radius tokens,
`Color.kt:77`), `Theme.kt` (`VeloraTheme`, shapes from `PhiTokens`), `Type.kt` (Vazirmatn for
Persian, SpaceGrotesk/Inter/JetBrainsMono for Latin — `Type.kt:11,41-46`).

---

## 3. UI ↔ logic state boundary map

### 3.1 How each screen gets its state TODAY

| Consumer | State source today | Notes |
|---|---|---|
| All screens | Single `FazoraUiState` (`VeloraUiState`) object, collected once in the shell | `VeloraShellScreen.kt:72`: `viewModel.uiState.collectAsStateWithLifecycle()`; the whole object is passed down through `VeloraNavGraph` (`NavGraph.kt:112-115`) into every screen. |
| All screens | `PlaybackPositionState` (narrow: position/duration/isPlaying) | Separate flow `viewModel.playbackPosition` (`VeloraViewModel.kt:79-91`); passed alongside `uiState` into Video and Player routes. Default parameter values re-derive it from `uiState.engineState` when not supplied (`NavGraph.kt:116-121`, `VideoPlayerScreen.kt:93-97`, `NowPlayingScreen.kt:80-84`). |
| Video screens | **Raw `PlayerEngine`** | `NavGraph.kt:184` passes `viewModel.playerEngine` (a `public val`, `VeloraViewModel.kt:55`) into `VideoPlayerScreen`; it is forwarded into `FullscreenVideoDialog` and `VeloraVideoSurface`, which downcasts it to `Media3PlayerEngine` to unwrap the raw `ExoPlayer` (`VeloraVideoSurface.kt:35`). |
| All screens | Local `remember {}` UI state | Dialog visibility, `isFullscreen`, `controlsVisible`, `controlsLocked`, gesture HUD text, text-field contents (folder path, playlist name, copilot input, backup JSON), selected genre, quick-filter query, Hi-Res toggle — all local to the composable, not in the ViewModel. |
| All screens | Actions = ViewModel method references / extension functions | Screens receive ~15–35 lambda callbacks each; NavGraph wires them to `VeloraViewModel` methods and to extension functions in `VeloraUiStateAssembler.kt` / `VeloraRadioAndDspExtensions.kt` (e.g. `setExperienceTier`, `playSmartPlaylist`, `executeCopilotCommand`). |
| Settings / tier | `settingsRepository.settingsFlow` → `AppSettings` inside `VeloraUiState` | `VeloraSettingsRepository.kt:255` derives `isSimpleMode` from the tier (`resolvedTier == SIMPLE`); `AppSettings` also carries a redundant `experienceTier` field defaulted from `isSimpleMode` (`PlaylistAndSettings.kt:106-107`). |
| Search | `_searchQuery` StateFlow + 250 ms debounce + `flatMapLatest` repo search | `VeloraViewModel.kt:106-114`. |
| Position tick | Engine-side 100 ms ticker | `Media3PlayerEngine.kt:475-488` (`delay(100L)` while playing, `syncStateFromPlayer` updates the whole `PlaybackState`). See §3.2. |
| Background playback | `VeloraPlaybackService` (MediaSessionService) | Started from the engine on playback (`Media3PlayerEngine.kt:277-283`); session callback validates URIs (`VeloraPlaybackService.kt:46-59`). |

### 3.2 The 10 Hz tick — what actually recomposes

The prompt's claim ("10 Hz tick recomputes the full UiState") is **stale at this HEAD**:
- The engine ticker still runs at 10 Hz (`Media3PlayerEngine.kt:483`, `delay(100L)`), and each
  tick updates the full `PlaybackState` object (`Media3PlayerEngine.kt:471-473`).
- BUT the ViewModel deliberately strips position from the structural flow:
  `VeloraViewModel.kt:94-97` — `structuralOrchestratorFlow` uses `distinctUntilChanged` with
  `positionMs` zeroed out on both sides, so position-only changes do **not** re-emit the
  20-input `combine` that builds `VeloraUiState` (`VeloraViewModel.kt:106-130`).
- Position reaches the UI only through the narrow `playbackPosition` flow
  (`VeloraViewModel.kt:79-91`, itself `distinctUntilChanged`), consumed by the seek bars,
  transport controls, MiniPlayerBar, lyrics highlighting and subtitle lookup.
- Residual cost (unmeasured): the ticker still allocates/updates a full `PlaybackState` 10×/s
  and the orchestrator `combine` still re-runs its mapping on every tick even if the
  downstream distinct-filter drops it — the filter is after the combine
  (`MediaOrchestrator.kt:94-108`).

### 3.3 Where each screen SHOULD get its state in the new UI (Phase 1/2 target)

| Screen | Target state source |
|---|---|
| All screens | Narrow, per-screen UiState slices (not the whole `VeloraUiState`): e.g. `LibraryUiState` (tracks, playlists, history, scan progress), `PlayerUiState` (track, transport, queue), `VideoUiState` (track, cinema settings, aspect/speed). |
| Video screens | **No raw engine.** A `VideoSurfaceState` carrying only a surface token / player handle owned by the service layer — `VeloraVideoSurface` must not downcast to `Media3PlayerEngine` nor touch `ExoPlayer` directly. |
| Position consumers (seek bar, MiniPlayerBar, lyrics, subtitles) | The existing narrow `PlaybackPositionState` flow only — keep the position-stripping guard from §3.2 as an explicit rule. |
| Gestures | One gesture implementation (already the case — §4.3) driven by a single `GestureConfig` (seek step, brightness/volume enable flags) instead of per-call-site no-op lambdas. |
| Tier gating | One source of truth: `ExperienceTier` only. Delete the parallel `isSimpleMode` boolean path (§5.1, §5.2). |
| Dialog / ephemeral UI state | Keep local `remember {}` for pure UI state (dialog open, text fields), but hoist anything that must survive rotation/process death (fullscreen, controls-locked) into saved state / ViewModel. |
| Background-video-audio setting | Either wire it to a real engine behaviour or delete the toggle — today it is write-only (§5.8). |

---

## 4. Prompt-vs-code contradictions (code is the reference)

| # | Prompt claim | Reality at HEAD `19e70bc` |
|---|---|---|
| C1 | "`PlayerAndEqScreen.kt` ~1,200 lines" and "`VideoPlayerScreen.kt` ~1,100–1,200 lines" (§2) | **No `PlayerAndEqScreen.kt` file exists.** The player composable `PlayerAndEqScreen` lives in `NowPlayingScreen.kt` (460 lines total, incl. stage card). `VideoPlayerScreen.kt` is **337** lines; its dialogs are a separate `VideoPlayerDialogs.kt` (458). The largest screen file is `LibraryScreen.kt` (473). The "1,200-line single-composable screen" problem does not exist at this HEAD — screens are already split into section/dialog files, though `NowPlayingScreen.kt` + `PlayerLyricsAndDialogs.kt` + `EqualizerAndDspScreen.kt` together are 1,205 lines for one route. |
| C2 | "Gesture layer copy-pasted 3× (~150 lines), versions diverged — one respects `controlsLocked`, another doesn't" (§2) | **Stale.** There is exactly **one** gesture implementation: `Modifier.veloraCinemaGestures` (`VeloraVideoSurface.kt:72-140`), called from 3 sites: inline video (`VideoPlayerScreen.kt:222`), fullscreen dialog (`VideoPlayerDialogs.kt:242`), spectrum canvas (`NowPlayingScreen.kt:339`). All three pass `controlsLocked` through. The remaining divergence is per-call-site wiring, not copied code (§5.4). |
| C3 | "Simple-Mode gate is dead: live `if (!isSimpleMode)` diverges from the tested rule (`FullscreenPolicy.kt:42`)" (§2) | **Stale / inverted.** The 3-tier policy (`FullscreenPolicy.isVideoControlVisible` / `isMusicControlVisible`, `FullscreenPolicy.kt:54-97`) **is** the live gate and is used by every screen. The legacy boolean overload `isProControlVisible` (`FullscreenPolicy.kt:44-47`, which maps non-simple → PRO) still exists but has **zero callers** (verified by grep). The live problem is the dual boolean/tier state, not a dead gate (§5.1, §5.2). |
| C4 | "10 Hz position tick triggers full UiState recompute" (§2) | **Mitigated, not gone.** See §3.2: position is stripped from the structural flow; only the narrow `playbackPosition` flow re-emits. The engine ticker itself is still 10 Hz and still rewrites a full `PlaybackState` per tick. |
| C5 | "ViewModel builds a second orchestrator and hands raw ExoPlayer to the UI" (§2) | **Half true.** The ViewModel does own a `MediaOrchestrator` (`VeloraViewModel.kt:58`) and exposes the raw engine (`playerEngine`, `VeloraViewModel.kt:55`) — but the UI never receives a raw `ExoPlayer` object directly; it receives the `PlayerEngine` interface and only `VeloraVideoSurface` downcasts it to unwrap ExoPlayer (`VeloraVideoSurface.kt:35`). The boundary violation is real but confined to one component. |
| C6 | Base commit `8d13bfb` | HEAD is `19e70bc` ("refactor(settings): introduce experience tiers"). The tier system in §2 is from this newer HEAD; `AppSettings` defaults are `isSimpleMode = true` (`PlaylistAndSettings.kt:106`), i.e. the default tier is SIMPLE. |
| C7 | "Each screen ≤ ~300 lines in the rebuild" (§5) as a contrast to today | Closest today: `RadioScreen.kt` 213, `SearchScreen.kt` 232, `EqualizerAndDspScreen.kt` 285 — but per *route* the player is 1,205 lines across 3 files and video is 795 across 2. The rebuild's per-route budget should be stated per route, not per file. |

---

## 5. Known issues visible in code (file:line evidence)

### 5.1 Dual tier state: `isSimpleMode` boolean + `ExperienceTier` (source of divergence risk)
- `AppSettings` carries both (`PlaylistAndSettings.kt:106-107`): `isSimpleMode: Boolean = true`
  and `experienceTier: ExperienceTier = if (isSimpleMode) SIMPLE else PRO` — note the default
  mapping collapses STANDARD to PRO.
- The settings repository re-derives the boolean from the tier
  (`VeloraSettingsRepository.kt:255`: `isSimpleMode = resolvedTier == SIMPLE`), so the two
  can only agree if every writer updates the tier; `setSimpleMode` (`VeloraViewModel.kt:213`)
  and `setExperienceTier` (`VeloraUiStateAssembler.kt:92-101`) are two separate write paths.
- `PlayerAndEqScreen`'s default `onSetExperienceTier` falls back to collapsing a tier into a
  boolean (`NowPlayingScreen.kt:119-121`); the same fallback exists in `SettingsScreen`
  (`SettingsScreen.kt:120-122`).

### 5.2 Dead tier-override branch in the video screen
- `VideoPlayerScreen.kt:114-118`:
  `val tier = if (isSimpleMode && uiState.settings.experienceTier == PRO) SIMPLE else …`
  This branch can never fire: `isSimpleMode` is derived from the tier, so `isSimpleMode == true`
  implies `tier == SIMPLE`, never PRO. The `isSimpleMode` parameter itself
  (`VideoPlayerScreen.kt:110`, passed from `NavGraph.kt:230`) is redundant — the tier alone
  decides. Remove the parameter and the branch.

### 5.3 Legacy boolean policy overload is dead but misleading
- `FullscreenPolicy.kt:44-47` — `isProControlVisible(isSimpleMode, group)` maps any
  non-simple tier to PRO (STANDARD would get PRO controls). Zero callers. It should be
  deleted, not "fixed", so nobody re-adopts it.

### 5.4 Gesture wiring divergences (per call-site, not copied code)
- `NowPlayingScreen.kt:339-356` — the spectrum canvas wires `veloraCinemaGestures` with
  `controlsLocked = false` (hardcoded), `onToggleControls = {}`, `onSetBrightness = {}`:
  the brightness vertical-drag gesture is a **silent no-op** on the music screen and tap does
  nothing, while the same gestures work on video. Double-tap seek and volume drag do work.
- `VeloraVideoSurface.kt:72-73` — `pointerInput(controlsLocked, positionMs, durationMs)`:
  `positionMs` changes 10×/s, so the tap-gesture `pointerInput` block **restarts on every
  position tick** (same for the drag block via `currentBrightness`/`currentVolume` keys).
  A double-tap straddling a tick boundary can be dropped. Keys should be stable
  (e.g. `controlsLocked` only, reading position from a remembered state).
- Fullscreen dialog passes the same ticking keys (`VideoPlayerDialogs.kt:242-256`).

### 5.5 Fullscreen orientation lock uses the aspect MODE, not the video's native ratio
- `VideoPlayerDialogs.kt:203-207`: `nativeRatio` is derived from `engine.videoAspectMode`
  (GOLDEN_PHI → φ, everything else → 16/9), then `FullscreenPolicy.shouldLockLandscape(nativeRatio)`
  locks landscape for any ratio > 1.0. A portrait video in FIT/FILL/SIXTEEN_NINE mode therefore
  still locks the device to landscape. The policy function is correct; its input is wrong —
  it needs the actual video dimensions.
- Fullscreen is a Compose `Dialog`, and `isFullscreen` / `controlsVisible` / `controlsLocked`
  are plain `remember {}` state (`VideoPlayerScreen.kt:131-136`): rotating the device or a
  process-death loses fullscreen/lock state. No `rememberSaveable` / ViewModel hoisting.

### 5.6 Raw player reaches the UI layer
- `VeloraVideoSurface.kt:35`: `val exoPlayer = (playerEngine as? Media3PlayerEngine)?.exoPlayer ?: return`
  — the UI component downcasts the domain `PlayerEngine` interface to the data-layer concrete
  class and unwraps ExoPlayer. If the cast fails the surface silently renders nothing
  (`?: return`). `NavGraph.kt:184` / `VideoPlayerScreen.kt:87` thread the engine through as a
  parameter. The new UI must receive a surface/player handle that is not a downcast.

### 5.7 Duplicated permission-request arrays (4 copies)
- The same `READ_MEDIA_AUDIO / READ_MEDIA_VIDEO / POST_NOTIFICATIONS` (Tiramisu+) vs
  `READ_EXTERNAL_STORAGE` array is built independently in `LibraryScreen.kt:103-114`,
  `MusicAndArtistScreens.kt:78-88`, `VideoPlayerScreen.kt:141-147`, and
  `VeloraShellScreen.kt:87-95`. One shared helper would remove the drift risk.

### 5.8 `backgroundVideoAudio` setting is write-only (unverified effect)
- The toggle exists (`SettingsScreen.kt:210`) and its setter only persists the preference and
  posts a banner (`VeloraUiStateAssembler.kt:144-155`). No consumer of
  `backgroundVideoAudioEnabled` was found in `Media3PlayerEngine.kt` or
  `VeloraPlaybackService.kt`. Background audio works in practice via the foreground
  `MediaSessionService` regardless of the toggle — so the toggle's observable effect is
  **unknown**; either wire it (e.g. pause video audio when backgrounded if off) or remove it.

### 5.9 Smart-rule builder collects fields the save path ignores
- `LibraryDialogs.kt:270-276`: the dialog collects rule name, keyword, min sample rate and a
  favorites-only checkbox, but calls `onSaveRule(name, 0, sr, 0, favoritesOnly, keyword)` —
  bitrate and min-plays are hardcoded `0`, and the min-sample-rate field falls back to 44100
  on bad input. The "min bitrate / min plays" criteria the rule model supports are unreachable.

### 5.10 LRC Studio only stamps the first 4 lines
- `PlayerLyricsAndDialogs.kt:434`: `lyrics.take(4).forEachIndexed` — the timestamp studio can
  only stamp lines 0–3; longer lyrics cannot be stamped beyond the fourth line.

### 5.11 No cover art anywhere; no image loading in the UI
- `TrackRowItem` (`VeloraSharedComponents.kt:551+`) shows a MusicNote/Movie icon tile; the
  Now Playing stage shows the spectrum canvas instead of artwork. No Coil/image pipeline is
  referenced from any screen. (Not a bug — a scope note for Phase 1: decide whether artwork
  is in scope; the rebuild prompt's approved library list includes Coil 3.)

### 5.12 Minor
- `MiniPlayerBar` has play/pause + next only — no previous button
  (`VeloraSharedComponents.kt:691+`).
- Video speed chips are 0.5/1.0/1.25/1.5/2.0 — no 0.75× step
  (`VideoPlayerDialogs.kt:137`).
- `VeloraUiState` carries both `engineState: EnginePlaybackState` and
  `playbackState: PlaybackState` (`VeloraUiStateAssembler.kt:55-88`) — two overlapping
  playback snapshots in one state object.
- Music quick-filter recomputes on every keystroke with no debounce
  (`MusicAndArtistScreens.kt:96-105`); harmless (in-memory filter) but inconsistent with the
  250 ms debounced global search.
- `VideoPlayerScreen` error banner's "Retry" action is just toggle-play/pause
  (`VideoPlayerScreen.kt:174-180`).

---

## 6. Unknowns (cannot be settled from code alone — need a device/emulator)

1. **Screenshots "before" (light + dark): NOT POSSIBLE HERE.** No emulator, no device, no
   Gradle run on this machine. `screenshots/before/` does not exist and no screenshot was
   taken. This is the Phase 0 blocker for §3 of the rebuild prompt — it needs a machine with
   an emulator/device.
2. **Background video audio (requirement 4):** does video audio keep playing when the app is
   backgrounded, and does the `backgroundVideoAudio` toggle change anything? Code suggests
   the service keeps it alive; the toggle's effect is unverified (§5.8).
3. **PiP:** `enterPictureInPictureMode` is called with a fixed 16:9 ratio (`NavGraph.kt:200-210`);
   whether PiP actually engages (manifest `supportsPictureInPicture`, activity config) was not
   verified — manifest not in audit scope.
4. **Fullscreen rotation mid-playback:** the Dialog + `remember {}` state (§5.5) — untested
   whether rotation keeps the video playing, keeps position, and restores the lock.
5. **EQ/DSP audibility:** whether band gains, preamp, bass boost, stereo width and crossfade
   observably change the audio (engine wiring is outside the UI layer).
6. **Radio streams:** built-in station catalogue + custom URL entry; stream reliability,
   buffering UI (there is no buffering indicator in `RadioScreen`) and failure behaviour
   untested.
7. **Demo samples:** `includeDemoSamples` / `purgeDemoSamples` behaviour and whether sample
   tracks are actually bundled — no media files exist in the repo.
8. **Google sign-in / backup restore:** Firebase auth and JSON backup round-trip untested.
9. **Visual verification of all tiers:** SIMPLE/STANDARD/PRO rendering, dark/light themes,
   Persian RTL layout, touch-target sizes (≥48dp), and contrast — code uses
   `minimumInteractiveComponentSize` in most places, but nothing was rendered.
10. **Plugin system:** declarative plugins list is a registry default
    (`VeloraUiStateAssembler.kt:68`); whether any real plugin exists and what toggling one does
    is unknown.
11. **Copilot commands:** the supported command set is implemented in the ViewModel extension
    (`VeloraUiStateAssembler.kt:205+`); the full command list was not catalogued here.
12. **Smart playlists / analytics:** rule evaluation (which tracks land in which mix) is domain
    logic, not UI — the UI only displays the results.

---

## 7. Environment honesty

- **No emulator / device:** none available on this machine.
- **No Gradle run:** no build, no detekt, no tests executed.
- **No real media test files:** no MP3/FLAC/MP4/MKV/SRT/corrupt files in the repo or on this
  machine.
- **No screenshots:** `screenshots/before/` was not created; nothing was rendered. Every
  visual claim in this document is derived from reading Compose code, not from observation.
- **No test was passed or failed** in this phase; nothing in this file asserts runtime
  behaviour beyond what the code states.

---

## 8. Audit checklist for Phase 3 (traceability)

- [ ] §2.1–§2.9: all **120** controls present in the new UI (or explicitly re-homed with a
      note in `UI-REBUILD-LOG.md`).
- [ ] §5.1–§5.3: single `ExperienceTier` source of truth; dead boolean overload and dead
      video-tier branch deleted.
- [ ] §5.4: one gesture implementation with stable `pointerInput` keys; no per-screen no-op
      wiring (brightness on the music screen either works or is removed).
- [ ] §5.5: fullscreen orientation lock uses the video's actual dimensions; fullscreen/lock
      state survives rotation.
- [ ] §5.6: no raw `PlayerEngine`/`ExoPlayer` reaches UI composables.
- [ ] §3.2: narrow position flow kept; no full-UiState recompute on the position tick
      (regression test: position tick must not re-emit the structural UiState).
- [ ] §5.8: `backgroundVideoAudio` toggle either wired to real behaviour or removed.
- [ ] §6.1: `screenshots/before/` captured (light + dark, all 8 routes) before any file is
      deleted in Phase 2.
