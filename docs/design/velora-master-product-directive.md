# VELORA — Beyond Playback

## Master UI/UX Design & Product Engineering Directive

This document is the governing brief for the next Velora redesign. It is adapted from the supplied product directive to the real repository: Android/Kotlin/Jetpack Compose with Media3 playback and separated domain/data/UI layers. The repository is not Rust-oriented; preserving the working Android architecture is intentional.

## Product ambition

Transform Velora into a distinctive premium media environment built around sound, motion, intelligence and precision. The experience is:

**Luxury × Cinematic × Minimalism × Intelligence**

Velora must not resemble a generic Spotify-style layout. Avoid excessive gradients, cheap glassmorphism, overloaded navigation, decorative UI without purpose and fake/nonfunctional controls.

## Visual identity

- Obsidian Black foundation.
- Carbon Black secondary surfaces.
- Champagne Gold signature accent.
- Soft Ivory primary typography.
- Restrained metallic gradients and controlled ambient lighting.
- Sophisticated depth with fewer, stronger surfaces.
- Artwork-first presentation whenever real artwork is available.

## Design system requirements

Define and keep reusable tokens for color, typography, spacing, grid, radii, elevation, icons, component states, motion, breakpoints and light/dark/high-contrast themes.

- Persian UI: RTL-safe and prepared for Vazirmatn-compatible typography.
- Latin UI: Manrope-compatible hierarchy; use repository-bundled fonts until a licensed asset is added.
- Spacing: 8/13/21/34/55 dp with 48 dp minimum interactive targets.
- Breakpoints: compact phone `<600dp`, expanded `600–839dp`, tablet/rail `>=840dp`.
- Motion: GPU-friendly, purposeful, cancellable and respectful of reduced-motion preferences.

## Screen priorities

### Home

Personalized listening dashboard with recently played, continue listening, recommendations, frequently played, dynamic playlists and recently imported media. The current Magic Listening Dashboard is the first implementation slice: one-tap listening, Surprise Me discovery, compact stats and quiet scan.

### Music Library

Support albums, artists, tracks, genres, folders, recent imports, favorites, sorting/filtering, search and library management. Progressive disclosure keeps the default listening path calm.

### Immersive Now Playing

The most important surface. Prioritize real album artwork when available, track identity, waveform/spectrum, transport, timeline, output route, quality, lyrics and queue. Advanced playback information expands rather than competing with the primary action.

### Equalizer & Audio Laboratory

Keep the existing real DSP connected: 10-band EQ, presets, preamp/headroom, bass, stereo width, output information, profiles and visualizer. Never expose a control that is not connected to real state.

### Playlists, Search, AI Studio and Settings

Organize these around clear user intent. AI features must expose provider/configuration state and never imply intelligence that is not implemented. Settings are grouped by Appearance, Audio, Playback, Library, Devices, AI, Language, Privacy, Performance and Accessibility.

## Navigation

Android uses bottom navigation plus a compact mini-player on phones and a rail on tablet widths. Optimize for one-handed use and gesture-friendly interactions. A future Windows surface must be designed as a desktop composition with sidebar, resizable panels and keyboard shortcuts; do not stretch mobile UI into desktop.

## Engineering constraints

- Preserve the existing Kotlin/Compose/Media3 architecture.
- Keep UI, playback logic, persistence and services separated.
- Reuse real state and existing callbacks; no decorative fake controls.
- Protect lifecycle, queue transitions, DSP numerical safety and background playback.
- Prioritize memory, battery, startup time, smooth scrolling and responsive layouts.

## Quality gates

Before a redesign slice is considered complete:

1. Inspect existing screen and state contracts.
2. Update or define design tokens.
3. Implement modular components connected to real state.
4. Test navigation and callbacks.
5. Validate compact, expanded, tablet, landscape and large font scale.
6. Validate Persian RTL, accessibility roles, state descriptions and contrast.
7. Test loading, empty, buffering, recoverable error and fatal error states.
8. Run Detekt, unit tests, compile, signed APK packaging and checksum verification.
9. Record known limitations; do not claim unsupported features.

## Execution phases

1. Repository and product audit.
2. Design system and visual identity.
3. Core shell and navigation.
4. Home, library and search.
5. Immersive player.
6. Audio Laboratory and playlists.
7. AI Studio and settings.
8. Responsive optimization and QA.
