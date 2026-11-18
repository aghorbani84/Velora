# Velora Design System — NEW (Phase 1 of the UI/UX rebuild)

> Status: **specification only — no code changed, nothing built, nothing tested on a device.**
> Branch: `feature/ui-rebuild-phase0-1`, HEAD `19e70bc` ("refactor(settings): introduce experience tiers").
> This file is the single source of truth for the rebuilt UI (Phase 2). Every screen built in
> Phase 2 must use these tokens and components; any value not listed here is a violation, not
> a creative choice. Audit lens: Design System Consistency Auditor (§0 of the rebuild prompt)
> + Apple HIG as review lens only. φ (1.618034) is an **Opinion** proportion lens, not a law —
> accessibility always beats φ (§8 lists every deviation with its percentage).
>
> **Environment honesty.** No emulator, no device, no Gradle run, no media files exist in this
> environment. Contrast ratios in §1 were computed from the hex values with the WCAG 2.1
> relative-luminance formula (computed, not rendered). Any value that would need a rendered
> screen to confirm is marked **UNTESTED** and must not be reported as verified.
>
> **Note on the previous version of this file.** A DESIGN-SYSTEM.md already existed at HEAD
> `19e70bc` (149 lines). It is superseded by this version: its light-theme token values did
> not match the code (`#FAF7F0` / `#8C5800` / `#B3261E` vs. `Color.kt`'s `#F9F6F0` /
> `#946200` / `#BA1A1A` — code wins per the rebuild prompt), its type scale used fractional
> steps (n = ±0.5, ±1/6) instead of the specified 16 × φ^(n/3), and it lacked the bottom-sheet
> and dialog components, the SIMPLE proof test design, the motion spec, and the tier
> build-on verdict. The old version remains recoverable via `git show HEAD:DESIGN-SYSTEM.md`;
> this file is an uncommitted working-tree change, not a commit.

## 0. Relationship to the existing code — BUILD ON, do not replace (tiers)

HEAD `19e70bc` already introduced experience tiers. The new design **builds on** that
foundation; it does not replace it:

- `ExperienceTier` enum already exists (`app/src/main/java/com/example/domain/model/PlaylistAndSettings.kt:85-89`):
  `SIMPLE` ("ساده (پخش آسان)"), `STANDARD` ("استاندارد (روزمره)"), `PRO` ("Studio Pro" /
  "حرفه‌ای (استودیو و اکولایزر)").
- DataStore already persists the tier (`VeloraSettingsRepository.kt`: `KEY_EXPERIENCE_TIER` +
  a `KEY_SIMPLE_MODE` mirror; `updateExperienceTier()` writes both atomically).
- `SettingsScreen.kt:107-138` already has an instant-switch tier selector
  (test tags `tier_option_SIMPLE/STANDARD/PRO`) and PRO-only sections are already gated
  (`SettingsScreen.kt:246`, `:333`).
- Tier gating already exists in screens: `VeloraTransportControls` hides shuffle/repeat in
  SIMPLE (`VeloraSharedComponents.kt:456`), and `LibraryScreen`, `MusicAndArtistScreens`,
  `NowPlayingScreen`, `SearchScreen`, `VeloraShellScreen`, `VideoPlayerScreen` all read
  `settings.experienceTier`.

Three defects in the existing tier implementation that Phase 2 must fix (recorded here, not
fixed here — this phase writes no code):

1. **D-1 — `updateSimpleMode()` erases STANDARD.** `VeloraSettingsRepository.kt:108-116` maps
   "not simple" → `PRO`. Any legacy boolean toggle path collapses STANDARD into PRO.
   Fix: delete the boolean path; only `updateExperienceTier()` writes the tier.
2. **D-2 — `VideoPlayerScreen.kt:114` reconciles the tier inconsistently.** It re-derives the
   tier from `isSimpleMode` when the stored tier is PRO, contradicting the stored value.
   Fix: every screen reads `settings.experienceTier` directly, never re-derives it.
3. **D-3 — Default tier is SIMPLE, the new default is STANDARD.** The repository defaults
   `isSimple` to `true`, so a fresh install resolves to SIMPLE. The new system defaults to
   STANDARD (§5). Migration rule: existing installs keep their stored tier; fresh installs
   default to STANDARD. Changing an existing user's stored tier silently is forbidden.

What is genuinely new in this document (did not exist at HEAD): the unified token tables
(§1–§3), the retired failing muted-text color, the bottom-sheet and dialog components (§4.6,
§4.7 — dialogs today are per-screen files: `LibraryDialogs.kt`, `VideoPlayerDialogs.kt`,
`PlayerLyricsAndDialogs.kt`), the SIMPLE composition-tree proof design (§5.1), and the
complete φ-deviation ledger (§8).

