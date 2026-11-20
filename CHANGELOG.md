# Changelog

All notable changes to the **VELORA** (*Beyond Playback.*) project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.6.0] - 2026-11-19

### Added
- **VELORA Hybrid UI (Stages 2–5):** Added Apple Music–style `HomeScreen` discovery (`Listen Now`, `Top Mixes`, `Made for You`, `Recently Played`, `Featured Radio`), consolidated `LibraryHubScreen` (`Collections/Playlists`, `Music`, `Artists`, `Radio`), glassy `MiniPlayerBar`, and 1-tap Queue & Synced Lyrics `ModalBottomSheet`s with a φ-capped artwork stage (`<= 0.618f`).
- **Wide / Desktop Adaptive Shell (`>= 600.dp`):** Added auto-collapsing `VeloraDesktopSidebar`, `DesktopPlayerBar`, and global keyboard shortcuts (`Space`, `←/→`, `↑/↓`, `M`, `F`).
- **Architecture & Review Docs:** Added P2b Desktop Playback Backend Open Decision (`docs/decisions/0003-desktop-playback-backend-and-hybrid-ui-review.md`, `VLCJ` candidate + pinned Compose Multiplatform `1.10.3` migration reference scripts) and hostile review ledger (`HYBRID-UI-INTEGRATION-REVIEW.md`).

### Changed
- **Design Tokens & Accessibility:** Upgraded muted text to WCAG AA `#8F8B83`, formalized R-C4 3-alpha tokens (`0.14f`, `0.45f`, `0.60f`), added `LocalVeloraColors` across all 4 `ThemePaletteMode` palettes, added tabular numerals (`tnum`), and applied `mixedScriptText()` BiDi isolation for mixed Persian/Latin strings.
- **Navigation & Decoupling:** Enforced strict 4-tab `SIMPLE` tier (`MUSIC`, `VIDEO`, `PLAYER`, `SETTINGS`) and 5-tab `STANDARD/PRO` mobile layout (`HOME`, `SEARCH`, `LIBRARY`, `VIDEO`, `SETTINGS`), and decoupled `FirebaseUser` from `SettingsScreen` via `CloudAccountProfile`.
- **Release Packaging:** Bumped canonical version to `2.6.0` and `versionCode` to `15` for seamless in-place updates over `2.5.3`.

### Fixed
- **Build & Test Reliability:** Upgraded KSP `2.3.5` → `2.3.6` with `-Djava.awt.headless=true` to eliminate KSP issue `#2763` (`AWT-EventQueue-0` `NullPointerException`), and isolated Robolectric Compose sandboxes / Koin teardown (`HybridUiTokensAndShortcutsTest`, `HybridUiShellRobolectricTest`, `ExampleRobolectricTest`).

## [2.3.2] - 2026-11-14

### Fixed
- **Release pipeline fingerprint verification:** Parse `keytool` and `apksigner` certificate digests consistently from stdout and stderr, preventing valid signed APKs from being rejected during publication.
- **Release metadata consistency:** Align the canonical version and Android `versionCode` for the final `2.3.2` release.

### Security
- Keep the fail-fast real-keystore requirement and certificate identity check; no ephemeral key may be used for a published APK.

## [2.3.3] - 2026-11-14

### Fixed
- **Certificate digest normalization:** Normalize `keytool` and `apksigner` certificate digests to hexadecimal before comparison and emit the normalized values for auditable CI diagnostics.

## [2.4.0] - 2026-11-14

### Changed
- **Audio Product Design System:** Added a traceable player UI/UX specification covering playback states, responsive breakpoints, φ-based spacing, accessibility, RTL and validation gates.
- **Player accessibility and RTL:** Localized output-route labels, improved Persian mode labels, exposed play/pause role and state semantics, and reserved navigation rail for tablet-width layouts.
- **Realtime DSP safety:** Kept the biquad cascade linear and applied bounded output limiting only after the full chain, with finite-value protection.

### Fixed
- **Playback lifecycle:** Added idempotent engine release, scope/listener cleanup, and bounded retry with backoff for recoverable I/O playback failures.

## [2.4.1] - 2026-11-14

### Fixed
- **Detekt release gate:** Wrapped the localized audio-route label expression for code-style compliance without changing its visual or accessibility behavior.

## [2.5.0] - 2026-11-14

### Changed
- **Magic Listening Dashboard:** Reimagined the Music home as a calm, premium one-glance dashboard with a saffron-to-obsidian hero, one-tap listening, Surprise Me discovery, compact collection stats, and a quieter library refresh action.
- **Simple mode clarity:** Replaced technical/emoji-heavy primary labels with human, localized listening language while keeping Studio Pro controls discoverable.
- **Interaction quality:** Added localized accessibility state descriptions to mini-player and track rows, preserving 48dp touch targets and active-track feedback.
- **Design system:** Documented the Magic Listening Dashboard pattern and its RTL/progressive-disclosure rules.

## [2.5.1] - 2026-11-14

### Fixed
- **Magic dashboard build:** Added the missing Compose size import required by the hero surface.

## [2.5.2] - 2026-11-15

### Changed
- **Neon Nocturne visual system:** Replaced the legacy saffron/gold visual language with the Aurora Signal palette: deep night, live cyan, violet depth and coral attention states.
- **Shell refresh:** Reframed the primary modes as `FIELD` and `LAB`, with a new signal-mark header and flatter navigation surface.
- **Now Playing refresh:** Updated player identity, status language, empty artwork treatment and Lab framing while preserving real Media3/ViewModel callbacks.
- **Design documentation:** Added the Neon Nocturne UI system specification and validation plan.

## [2.5.3] - 2026-11-15

### Changed
- **Responsive Home:** Switched primary actions to a full-width vertical stack below `420dp`, while preserving the two-column phi-weighted layout on wider phones.
- **Responsive Now Playing:** Stacked track metadata and secondary actions on compact screens, made artwork ratio-aware, and prevented visualizer/transport controls from clipping.
- **Responsive interaction surfaces:** Added independent horizontal scrolling for visualizer modes and transport controls where the available width cannot contain every control safely.
- **Release packaging:** Increased Android `versionCode` to `14` so the responsive build installs as a valid in-place update over v2.5.2.

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
