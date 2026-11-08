# Changelog

All notable changes to the **VELORA** (*Beyond Playback.*) project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Fixed
- **Release Pipeline Signing Fail-Fast & R8 Verification (`.github/workflows/release.yml`, `app/proguard-rules.pro`):** Changed missing release signing secrets (`RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) from a non-fatal `::warning::` to an immediate `::error::` + `exit 1` fail-fast gate before `assembleRelease`, rotated compromised upload key material, and verified R8 release minification (`assembleRelease`).

---

## [2.3.0] - 2026-11-08

### Added
- **Single Version Source of Truth (`VERSION` & `gradle.properties`):** Unified `versionName = "2.3.0"` (`versionCode = 6`) across `app/build.gradle.kts`, `.github/workflows/release.yml`, `README.md`, and `CHANGELOG.md`.
- **ADR-0002 (`docs/decisions/0002-application-identity-and-namespace-retention-adr.md`):** Documented architectural decision to retain `applicationId = "com.aistudio.fazora.audiosync"` for zero-data-loss in-place APK upgrades and Firebase `google-services.json` compatibility.
- **Dynamic Material 3 Color Palette Engine (`ThemePaletteMode`):** Support for Dark Saffron Signature, Android 12+ Material You Dynamic System Palette, and High-Contrast AAA Gold (`VeloraTheme`).
- **Precision Playback Gesture Engine (`VeloraGestureEngine`):** Double-tap left/right to seek `-10s` / `+10s`, double-tap center to toggle play/pause, and vertical swipe on left/right zones for brightness and volume control.
- **Phase 2 Intelligence Suite (`VeloraPhase2Suite`):** Local listening statistics dashboard, rule-based smart playlist builder, in-app `.LRC` timestamp studio, `.M3U` / `.M3U8` import/export, and JSON snapshot backup/restore.

### Changed
- **UI & Navigation Modularization:** Extracted `VeloraNavGraph` (`NavGraph.kt`), `VeloraShellScreen`, `VideoPlayerScreen`, `SearchScreen`, and `SettingsScreen` out of monolithic files; reduced `MainActivity.kt` to 42 lines.
- **Security Hardening:** Removed tracked upload keystores, added `ci-signing.properties` template, and replaced inaccurate "SSRF" terminology with **Secure Stream Validation (`HTTPS`/`HLS`/`DASH` URL sanitization)**.

---

## [2.2.0] - 2026-11-08

### Added
- **Studio DSP Rack & Live Radio Tuner (`VeloraRadioAndDspSuite`):** Preamp gain (`-12 dB` to `+12 dB`), Sub-Bass Boost (`0–100%`), 3D Stereo Width (`0–200%`), A-B Loop controller, Smart Sleep Timer, and curated Persian & World radio streams.

---

## [2.1.0] - 2026-11-07

### Added
- **In-Place APK Upgrade Compatibility:** Standardized debug and release signing configurations for seamless over-the-air updates without uninstalling.

---

## [2.0.0] - 2026-11-07

### Added
- **VELORA Brand & Dark Saffron UI/UX Transformation:**
  - Complete visual transformation to the **Dark Saffron & Champagne Gold** design system (`#0B0B0D` Obsidian, `#121216` Secondary Background, `#19191F` Elevated Surface, `#202027` Card Surface, `#E5A817` Primary Saffron, `#C88712` Deep Saffron, `#F5C451` Bright Saffron, `#D9BD83` Champagne Gold, `#BDA36B` Soft Gold, `#F5F2EB` Primary Text).
  - Full bilingual English and Persian (`values-fa/strings.xml`) localization with automatic RTL layout mirroring.
- **Central Media Orchestration Layer (`com.example.orchestrator`):**
  - Created `IMediaOrchestrator` interface and `MediaOrchestrator` implementation wired via Koin (`VeloraModule.kt`).
  - Exposed reactive `StateFlow<PlaybackState>`, `StateFlow<MediaOrchestratorState>`, and `SharedFlow<VeloraMediaEvent>` for unidirectional UI consumption.
  - Coordinated `play(mediaItem)`, `pause()`, `resume()`, `togglePlayPause()`, `skipToNext()`, `skipToPrevious()`, `stop()`, and `seekTo(position)` across Media3 playback, Room v3 offline history, and DataStore session checkpoints.
- **VELORA Intelligence & Ecosystem Engines:**
  - `VeloraCompatibilityMatrix` & `VeloraPlatformMatrix`: Living platform capability matrix and universal format/codec inspector (`FLAC`, `WAV`, `ALAC`, `AAC`, `MP3`, `OGG`, `OPUS`, `MP4`, `MKV`, `WEBM`, `HLS .m3u8`, `MPEG-DASH .mpd`).
  - `VeloraOmniSearchEngine`: Unified local & remote stream search with prefix, substring, Levenshtein typo-tolerant matching, and semantic intent queries (`favorite`, `unfinished`, `hi-res`, `night`).
  - `VeloraSmartPlaylistEngine`: Deterministic, explainable local recommendation engine generating *Daily Saffron Mix*, *Most Played*, *Continue Listening*, *Hidden Gems & Hi-Res*, and *Favorite Collection*.
  - `VeloraLyricsAndSubtitleEngine` & `VeloraSubtitleExporter`: Synchronized lyrics and subtitle resolution with `±250ms` offset calibration and standards-compliant `SRT` and `WebVTT` export.
  - `VeloraMediaCopilot` & `VeloraPersonalMediaGraph`: Deterministic natural-language command router and artist/album/format relationship graph.
  - `TimeMachineBookmark`: Timestamped personal media bookmarks with one-tap seek restoration.
  - `VeloraDownloadValidator`: Strict security validator for `HTTPS`/`HLS`/`DASH` streams and authorized `magnet:?xt=urn:btih:` URIs with RFC-1918 SSRF and path-traversal protection.
  - `VeloraPluginRegistry` & `AdaptiveExperienceMode`: Capability-based Tier 1 declarative plugin lifecycle and context-aware experience modes (*Standard*, *Night Saffron*, *Headphone Hi-Fi*, *Driving Safe*, *Battery Saver*).
- **GitHub Engineering & Automated Release Pipeline:**
  - Added `.github/workflows/release.yml` for automated building, signing, SHA-256/MD5 checksumming, and publishing of downloadable `.apk` and `.aab` assets to GitHub Releases.
  - Added custom vector architecture and banner assets (`docs/assets/velora-banner.svg`, `docs/assets/velora-architecture.svg`), `CONTRIBUTING.md`, `SECURITY.md`, `LICENSE`, and structured GitHub Issue/PR templates.

---

## [1.0.0] - 2026-11-06

### Added
- **Phase 0 (Scaffold & Golden Ratio Design Tokens):** Clean Architecture (`domain`, `data`, `ui`), Koin DI, bundled Google Fonts (`Space Grotesk`, `Inter`, `JetBrains Mono`), and $\varphi = 1.618$ Fibonacci tokens (`8/13/21/34/55 dp`).
- **Phase 1 (Media3 Playback Engine):** `Media3PlayerEngine` and foreground `VeloraPlaybackService` (`MediaSessionService`), bilateral seek clamping (`[0, durationMs]`), volume/mute, and explicit `PlaybackStatus.ERROR` surfacing on missing or corrupt files.
- **Phase 2 (Library Scanner & Room v3 Cache):** `MediaLibraryScanner` supporting Android 13+ `MediaStore` (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`), SAF folder trees (`OpenDocumentTree`), `MediaMetadataRetriever` enrichment, strict path allowlist (`PhiDesignMetrics.isSafeMediaFolder`), deduplication by `mediaStoreId` and `uri`, offline playback history cache, and `BundledMediaSynthesizer` studio reference WAV/MP4 generators.
- **Phase 3 (Queue, Playlists, 10-Band DSP EQ & Goertzel RTA, Settings):**
  - `PlaybackQueue` honoring `QueueMode.SHUFFLE` and `RepeatMode.OFF / ALL / ONE`.
  - `TenBandBiquadAudioProcessor` real 10-band IIR peaking biquad cascade + hardware 5-band `Equalizer` millibel sync + real-time Goertzel 10-band spectrum analyzer and 64-point time-domain waveform.
  - Persistent playlists and custom 10-band EQ presets in Room.
  - Atomic `VeloraSettingsRepository` with `ReplaceFileCorruptionHandler` backup and recovery.
- **Phase 4 & 5 (Video Player, Cloud Sync & Desktop ADR):**
  - Real Media3 `PlayerView` video surface with click/drag seek, playback speed (`0.5x`–`2.0x`), aspect ratio framing (`Fit`, `Fill`, `Phi 1.618:1`, `16:9`), subtitle/audio track controls, and auto-hiding overlay.
  - Firebase Authentication (Google Sign-In via Jetpack Credential Manager) and zero-trust Firestore cloud synchronization (`VeloraCloudRepository`).
  - ADR `docs/decisions/0001-desktop-cmp-libvlc-adr.md` evaluating Compose Multiplatform Desktop with `libVLC` (`vlcj`).