## 1. Color tokens

One main accent + one secondary accent, nothing more. Dark is a real dark palette (Obsidian /
Saffron), **not** an inverted light palette: different hues, different accent values, different
error values per theme. All ratios below are WCAG 2.1 luminance ratios computed from the hex
values; rendered-screen confirmation is **UNTESTED**.

### 1.1 Dark — "Obsidian Saffron" (15 tokens)

| Token | Value | Role | Key pairs (ratio) |
|---|---|---|---|
| `bg` | `#0B0B0D` | App background | — |
| `surface` | `#121216` | Cards, sheets, lists | — |
| `surfaceElevated` | `#19191F` | Raised surfaces, player bar | primary text on it 15.65:1 |
| `surfaceCard` | `#202027` | Track cards | primary text on it 14.48:1 |
| `textPrimary` | `#F5F2EB` | Headings, body | on `bg` 17.59:1; on `surface` 16.71:1 |
| `textSecondary` | `#A5A29B` | Metadata, subtitles | on `surface` 7.33:1; on `bg` 7.72:1 |
| `textMuted` | `#8F8B83` | **NEW** — captions, timestamps | on `surface` 5.51:1; on `bg` 5.80:1; on `surfaceCard` 4.77:1 |
| `accent` (main) | `#E5A817` Saffron | Primary actions, play, progress | on `bg` 9.31:1; on `surface` 8.85:1 |
| `accentSecondary` | `#D9BD83` Champagne Gold | Secondary highlights, active states | on `bg` 10.82:1; on `surfaceCard` 8.91:1 |
| `accentBright` | `#F5C451` | Large display accents only, dark only | on `bg` 12.08:1 |
| `accentContainer` | `#2C2312` | Tinted container behind accent text | `textPrimary` on it: passes (dark container) |
| `onAccent` | `#0B0B0D` | Text/icons on the accent fill | on `accent` 9.31:1 |
| `error` | `#FF6B6B` | Errors, destructive | on `bg` 7.09:1; on `surface` 6.73:1 |
| `success` | `#4ADE80` | Success only | on `bg` 11.29:1; on `surface` 10.72:1 |
| `divider` | `#2A2927` | Hairline dividers, borders | Decorative only — never a text/background pair |

### 1.2 Light — "Daylight Ivory" (15 tokens)

| Token | Value | Role | Key pairs (ratio) |
|---|---|---|---|
| `bg` | `#F9F6F0` | App background | — |
| `surface` | `#FFFFFF` | Cards, sheets | — |
| `surfaceElevated` | `#EDE7DC` | Raised surfaces | `textPrimary` on it 15.18:1 |
| `surfaceCard` | `#FFFFFF` | Track cards (same as surface; border distinguishes) | — |
| `textPrimary` | `#121216` | Headings, body | on `bg` 17.32:1; on `surfaceElevated` 15.18:1 |
| `textSecondary` | `#4C4639` | Metadata, subtitles | on `bg` 8.68:1; on `surface` 9.36:1 |
| `textMuted` | `#6B655A` | **NEW** — captions, timestamps | on `bg` 5.36:1; on `surface` 5.78:1; on `surfaceElevated` 4.70:1 |
| `accent` (main) | `#946200` | Primary actions, play, progress | on `bg` 4.86:1; on `surface` 5.24:1 |
| `accentSecondary` | `#BDA36B` Soft Gold | Secondary highlights — large text / icons only | Decorative/large-only (see rule below) |
| `accentBright` | — (not used in light) | `accentBright` is dark-only; light uses `accent` | — |
| `accentContainer` | `#FFDEA3` | Tinted container behind accent text | `onAccentContainer` `#2F1E00` on it 12.42:1 |
| `onAccent` | `#FFFFFF` | Text/icons on the accent fill | on `accent` 5.24:1 |
| `error` | `#BA1A1A` | Errors, destructive | on `bg` 5.99:1; on `surface` 6.46:1 |
| `success` | `#1E7B34` | **NEW** — success only | on `bg` 4.94:1; on `surface` 5.33:1 |
| `divider` | `#CFC8B8` | Hairline dividers, borders | Decorative only |

### 1.3 Color rules (binding)

- **R-C1.** Every screen uses only these tokens. No hex literal outside the theme files.
  (Existing `Color.kt` values are kept where they match; see §1.4 for what changes.)
- **R-C2.** `accentSecondary` (`#BDA36B`) in light mode is large-text (≥19sp bold) and icon-only:
  as body text on `#F9F6F0` it would be ≈2.8:1 and fail. Same for `accentBright` in dark —
  never as small text on light; it has no light value at all.
