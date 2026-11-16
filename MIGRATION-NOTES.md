# VELORA — UI Migration & Multiplatform Architecture Notes (Phase 3)

**Repository:** `aghorbani84/Velora`  
**Version:** `2.5.3`  

---

## 1. UI Layer Audit & Decoupling (`commonMain` Readiness)

During Phase 3, every file under `app/src/main/java/com/example/ui/` and `domain/` was audited for platform-specific Android or Media3 dependencies:

1. **Zero `ExoPlayer` / `MediaItem` Leakage in Screen Composables & Domain (`F8`, `F10`, `F16`):**
   - Previously, `NavGraph.kt` read `viewModel.exoPlayer` (`androidx.media3.exoplayer.ExoPlayer`) and passed it into `VideoPlayerScreen.kt`, which directly inflated `androidx.media3.ui.PlayerView` in two separate places (inline and fullscreen).
   - **Resolution:** Created `VeloraVideoSurface.kt` (`@Composable fun VeloraVideoSurface(playerEngine: PlayerEngine?, videoAspectMode: VideoAspectMode, surfaceTestTag: String, modifier: Modifier)`) and `Modifier.veloraCinemaGestures(...)`.
   - `VideoPlayerScreen.kt` and `NavGraph.kt` now depend strictly on `com.example.domain.engine.PlayerEngine` and pure domain models (`Track`, `VideoAspectMode`, `FullscreenPolicy`, `VideoControlGroup`).
   - `IMediaOrchestrator.kt` has zero Android or Media3 imports.

2. **`expect` / `actual` Platform Boundary Table:**

| Boundary Contract (`commonMain`) | Android Implementation (`androidMain`) | Desktop Implementation (`desktopMain`) |
| :--- | :--- | :--- |
| `interface PlayerEngine` | `Media3PlayerEngine` (`ExoPlayer` + `TenBandBiquadAudioProcessor` + `VeloraPlaybackService`) | `DesktopVlcjPlayerEngine` (VLCJ `EmbeddedMediaPlayer` + native PCM audio callback into `EqSpecification` 64-bit Biquad cascade) |
| `@Composable fun VeloraVideoSurface(...)` | `AndroidView` inflating `R.layout.velora_video_player_view` (`PlayerView` with `surface_type="surface_view"`) | Compose Desktop `Canvas` / `SwingPanel` rendering frames from VLCJ `CallbackMediaPlayerComponent` (`ByteBuffer` / `BufferedImage`) |
| `class VeloraSettingsRepository` | Jetpack `PreferenceDataStoreFactory` backed by `context.preferencesDataStoreFile("velora_settings")` | KMP `PreferenceDataStoreFactory.createWithPath` backed by Okio `Path` (`~/.config/velora/velora_settings.preferences_pb`) |
| `abstract class VeloraDatabase : RoomDatabase()` | `Room.databaseBuilder(context, VeloraDatabase::class.java, "velora_media.db")` | Room KMP `Room.databaseBuilder<VeloraDatabase>(name = dbFile.absolutePath).setDriver(BundledSQLiteDriver())` |
| `class MediaLibraryScanner` | `MediaStore` + SAF `DocumentFile` + `MediaMetadataRetriever` | `java.nio.file.Files.walk` + JAudioTagger / FFprobe metadata extraction |
| `class VeloraAudioDeviceManager` | `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)` + `AudioDeviceCallback` | Java Sound `AudioSystem.getMixerInfo()` |

---

## 2. Velora Design System (Light, Dark Saffron, OLED High-Contrast & Persian RTL)

### 2.1 Color Palette & WCAG 2.1 AA / APCA Verification
The design system in `ui/theme/Color.kt` and `ui/theme/Theme.kt` provides **four complete, user-selectable `ColorScheme` modes** (`ThemePaletteMode`):

1. **`VELORA_SAFFRON` (Studio Dark — Signature Default):**
   - Background Obsidian `#0B0B0D`, Surface Carbon `#121216`, Elevated `#19191F`, Card `#202027`
   - Primary Saffron `#E5A817` (9.39:1 AA contrast on Obsidian), Champagne Gold `#D9BD83` (10.74:1)
   - Primary Text Ivory `#F5F2EB` (17.46:1 AAA contrast on Obsidian), Secondary Text `#A5A29B` (7.36:1 AA)
2. **`VELORA_DAYLIGHT` (Daylight Ivory — Full Light Mode):**
   - Background Warm Ivory `#F9F6F0`, Surface Pure White `#FFFFFF`, Surface Variant `#EDE7DC`
   - Primary Deep Amber Saffron `#946200` on White (`#FFFFFF`), Primary Text `#121216`, Secondary Text `#4C4639`
