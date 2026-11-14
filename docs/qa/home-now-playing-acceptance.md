# VELORA Home & Now Playing — Acceptance Report

**Scope:** Redesign and implementation of Home/Music and Now Playing.

**Acceptance rule:** Only `TESTED` counts as complete. `IMPLEMENTED` means the code exists and is connected to real state, but reproducible test evidence is still required.

**Repository:** `aghorbani84/Velora`

**Implementation commit:** `57e2977a7869896984156ce073630d879d6f49d8`

**Generated:** 2026-11-14

## Evidence index

- Source implementation: `app/src/main/java/com/example/ui/screens/LibraryAndMusicScreens.kt` and `PlayerAndEqScreen.kt`.
- Navigation wiring: `app/src/main/java/com/example/ui/navigation/NavGraph.kt`.
- Existing domain/repository tests: `app/src/test/java/com/example/`.
- CI run for implementation commit: [GitHub Actions run 43616992280](https://github.com/aghorbani84/Velora/actions/runs/43616992280). At report creation, the run was queued; no result is claimed here.
- Local build attempt: failed before compilation because this sandbox has no Android SDK/`ANDROID_HOME`. This is an environment limitation, not a product test result.

## Feature acceptance matrix

| Feature | Implementation Status | Test Status | Evidence | Remaining Issues |
|---|---|---|---|---|
| Home hero and one-tap Start Listening | IMPLEMENTED | IN PROGRESS | `MusicScreen` calls the real `onPlayAll` callback wired to `viewModel.playAllMusic(false)` | Must pass CI and UI interaction test |
| Home Surprise Me | IMPLEMENTED | IN PROGRESS | Real `onShuffleAll` callback wired to `viewModel.playAllMusic(true)` | Must pass CI and UI interaction test |
| Home collection statistics | IMPLEMENTED | IN PROGRESS | Counts read from `audioTracks`, `favoriteTracks`, and `playlists` in `VeloraUiState` | Visual verification at large font scale pending |
| Home Continue Listening | IMPLEMENTED | IN PROGRESS | Reads first real `playbackHistory` entry and calls `onPlayTrack` | Need UI test for empty and populated history |
| Home Recently Played | IMPLEMENTED | IN PROGRESS | Reads real `playbackHistory`, de-duplicates by track id and calls `onPlayTrack` | Need UI test for populated/empty states |
| Home Favorites | IMPLEMENTED | IN PROGRESS | Reads real `favoriteTracks` and calls `onPlayTrack` | Need UI test and RTL verification |
| Home artwork | IMPLEMENTED | IN PROGRESS | Uses real `Track.artworkUri` through Coil; deterministic headphones fallback when absent | Need device/emulator image rendering test |
| Home media list | IMPLEMENTED | IN PROGRESS | Existing real `TrackRowItem` callbacks remain connected to queue, playlists and favorites | Need scroll/overflow/large-font UI test |
| Now Playing current-track identity | IMPLEMENTED | IN PROGRESS | Uses real engine/queue current track and metadata | Need UI rendering evidence |
| Now Playing artwork and no-artwork fallback | IMPLEMENTED | IN PROGRESS | Uses real `Track.artworkUri`; fallback is a functional empty artwork state | Need emulator/device rendering evidence |
| Now Playing empty state | IMPLEMENTED | IN PROGRESS | Handles `activeTrack == null` with a localized actionable message | Need UI test confirming no crash and navigation path |
| Now Playing playback status | IMPLEMENTED | IN PROGRESS | Maps real `PlaybackStatus` values: IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR | Need state-driven UI tests |
| Play/pause | IMPLEMENTED | IN PROGRESS | Existing callback is wired to `viewModel.togglePlayPause()` | Domain coverage exists; screen-level evidence pending |
| Next/previous | IMPLEMENTED | IN PROGRESS | Existing callbacks are wired to `skipToNext()` and `skipToPrevious()` | Screen-level evidence pending |
| Seek and exact seek | IMPLEMENTED | IN PROGRESS | Existing slider and exact-seek dialog call real `viewModel.seekTo()` | Need UI test for drag and exact input |
| Queue selection/removal | IMPLEMENTED | IN PROGRESS | Existing queue rows call real `selectQueueTrack()` and `removeTrackFromQueue()` | Need UI test |
| Shuffle and repeat | IMPLEMENTED | IN PROGRESS | Existing controls call real ViewModel actions and show state | Need UI semantics/state test |
| Audio route/quality identity | IMPLEMENTED | IN PROGRESS | Existing route chip reads real `uiState.audioRoute` | Route switching itself is not exposed by this screen and is not claimed |
| Lyrics/visualizer/DSP progressive disclosure | IMPLEMENTED | IN PROGRESS | Existing Studio mode and lyrics/visualizer sections remain connected to real state | Need UI test and visual evidence |
| Loading/buffering/error states | IMPLEMENTED | IN PROGRESS | Error banner and `PlaybackStatus.BUFFERING` label exist; retry action is not part of current requested screen contract | Need explicit UI test; retry UX remains a limitation |
| Navigation and back behavior | IMPLEMENTED | NOT TESTED | Existing NavGraph route wiring is present | No emulator/browser evidence in this environment |
| Compact Android layout | IMPLEMENTED | NOT TESTED | Compose responsive primitives and horizontal rows are used | No emulator screenshot or automated UI run available |
| Large Android/tablet layout | IMPLEMENTED | NOT TESTED | Existing adaptive Compose structure is preserved | No tablet test evidence |
| Persian RTL | IMPLEMENTED | NOT TESTED | Persian branches and semantics strings are present | Must run locale/RTL UI test |
| Accessibility semantics | IMPLEMENTED | NOT TESTED | Button roles/state descriptions and minimum touch targets are present | TalkBack/semantics test evidence pending |
| Windows desktop | NOT STARTED | NOT TESTED | Repository is Android/Kotlin; no Windows target exists in this repository | Requires a separate desktop target and architecture decision |
| Resized Windows window | NOT STARTED | NOT TESTED | No Windows application target exists | Cannot test without implementing a Windows target |

## Required test status

### Automated evidence currently available

The repository contains unit/Robolectric/domain tests covering queue behavior, DSP math, settings persistence, playlist persistence, playback orchestration, search behavior and security constraints. These tests are not sufficient evidence for all screen-level acceptance items above.

The implementation commit has a GitHub Actions run, but its result must be read after completion. No success is inferred from a queued run.

### Not yet evidenced

- Compose UI rendering for Home and Now Playing.
- Screenshot/visual regression evidence.
- Screen recording of real playback interactions.
- Physical Android device verification.
- Startup time, memory, CPU and battery measurements.
- Windows desktop and resized-window verification.
- TalkBack execution and large-font visual review.

## Known limitations

1. This repository targets Android; Windows acceptance items cannot be marked complete without a separate Windows application target.
2. The local sandbox used for this change does not contain an Android SDK, so local Gradle verification stopped before compilation.
3. The current Now Playing error banner supports dismiss/clear but does not yet expose a dedicated retry action in this screen contract.
4. Real audio playback demonstration requires an Android emulator/device with media content; it has not been fabricated here.

## Exit criteria for marking this scope TESTED

- CI for the implementation commit is green, including Detekt, unit tests and APK compilation.
- Add and run Compose UI tests for populated/empty Home, populated/empty Now Playing, status labels, callbacks, RTL and semantics.
- Execute on at least one small Android device/emulator and one large/tablet configuration.
- Capture reproducible screenshots and a short recording of real play/pause, seek, next/previous and queue interaction.
- Record performance measurements and attach the build/test artifacts.