- **R-C3.** No dynamic/Material-You color. The existing `balanceDynamicColorScheme()`
  (`Theme.kt`) re-hues the palette from the wallpaper and breaks token discipline. The new
  system ships three fixed palettes only: Saffron dark, Daylight light, High-Contrast mono
  (existing OLED tokens kept as the accessibility palette: bg `#000000`, text `#FFFFFF`,
  border `#3E3E44`, error `#FF5252`). If dynamic color ever returns, it must pass the same
  ≥4.5:1 gate on every pair before shipping.
- **R-C4.** Opacity tints: only `accent` at 14% for the error-banner-style tinted surface
  (existing `VeloraErrorBanner` pattern) and `accent` at 45% for the mini-player border.
  No other alpha tints — if a state needs emphasis, use a token, not an opacity.
- **R-C5.** Minimum contrast: body text ≥4.5:1, large text (≥19sp bold / ≥24sp) ≥3:1,
  icons that carry meaning ≥4.5:1 against their background, focus ring ≥3:1 against the
  adjacent surface. Rendered-screen verification: **UNTESTED**.

### 1.4 What changes vs. the existing `Color.kt` / `Theme.kt`

| Existing value | Verdict |
|---|---|
| `VeloraMutedText #77746E` | **RETIRED as text.** 4.01:1 on `surface`, 4.22:1 on `bg` — fails AA. Replaced by `textMuted #8F8B83` (dark) / `#6B655A` (light). The hex may remain only as a decorative value, never as text. |
| `VeloraDeepSaffron #C88712` | Kept as a decorative/large-only dark accent (6.49:1 on `bg`); **not** text on light (2.81:1 on `bg`). Not a token — do not use in new screens. |
| `VeloraBrightSaffron #F5C451` | Kept as dark-only `accentBright`; 1.51:1 on light `bg` — never in light mode. |
| `FazoraEmeraldOk #4ADE80` | Kept as dark `success`; light `success` is new `#1E7B34` (4.94:1). |
| `VeloraDaylightContainer #FFDEA3`, `OnContainer #2F1E00` | Kept as `accentContainer` / `onAccentContainer` (12.42:1). |
| `VeloraShapes` (8/13/21) | Kept; see §3. |
| `PhiTokens` spacing | Kept; see §3. |

## 2. Typography

### 2.1 Fonts (all four already bundled in `app/src/main/res/font/` — no new dependency)

| Script / use | Font | File (bundled) |
|---|---|---|
| Persian — everything (headings, body, labels, digits in Persian text) | **Vazirmatn** (Normal/Medium/SemiBold/Bold) | `vazirmatn.ttf` |
| Latin display / headings | **Space Grotesk** (Normal/Medium/SemiBold/Bold) | `space_grotesk.ttf` |
| Latin body / UI text | **Inter** (Normal/Medium/SemiBold/Bold) | `inter.ttf` |
| Numbers, timecodes, durations, EQ dB values | **JetBrains Mono** (tabular figures) | `jetbrains_mono.ttf` |

Rules: **R-T1.** Persian text is RTL everywhere, in every screen — left-aligned Persian text
is a defect. **R-T2.** Latin digits inside Persian sentences keep LTR embedding (timecodes
`03:24`, dB values `+3.0 dB`) and use JetBrains Mono so columns align. **R-T3.** Vazirmatn
has Latin glyphs, but Latin prose never uses it — Inter/Space Grotesk are the Latin faces.
**R-T4.** No letter-spacing on Persian text (existing `Type.kt` already does this: 0sp for
Persian). Latin display may use −0.5sp / −0.25sp as today.

### 2.2 Type scale — 16 × φ^(n/3), φ = 1.618034

Base 16sp (n=0). Adopted values are rounded to the 0.5sp grid; the rounding deviation from
the exact φ value is ≤1.2% for every step (see §8, D-φ1).

| Step n | Exact (sp) | Adopted | Role | Line height | Weight |
|---|---|---|---|---|---|
| −3 | 9.89 | **10** | Overline / micro-caption | 14 (1.40×) | Medium |
| −2 | 11.61 | **11.5** | Caption | 16 (1.39×) | Medium |
| −1 | 13.63 | **13.5** | Small body / metadata | 20 (1.48×) | Regular |
| 0 | 16.00 | **16** | Body (base) | 24 (1.50×) | Regular |
| +1 | 18.78 | **19** | Title / large label | 26 (1.37×) | SemiBold |
| +2 | 22.05 | **22** | Section title | 28 (1.27×) | Bold |
| +3 | 25.89 | **26** | Headline | 34 (1.31×) | Bold |
| +4 | 30.39 | **30.5** | Large headline | 38 (1.25×) | Bold |
| +5 | 35.68 | **35.5** | Display | 44 (1.24×) | Bold |
| +6 | 41.89 | **42** | Hero display (Now Playing cover title / empty-state numeral) | 52 (1.24×) | Bold |

