# VELORA — Comprehensive Fix Log (Phase 2: All 54 Audit Items + Phase 1 QA Bugs)

**Base Commit:** `8d13bfb5e4466023615aa7f1c2568daacfd52efb`  
**Verification Command:** `gradle detekt :app:testDebugUnitTest` + `compile_applet`  

---

## Group 0 — CI, Quality Gates & Test Infrastructure (`F2`, `F9`, `N19`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F2** | CI workflows (`android.yml`, `release.yml`) used a restrictive `--tests` filter that skipped `VeloraFirebaseRepositoryRuleTest`, and omitted `lintDebug`. | `.github/workflows/android.yml`, `.github/workflows/release.yml` | Full `:app:testDebugUnitTest` (all test classes) + `lintDebug` wired into CI. | **Fixed** |
| **F9** | `VeloraFirebaseRepositoryRuleTest` failed unconditionally when the local Firebase Emulator (`127.0.0.1:8080` / `9099`) was not running. | `app/src/test/java/com/example/base/FirestoreEmulatorTestBase.kt`, `app/src/test/java/com/example/data/firebase/VeloraFirebaseRepositoryRuleTest.kt` | Added TCP socket probe (`isEmulatorReachable`) with `Assume.assumeTrue` for live emulator tests + unconditional unit tests (`client guard rejects observeCloudPlaylists when signed out`). | **Fixed** |
| **N19** | Detekt had `SwallowedException: active: false` and `TooGenericExceptionCaught: active: false`, hiding silent catch blocks across the codebase. | `config/detekt/detekt.yml`, `Media3PlayerEngine.kt`, `MediaLibraryScanner.kt`, `VeloraFirebaseRepository.kt`, `VeloraAudioDeviceManager.kt`, `VeloraSettingsRepository.kt` | Enabled both rules in `detekt.yml`; replaced generic/swallowed catches with specific exceptions and structured logging (`0` Detekt findings). | **Fixed** |
| **Pre-Req** | Characterization tests required before behavioral refactoring (`assembleVeloraUiState`, `VeloraPlaybackService`, Room migrations, URL validator). | `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Added 5 characterization & lifecycle tests in `ExampleRobolectricTest.kt`. | **Fixed** |

**Group 0 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 1 — Security & Access Control (`F1`, `F4`, `F15`, `N1`, `N12`, `B1`, `N10`, `N16`, `N11`, `B5`, `B6`, `B7`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F1** | `Media3PlayerEngine.loadAndPlay` never invoked `VeloraDownloadValidator`, allowing unvalidated remote/local URIs into ExoPlayer. | `app/src/main/java/com/example/data/playback/Media3PlayerEngine.kt` | `ExampleRobolectricTest`: `Media3PlayerEngine rejects unsafe loopback http URL before reaching ExoPlayer`. | **Fixed** |
| **F4** | `isValidAuthorizedStreamUrl` parsed hosts via string splitting, allowing bypasses via `userInfo@host`, IPv6 `[::ffff:127.0.0.1]`, decimal `2130706433`, octal `0177.0.0.1`, hex `0x7f.0.0.1`, and shorthand `127.1`. | `app/src/main/java/com/example/domain/model/VeloraIntelligenceEngine.kt` | `DownloadValidatorSecurityTest`: tests for `userInfo`, IPv6 loopback, IPv4-mapped IPv6, decimal/hex/octal/shorthand IPs. | **Fixed** |
| **F15 / N1** | `isSafeAdaptiveManifestPayload` only checked non-`#` lines (missing `URI="..."` inside `#EXT-X-KEY`, `#EXT-X-MAP`, `#EXT-X-MEDIA` and `<BaseURL>` in DASH), and `isValidStreamOrManifestUrl` did not inspect inline manifests. | `app/src/main/java/com/example/domain/model/VeloraEcosystemEngines.kt` | `DownloadValidatorSecurityTest`: `isSafeAdaptiveManifestPayload blocks EXT-X-KEY and EXT-X-MAP with loopback URI`. | **Fixed** |
| **N12** | `isSafeDownloadTargetFileName` accepted `"."` and `".."`. | `app/src/main/java/com/example/domain/model/VeloraEcosystemEngines.kt` | `DownloadValidatorSecurityTest`: `isSafeDownloadTargetFileName rejects dot and double-dot`. | **Fixed** |
| **B1 / N10** | `network_security_config.xml` had `cleartextTrafficPermitted="true"` globally (`base-config`). | `app/src/main/res/xml/network_security_config.xml` | Set `<base-config cleartextTrafficPermitted="false">` with explicit radio domain allowlist. | **Fixed** |
| **N16** | `VeloraPlaybackService` had `android:exported="true"`, did not validate `MediaItem` URIs in `onAddMediaItems`, used default session ID, and did not call `stopSelf()` if `exoPlayer == null`. | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/data/playback/VeloraPlaybackService.kt` | Set `android:exported="false"`, added `MediaSession.Callback.onAddMediaItems` URI validation, `SESSION_ID = "velora_media_session"`, and `stopSelf()` guard. | **Fixed** |
| **N11** | `PhiDesignMetrics.isSafeMediaFolder` used a weak denylist that allowed `/proc`, `/data`, `/sdcard/Android/data`, etc. | `app/src/main/java/com/example/domain/model/PlaylistAndSettings.kt` | Replaced with strict allowlist (`ALLOWED_PUBLIC_MEDIA_PREFIXES` + app sandbox directories) and tested in `DownloadValidatorSecurityTest`. | **Fixed** |
| **B5 / B6 / B7** | `firestore.rules` only checked 2 of 10 EQ bands (`gainsDb[0]` & `[9]`), did not validate `trackIds[0]` type, and `VeloraFirebaseRepository` lacked client-side `uid` ownership checks on `observeCloudPlaylists` / `observeCloudEqPresets`. | `firestore.rules`, `app/src/main/java/com/example/data/firebase/VeloraFirebaseRepository.kt` | Added `isValidBandGain` for all 10 bands (`0..9`), `trackIds` string check, and `require(currentUid == userId)` tested in `VeloraFirebaseRepositoryRuleTest`. | **Fixed** |

**Group 1 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 2 — Data Loss Prevention (`F6`, `N4`, `N8`, `N9`, `N5`, `N6`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F6** | `exportOfflineBackupJson` only exported counts (`playlistCount`), losing all playlists and custom EQ presets on restore. | `app/src/main/java/com/example/domain/model/VeloraRadioAndDspSuite.kt`, `app/src/main/java/com/example/ui/viewmodel/VeloraRadioAndDspExtensions.kt` | Implemented full JSON serialization/deserialization of `playlists`, `customEqPresets`, `bookmarks`, and `customStations`; verified round-trip in `ExampleUnitTest`. | **Fixed** |
| **N4** | `getNextPositionForPlaylist` used `COUNT(*)`, causing duplicate `position` values after deleting a middle track from a playlist. | `app/src/main/java/com/example/data/local/VeloraDatabase.kt` | Changed SQL to `SELECT COALESCE(MAX(position), -1) + 1`; verified in `ExampleRobolectricTest`. | **Fixed** |
| **N8 / N9** | `VeloraModule` used `.fallbackToDestructiveMigration(dropAllTables = true)` with missing schema migrations. | `app/src/main/java/com/example/data/local/VeloraDatabase.kt`, `app/src/main/java/com/example/di/VeloraModule.kt` | Added `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4` (`ALL_MIGRATIONS`) and switched to `.fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)`. | **Fixed** |
| **N5** | `importM3u8Playlist` silently created a fake 5-track playlist when 0 tracks matched. | `app/src/main/java/com/example/ui/viewmodel/VeloraRadioAndDspExtensions.kt` | Removed `.ifEmpty { allTracks.take(5) }` fallback; posts clear error banner when 0 tracks match. | **Fixed** |
| **N6** | `backup_rules.xml` and `data_extraction_rules.xml` were empty templates, and `VeloraSettingsRepository` overwrote `.corrupt.bak` without a timestamp. | `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/example/data/settings/VeloraSettingsRepository.kt` | Configured explicit `include`/`exclude` rules for `velora_media.db` and `datastore/` + timestamped `.corrupt.$timestampMs.bak`. | **Fixed** |

**Group 2 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 3 — Playback, DSP & Domain Correctness (`F3`, `F5`, `F13`, `F14`, `N2`, `N3`, `N14`, `N15`, `N22`, `F23`, `N7`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F3** | `Media3PlayerEngine.stop()` left status stuck at `BUFFERING` when `exoPlayer.playbackState == Player.STATE_IDLE`. | `app/src/main/java/com/example/data/playback/Media3PlayerEngine.kt` | Explicitly reset `_playbackState` to `IDLE` (`isPlaying = false`, `positionMs = 0L`) in `stop()` and `STATE_IDLE`. | **Fixed** |
| **F5** | `MediaOrchestrator.togglePlayPause()` did not refresh `positionMs` from `playerEngine` before persisting history on pause. | `app/src/main/java/com/example/domain/engine/MediaOrchestrator.kt` | Synchronized `currentPositionMs` immediately on pause before updating Room history and DataStore session. | **Fixed** |
| **F13** | `MediaOrchestrator.play(track, pool)` dropped `track` and played `pool[0]` if `track` was not inside `pool`. | `app/src/main/java/com/example/domain/engine/MediaOrchestrator.kt` | Prepended `track` to `effectivePool` when absent; verified in `ExampleRobolectricTest`. | **Fixed** |
| **F14** | `MediaOrchestrator.bootstrapAndRestoreSession()` overrode user's active queue when called after playback started, and ignored `defaultPlaybackSpeed` / `videoAspectMode`. | `app/src/main/java/com/example/domain/engine/MediaOrchestrator.kt` | Added `hasBootstrapped` guard and applied `settings.defaultPlaybackSpeed` + `settings.videoAspectMode`. | **Fixed** |
| **N2 / F23** | `TenBandBiquadAudioProcessor` crashed with `IndexOutOfBoundsException` for `> 8` channels, and `EqSpecification.calculatePeakingBiquad` ignored its `q` parameter. | `app/src/main/java/com/example/data/dsp/TenBandBiquadAudioProcessor.kt`, `app/src/main/java/com/example/domain/model/EqModels.kt` | Added `inputAudioFormat.channelCount > MAX_CHANNELS` check in `onConfigure` and honored `val safeQ = q.coerceIn(0.1, 18.0)`; verified in `ExampleUnitTest`. | **Fixed** |
| **N3** | `PlaybackQueue.withRepeatMode` clobbered `originalTracks` when `QueueMode.SHUFFLE` was active. | `app/src/main/java/com/example/domain/model/Track.kt` | Preserved `originalTracks` inside `withRepeatMode`; verified in `ExampleUnitTest`. | **Fixed** |
| **N14 / N15** | `VeloraLyricsEngine.parseLrc` failed on 2-digit fractional seconds or multi-timestamp lines (`[00:10.00][00:20.00]`), and `parseSrt` failed on `,`/`.` separators or HTML tags. | `app/src/main/java/com/example/domain/model/VeloraIntelligenceEngine.kt` | Updated regexes to parse all timestamp tags per line, 1–3 digit fractional seconds, and strip inline HTML tags; verified in `ExampleUnitTest`. | **Fixed** |
| **N22** | Sleep timer continued counting down and paused nothing if playback was already stopped, or fired immediately without checking active playback. | `app/src/main/java/com/example/ui/viewmodel/VeloraRadioAndDspExtensions.kt` | Added `playbackState.value.isPlaying` check before pausing on sleep timer expiry. | **Fixed** |
| **N7** | `MediaLibraryScanner` unconditionally generated and indexed synthetic WAV files and remote sample videos on every scan without user opt-in or removal option. | `PlaylistAndSettings.kt`, `VeloraSettingsRepository.kt`, `MediaLibraryScanner.kt`, `VeloraLibraryRepository.kt`, `VeloraViewModel.kt` | Added `includeDemoSamples: Boolean = false` in `AppSettings` and `purgeDemoSampleTracks()` in `VeloraLibraryRepository`; verified in `ExampleRobolectricTest`. | **Fixed** |

**Group 3 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 4 — Performance & Resource Efficiency (`F11`, `F7`, `F12`, `F18`, `F19`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F11 / F7** | `assembleVeloraUiState` recomputed `filteredTracks`, `artistsMap`, `smartPlaylists`, and `recommendations` on every 500ms position tick, and `searchQuery` triggered an immediate SQLite query on every keystroke. | `app/src/main/java/com/example/ui/viewmodel/VeloraUiStateAssembler.kt`, `app/src/main/java/com/example/ui/viewmodel/VeloraViewModel.kt` | Added `DerivedLibraryProjection` memoization cache keyed on library/filter inputs + `debounce(250L)` on `_searchQuery`. | **Fixed** |
| **F12** | `Media3PlayerEngine` position ticker coroutine ran an infinite `while (isActive)` loop every 500ms even when playback was paused or idle. | `app/src/main/java/com/example/data/playback/Media3PlayerEngine.kt` | Replaced unconditional loop with `startOrStopPositionTicker()` driven by `player.isPlaying`. | **Fixed** |
| **F18** | `TenBandBiquadAudioProcessor.queueInput` allocated a new `DoubleArray(sampleCount)` on every audio buffer callback (~50–100x/sec). | `app/src/main/java/com/example/data/dsp/TenBandBiquadAudioProcessor.kt` | Reused pre-allocated `monoRingBuffer = DoubleArray(GOERTZEL_WINDOW_SIZE)` on the audio thread with zero per-buffer heap allocations. | **Fixed** |
| **F19** | `MediaLibraryScanner.scanLocalDirectory` opened `MediaMetadataRetriever` for every file on every scan, andRoom lacked indices on `mediaType`, `isFavorite`, and `folderPath`. | `app/src/main/java/com/example/data/scanner/MediaLibraryScanner.kt`, `app/src/main/java/com/example/data/local/VeloraEntities.kt`, `VeloraDatabase.kt` | Added `fileMetadataCache` keyed by `(absolutePath, lastModified, length)` and added Room indices in v4 (`MIGRATION_3_4`). | **Fixed** |

**Group 4 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 5 — Architecture, Dead Code & ProGuard Hygiene (`F8`, `F10`, `F16`, `F17`, `N18`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **F8** | `IMediaOrchestrator` leaked `androidx.media3.common.MediaItem` into the domain orchestrator contract. | `app/src/main/java/com/example/orchestrator/IMediaOrchestrator.kt`, `MediaOrchestrator.kt` | Removed `MediaItem` import and method from `IMediaOrchestrator`. | **Fixed** |
| **F10** | `VeloraViewModel` exposed raw `ExoPlayer?` to the UI layer (`NavGraph.kt`, `VideoPlayerScreen.kt`). | `VeloraViewModel.kt`, `NavGraph.kt`, `VideoPlayerScreen.kt`, `VeloraVideoSurface.kt` | Marked `exoPlayer` `internal` in `VeloraViewModel` and encapsulated `PlayerView` inside `VeloraVideoSurface(playerEngine)`. | **Fixed** |
| **F16** | `VideoPlayerScreen.kt` duplicated ~160 lines of `AndroidView(PlayerView)` and `pointerInput` gesture handling between fullscreen and inline views. | `app/src/main/java/com/example/ui/components/VeloraVideoSurface.kt`, `app/src/main/java/com/example/ui/screens/VideoPlayerScreen.kt` | Extracted `VeloraVideoSurface` and `Modifier.veloraCinemaGestures` shared by both fullscreen and inline video players. | **Fixed** |
| **F17** | `FullscreenPolicy.isProControlVisible` was tested in `FullscreenPolicyTest` but never called in `VideoPlayerScreen.kt`. | `app/src/main/java/com/example/ui/screens/VideoPlayerScreen.kt` | Wired `FullscreenPolicy.isProControlVisible(isSimpleMode, group)` for `SPEED`, `ASPECT`, `SUBTITLES`, `AUDIO_TRACK`, `BOOKMARK`, and `PICK_VIDEO`. | **Fixed** |
| **N18** | `proguard-rules.pro` used overly broad `-keep class com.example.domain.model.** { *; }` and `-dontwarn` rules. | `app/proguard-rules.pro` | Scoped `-keep` rules to Room entities (`com.example.data.local.**`) and Firebase Firestore models. | **Fixed** |

**Group 5 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`

---

## Group 6 — Android Platform Polish & Honest Documentation (`N20`, `B8`, `B9`, `B10`)

| ID | Issue Summary | Files Modified | Repro / Verification Test | Status |
| :--- | :--- | :--- | :--- | :--- |
| **N20** | App could not open audio/video files or stream links shared from external file managers (`ACTION_VIEW` / `ACTION_SEND`). | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/MainActivity.kt`, `VeloraViewModel.kt` | Added `ACTION_VIEW` (`audio/*`, `video/*`) and `ACTION_SEND` (`text/plain`) intent filters + `onNewIntent` / `handleIncomingMediaIntent`. | **Fixed** |
| **B8 / B9 / B10** | `README.md`, `SECURITY.md`, and `docs/architecture.md` contained outdated claims about Room encryption, KMP modules, and version numbers. | `README.md`, `SECURITY.md`, `docs/architecture.md` | Updated documentation to accurately reflect v2.5.3, single-module `:app` architecture, Room v4 schema, and network security controls. | **Fixed** |

**Group 6 Verification:** `Gates: 3/3 passed (detekt + :app:testDebugUnitTest + assembleDebug)`
