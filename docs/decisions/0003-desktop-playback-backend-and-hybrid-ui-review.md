# ADR 0003: P2b Desktop Playback Backend Open Decision & KMP Reference Migration Scripts

- **Status:** Open Decision (P2b — Pending Desktop JVM Hardware Benchmark)
- **Date:** 2026-11-18
- **Supersedes / Extends:** [ADR 0001](./0001-desktop-cmp-libvlc-adr.md)
- **Active Build Strategy:** Keep single-module `:app` for live Android builds (`AGP 9.1.1`, `Kotlin 2.2.10`, `compileSdk 36`, `Compose BOM 2024.09.00`, `Media3 1.5.1`). Use the migration templates below only when entering Phase P6 (`:shared-ui` + `:desktopApp`).

---

## 1. P2b Desktop Playback Backend Comparison (Open Decision)

In compliance with Phase P2b, desktop media playback remains cleanly behind `com.example.domain.engine.PlayerEngine` with **zero fake desktop playback claims** until a desktop target passes empirical playback verification on Linux/Windows/macOS.

| Criterion | **VLCJ (libVLC 4.8.x)** *(Recommended)* | **JavaFX Media** | **GStreamer-Java (gst1-java-core)** |
|---|---|---|---|
| **Codec Coverage** | Broadest native muxer/codec matrix (MP3, FLAC, ALAC, Opus, Vorbis, MKV, MP4, AVI, WebM, ICY/HLS) | Limited (MP3, AAC, MP4 H.264; lacks native FLAC/MKV/Opus out of the box) | Broad if native plugins (`gst-plugins-good/bad/ugly`) are shipped |
| **Hardware Acceleration** | VA-API / VDPAU (Linux), DXVA2 / D3D11 (Windows), VideoToolbox (macOS) | Software-heavy pipeline unless platform codecs match | VA-API / NVDEC / D3D11 via plugins |
| **10-Band EQ & DSP** | Built-in `libvlc_audio_equalizer_*` 10-band ISO API | 10-band `AudioEqualizer` API (fewer audio formats supported) | `equalizer-10bands` GStreamer element |
| **Licensing** | GPLv2+ / LGPLv2.1+ (`libvlc` LGPL if built without GPL plugins; check redistribution) | GPLv2 with Classpath Exception | LGPLv2+ (`gst1-java-core` LGPL; plugin bundle licenses vary) |
| **Maintenance & JVM Fit** | Mature Maven artifact (`uk.co.caprica:vlcj:4.8.3`), `CallbackMediaPlayer` paints into Compose `ImageBitmap` | Bundled via OpenJFX modules; slower release cadence for modern codecs | Active bindings; native distribution per OS requires packaging plugins |
| **Binary Footprint** | ~45–75 MB native `libvlc` + plugins per desktop OS | ~35–50 MB OpenJFX runtime modules | ~60–110 MB GStreamer base + plugin suite |
| **Status** | **Recommended candidate for P2b prototype** | Rejected as primary (insufficient audiophile FLAC/MKV coverage) | Viable fallback if LGPL plugin tree is preferred over libVLC |

**Boundary Rule:** No desktop transport claims may be marked verified until tested on an actual JVM desktop target (`[UNVERIFIED — needs Desktop JVM execution]`).

---

## 2. Ready-to-Wire KMP Migration Templates (`CMP 1.10.3` Pinned)

Why **Compose Multiplatform `1.10.3`** (and NOT `1.12.2`):
- Matches the repo baseline (`AGP 9.1.1`, `Kotlin 2.2.10`, `compileSdk 36`, `targetSdk 36`, `minSdk 26`).
- Upgrading to CMP `1.12.2` forces `compileSdk 37.1+`, `AGP 9.2.0+`, and Kotlin `2.4.x`, breaking current project alignment.

### 2.1 Reference `settings.gradle.kts` Addition (DO NOT apply until Phase P6)

```kotlin
// Include when migrating `:shared-ui` and `:desktopApp` in Phase P6:
include(":app")
include(":shared-ui")
include(":desktopApp")
```

### 2.2 Reference `shared-ui/build.gradle.kts` (`com.android.kotlin.multiplatform.library`)

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.velora.shared.ui"
        compileSdk = 36
        minSdk = 26
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3) // CMP 1.10.3 Material3
            implementation(compose.materialIconsExtended)
            implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
```

### 2.3 Reference `desktopApp/src/desktopMain/kotlin/Main.kt`

```kotlin
package com.velora.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VELORA — Beyond Playback",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        // Wire DesktopVlcPlayerEngine (pending P2b approval) + shared VeloraShellScreen here
    }
}
```