Line-height rule: body/labels 1.50×, headlines/display 1.25× (deviations vs φ 1.618× are
listed in §8, D-φ2 — density and large-text leading, not accessibility: minimum line height
never harms contrast or touch targets).

### 2.3 Mapping from the existing `Type.kt` (what changes)

| Existing style | Old size | New size | Note |
|---|---|---|---|
| `displayLarge` | 34 | **35.5** (n=+5) | Old 34 was −4.7% off φ; new is −0.5%. |
| `headlineLarge` | 26 | **26** (n=+3) | Unchanged (was +0.4%). |
| `headlineMedium` | 20 | **22** (n=+2) | Old 20 was −9.3% off φ. |
| `titleMedium` | 17 | **19** (n=+1) | Old 17 was −9.5% off φ. |
| `bodyLarge` | 16 | **16** (n=0) | Unchanged (base). |
| `bodyMedium` | 15 | **16** (n=0) | Old 15 was +9.9% off the n=−1 step; merged into base — one body size, not two. |
| `bodySmall` | 13.5 | **13.5** (n=−1) | Unchanged (−0.9%). |
| `labelLarge` | 14 | **13.5** (n=−1) | Old 14 was +2.6% off; merged into n=−1. |
| `labelMedium` | 12.5 | **11.5** (n=−2) | Old 12.5 was +7.7% off φ. |
| `labelSmall` | 11.5 | **10** (n=−3) | Old 11.5 was +16.3% off φ — the largest old deviation; fixed. |

Minimum body text is 13.5sp (n=−1); 11.5sp and 10sp are metadata/captions only, never
paragraph text. **UNTESTED:** on-device legibility of 10sp Persian captions — verify in
Phase 3 QA on a real device; if unreadable, the floor rises to 11.5sp and §8 gains an entry.

## 3. Spacing, radii, layout

### 3.1 Fibonacci spacing scale (dp)

`2, 8, 13, 21, 34, 55` — exact Fibonacci numbers (0% deviation), plus one micro-step:

| Token | Value | Use |
|---|---|---|
| `Space2` | 2dp | Icon-to-label micro-gap, divider-adjacent gaps |
| `Space4` | 4dp | Half-step, hairline-adjacent micro-gaps (§8, D-φ3: −20.0% vs nearest Fibonacci 5) |
| `Space8` | 8dp | Icon gaps, chip padding, tight stacks |
| `Space13` | 13dp | Card inner padding (horizontal), button horizontal padding, row gaps |
| `Space21` | 21dp | Section padding, card inner padding (vertical), list gaps |
| `Space34` | 34dp | Section separation, empty-state padding |
| `Space55` | 55dp | Hero gaps, play-button diameter, large icon wells |

Rules: **R-S1.** Screen edge padding is 21dp; list item vertical rhythm is 8dp between rows,
21dp between sections. **R-S2.** No raw `.dp` literals in screens — every dimension is a
token (existing `PhiTokens` already enforces this pattern; keep it). **R-S3.** Layout split
61.8 / 38.2: Now Playing cover art takes 61.8% of the available height, controls + queue
38.2%; video surface 61.8% / control strip 38.2% in landscape split layouts.

### 3.2 Radii (unified)

| Token | Value | Use |
|---|---|---|
| `RadiusS` | 8dp | Cover thumbnails, chips, small badges |
| `RadiusM` | 13dp | Buttons, track cards, seek-bar thumb track, mini-player |
| `RadiusL` | 21dp | Section cards, bottom sheets (top corners), dialogs, empty state |
| `RadiusXL` | 34dp | Hero cover art, large sheets |

One radius per component class, everywhere: every button 13dp, every card 21dp, every sheet
21dp top. The play/pause control is a **circle** (radius = 50% of its 55dp size) — a shape,
not a radius step (§8, D-φ4).

### 3.3 Structural tokens (kept from `PhiTokens`, with deviations noted in §8)

- `MinTouchTarget` 48dp (§8, D-φ5: −12.7% vs 55 — the accessibility minimum wins).
- `IconSize` 16 / 20 / 24 / 34dp (§8, D-φ6: +0.0% / +6.5% / +8.8% / −4.7%).
- `AvatarSize` 48dp (same −12.7% bucket as the touch target).
- `SpectrumHeight` 144dp, `VideoSurfaceHeight` 233dp, `CardWidthCompact` 233dp,
  `MaxContentWidth` 610dp — exact Fibonacci, 0%.
