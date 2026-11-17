# VELORA UI/UX Rebuild Log (Phase 2)

## 1. Deleted Monolithic UI Files

| Deleted File | Old Line Count | Reason for Deletion |
|---|---|---|
| `app/src/main/java/com/example/ui/screens/LibraryAndMusicScreens.kt` | 1,767 lines | Monolithic file combining Library, Music, Artists, playlists, folders, history, and raw `.dp`/hex literals. |
| `app/src/main/java/com/example/ui/screens/PlayerAndEqScreen.kt` | 1,635 lines | Monolithic file combining Now Playing, 10-band EQ, lyrics, queue, and dialogs; recomposed entire screen on 500ms position ticks. |
| `app/src/main/java/com/example/ui/screens/RadioAndDspComponents.kt` | 729 lines | Unrelated Radio tuner and Studio DSP rack bundled together. |
| `app/src/main/java/com/example/ui/screens/Phase2Components.kt` | 703 lines | Catch-all file mixing smart rules, M3U8 dialogs, LRC studio, and video subtitle overlays. |
| *(Replaced)* `VideoPlayerScreen.kt` (old) | 1,150+ lines | Replaced by modular `VideoPlayerScreen.kt` + `VideoPlayerDialogs.kt`. |
| *(Replaced)* `VeloraSharedComponents.kt` (old) | 767 lines | Replaced by modular `VeloraSharedComponents.kt` + `VeloraSpectrumCanvas.kt`. |

---

## 2. Newly Built Modular UI & Theme Files (Screen-by-Screen)

| Screen / Module | Logical Branch / PR Group | New Source File(s) | Line Count | Key Architecture & Token Improvements | Gate Status |
|---|---|---|---|---|---|
| **Design System & Theme** | `ui/design-system` | `Color.kt`, `Type.kt`, `Theme.kt`, `PlaylistAndSettings.kt`, `FullscreenPolicy.kt` | < 170 lines each | Added bundled `Vazirmatn` (`R.font.vazirmatn`) for Persian RTL; complete Light/Dark/OLED tokens; `PhiTokens` Fibonacci scale; 3-tier `ExperienceTier` (`SIMPLE`, `STANDARD`, `PRO`). | `Gates: 3/3 passed` (`detekt` + `testDebugUnitTest` + `assembleDebug`) |
| **Shared Component Library** | `ui/shared-components` | `VeloraSharedComponents.kt`, `VeloraSpectrumCanvas.kt`, `VeloraVideoSurface.kt` | < 350 lines each | 8 standardized components (`VeloraPrimaryButton`, `VeloraSecondaryButton`, `VeloraSectionCard`, `TrackRowItem`, `VeloraSeekBar`, `VeloraTransportControls`, `VeloraEmptyState`/`VeloraLoadingSkeleton`/`VeloraErrorBanner`, `MiniPlayerBar`) + single shared `Modifier.veloraCinemaGestures`. | `Gates: 3/3 passed` |
| **1. Library, Playlists & Artists** | `ui/screen-library` | `LibraryScreen.kt`, `LibraryDialogs.kt`, `MusicAndArtistScreens.kt` | < 300 lines each | Tokenized cards, `VeloraEmptyState`, animated `VeloraLoadingSkeleton` during scan, SAF file/folder import, M3U8 & Smart Rule dialogs, master-detail `ArtistScreen`. | `Gates: 3/3 passed` |
| **2. Music Player (Now Playing)** | `ui/screen-music-player` | `NowPlayingScreen.kt`, `PlayerLyricsAndDialogs.kt` | < 300 lines each | Consumes narrow `PlaybackPositionState` so 500ms ticks only update `VeloraSeekBar`; uses shared `veloraCinemaGestures`; gates Queue & Lyrics by `ExperienceTier`. | `Gates: 3/3 passed` |
| **3. Equalizer & Studio DSP** | `ui/screen-equalizer` | `EqualizerAndDspScreen.kt` | < 260 lines | 10-band Biquad EQ with auto-headroom readout, custom presets, Preamp, Sub-Bass, 3D Stereo Width, A-B Loop, Crossfade, Sleep Timer — **strictly rendered only in `ExperienceTier.PRO`**. | `Gates: 3/3 passed` |
| **4. Video Player** | `ui/screen-video-player` | `VideoPlayerScreen.kt`, `VideoPlayerDialogs.kt` | < 300 lines each | Encapsulates `ExoPlayer` inside `VeloraVideoSurface`; uses the single shared `veloraCinemaGestures`; enforces `FullscreenPolicy` orientation lock and 3-tier control visibility. | `Gates: 3/3 passed` |
| **5. OmniSearch & Copilot** | `ui/screen-search` | `SearchScreen.kt` | < 220 lines | 250ms debounced input, semantic suggestion chips, natural-language Media Copilot command bar, and `VeloraEmptyState` for zero-result queries. | `Gates: 3/3 passed` |
| **6. Live Radio Tuner** | `ui/screen-playlists-radio` | `RadioScreen.kt` | < 190 lines | Genre filter chips, live HTTP/HLS/ICY stream cards, and custom station dialog. | `Gates: 3/3 passed` |
| **7. Settings & App Shell** | `ui/screen-settings` | `SettingsScreen.kt`, `VeloraShellScreen.kt`, `NavGraph.kt` | < 300 lines each | Instant 3-Tier selector (`SIMPLE` / `STANDARD` / `PRO`), 4 theme palettes, English LTR / Persian RTL (`Vazirmatn`) toggle, JSON Backup/Restore, and Firebase Cloud Sync. | `Gates: 3/3 passed` |

---

## 3. Narrow State & Recomposition Boundary Verification

- `VeloraViewModel.playbackPosition: StateFlow<PlaybackPositionState>` emits `(positionMs, durationMs, isPlaying)` with `.distinctUntilChanged()`.
- `VeloraViewModel.structuralOrchestratorFlow` filters out position-only changes (`old.copy(engineState = old.engineState.copy(positionMs = 0L)) == new.copy(engineState = new.engineState.copy(positionMs = 0L))`) before feeding the 20-flow `uiState` combine.
- Result: 500ms playback position ticks never re-run `assembleVeloraUiState` or recompose full screen lists.
