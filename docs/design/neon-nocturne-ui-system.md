# Velora — Neon Nocturne / Aurora Signal

## Design intent

Velora is no longer presented as a saffron-and-gold catalogue of cards. The new direction is an **audio instrument**: quiet at rest, luminous while sound is active, and precise when the user opens the Lab. The visual language is deep navy, signal cyan, violet depth and coral attention. Surfaces are fewer, flatter and more intentional; color appears as a state signal, not decoration.

The existing Android/Compose/Media3 architecture and real callbacks remain unchanged. This redesign changes the visual system and interaction framing without fabricating capabilities.

## State map

| Playback state | Visual treatment | User action |
|---|---|---|
| Empty/IDLE | Night canvas, quiet cyan mark, “Ready” identity | Choose a track from Home or Library |
| BUFFERING | Cyan progress signal and short preparing label | Wait; transport remains state-driven |
| PLAYING | Cyan active signal, visible position and live telemetry | Pause, seek, next/previous |
| PAUSED | Muted signal, stable identity and resume control | Resume |
| ENDED | Completed timeline and calm status | Replay or choose another track |
| Recoverable ERROR | Coral border and readable error message | Clear/dismiss or select another track |
| No artwork | Instrument glyph fallback with no fake image | Continue playback normally |

## Design tokens

- Canvas: `#070A12` deep night.
- Surface: `#0D1424` blue-black.
- Elevated surface: `#14213A`.
- Card surface: `#1C2C4A`.
- Live signal: `#6EE7FF` cyan.
- Depth signal: `#B8A1FF` violet.
- Attention: `#FF8BA7` coral.
- Primary text: `#F4F7FF`.
- Secondary text: `#A8B4CC`.
- Spacing remains the tested 8/13/21/34/55dp scale.
- Interactive targets remain at least 48dp.
- Corner language is tightened to 10/16/24dp; surfaces are not pill-shaped by default.
- Breakpoints remain compact phone `<600dp`, expanded `600–839dp`, tablet rail `>=840dp`.

### Responsive behavior

- **Compact phone `<420dp` content width:** Home primary actions become a full-width vertical stack; no button relies on a fractional-weight row. Now Playing metadata stacks identity, route and secondary actions; visualizer modes and transport controls remain reachable through horizontal scrolling instead of clipping.
- **Expanded phone `420–599dp`:** Home actions share the row using the φ split; content cards remain full-width; horizontally curated shelves retain intentional horizontal scrolling.
- **Expanded/tablet `600–839dp`:** Home and player use full-width content cards with stable hierarchy; the shell keeps bottom navigation while long shelves remain independently scrollable.
- **Tablet rail `>=840dp`:** The shell switches to navigation rail; content retains a readable max hierarchy rather than stretching controls across the entire window.
- **Large text:** Track identity and route/action controls are stacked instead of competing for one horizontal line; every action remains at least 48dp.

## Component decisions

### Shell and navigation

The shell now presents `VELORA` as a small signal mark plus `FIELD` or `LAB` mode, rather than a gold brand badge. Navigation keeps the real destination model and callbacks, while the new palette and flatter header establish the new product personality.

### Home

Home is a listening field: one primary listening action, a quiet Surprise Me action, compact real library telemetry, Continue Listening, Recently Played and Favorites. All content comes from `VeloraUiState`; no demo tracks are introduced.

### Now Playing

Now Playing is the main instrument surface. Artwork or a deterministic no-artwork state leads the hierarchy, followed by track identity, real route information, live status, timeline and transport. EQ/DSP remains behind Lab disclosure so the default path is not overloaded.

### Mini-player

The mini-player remains intentionally small: identity, progress, play/pause, next and repeat. A tap opens the full player, and swipe direction continues to map to previous/next through the existing callbacks.

### Lab

The existing 10-band EQ, visualizer, DSP rack, lyrics, bookmarks and queue remain connected to real state. The new palette treats them as a measurement workspace instead of decorative premium cards.

## Accessibility and localization

- Existing semantics roles, test tags, minimum touch targets and Persian branches are preserved.
- Cyan, violet and coral are not used as the sole carrier of state; labels and icons remain present.
- Contrast targets are checked against the new night canvas through the existing `PhiDesignMetrics` utilities.
- RTL layout continues to be provided by the shell's `CompositionLocalProvider`.

## Validation plan

1. Run Detekt, unit tests, Kotlin compile and APK packaging in CI.
2. Add/execute Compose UI tests for empty/populated Home and Now Playing.
3. Validate compact phone, expanded phone, tablet rail, Persian RTL and large font scale.
4. Capture real Android screenshots and a real-audio interaction recording before marking screen features `TESTED`.

## Known limitations

The current environment cannot provide a physical Android device, emulator screenshots, TalkBack evidence, or a Windows target. Those remain explicit acceptance gaps and are not claimed as complete.