- `WideScreenBreakpoint` 600dp (§8, D-φ7: −1.6% vs 610).
- Borders: hairline **1dp only**. The old 1.5dp `BorderAccent` is retired — use 1dp, or 2dp
  for a selected-state ring (2dp is exact Fibonacci).

## 4. Shared components — one implementation each

Every component below exists exactly once, in `ui/components/`, and every screen uses that
one. A screen-local copy is a defect. Existing components are kept where they already match;
new ones are specified here for Phase 2. All components take `ExperienceTier` where the tier
changes their content, and expose `testTag`s for the §5.1 proof test.

| # | Component | Status at HEAD | Spec (new system) |
|---|---|---|---|
| 4.1 | `VeloraPrimaryButton` | Exists — keep | Accent fill, `onAccent` text, radius 13dp, min height 48dp, horizontal padding 13dp, vertical 8dp. One primary action per screen (§0-d rule 1). Disabled state: no dead buttons — a button that can never act is removed, not greyed out (§0-d rule 4). |
| 4.2 | `VeloraSecondaryButton` | Exists — keep | Outlined, `surfaceVariant` fill, radius 13dp, min height 48dp. `selected` state uses `accentContainer` fill + accent text. Used for tier switching, filters, secondary actions. |
| 4.3 | `VeloraTrackCard` (`TrackRowItem`) | Exists — keep, tighten | Radius 13dp, 48dp cover thumb (radius 8dp), title 16sp, artist/album 13.5sp, format line 11.5sp JetBrains Mono. Active card: `accentContainer` fill + accent border. In SIMPLE mode the card exposes **only** play on tap — favorite/queue/playlist buttons are not composed (see §5.1). |
| 4.4 | `VeloraSeekBar` | Exists — keep | Accent active track, `surfaceVariant` inactive, 4dp track height (§8, D-φ3 bucket), thumb 20dp. Time labels 11.5sp JetBrains Mono, elapsed in accent, total in `textSecondary`. Reads position from the narrow `PlaybackPositionState` (position/duration only) — never from a full UiState recompute (the 10 Hz tick rule). |
| 4.5 | `VeloraTransportControls` | Exists — keep, tighten | Play/pause 55dp accent circle; prev/next 34dp icons; shuffle/repeat **not composed** in SIMPLE (already gated at `VeloraSharedComponents.kt:456`). All buttons ≥48dp touch target via `minimumInteractiveComponentSize()`. |
| 4.6 | `VeloraBottomSheet` | **NEW** — no shared sheet exists today | Radius 21dp top corners, drag handle 34dp wide / 4dp tall in `divider`, scrim `bg` at 60%. One sheet per screen max; sheets host secondary actions (queue, track menu, "Pro settings" door) — never the primary action. |
| 4.7 | `VeloraDialog` | **NEW** — standardize the three per-screen dialog files | Radius 21dp, 21dp padding, title 19sp SemiBold, body 16sp, actions are `VeloraPrimaryButton` + `VeloraSecondaryButton` only. Replaces ad-hoc dialogs in `LibraryDialogs.kt`, `VideoPlayerDialogs.kt`, `PlayerLyricsAndDialogs.kt` — those files are deleted in Phase 2 and their content re-implemented on this component. |
| 4.8 | `VeloraEmptyState` | Exists — keep | Radius 21dp, 34dp padding, 55dp accent-container icon well, title 22sp Bold, subtitle 16sp `textSecondary`, optional primary action. Every list/search/playlist screen has a designed empty state (§0-d rule 5) — never a blank screen. |
| 4.9 | `VeloraLoadingSkeleton` | Exists — **retime** | Same structure (label + 55dp rows, radius 13dp), but pulse **200–300ms** (§6). The existing 800ms pulse is +166.7% over the cap (§8, D-φ8) and must change. |
| 4.10 | `VeloraErrorBanner` | Exists — keep | Error tint 14%, radius 13dp, error icon + message 16sp + one secondary action ("تلاش دوباره" / "Retry"). A corrupt file shows this banner with its name and a "skip" action — never a crash, never a white screen (§0-d rule 5). |

Also carried over unchanged: `VeloraSectionCard` (radius 21dp, 21dp padding) and
`MiniPlayerBar` (radius 13dp, accent progress line, play/next only — in SIMPLE it shows
play/next only; the bar itself is present in all tiers because it is the background-playback
surface, §7).