3. **`HIGH_CONTRAST_MONOCHROME` (Pure OLED 21:1):**
   - True `#000000` background, `#FFFFFF` primary & text for zero OLED power draw and maximum low-vision legibility.
4. **`SYSTEM_DYNAMIC` (Material You + Mathematical Contrast Clamping):**
   - Uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+ (`API 31+`) and passes the result through `balanceDynamicColorScheme()` (`Theme.kt:94`), which verifies `PhiDesignMetrics.meetsWcagAa(primaryInt, bgInt, 4.5)` and clamps low-contrast wallpaper accents automatically.

### 2.2 Typography, Spacing & RTL Architecture
- **Bundled Local Fonts (`res/font/`):**
  - Display & Headlines: `SpaceGrotesk` (`space_grotesk_bold.ttf`, `space_grotesk_medium.ttf`)
  - Body & Bilingual UI: `PlusJakartaSans` (`plus_jakarta_sans_regular.ttf`, `plus_jakarta_sans_semibold.ttf`, `plus_jakarta_sans_bold.ttf`)
  - Technical Telemetry, Timecodes & Hz/dB Readouts: `JetBrainsMono` (`jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`, `jetbrains_mono_bold.ttf`) with tabular numerals (`tnum`).
- **Golden Ratio & Fibonacci Spacing (`PhiTokens`):**
  - Spacing scale: `8.dp`, `13.dp`, `21.dp`, `34.dp`, `55.dp`
  - Corner radii: `8.dp` (small), `13.dp` (medium), `21.dp` (large)
  - Minimum interactive touch target: `48.dp` (`Modifier.minimumInteractiveComponentSize()`)
- **Full Persian (فارسی) RTL Support:**
  - `VeloraShellScreen.kt` wraps the entire UI tree in `CompositionLocalProvider(LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr)` so navigation bars, rails, lists, and dialogs mirror naturally in Persian while timecodes and frequency graphs remain LTR.

---

## 3. Library Catalog & Toolchain Compatibility Notes

| Library in Prompt Catalog | Prompt Target Version | Repository Verified Version (`libs.versions.toml`) | Architectural Rationale |
| :--- | :--- | :--- | :--- |
| **Kotlin + Compose** | Kotlin `2.4.21` + CMP `1.12.2` | Kotlin `2.2.10` (AGP `9.1.1`) + Compose BOM `2024.09.00` | Preserves 100% compatibility with the pre-cached Android Gradle Plugin `9.1.1` and KSP `2.3.5` toolchain in CI and the build container. |
| **Image Loading (Coil)** | Coil `3.6.3` | `io.coil-kt:coil-compose:2.7.0` | Already integrated and cached in `:app`; zero network overhead for local `content://` album art URIs. |
| **Adaptive Layout** | Material 3 Adaptive `1.3.0` | Responsive `BoxWithConstraints` (`maxWidth >= 680.dp` switching between `NavigationBar` and `NavigationRail` + two-pane `ArtistScreen` & `PlayerAndEqScreen`) | Achieves canonical Compact/Expanded adaptive layouts without adding transitive experimental dependencies. |
| **EQ & Spectrum Charts** | Vico `3.3.1` | Custom 60fps `Canvas` (`BiquadFrequencyResponseCanvas` + Goertzel 10-band spectrum visualizer in `SharedComponents.kt` & `PlayerAndEqScreen.kt`) | Directly plots the exact analytical transfer function $20 \log_{10} |H(e^{j\omega})|$ of the 10 cascaded RBJ Biquad filters and real-time Goertzel energies with zero object allocation during recomposition. |
| **Dependency Injection** | Koin `4.2.2` | `io.insert-koin:koin-bom:4.0.2` | Koin 4.0.2 is already active in `VeloraModule.kt` and tested in `ExampleRobolectricTest`. |

---

## 4. Evidence-Based Desktop Playback Engine Evaluation (`desktopMain`)

Because `androidx.media3:media3-exoplayer` is Android-only, `desktopMain` requires a native JVM/Desktop playback backend implementing `PlayerEngine`. Below is the engineering comparison of the three candidates across VELORA's core requirements:

| Criterion | **1. VLCJ 4.8.x (LibVLC 3.x / 4.x)** | **2. GStreamer (`gst1-java-core` 1.4.x)** | **3. JavaFX Media (`javafx.scene.media`)** |
| :--- | :--- | :--- | :--- |
| **Audio & Video Codec Coverage** | **Excellent:** Built-in FFmpeg demuxers/decoders for MP3, FLAC, WAV, OGG/Opus, AAC/M4A, ALAC, MP4, MKV, WebM, AVI, TS, HLS (`.m3u8`), DASH (`.mpd`). | **Excellent:** Full codec support via `gst-plugins-base/good/bad/ugly` + `gst-libav`. | **Poor:** Limited to MP3, WAV, AAC, and MP4 (H.264/AVC). No native MKV, WebM, Opus, or DASH support. |
| **Seek Accuracy & Gapless** | Frame-accurate video seeking + native audio scrubbing and subtitle track switching. | Frame-accurate seeking (`SeekFlags.ACCURATE`) + native `playbin3` gapless transitions. | Coarse keyframe seeking; weak multi-audio/subtitle track API. |
| **10-Band Biquad EQ & Raw PCM Access** | **Two paths:** (a) Native `libvlc_audio_equalizer_new` (10-band ISO), OR (b) `DirectAudioPlayer` (`play(Pointer samples, int count, long pts)`) routing raw S16N/FL32 PCM directly through VELORA's shared Kotlin `EqSpecification` 64-bit Biquad + Goertzel engine. | **Two paths:** (a) Native `equalizer-10bands` GStreamer element, OR (b) `AppSink` / `AudioFilter` element intercepting raw PCM buffers. | Only provides a basic `AudioEqualizer` without raw PCM buffer interception for VELORA's custom 64-bit Biquad cascade or Goertzel spectrum analyzer. |
| **Compose Desktop Video Rendering** | `CallbackMediaPlayerComponent` / `DirectBufferFormat` writes BGRA frames directly into a Compose `ImageBitmap` / Skia `Bitmap` at 60fps (no heavyweight AWT `Window` clipping issues). | `AppSink` (`video/x-raw,format=BGRA`) into Skia `Bitmap`. | Requires `JFXPanel` Swing interop, which suffers from z-order clipping and extra GPU-to-CPU copies inside Compose Desktop. |
| **License** | **GPLv2+ (vlcj) / LGPLv2.1+ (LibVLC)** — Compatible with open-source VELORA distribution; dynamically links system or bundled `libvlc`. | **LGPLv2.1+ (`gst1-java-core` & GStreamer core)** — Permissive dynamic linking. | **GPLv2 with Classpath Exception**. |
| **Cross-Platform Desktop Packaging** | High maturity on Windows (`.msi`), macOS (`.dmg`), and Linux (`.deb`/`.AppImage`). | Requires bundling ~120MB of GStreamer plugins + GObject Introspection on Windows/macOS. | Bundled via `org.openjfx.javafxplugin`, but crippled by missing MKV/FLAC/WebM codecs on several OS targets. |

### Selected Desktop Engine Recommendation: **VLCJ (`uk.co.caprica:vlcj:4.8.3`)** (with GStreamer as secondary LGPL alternative)
- **Why VLCJ Wins for VELORA:**
  1. Matches Android Media3's container and codec matrix (MP3, FLAC, Opus, MKV, WebM, HLS, DASH) out of the box without OS-codec dependencies.
  2. Supports direct Skia `Bitmap` video rendering in Compose Multiplatform Desktop via `BufferFormatCallback` without Swing/JavaFX z-ordering bugs.
  3. Allows routing decoded PCM frames through VELORA's exact `commonMain` 64-bit Biquad and Goertzel DSP math (`EqSpecification.kt`), guaranteeing identical acoustic response on Android and Desktop.

---

## 5. What iOS Needs Later (`iosMain` Roadmap)

To extend VELORA to `iosMain` (`iosArm64`, `iosSimulatorArm64`) in a future phase:
1. **`IosAvPlayerEngine` (`actual class PlayerEngine`):**
   - Backed by `AVQueuePlayer` + `AVAudioEngine` (`AVAudioUnitEQ` with 10 parametric bands set to `EqSpecification.ISO_FREQUENCIES_HZ`, or `MTAudioProcessingTap` feeding VELORA's shared Kotlin 64-bit Biquad cascade).
   - Background audio & lock-screen integration via `MPNowPlayingInfoCenter` and `MPRemoteCommandCenter`.
2. **`VeloraVideoSurface` (`actual fun VeloraVideoSurface`):**
   - Implemented via `UIKitView` wrapping `AVPlayerLayer` / `AVPlayerViewController` (with `showsPlaybackControls = false` so VELORA's shared Compose cinema HUD and gesture engine remain unified).
3. **Storage & Media Discovery:**
   - Room KMP (`BundledSQLiteDriver`) + DataStore inside `NSApplicationSupportDirectory`.
   - Local file import via `UIDocumentPickerViewController` (`UTTypeAudio`, `UTTypeMovie`) and `MPMediaQuery` for Apple Music library tracks.
