# VELORA Audio Product UI/UX Specification

## Design intent

VELORA is a calm, premium audio workspace: the user should understand what is playing, what will happen next, and how to recover from a problem within one glance. Studio controls must be powerful without making the default listening path feel like an engineering console.

## State map

| State | Primary feedback | Primary action |
|---|---|---|
| Empty | Explain how to add or scan media | Scan library |
| Loading | Progress indicator and source label | Cancel when supported |
| Buffering | Keep identity visible; show buffering, not Playing | Retry if timeout |
| Playing | Stable progress, play/pause, output route | Pause / seek |
| Paused | Preserve position and clear resume affordance | Play |
| Ended | Queue continuation or completion state | Next / replay |
| Recoverable error | Short explanation with retry | Retry |
| Fatal error | Explain unsupported/corrupt media | Clear / choose another |

## Design tokens

- **Spacing:** 8, 13, 21, 34, 55 dp; use 48 dp minimum interactive target and 56 dp primary play target.
- **Layout ratios:** prefer 0.618 / 0.382 for two-column emphasis on expanded layouts; never sacrifice legibility to preserve a ratio.
- **Breakpoints:** compact `<600dp`, expanded `600–839dp`, rail `>=840dp`; every compact layout needs a vertical fallback.
- **Corners:** 8 dp controls, 13 dp cards, 21 dp hero surfaces.
- **Typography:** identity uses headline scale; metadata uses body/label scale; never encode hierarchy with color alone.
- **Contrast:** WCAG AA minimum; use APCA checks for primary text and focused controls.

## Player composition

1. Identity and output route.
2. Visualizer with explicit mode and telemetry.
3. Timeline with drag and exact-seek fallback.
4. Primary transport: previous, play/pause, stop, next.
5. Secondary transport: shuffle, repeat, mute, volume.
6. Queue and lyrics as progressive disclosure.
7. Studio DSP and advanced settings below the listening path.

## Accessibility gates

- Every icon-only control has a localized content description.
- Every important clickable surface exposes `Role.Button`, selected/active state, and a meaningful state description.
- All controls remain usable at fontScale 1.0, 1.3 and 2.0, in RTL and landscape.
- Primary controls are at least 48 dp; play/pause is at least 56 dp.
- Error and status messages are actionable and announced without relying on color.

## Release validation

Run unit tests plus Compose/instrumentation checks for: semantics tree, RTL, dark/high-contrast themes, compact phone, tablet/rail, landscape, large font scale, process recreation, buffering/error states, and queue transitions. Record known limitations rather than claiming universal support without evidence.