**Component rules:** R-K1. No per-screen duplicate of any component above. R-K2. No hardcoded
colors, sizes, or radii inside a component — tokens only (auditor checklist §0-a). R-K3.
Every meaningful icon has a `contentDescription` (localized); decorative icons use `null`.
R-K4. Exactly **one** gesture implementation for music + video (double-tap seek, swipe
volume/brightness) — the three copy-pasted gesture layers (~150 lines, already diverged)
are deleted in Phase 2 and replaced by one shared handler.

## 5. Experience tiers — the heart of the product requirements

Tier is a single `ExperienceTier` value from DataStore (§0), read as state, applied instantly
with no restart. Tier switching is a recomposition, not a relaunch: the shell and the current
screen re-read `settings.experienceTier` and re-compose. **No screen may cache the tier in a
`remember` that survives a switch.**

### 5.1 SIMPLE — "only what's needed"

Visible, and nothing else:

- Play/pause (55dp accent circle), previous, next.
- `VeloraSeekBar` (position + duration).
- Volume (slider in the player; hardware keys everywhere).
- Current track title/artist + cover art. Mini-player bar.
- Library list (tap to play), search box (type + results, no filters), settings entry.

**Provably simple — the test design (Phase 2 implements it; the design is fixed here):**

- Test `SimpleModeCompositionTest` (Compose UI test, `createAndroidComposeRule`):
  1. Set tier SIMPLE via `updateExperienceTier(SIMPLE)` (DataStore, no restart).
  2. On each of the 8 destinations (library, music, radio, artists, video, search, player,
     settings), assert the composition tree contains **zero** nodes with any pro test tag:
     `transport_shuffle_button`, `transport_repeat_button`, `eq_*`, `speed_*`,
     `subtitle_*`, `audio_track_*`, `ab_loop_*`, `pip_button`, `video_auto_hide_slider`.
     `tier_option_PRO` is allowed (it is the door out, not a pro control) — everything
     else in the PRO list must be absent.
  3. Assert the SIMPLE allow-list is present: `transport_play_pause_button`,
     `transport_prev_button`, `transport_next_button`, `player_seek_slider`.
  4. Assert with semantics: no node exposes a pro `contentDescription` ("Toggle Shuffle",
     "Cycle Repeat Mode", "Equalizer", …) in SIMPLE.
- The proof is structural (absence from the tree), not visual: hiding a control with
  `alpha(0)` or `visibility = Gone` fails the test — the composable must not be emitted.
  `if (tier != SIMPLE)` gating at the call site is the only accepted mechanism (the
  existing `VeloraTransportControls` already does this correctly).
- This test runs in CI on every PR. A screen that cannot pass it in SIMPLE does not ship.

### 5.2 STANDARD — the default (fresh installs; existing users keep their stored tier)

Everything in SIMPLE, plus the everyday set:

- Queue (view, add, remove, reorder), repeat (off/all/one), shuffle.
- Playlists (create, add/remove track, delete playlist).
- Search with filters + debounced input (no full search per keystroke).
- Favorites, artists/albums views, radio.
- Video: fullscreen, aspect modes (FIT/FILL/GOLDEN_PHI/16:9), rotation, double-tap seek,
  brightness/volume gestures, bookmarks.
- Sleep timer, crossfade, background playback controls (notification / lock screen).

STANDARD is the default because it is the complete everyday player: nothing a daily user
needs is behind a door, and nothing pro is in the way.

### 5.3 PRO — "Studio" — everything, behind one explicit door

Everything in STANDARD, plus — all reachable only through the single labelled door
**"تنظیمات حرفه‌ای / Pro Settings"** (a bottom sheet from the player, and a section in
Settings). No pro control appears on any main screen:

- 10-band EQ with live curve + presets (local + cloud), per-band gains in dB.
- Playback speed 0.5×–2.0×, A-B loop.
- Subtitles (SRT track select, font scale, colour), audio-track select.
- Gestures panel (double-tap seek distance, swipe sensitivity), PiP, video auto-hide
  timeout (1500–15000ms), adaptive audio mode, declarative plugins, backup JSON
  export/import, demo-sample management.
- The existing PRO-only Settings sections (`SettingsScreen.kt:246`, `:333`) move unchanged
  in content behind this door — their content is kept, their placement is standardized.

**Tier switching:** instant, no restart (§0). Switching STANDARD → SIMPLE while the EQ
screen is open navigates back to the player (the EQ destination does not exist in SIMPLE);
EQ gains themselves are preserved in DataStore and restored on return to PRO — switching
tiers never destroys user data or playback state.

### 5.4 Tier × screen matrix (what each destination shows)

