# ADR 0001: Compose Multiplatform Desktop Engine Evaluation (libVLC / vlcj vs libmpv)

- **Status:** Proposed (Phase 5 Desktop Extension)
- **Date:** 2026-11-06
- **Confidence:** 92% (Fact: `PlayerEngine` in `com.example.domain.engine` has zero Android imports; Theory: `vlcj` 4.8.x CallbackMediaPlayer integrates cleanly with Compose Desktop `Canvas`/`ImageBitmap`).

## Context
Fazora's Android application is complete and uses `Media3PlayerEngine` behind the pure Kotlin `PlayerEngine` interface (`com.example.domain.engine.PlayerEngine`). For Phase 5 Desktop support via Compose Multiplatform (JVM Linux/Windows), `androidx.media3` is Android-only, requiring a native desktop implementation of `PlayerEngine`.

## Evaluation: libVLC (via vlcj) vs libmpv (via JNI/JNA)

| Criterion (Weight) | libVLC (`vlcj` 4.8.x) | libmpv (JNA) | Notes |
|---|---|---|---|
| Audio + Video Playback | 9/10 | 9/10 | Both decode MP3, FLAC, WAV, MP4, MKV natively |
| Seeking Accuracy | 8/10 | 9/10 | Both support frame-accurate seek |
| 10-Band Native EQ API | 10/10 | 8/10 | libVLC has a dedicated 10-band ISO `Equalizer` API (`libvlc_audio_equalizer_*`) matching Fazora's 10 bands directly |
| JVM / Compose Desktop Integration | 9/10 | 6/10 | `vlcj` provides mature Maven artifacts and `BufferFormatCallback` for zero-JNI-boilerplate frame rendering into Compose `ImageBitmap` |
| Cross-Platform Packaging (Linux/Win) | 9/10 | 7/10 | `libvlc` is standard on Linux distros and easily bundled on Windows |
| **Total Score (out of 50)** | **45/50** | **39/50** | **Winner: libVLC (`vlcj`)** |

## Decision
When building the Desktop JVM target in a multi-module Compose Multiplatform layout (`:shared:domain`, `:androidApp`, `:desktopApp`), implement `DesktopVlcPlayerEngine : PlayerEngine` using `uk.co.caprica:vlcj:4.8.3` and bind it in the Desktop Koin module (`single<PlayerEngine> { DesktopVlcPlayerEngine() }`).

## Cost to Reverse
Low (~1 day): `PlayerEngine` isolates all playback, EQ, and track-selection calls from `FazoraViewModel` and Compose UI screens.