| Destination | SIMPLE | STANDARD | PRO |
|---|---|---|---|
| Library | List, tap-to-play | + filters, sort, favorites | + plugins section (Settings only) |
| Music (Now Playing) | Play/pause/prev/next, seek, volume | + queue, repeat, shuffle, lyrics | + "Pro Settings" door → EQ/speed/A-B |
| Radio | Play/pause, next station | + station list, favorites | — (no pro surface) |
| Artists | List, tap-to-play | + albums, sort | — |
| Video | Play/pause, seek, fullscreen | + aspect, gestures, bookmarks, rotation | + subtitles, audio track, speed, PiP |
| Search | Box + results | + filters, history | — |
| Player & EQ | Player only (EQ hidden) | Player + queue | Player + EQ screen (10-band + curve) |
| Settings | Theme, language, tier switch | + backup, scan, folders | + auto-hide, plugins, backup JSON, demo samples |

Max 3 navigation levels to anything (§0-d rule 2): Shell → destination → sheet/dialog.
The EQ screen is reached as Player → "Pro Settings" sheet → EQ (2 levels).

## 6. Motion

- Durations: **200–300ms** only. `tween(200)` for fades and small moves; `tween(300)` for
  sheet/dialog enter-exit and screen transitions. Nothing slower, nothing bouncy.
- Easing: `FastOutSlowInEasing` for enter, `LinearOutSlowInEasing` for exit.
- The loading skeleton pulse is retimed to 250ms per phase (§4.9).
- No animation may delay playback: play/pause responds on the tap frame; the icon morph
  is cosmetic and runs after the state change, never before it.
- Reduced motion: if the system animator scale is off, all durations become 0ms
  (instant state change). **UNTESTED** on device — verify in Phase 3 QA.

## 7. Fullscreen video & background playback (product requirements 3 + 4)

These are UX contracts the rebuilt UI must preserve; the engine/service work is out of
scope for Phase 1 (no code changes here) and is recorded as already-verified behavior:

- **Fullscreen:** a real fullscreen mode — immersive system-bar hiding + landscape lock
  while fullscreen (existing `FullscreenPolicy.shouldLockLandscape`, unit-tested). The
  fullscreen button is always visible in STANDARD/PRO video; in SIMPLE it is visible too
  (fullscreen is a basic need, not a pro feature). Rotation mid-playback keeps position;
  exiting fullscreen returns to the inline player at the same position.
- **Background playback, music AND video:** playback continues when the app is backgrounded;
  nothing in the UI pauses, releases, or stops playback on lifecycle events. The mini-player
  bar (§4) is the in-app surface; notification + lock-screen controls are the out-of-app
  surface. For video, backgrounding keeps the **audio** playing (video surface detaches,
  audio continues) — the existing `backgroundVideoAudioEnabled` setting (default ON) stays.
- **UNTESTED in this environment:** no device exists here to confirm background behavior
  on a real build; Phase 3 QA must run the §6-a scenario "background music and video" on a
  real device/emulator.

## 8. φ-deviation ledger — every deviation, with reason and percentage

φ = 1.618034 is an Opinion lens. Accessibility always beats φ. Deviations are computed
against the nearest φ-scale value (type: exact 16×φ^(n/3); spacing: nearest Fibonacci or
φ-step; radii: nearest Fibonacci).

| # | Item | Adopted | φ ideal | Deviation | Reason |
|---|---|---|---|---|---|
| D-φ1 | Type rounding (all 10 steps) | 10 / 11.5 / 13.5 / 16 / 19 / 22 / 26 / 30.5 / 35.5 / 42sp | 9.89 / 11.61 / 13.63 / 16 / 18.78 / 22.05 / 25.89 / 30.39 / 35.68 / 41.89sp | **+1.1% / −0.9% / −0.9% / 0% / +1.2% / −0.2% / +0.4% / +0.4% / −0.5% / +0.3%** (max 1.2%) | Rounding to the 0.5sp grid so sizes are specifiable and testable. |
| D-φ2 | Line heights | body 1.50×, labels 1.40×, headlines 1.30×, display 1.25× | 1.618× | **−7.3% / −13.5% / −19.7% / −22.7%** | Density + standard leading. Large display text with φ leading wastes a full line of screen; body 1.50× is the Material readability standard. No accessibility loss: contrast and touch targets unaffected. |
| D-φ3 | Micro-spacing 4dp | 4dp | 5dp (nearest Fibonacci) | **−20.0%** | Half of the 8dp step; used only for hairline-adjacent micro-gaps and the 4dp seek/mini-player progress track. Pixel-grid clean at all densities. |
| D-φ4 | Play button circle | radius 27.5dp (55dp circle) | 34dp (nearest Fibonacci radius) | **−19.1%** | A circle's radius is determined by its diameter, not by the radius scale. Diameter 55dp is exact Fibonacci. |
| D-φ5 | Min touch target 48dp | 48dp | 55dp (nearest Fibonacci) | **−12.7%** | **Accessibility beats φ.** 48dp is the WCAG/Material minimum; the primary play control uses the full 55dp. A 55dp minimum everywhere would push list rows past one-line density. |
| D-φ6 | Icon sizes 20 / 24 / 34dp | 20 / 24 / 34dp | 18.78 / 22.05 / 35.68dp | **+6.5% / +8.8% / −4.7%** (16dp exact, 0%) | Icon optical sizes: 20/24dp are the Material icon grid; 34dp matches the 55dp play button's inner icon (34/55 = 0.618 — the φ split itself). |
| D-φ7 | Wide-screen breakpoint 600dp | 600dp | 610dp (Fibonacci) | **−1.6%** | 600dp is the Material `WindowSizeClass` medium breakpoint; breaking from the platform class would split every responsive rule. |
| D-φ8 | Skeleton pulse 800ms | must become 250ms | 300ms cap (§6) | **+166.7%** (existing code; to be fixed in Phase 2) | The existing `VeloraLoadingSkeleton` pulse (800ms) predates the motion rule. Retiming it is a Phase 2 change, listed here so the audit catches it. |

Deliberately **not** deviations (0% — exact φ/Fibonacci): spacing 2/8/13/21/34/55dp;
radii 8/13/21/34dp; spectrum height 144dp; video surface 233dp; compact card width 233dp;
max content width 610dp; 1dp/2dp borders; the 61.8/38.2 layout split (§3.1, R-S3).

Retired deviations (fixed by this document, kept for the audit trail): old `bodyMedium`
15sp (+9.9% off n=−1) merged into 16sp; old `labelSmall` 11.5sp (+16.3% off n=−3) moved
to 10sp; old `titleMedium` 17sp (−9.5%) → 19sp; old `headlineMedium` 20sp (−9.3%) → 22sp;
old `labelMedium` 12.5sp (+7.7%) → 11.5sp; old muted text `#77746E` (4.01:1, failed AA)
→ `#8F8B83` / `#6B655A`; old 1.5dp border accent → 1dp/2dp only.

## 9. Audit checklist (per screen, Phase 3 — from §0-a)

Verdict `PASS` / `PASS WITH CHANGES` / `FAIL` with one sentence; confidence High/Medium/Low
with one sentence. Checklist: colors are tokens, not hardcoded / typography from §2.2 only /
spacing from §3.1 only / one shared implementation per component (§4) / icons consistent in
style and size (§3.3) / accessibility: contrast ≥4.5:1, touch target ≥48dp, clear focus state,
alt text on meaningful icons / naming per this document / no pro component in the SIMPLE
composition tree (§5.1 test) / Persian RTL everywhere / no dead controls (§0-d rule 4).

## 10. Open questions for the owner (not decisions — Phase 2 must not guess)

1. **Default tier migration (D-3):** fresh installs default to STANDARD, but should existing
   SIMPLE-default users (stored tier SIMPLE from the old `isSimple=true` default) be migrated
   to STANDARD on update, or keep SIMPLE until they switch? Recommendation: keep stored tier.
2. **High-Contrast mono palette:** keep as a third palette option in Settings (as today), or
   promote it to the system-accessibility follows-device setting? Recommendation: keep as an
   explicit option; it is a user choice, not a default.
3. **10sp captions (n=−3):** if Phase 3 QA finds 10sp Persian captions unreadable on a real
   device, the floor rises to 11.5sp — confirm in QA, do not decide here.
4. **Dynamic color:** §1.3 (R-C3) removes Material-You re-huing. If the owner wants it back,
   it returns only behind the same ≥4.5:1 gate, as a PRO setting, not a default.

---
*Sources read for this document: rebuild prompt `~/workspace/your_files/velora-ui-ux-rebuild-prompt.md`
(§0, §4); `~/workspace/PROJECT_HQ/projects/velora/REQUIREMENTS.md`; HEAD `19e70bc` —
`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`; `ui/components/VeloraSharedComponents.kt`;
`ui/navigation/NavGraph.kt` (8 destinations); `ui/screens/SettingsScreen.kt`;
`domain/model/PlaylistAndSettings.kt` (ExperienceTier); `data/settings/VeloraSettingsRepository.kt`.
Contrast ratios computed from hex values with the WCAG 2.1 luminance formula; type-scale and
deviation percentages computed arithmetically. No Gradle run, no device, no screenshots exist
for this phase — anything marked UNTESTED stays untested until Phase 3.*
