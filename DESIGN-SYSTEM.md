# Velora Design System — NEW (Phase 1 of the UI/UX rebuild)

> Status: **specification only — no code changed, nothing built, nothing tested on a device.**
> Branch: `feature/ui-rebuild-phase0-1`, HEAD `19e70bc` ("refactor(settings): introduce experience tiers").
> This file is the single source of truth for the rebuilt UI (Phase 2). Every screen built in
> Phase 2 must use these tokens and components; any value not listed here is a violation, not
> a creative choice. Audit lens: Design System Consistency Auditor (§0 of the rebuild prompt)
> + Apple HIG as review lens only. φ (1.618034) is an **Opinion** proportion lens, not a law —
> accessibility always beats φ (§8 lists every deviation with its percentage).
>
> **Revision 2 — 2026-11-18 (this version).** Aligned with `UI-AUDIT.md` (519 lines, the
> Phase 0 reference, unchanged) per the Gemini review in AI Studio chat
> `1iF3XVNAIxzo2nSNjwlPvRL6N_32jbCly` ("بررسی معماری و طراحی رابط کاربری"). Every Gemini
> finding is addressed in this revision: SIMPLE destinations, the Music/Player matrix rows,
> the stale gesture claim, the write-only `backgroundVideoAudioEnabled` toggle, cover-art and
> Coil scope, undocumented tier moves, missing tokens, typography boundaries, the R-S3 layout
> budget, the Pro Settings door, the SIMPLE proof test, the missing §4 components, and the
> full 120-control coverage map (§12). Where a Gemini claim conflicted with the code, the
> code wins and the discrepancy is recorded inline (see §0-a).
>
> **Revision 3 — 2026-11-18 (this version).** Two Phase 2 blockers closed as decisions
> (delegated authority, code-verified): the D-3 default-tier migration (§10.1, item 1 —
> "keep stored tier", precise rule recorded) and `backgroundVideoAudioEnabled` (§7 —
> **WIRE**, not delete, with the wiring contract). No other section reopened; UI-AUDIT.md
> unchanged.
>
> **Committed status — correction of a Gemini error.** Gemini's review stated this document
> was "an uncommitted change on the working tree". That is wrong: Revision 1 was committed as
> `5847f90` on branch `feature/ui-rebuild-phase0-1`, pushed to GitHub, and is under review in
> PR #3. This Revision 2 is committed on the same branch and updates the same PR.
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
> build-on verdict. The old version remains recoverable via `git show HEAD:DESIGN-SYSTEM.md`.

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
   STANDARD (§5). Migration rule (DECIDED 2026-11-18, precise form in §10.1): existing installs keep their stored tier; fresh installs
   default to STANDARD. Changing an existing user's stored tier silently is forbidden.

What is genuinely new in this document (did not exist at HEAD): the unified token tables
(§1–§3), the retired failing muted-text color, the bottom-sheet and dialog components (§4.6,
§4.7 — dialogs today are per-screen files: `LibraryDialogs.kt`, `VideoPlayerDialogs.kt`,
`PlayerLyricsAndDialogs.kt`), the SIMPLE composition-tree proof design (§5.1), and the
complete φ-deviation ledger (§8).

### 0-a. Where this revision disagreed with the Gemini review (code wins)

Gemini reviewed the two documents without source access; four of its claims were checked
against the code and corrected here instead of adopted:

| Gemini claim | Code reality | How this revision treats it |
|---|---|---|
| DESIGN-SYSTEM.md is an uncommitted working-tree change | Committed `5847f90`, pushed, PR #3 open | Stated as committed; see header |
| Rotation loses fullscreen/lock state (Dialog + `remember {}`) | `MainActivity` declares `configChanges="orientation\|screenSize\|smallestScreenSize\|screenLayout\|keyboardHidden"` (`AndroidManifest.xml:37`), so rotation does **not** recreate the activity and `remember {}` survives it | Rotation is safe by config; **process death** is the real loss — state must still be hoisted (§7, §11). Marked UNTESTED on device |
| Cover art / Coil 3 feasibility "cannot be judged" | `Track.artworkUri: String?` exists (`domain/model/Track.kt:18`, `VeloraEntities.kt:39`); the scanner populates it from MediaStore album art (`MediaLibraryScanner.kt:194`); Coil **2.7.0** is already a declared dependency (`libs.versions.toml:4`, `app/build.gradle.kts:142`) but is imported by **zero** Kotlin files | Cover art is IN SCOPE with Coil 2.7.0 (not Coil 3) and a no-cover fallback (§4.13) |
| SimpleModeCompositionTest runner "unspecified" | Robolectric 4.16.1 is in `testImplementation` with `isIncludeAndroidResources = true` (`app/build.gradle.kts:85,155`); `androidx.compose.ui.test.junit4` is in `testImplementation` (`:150`); `ExampleRobolectricTest` proves Robolectric + Activity launch work | Runner is a Robolectric local unit test (§5.1); the `createAndroidComposeRule`-under-Robolectric pattern itself is still UNTESTED in this project |

## 1. Color tokens

One main accent + one secondary accent, nothing more. Dark is a real dark palette (Obsidian /
Saffron), **not** an inverted light palette: different hues, different accent values, different
error values per theme. All ratios below are WCAG 2.1 luminance ratios computed from the hex
values; rendered-screen confirmation is **UNTESTED**.

### 1.1 Dark — "Obsidian Saffron" (19 tokens)

| Token | Value | Role | Key pairs (ratio) |
|---|---|---|---|
| `bg` | `#0B0B0D` | App background | — |
| `surface` | `#121216` | Cards, sheets, lists | — |
| `surfaceElevated` | `#19191F` | Raised surfaces, player bar | primary text on it 15.65:1 |
| `surfaceCard` | `#202027` | Track cards | primary text on it 14.48:1 |
| `surfaceVariant` | `#1E1E24` | **NEW** — secondary-button fill, inactive seek track | `textPrimary` on it 14.83:1; `textSecondary` 6.51:1; `accent` 7.85:1 |
| `textPrimary` | `#F5F2EB` | Headings, body | on `bg` 17.59:1; on `surface` 16.71:1 |
| `textSecondary` | `#A5A29B` | Metadata, subtitles | on `surface` 7.33:1; on `bg` 7.72:1 |
| `textMuted` | `#8F8B83` | **NEW** — captions, timestamps | on `surface` 5.51:1; on `bg` 5.80:1; on `surfaceCard` 4.77:1 |
| `textDisabled` | `#7A766E` | **NEW** — text on temporarily-disabled controls | on `bg` 4.35:1; on `surface` 4.13:1 (gate R-C6: ≥3:1, disabled text is WCAG-exempt) |
| `controlDisabled` | `#1C1C20` | **NEW** — fill of temporarily-disabled controls | `textDisabled` on it 3.76:1 |
| `accent` (main) | `#E5A817` Saffron | Primary actions, play, progress | on `bg` 9.31:1; on `surface` 8.85:1 |
| `accentSecondary` | `#D9BD83` Champagne Gold | Secondary highlights, active states | on `bg` 10.82:1; on `surfaceCard` 8.91:1 |
| `accentBright` | `#F5C451` | Large display accents only, dark only | on `bg` 12.08:1 |
| `accentContainer` | `#2C2312` | Tinted container behind accent text | `textPrimary` on it **13.85:1**; `onAccentContainer` on it **11.97:1** |
| `onAccentContainer` | `#FFDEA3` | **NEW** — text on `accentContainer` (dark) | on `accentContainer` 11.97:1 |
| `onAccent` | `#0B0B0D` | Text/icons on the accent fill | on `accent` 9.31:1 |
| `error` | `#FF6B6B` | Errors, destructive | on `bg` 7.09:1; on `surface` 6.73:1 |
| `success` | `#4ADE80` | Success only | on `bg` 11.29:1; on `surface` 10.72:1 |
| `divider` | `#2A2927` | Hairline dividers, borders | Decorative only — never a text/background pair |

### 1.2 Light — "Daylight Ivory" (19 tokens — `accentBright` not used in light)

| Token | Value | Role | Key pairs (ratio) |
|---|---|---|---|
| `bg` | `#F9F6F0` | App background | — |
| `surface` | `#FFFFFF` | Cards, sheets | — |
| `surfaceElevated` | `#EDE7DC` | Raised surfaces | `textPrimary` on it 15.18:1 |
| `surfaceCard` | `#FFFFFF` | Track cards (same as surface; border distinguishes) | — |
| `surfaceVariant` | `#EFE9DC` | **NEW** — secondary-button fill, inactive seek track | `textPrimary` on it 15.45:1; `textSecondary` 7.74:1. `accent` on it is 4.33:1 — **no text pair uses accent on `surfaceVariant`** (secondary-button text is `textPrimary`; the selected state uses `accentContainer`); the seek track is decorative |
| `textPrimary` | `#121216` | Headings, body | on `bg` 17.32:1; on `surfaceElevated` 15.18:1 |
| `textSecondary` | `#4C4639` | Metadata, subtitles | on `bg` 8.68:1; on `surface` 9.36:1 |
| `textMuted` | `#6B655A` | **NEW** — captions, timestamps | on `bg` 5.36:1; on `surface` 5.78:1; on `surfaceElevated` 4.70:1 |
| `textDisabled` | `#7E776B` | **NEW** — text on temporarily-disabled controls | on `bg` 4.11:1; on `surface` 4.43:1 (gate R-C6: ≥3:1) |
| `controlDisabled` | `#E7E1D3` | **NEW** — fill of temporarily-disabled controls | `textDisabled` on it 3.40:1 |
| `accent` (main) | `#946200` | Primary actions, play, progress | on `bg` 4.86:1; on `surface` 5.24:1 |
| `accentSecondary` | `#BDA36B` Soft Gold | Secondary highlights — large text / icons only | Decorative/large-only (see rule below) |
| `accentBright` | — (not used in light) | `accentBright` is dark-only; light uses `accent` | — |
| `accentContainer` | `#FFDEA3` | Tinted container behind accent text | `onAccentContainer` `#2F1E00` on it 12.42:1 |
| `onAccentContainer` | `#2F1E00` | Text on `accentContainer` (light) | on `accentContainer` 12.42:1 |
| `onAccent` | `#FFFFFF` | Text/icons on the accent fill | on `accent` 5.24:1 |
| `error` | `#BA1A1A` | Errors, destructive | on `bg` 5.99:1; on `surface` 6.46:1 |
| `success` | `#1E7B34` | **NEW** — success only | on `bg` 4.94:1; on `surface` 5.33:1 |
| `divider` | `#CFC8B8` | Hairline dividers, borders | Decorative only |

### 1.2b High-Contrast mono — complete token table (18 tokens)

R-C3 names this as the third palette but Revision 1 listed only 4 colors. The full table,
verified against the code: `ThemePaletteMode.HIGH_CONTRAST_MONOCHROME` exists
(`PlaylistAndSettings.kt:82`, "OLED High Contrast"). Mono has no hue: `accent` is white,
success is conveyed by icon + text, not color.

| Token | Value | Key pairs (ratio) |
|---|---|---|
| `bg` | `#000000` | — |
| `surface` | `#0D0D0D` | `textPrimary` on it 19.30:1 |
| `surfaceElevated` | `#1A1A1A` | `textPrimary` on it 17.40:1 |
| `surfaceCard` | `#0D0D0D` (same as surface; border distinguishes) | — |
| `surfaceVariant` | `#1E1E1E` | `textPrimary` on it 16.67:1 |
| `textPrimary` | `#FFFFFF` | on `bg` 21.00:1 |
| `textSecondary` | `#D9D9D9` | on `bg` 14.88:1; on `surface` 13.77:1 |
| `textMuted` | `#BFBFBF` | on `bg` 11.42:1; on `surface` 10.57:1; on `surfaceElevated` 9.46:1 |
| `textDisabled` | `#8A8A8A` | on `bg` 6.00:1 (gate R-C6: ≥3:1) |
| `controlDisabled` | `#1A1A1A` | `textDisabled` on it 5.10:1 |
| `accent` | `#FFFFFF` | on `bg` 21.00:1 |
| `accentSecondary` | `#BFBFBF` | on `bg` 11.42:1 |
| `accentContainer` | `#2E2E2E` | `onAccentContainer` on it 13.58:1 |
| `onAccentContainer` | `#FFFFFF` | on `accentContainer` 13.58:1 |
| `onAccent` | `#000000` | on `accent` 21.00:1 |
| `error` | `#FF5252` | on `bg` 6.58:1; on `surface` 6.09:1 |
| `success` | `#FFFFFF` (icon + text carry the meaning, not hue) | on `bg` 21.00:1 |
| `divider` | `#3E3E44` | Decorative only |

### 1.3 Color rules (binding)

- **R-C1.** Every screen uses only these tokens. No hex literal outside the theme files.
  (Existing `Color.kt` values are kept where they match; see §1.4 for what changes.)
- **R-C2.** `accentSecondary` (`#BDA36B`) in light mode is large-text (≥19sp bold) and icon-only:
  as body text on `#F9F6F0` it would be ≈2.8:1 and fail. Same for `accentBright` in dark —
  never as small text on light; it has no light value at all.
- **R-C3.** No dynamic/Material-You color. The code has **four** `ThemePaletteMode` options
  (`PlaylistAndSettings.kt:78-83`): `SYSTEM_DYNAMIC` ("Dynamic (System M3)"),
  `VELORA_SAFFRON`, `VELORA_DAYLIGHT`, `HIGH_CONTRAST_MONOCHROME`. The new system retires
  `SYSTEM_DYNAMIC` in Phase 2: `balanceDynamicColorScheme()` (`Theme.kt`) re-hues the
  palette from the wallpaper and breaks token discipline. The three shipped palettes are
  Saffron dark, Daylight light, High-Contrast mono (§1.2b). If dynamic color ever returns,
  it must pass the same ≥4.5:1 gate on every pair before shipping. (Revision 1 said "three
  palettes" without naming the fourth code option — that omission is fixed here; the
  retirement itself is a documented Phase 2 decision, not a claim about the code.)
- **R-C4 (amended — resolves the Revision 1 contradiction).** Opacity tints: exactly three
  are allowed — `error` at 14% for the error-banner tinted surface (§4.10; Revision 1
  wrongly wrote "`accent` at 14%" for the error banner while §4.10 used the error hue),
  `accent` at 45% for the mini-player border, and `bg` at 60% for the bottom-sheet scrim
  (§4.6; Revision 1 used it while the rule forbade it). No other alpha tints — if a state
  needs emphasis, use a token, not an opacity.
- **R-C5.** Minimum contrast: body text ≥4.5:1, large text (≥19sp bold / ≥24sp) ≥3:1,
  icons that carry meaning ≥4.5:1 against their background, focus ring ≥3:1 against the
  adjacent surface. Rendered-screen verification: **UNTESTED**.
- **R-C6 (new).** Temporarily-disabled controls (a Save button with an empty text field, EQ
  sliders while EQ is off — audit §2.7.17) use the `textDisabled` + `controlDisabled`
  tokens, never an alpha and never a greyed-out look achieved with opacity. Disabled text
  gate: ≥3:1 (WCAG exempts inactive components from the 4.5:1 body-text rule; the chosen
  values reach 3.40–4.43:1). A control that can **never** act is removed, not disabled
  (§4.1) — `textDisabled` is only for controls that become enabled by user input.

### 1.4 What changes vs. the existing `Color.kt` / `Theme.kt`

| Existing value | Verdict |
|---|---|
| `VeloraMutedText #77746E` | **RETIRED as text.** 4.01:1 on `surface`, 4.22:1 on `bg` — fails AA. Replaced by `textMuted #8F8B83` (dark) / `#6B655A` (light). The hex may remain only as a decorative value, never as text. |
| `VeloraDeepSaffron #C88712` | Kept as a decorative/large-only dark accent (6.49:1 on `bg`); **not** text on light (2.81:1 on `bg`). Not a token — do not use in new screens. |
| `VeloraBrightSaffron #F5C451` | Kept as dark-only `accentBright`; 1.51:1 on light `bg` — never in light mode. Also the dark `onAccentContainer` value (§1.1). |
| `FazoraEmeraldOk #4ADE80` | Kept as dark `success`; light `success` is new `#1E7B34` (4.94:1). |
| `VeloraDaylightContainer #FFDEA3`, `OnContainer #2F1E00` | Kept as `accentContainer` / `onAccentContainer` (12.42:1). Dark gains its own `onAccentContainer #FFDEA3` (11.97:1 on `#2C2312`). |
| `VeloraShapes` (8/13/21) | Kept; see §3. |
| `PhiTokens` spacing | Kept; see §3. |
| (none — new tokens) | `surfaceVariant`, `textDisabled`, `controlDisabled` are new in both themes (§1.1, §1.2, §1.2b). |

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

**R-T2 boundary (new — resolves the §2.1 vs R-T2 contradiction).** "Digits in Persian text"
(§2.1) and "data digits" (R-T2) are different things; the boundary is the *role* of the
number, not the language of the sentence:

| Number role | Digits | Font | Example |
|---|---|---|---|
| Persian prose counts (a number that is part of a Persian sentence) | Persian digits (۱۲۳) | Vazirmatn | «۱۲ آهنگ»، «۳ پلی‌لیست» |
| Persian calendar dates in prose | Persian digits | Vazirmatn | «۱۵ آبان» |
| Measurements and data values, in any language (time, duration, dB, Hz, bitrate, file size, percentage, track position) | Latin digits (123) | JetBrains Mono, LTR-embedded | `03:24`، `+3.0 dB`، `96 kHz`، `1411 kbps`، `45%` |
| UI chrome counts (track-count badge, queue position "3/12") | Latin digits | JetBrains Mono | `21 tracks` in the header badge is rendered `21` |
| Mixed metadata (track/artist/album names, file paths) | As authored by the user/data | R-T3 mechanism below | A Latin song title inside a Persian list keeps its Latin glyphs |

**R-T3 mechanism (new — states the how, not just the rule).** Compose `FontFamily` does not
do per-script fallback, so mixed-language text is rendered in two layers:
- **(a) Static UI strings** are localized resources: the Persian string set renders in
  Vazirmatn, the English string set in Inter/Space Grotesk, chosen by locale. No mixed
  string is ever set in a single `Text` with one family.
- **(b) Dynamic user metadata** (track title, artist, album, file paths — script unknown at
  authoring time) goes through a `mixedScriptText()` helper that splits the string into
  Persian-script and Latin-script runs and applies Vazirmatn vs Inter via `AnnotatedString`
  `SpanStyle` per run. If a run cannot be classified, it renders in Inter (Latin is the
  safe default for file paths and formats). This helper is a Phase 2 deliverable; until it
  exists, dynamic metadata renders in Inter with Vazirmatn fallback glyphs — recorded as a
  known gap, not as compliant.

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

**Custom tokens (new — completes the §2.2 → Type.kt mapping).** Material `Typography` has
no slots for n=+4 and n=+6, so they are custom tokens on `VeloraTypography`, not Material
slots: `VeloraTypography.largeHeadline` = 30.5sp / 38 line height / Bold (n=+4, large
headline), `VeloraTypography.heroDisplay` = 42sp / 52 line height / Bold (n=+6, hero
display). All other steps map to the slots above. No screen invents a size outside §2.2
plus these two tokens.

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
token (existing `PhiTokens` already enforces this pattern; keep it). **R-S3 (revised — see
§3.1b).** The 61.8 / 38.2 split is now a **cap, not an exact split** (Revision 1's exact
split does not fit the controls — proven below).

### 3.1b Vertical budget — why the exact 61.8 / 38.2 split cannot stand (new)

Usable height on a standard phone after the top bar, mini-player and nav bar is ~640–780dp.
38.2% of that is **244–298dp**. The Now Playing controls stack, measured from the tokens:

| Row | Height |
|---|---|
| Title (26sp, line height 34dp) | 34dp |
| Artist (13.5sp, line height 20dp) | 20dp |
| Seek bar row (48dp touch target incl. time labels) | 48dp |
| Transport row (55dp play circle governs the row) | 55dp |
| Volume slider row (48dp touch target) | 48dp |
| Gaps between 5 rows (4 × 8dp) | 32dp |
| **Controls subtotal** | **237dp** |
| Queue header | 24dp |
| One queue row | 56dp |
| **Controls + queue with ONE row visible** | **317dp** |

317dp > 298dp (the best case) and > 244dp (the worst case) — the exact split is short by
**19–73dp even for a single queue row**, and it breaks the 48dp touch minimum if squeezed.
Revised rule **R-S3**: the stage (cover art / spectrum, §3.1c) takes **up to 61.8%** of the
available height; the controls + queue area takes the remainder with a **minimum of 317dp**;
when the remainder is smaller than 317dp the stage shrinks below 61.8% (never the controls);
the queue is an independently scrolling `LazyColumn` that shows at least one row and expands
to fill the remaining space. The stage never pushes a control below its touch minimum.

### 3.1c The Now Playing stage — cover art vs spectrum (new)

Audit §2.7.2: today the stage is the spectrum canvas, not artwork. This revision keeps the
stage and adds cover art as its default layer (decision in §4.13):

- **Stage modes** (PRO-only `VisualizerMode` selector, audit §2.7.3): **Artwork** (default) —
  cover art fills the stage with the spectrum strip overlaid at the bottom (§3.3);
  **Spectrum** — the full-stage `SpectrumAnalyzerCanvas` (today's behaviour); **Off** — a
  plain `surface` stage with title text only. The mode is a user choice, persisted.
- **SpectrumHeight 144dp** (§3.3) is the height of the spectrum strip in Artwork mode and
  of the canvas header in Spectrum mode — it is a strip inside the stage, not a second stage.
- **VideoSurfaceHeight 233dp** (§3.3) is the inline video surface height: it is exactly
  16:9 at a 414dp-wide screen (414 × 9/16 = 233.4dp) — the derivation Revision 1 omitted.
  In landscape split layouts the 61.8 / 38.2 split applies to surface vs control strip
  instead of the fixed height.

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
| 4.1 | `VeloraPrimaryButton` | Exists — keep | Accent fill, `onAccent` text, radius 13dp, min height 48dp, horizontal padding 13dp, vertical 8dp. One primary action per screen (§0-d rule 1). Disabled state: a temporarily-disabled button uses `textDisabled` on `controlDisabled` (R-C6); a button that can never act is removed, not greyed out (§0-d rule 4). |
| 4.2 | `VeloraSecondaryButton` | Exists — keep | Outlined, `surfaceVariant` fill (now defined, §1.1/§1.2), radius 13dp, min height 48dp. `selected` state uses `accentContainer` fill + accent text. Used for tier switching, filters, secondary actions. |
| 4.3 | `VeloraTrackCard` (`TrackRowItem`) | Exists — keep, tighten | Radius 13dp, 48dp cover thumb (radius 8dp, §4.13), title 16sp, artist/album 13.5sp, format line 11.5sp JetBrains Mono. Active card: `accentContainer` fill + accent border. In SIMPLE mode the card exposes **only** play on tap — favorite/queue/playlist buttons are not composed (see §5.1). |
| 4.4 | `VeloraSeekBar` | Exists — keep | Accent active track, `surfaceVariant` inactive (decorative — no text pair, §1.2), 4dp track height (§8, D-φ3 bucket), thumb 20dp. Time labels 11.5sp JetBrains Mono, elapsed in accent, total in `textSecondary`. Reads position from the narrow `PlaybackPositionState` (position/duration only) — never from a full UiState recompute (the 10 Hz tick rule). |
| 4.5 | `VeloraTransportControls` | Exists — keep, tighten | Play/pause 55dp accent circle; prev/next 34dp icons; shuffle/repeat **not composed** in SIMPLE (already gated at `VeloraSharedComponents.kt:456`). All buttons ≥48dp touch target via `minimumInteractiveComponentSize()`. |
| 4.6 | `VeloraBottomSheet` | **NEW** — no shared sheet exists today | Radius 21dp top corners, drag handle 34dp wide / 4dp tall in `divider`, scrim `bg` at 60% (allowed by amended R-C4). One sheet per screen max; sheets host secondary actions (queue, track menu, "Pro settings" door) — never the primary action. **Not usable over the fullscreen video Dialog** — see §5.3b. |
| 4.7 | `VeloraDialog` | **NEW** — standardize the three per-screen dialog files | Radius 21dp, 21dp padding, title 19sp SemiBold, body 16sp, actions are `VeloraPrimaryButton` + `VeloraSecondaryButton` only. Replaces ad-hoc dialogs in `LibraryDialogs.kt`, `VideoPlayerDialogs.kt`, `PlayerLyricsAndDialogs.kt` — those files are deleted in Phase 2 and their content re-implemented on this component. **Before deletion, inventory all 125 testTags** (audit §0) and map each one to the new component or retire it explicitly — a test that references a deleted dialog's tag is a Phase 2 failure, not a cleanup. |
| 4.8 | `VeloraEmptyState` | Exists — keep | Radius 21dp, 34dp padding, 55dp accent-container icon well, title 22sp Bold, subtitle 16sp `textSecondary`, optional primary action. Every list/search/playlist screen has a designed empty state (§0-d rule 5) — never a blank screen. |
| 4.9 | `VeloraLoadingSkeleton` | Exists — **retime** | Same structure (label + 55dp rows, radius 13dp), but pulse **200–300ms** (§6). The existing 800ms pulse is +166.7% over the cap (§8, D-φ8) and must change. |
| 4.10 | `VeloraErrorBanner` | Exists — keep | **Error tint 14%** (error hue — Revision 1 wrongly said accent 14%; amended R-C4), radius 13dp, error icon + message 16sp + one secondary action ("تلاش دوباره" / "Retry"). A corrupt file shows this banner with its name and a "skip" action — never a crash, never a white screen (§0-d rule 5). The video Retry action today only toggles play/pause (audit §5.12) — Phase 2 gives it a real retry (re-load the item), it is not specified as toggle-play. |
| 4.11 | `VeloraVideoSurface` | Exists — keep, re-contract | Media3 `PlayerView` in `AndroidView`, aspect resize modes, test tag `surfaceTestTag`. **New contract (§11):** receives a `VideoSurfaceState` (surface token / player handle owned by the service layer), never a raw `PlayerEngine` — the current downcast to `Media3PlayerEngine` to unwrap `ExoPlayer` (`VeloraVideoSurface.kt:35`) is deleted in Phase 2. The shared gesture modifier `veloraCinemaGestures` lives here (§4.14). |
| 4.12 | `SpectrumAnalyzerCanvas` | Exists — keep, place explicitly | Spectrum bars + EQ curve overlay, visualizer modes. Placed per §3.1c: in Artwork stage mode it is the 144dp strip at the bottom of the stage; in Spectrum stage mode it fills the whole stage. It is **not** a second stage and never competes with cover art for the same area. |
| 4.13 | `VeloraCoverArt` | **NEW** — cover art is IN SCOPE | Renders `Track.artworkUri` (exists: `domain/model/Track.kt:18`, populated by the scanner from MediaStore album art, `MediaLibraryScanner.kt:194`) with the already-declared **Coil 2.7.0** dependency (`app/build.gradle.kts:142`; imported by zero files today — Phase 2 wires it, no new dependency, **not Coil 3**: the rebuild prompt's approved list said Coil 3, the project ships Coil 2.7.0 and the design follows the project). **No-cover fallback is specified, not assumed:** when `artworkUri` is null or fails to load, the component renders the current icon tile (MusicNote/Movie) on `surfaceVariant` — never a blank box, never a broken-image glyph. Sizes: 48dp thumb in `VeloraTrackCard` (radius 8dp), stage cover per §3.1b/§3.1c (radius 34dp hero). |

Also carried over unchanged: `VeloraSectionCard` (radius 21dp, 21dp padding) and
`MiniPlayerBar` (radius 13dp, accent progress line, play/next only — **visible on every
route including Player and Video** (it is rendered in the shell `bottomBar`,
`VeloraShellScreen.kt:184`, whenever a track is loaded, audit §2.1.2). That is deliberate
and kept: it is the persistent background-playback surface and the tap target back to the
Player route. On the Player route it duplicates the transport controls — accepted
duplication, not a bug; hiding it there would remove the one control that is always
reachable. It has no Previous button (audit §5.12) — that gap is recorded in §12, not
silently fixed here.

**Component rules:** R-K1. No per-screen duplicate of any component above. R-K2. No hardcoded
colors, sizes, or radii inside a component — tokens only (auditor checklist §0-a). R-K3.
Every meaningful icon has a `contentDescription` (localized); decorative icons use `null`.
R-K4 (corrected — replaces the Revision 1 claim). There is exactly **one** gesture
implementation today: `Modifier.veloraCinemaGestures` (`VeloraVideoSurface.kt:72-140`),
called from 3 sites (inline video, fullscreen dialog, spectrum canvas — audit §4 C2). The
Revision 1 claim of "three copy-pasted gesture layers (~150 lines), already diverged" is
**stale and deleted**. What Phase 2 actually fixes, from audit §5.4:
- **(a) Unstable `pointerInput` keys.** `VeloraVideoSurface.kt:72-73` keys the tap block on
  `positionMs`, which changes 10×/s — the block restarts on every position tick and a
  double-tap straddling a tick boundary can be dropped. Fix: stable keys (`controlsLocked`
  only), position read from a remembered state.
- **(b) Silent no-ops on the music screen.** `NowPlayingScreen.kt:339-356` wires brightness
  and tap-toggle to `{}` — brightness drag does nothing on the music screen while the same
  gesture works on video. Fix: on the music screen the brightness drag is **removed**
  (no brightness gesture on an audio-only stage); volume drag and double-tap seek stay.
  A gesture that does nothing must not exist.
- **(c) One `GestureConfig`.** A single config object (seek step, brightness/volume enable
  flags — audit §3.3) drives all three call sites instead of per-site lambdas.

## 5. Experience tiers — the heart of the product requirements

Tier is a single `ExperienceTier` value from DataStore (§0), read as state, applied instantly
with no restart. Tier switching is a recomposition, not a relaunch: the shell and the current
screen re-read `settings.experienceTier` and re-compose. **No screen may cache the tier in a
`remember` that survives a switch.**

### 5.1 SIMPLE — "only what's needed"

Visible, and nothing else. **Corrected to the audit** (`VeloraShellScreen.kt:131-143`):
in SIMPLE only **4 destinations** are shown — Music, Video, Player, Settings — and home is
MUSIC. Library, Artists, Radio and Search are **hidden** (removed from nav); switching to
SIMPLE while on a hidden destination force-navigates to Music. Revision 1 wrongly said the
Library list and search box are available in SIMPLE — they are not, and neither is any
other Library/Artists/Radio/Search surface.

- Play/pause (55dp accent circle), previous, next.
- `VeloraSeekBar` (position + duration).
- Volume (slider in the player; hardware keys everywhere).
- Current track title/artist + cover art (§4.13). Mini-player bar.
- Settings entry (theme, language, tier switch, and the ungated settings rows — §5.4).

**Provably simple — the test design (Phase 2 implements it; the design is fixed here):**

- Test `SimpleModeCompositionTest` (Compose UI test, `createAndroidComposeRule`):
  1. Set tier SIMPLE via `updateExperienceTier(SIMPLE)` (DataStore, no restart).
  2. Assert the shell shows **exactly the 4 visible destinations** (music, video, player,
     settings) and that library, artists, radio, search are **absent** from the navigation
     semantics — then, on each of the 4 visible destinations, assert the composition tree
     contains **zero** nodes with any pro test tag (see the full list below). `tier_option_PRO`
     is allowed (it is the door out, not a pro control) — everything else in the PRO list
     must be absent.
  3. Assert the SIMPLE allow-list is present: `transport_play_pause_button`,
     `transport_prev_button`, `transport_next_button`, `player_seek_slider`.
  4. Assert with semantics: no node exposes a pro `contentDescription` ("Toggle Shuffle",
     "Cycle Repeat Mode", "Equalizer", …) in SIMPLE.
- **The full absence list (expanded — Revision 1 checked only ~9 pro tags).** On the
  Player destination in SIMPLE, all of these must be absent from the tree: `transport_shuffle_button`,
  `transport_repeat_button`, `eq_*`, `speed_*`, `subtitle_*`, `audio_track_*`, `ab_loop_*`,
  `pip_button`, `video_auto_hide_slider`, **and** `queue_*`, `favorite_button`,
  `add_to_playlist_*`, `player_stop_button`, `exact_seek_button`, `tag_editor_button`,
  `lyrics_*`, `lrc_studio_*`, `time_machine_*`, `visualizer_mode_*`, `sleep_timer_*`,
  `crossfade_*`, `pro_settings_door` (§5.3b — the door itself is removed in the SIMPLE
  player). On the Music destination: `shuffle_all_button`, `quick_filter_field`,
  `hires_filter_toggle`. On the Video destination: `speed_*`, `subtitle_*`,
  `audio_track_*`, `bookmark_button`. On the Settings destination:
  `video_auto_hide_slider`, `plugin_toggle_*`. (Tag names are the contract — Phase 2 must
  use exactly these tags; a renamed tag that dodges the test is a violation.)
- **The Pro Settings door is removed in the SIMPLE player.** Per "Visible, and nothing
  else", the door is itself a pro control: in SIMPLE the player has no "Pro Settings" entry
  anywhere. The only way out of SIMPLE is the tier switch in Settings.
- The proof is structural (absence from the tree), not visual: hiding a control with
  `alpha(0)` or `visibility = Gone` fails the test — the composable must not be emitted.
  `if (tier != SIMPLE)` gating at the call site is the only accepted mechanism (the
  existing `VeloraTransportControls` already does this correctly).
- **Runner: Robolectric local unit test.** `robolectric` 4.16.1 is in `testImplementation`
  with `isIncludeAndroidResources = true`, and `androidx.compose.ui.test.junit4` is in
  `testImplementation` (`app/build.gradle.kts:85,150,155`); `ExampleRobolectricTest` proves
  Robolectric + Activity launch work in this project. The test runs in CI on every PR with
  no emulator. **UNTESTED:** no `createAndroidComposeRule` test exists in this project
  yet — the pattern under Robolectric must be proven by the first run of this test in
  Phase 2; if it cannot run under Robolectric, it moves to `androidTest` (emulator) and
  this section is amended, not silently reinterpreted.
- This test runs in CI on every PR. A screen that cannot pass it in SIMPLE does not ship.

### 5.2 STANDARD — the default (fresh installs; existing users keep their stored tier)

Everything in SIMPLE, plus the everyday set:

- Queue (view, add, remove — **no reorder**: queue reorder does not exist in the code,
  audit §2.7.29; it was listed in Revision 1 as if it did — removed, see §10.5).
- Playlists (create, add/remove track, delete playlist), M3U8 export/import, smart
  playlists + analytics, playback history.
- Search with filters + debounced input (no full search per keystroke), semantic shortcut
  chips, "valid songs only", Media Copilot.
- Favorites, artists views, radio (genre chips, Tune Live, add to queue, custom station).
- Video: fullscreen, aspect modes (FIT/FILL/GOLDEN_PHI/16:9 — moved from PRO, justified
  in §5.5), rotation, double-tap seek, brightness/volume gestures, scan/pick-video.
- Background playback controls (notification / lock screen).
- Library folder **viewing** is not a STANDARD feature — folder management stays PRO (§5.5).

STANDARD is the default because it is the complete everyday player: nothing a daily user
needs is behind a door, and nothing pro is in the way.

### 5.3 PRO — "Studio" — everything, behind one explicit door

Everything in STANDARD, plus — all reachable only through the single labelled door
**"تنظیمات حرفه‌ای / Pro Settings"** (a bottom sheet from the player, and a section in
Settings). No pro control appears on any main screen:

- 10-band EQ with live curve + presets (**local only** — cloud presets do not exist in the
  code, audit §2.7.16; Revision 1 listed them — removed, see §10.5), per-band gains in dB.
- Playback speed 0.5×–2.0×, A-B loop, sleep timer, crossfade (both live on the PRO EQ/DSP
  screen — reverted from STANDARD, §5.5).
- Subtitles (SRT track select, font scale, colour), audio-track select, video bookmarks.
- Video auto-hide timeout (1500–15000ms), adaptive audio mode is **not** PRO (it is ungated
  in the code, audit §2.9.4 — Revision 1 listed it here; moved to all tiers), declarative
  plugins, demo-sample management is **not** PRO (ungated, audit §2.9.9/11 — moved to all
  tiers). PiP is **not** PRO (always-on, §5.5).
- The existing PRO-only Settings sections (`SettingsScreen.kt:246`, `:333`) move unchanged
  in content behind this door — their content is kept, their placement is standardized.

**Tier switching:** instant, no restart (§0). Switching STANDARD → SIMPLE while the EQ
screen is open navigates back to the player (the EQ destination does not exist in SIMPLE);
EQ gains themselves are preserved in DataStore and restored on return to PRO — switching
tiers never destroys user data or playback state.

### 5.3b The Pro Settings door in fullscreen video (new — resolves the Z-order problem)

`VideoPlayerScreen` fullscreen is a Compose `Dialog` (`FullscreenVideoDialog`,
audit §2.6.19). A `ModalBottomSheet` creates its **own window** and cannot reliably overlay
another Dialog's window (Z-order / window-token problem in Jetpack Compose). The pattern is
therefore:

- **On non-dialog screens** (inline video, player, settings): the door is a
  `ModalBottomSheet` hosting `VeloraBottomSheet` (§4.6).
- **In fullscreen video:** the door is an **in-dialog bottom panel** — a `Column` anchored
  to the bottom of the `FullscreenVideoDialog` itself, with the same 21dp top radius, drag
  handle and content as the sheet, but rendered inside the dialog's window. It is not a
  `ModalBottomSheet`, not a second Dialog. Closing it returns to the fullscreen controls;
  it never exits fullscreen by itself.
- No pro control appears on the fullscreen surface itself; the door is the only entry point.

### 5.4 Tier × screen matrix (what each destination shows) — corrected to the audit

"—" means the destination is hidden in that tier or has no surface there. Every row is
traceable to audit §2; the full control-by-control map is §12.

| Destination | SIMPLE | STANDARD | PRO |
|---|---|---|---|
| Library | — (hidden; not in nav) | Playlists, M3U8, smart playlists + analytics, history, filters, sort, track list, scan, import | + folder management, smart-rule builder |
| Music | Play All, track list (tap to play), scan | + Shuffle All, quick filter, Hi-Res filter | — (no pro surface; EQ lives on the Player route) |
| Radio | — (hidden) | Genre chips, Tune Live, add to queue, custom station dialog | — |
| Artists | — (hidden) | Selector chips, Play Artist, Shuffle Artist, track list | — |
| Video | Surface, lock/unlock, tap toggle, seek, play/pause, fullscreen, PiP, scan/pick-video, track list | + aspect modes, double-tap seek, brightness/volume gestures | + speed, subtitles, audio track, bookmarks |
| Search | — (hidden) | Query, semantic chips, valid-songs-only, Media Copilot, results | — |
| Player & EQ | Player only: transport, seek, volume, mute, title/artist + stage (EQ hidden, no Pro door) | + queue, repeat, shuffle, stop, exact-seek, favorite, tag editor, lyrics + offset | + EQ screen (10-band + curve, local presets, preamp, sub-bass, stereo width, A-B, crossfade, sleep timer), visualizer-mode selector, LRC Studio, time-machine bookmarks |
| Settings | Theme, language, tier switch, auto-scan, valid-songs-only, background-video-audio (decision open, §7), low-battery DSP saver, demo samples, scan now, purge demo, Google sign-in, backup export/restore | (no STANDARD-only rows — every ungated row is already visible in SIMPLE) | + video auto-hide timeout, declarative-plugin toggles |

Max 3 navigation levels to anything (§0-d rule 2): Shell → destination → sheet/dialog.
The EQ screen is reached as Player → "Pro Settings" sheet → EQ (2 levels).

### 5.5 Tier-move decisions — every undocumented move from Revision 1, decided (new)

Revision 1 moved several features between tiers without a reason. Each move is now either
justified or reverted; the audit is the reference unless a reason is recorded here.

| Feature | Audit value | Revision 1 | This revision | Decision / reason |
|---|---|---|---|---|
| Aspect modes (FIT/FILL/GOLDEN_PHI/16:9) | PRO (§2.6.12) | STANDARD | **STANDARD** | Justified: basic framing — FIT is the default view every video user needs; the sibling control (fullscreen) is already basic, not pro. |
| Video bookmarks | PRO (§2.6.15) | STANDARD | **PRO (reverted)** | Scene bookmarks are a power feature; no everyday need. |
| PiP | Always-on (§2.6.9, no tag) | PRO | **Always-on (reverted)** | Platform multitasking, ungated in code, `supportsPictureInPicture=true` in the manifest; gating it would remove an existing capability. Auto-PiP on exit follows the same rule (always-on). |
| Sleep timer | PRO (§2.7.23) | STANDARD | **PRO (reverted)** | Lives on the PRO EQ/DSP screen; STANDARD has no home for it after the EQ screen is separated. |
| Crossfade | PRO (§2.7.22) | STANDARD | **PRO (reverted)** | Same as sleep timer. |
| Folder management | PRO (§2.2.5–7) | STANDARD | **PRO (reverted)** | Library administration, not everyday playback. |
| Backup export/restore | Ungated (§2.9.14–15) | STANDARD + PRO ("backup JSON") | **All tiers (clarified)** | Data safety for every user; Settings is a SIMPLE-visible route, so every ungated row is visible in SIMPLE. The duplicate PRO "backup JSON" row was the same feature listed twice — removed. |

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
scope for Phase 1 (no code changes here):

- **Fullscreen:** a real fullscreen mode — immersive system-bar hiding + landscape lock
  while fullscreen. **Corrected (Revision 1 overstated the guarantee):** the lock input
  must be the video's **actual dimensions** (`VideoSize` from Media3), not the aspect
  *mode* — today `VideoPlayerDialogs.kt:203-207` derives the ratio from
  `videoAspectMode` (GOLDEN_PHI → φ, everything else → 16/9), so a portrait video in
  FIT/FILL mode still locks to landscape (audit §5.5). Phase 2 wires the real dimensions
  into `FullscreenPolicy.shouldLockLandscape` (the policy function itself is correct).
  **Rotation:** Revision 1 and Gemini both claimed rotation loses fullscreen state. That
  is wrong for rotation: `MainActivity` declares
  `configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"`
  (`AndroidManifest.xml:37`), so rotation does **not** recreate the activity and the
  Dialog's `remember {}` state survives it. **Process death** is the real loss — fullscreen
  and lock state must still be hoisted to the ViewModel / saved state in Phase 2 (§11).
  Rotation behaviour on a real device is **UNTESTED** — verify in Phase 3 QA. The
  fullscreen button is always visible in STANDARD/PRO video; in SIMPLE it is visible too
  (fullscreen is a basic need, not a pro feature). Exiting fullscreen returns to the inline
  player at the same position.
- **Background playback, music AND video:** playback continues when the app is backgrounded;
  nothing in the UI pauses, releases, or stops playback on lifecycle events. The mini-player
  bar (§4) is the in-app surface; notification + lock-screen controls are the out-of-app
  surface. For video, backgrounding keeps the **audio** playing (video surface detaches,
  audio continues).
- **`backgroundVideoAudioEnabled` — DECIDED: WIRE (2026-11-18, code-verified).** Revision 1 recorded
  this setting (default ON) as "already-verified behavior". That was wrong: the toggle is
  **write-only** — it has no consumer in `Media3PlayerEngine` or `VeloraPlaybackService`
  (audit §5.8); the only places it appears are the DataStore read/write
  (`VeloraSettingsRepository.kt`: `KEY_BG_VIDEO_AUDIO`, default `true`;
  `updateBackgroundVideoAudio()`), the `AppSettings` field
  (`PlaylistAndSettings.kt:110`, default `true`), and the Settings toggle itself
  (`SettingsScreen.kt:210`, tag `settings_bg_video_audio_switch`). Background audio works
  in practice via the foreground `MediaSessionService` regardless of the toggle.
  **Decision: wire it, do not delete it.** Deleting would remove a user-facing control for a
  locked product requirement (background playback for BOTH music and video must never stop —
  and the toggle's OFF state is the user's explicit opt-out of *video* audio in the
  background). **Wiring contract (Phase 2 task):** `MediaOrchestrator` is the wiring point —
  it already holds `VeloraSettingsRepository` (`MediaOrchestrator.kt:76`) and reads settings
  at bootstrap (`:124`), and it exposes `pause()` (`:194-195`, delegates to
  `playerEngine.pause()`); the current item's kind is available as `Track.mediaType`
  (`Track.kt:17`, `MediaType.VIDEO`). No background hook exists anywhere in the code today
  (no `ProcessLifecycleOwner` / `ON_STOP` observer) — Phase 2 adds one: on app-background
  (`ON_STOP`), if the current item is a video AND `backgroundVideoAudioEnabled` is OFF →
  `orchestrator.pause()`. If the toggle is ON (default) or the current item is audio → do
  nothing; playback continues through the foreground `MediaSessionService`
  (`VeloraPlaybackService`, whose `onTaskRemoved` only stops a non-playing service). No
  auto-resume on return to foreground — resume is a user action, and the notification keeps
  offering it. **Verification (Phase 3 QA, device):** video playing + toggle OFF +
  background the app → audio stops; toggle ON + background → audio continues; audio track
  + toggle OFF + background → keeps playing. All three are **UNTESTED** in this environment
  (no device). Until the wiring lands, the toggle stays in Settings as-is and is flagged
  here, not presented as working.
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
max content width 610dp; 1dp/2dp borders.

**Retired (Revision 2):** the exact 61.8 / 38.2 layout split (§3.1 R-S3 in Revision 1) is
retired as an exact rule — §3.1b proves it under-budgets the controls by 19–73dp. The
split survives as a **cap** (stage ≤ 61.8%, controls ≥ 317dp minimum). This is a rule
correction, not a φ deviation: the ratio itself was never the problem, the missing
minimum was.

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
composition tree (§5.1 test) / Persian RTL everywhere / no dead controls (§0-d rule 4) /
**all 120 audit controls mapped in §12 (or explicitly re-homed with a note)** / **all 125
testTags inventoried before any dialog file is deleted (§4.7)** / disabled states use
`textDisabled`/`controlDisabled`, never alpha (R-C6) / no alpha outside the three allowed
tints (R-C4).

## 10. Open questions for the owner (not decisions — Phase 2 must not guess)

Items marked **DECIDED** were decided 2026-11-18 under delegated authority, code-verified,
and are recorded here so Phase 2 does not re-decide them.

1. **Default tier migration (D-3) — DECIDED 2026-11-18 (code-verified): keep stored tier.**
   **Rule, by stored state** (resolution in `VeloraSettingsRepository.kt:231-237`):
   - Stored `experience_tier` that parses → keep it verbatim (SIMPLE, STANDARD, or PRO).
     No write, no migration step touches it.
   - No tier stored, but `is_simple_mode` stored (legacy install) → keep the
     boolean-derived tier (`true` → SIMPLE, `false` → PRO). The read resolution already
     does this; the §11 one-shot migration only makes the value explicit (writes exactly
     the derived tier) and deletes the legacy key/path — it never writes STANDARD over a
     stored value.
   - Neither key stored (fresh install) → **STANDARD**. This is the only behaviour that
     changes, and it requires a code change: today `prefs[KEY_SIMPLE_MODE] ?: true`
     (`:231`) conflates "absent" with "stored true", so a fresh install resolves to
     SIMPLE. Phase 2 reads the boolean as nullable and falls back to STANDARD only when
     both keys are absent. The `AppSettings` constructor default (`isSimpleMode = true`,
     `PlaylistAndSettings.kt:106-107`) is aligned to STANDARD in the same change.
   **Why this is safe:** the resolution reads stored values first and no code path
   overwrites a stored tier on read; `updateExperienceTier()` writes tier + mirror
   atomically (`:118-123`). An existing user therefore keeps their exact effective tier
   across the update — zero DataStore writes to existing users are required for
   correctness. **Precondition:** D-1 must land in the same change — `updateSimpleMode()`
   is still called from `VeloraViewModel.kt:215` and maps "not simple" → PRO, so the
   legacy boolean path must be deleted before the new default is introduced, or a legacy
   toggle call could still erase STANDARD. D-2 (`VideoPlayerScreen.kt:114` re-derivation)
   is fixed in the same change: screens read `settings.experienceTier` directly.
   **Verification (Phase 2):** unit test over `mapPreferencesToSettings` for all stored
   states (tier SIMPLE / STANDARD / PRO, boolean-only true, boolean-only false, and empty
   prefs → STANDARD). UNTESTED here (no Gradle run in this environment).
2. **High-Contrast mono palette:** keep as a third palette option in Settings (as today), or
   promote it to the system-accessibility follows-device setting? Recommendation: keep as an
   explicit option; it is a user choice, not a default.
3. **10sp captions (n=−3):** if Phase 3 QA finds 10sp Persian captions unreadable on a real
   device, the floor rises to 11.5sp — confirm in QA, do not decide here.
4. **Dynamic color — decided, removed from open questions.** Revision 1 asked whether to
   bring Material-You back. The decision is now recorded in R-C3: `SYSTEM_DYNAMIC` is
   retired in Phase 2 (token discipline wins); it returns only behind the ≥4.5:1 gate on
   every pair, as a PRO setting, not a default. If the owner disagrees, this is the item to
   reopen — the decision is explicit, not silent.
5. **Removed features — future candidates, not existing UI.** Revision 1 designed UI for six
   features that do not exist in the code (audit has no trace of them). They are **removed**
   from §§5.2–5.4. Each is a candidate only after its domain/data prerequisite lands in a
   separate Phase 2 task — never build the UI first:
   - Cloud EQ presets — needs a cloud sync backend (none exists; Firebase is local-first).
   - Queue reorder — needs reorder support in `PlaybackQueue` / orchestrator (today: view,
     add, remove only).
   - Gesture-settings panel (double-tap distance, swipe sensitivity) — needs a persisted
     `GestureConfig` (audit §3.3 proposes it as a config object, not as a settings screen).
   - Search history — needs a persisted query-history store.
   - Artists albums view — needs an album grouping in the library layer (today: artist
     chips + track list only).
   - Radio next-station / favorites — needs a station model with ordering + favorites
     (today: genre chips + Tune Live + add-to-queue + custom station only).

## 11. Architecture contracts (from `ARCHITECTURE.md`, branch `docs/architecture`)

These are not re-decided here — they are the architecture doc's contracts, restated so
Phase 2 does not rebuild the UI on top of the old state boundary. Where Gemini raised an
architecture risk, the contract that answers it is cited.

- **UiState slicing.** The single god `VeloraUiState` (35 fields, audit §3.1) is sliced
  into per-screen states (`LibraryUiState`, `PlayerUiState`, `VideoUiState`, …). Position
  consumers keep using only the narrow `PlaybackPositionState` flow, and the
  position-stripping guard (audit §3.2 — `distinctUntilChanged` with `positionMs` zeroed)
  is an explicit rule, not an accident. Spectrum/visualizer telemetry (~20 Hz) travels on
  its own flow and never enters the structural combine (ARCHITECTURE.md §2.3 — the live
  leak the audit's §3.2 filter does not catch: spectrum fields pass the position filter).
- **No raw engine in UI.** `VeloraVideoSurface` receives a `VideoSurfaceState` (surface
  token / player handle owned by the service layer). The downcast
  `(playerEngine as? Media3PlayerEngine)?.exoPlayer` (`VeloraVideoSurface.kt:35`) is
  deleted; `PlayerEngine` is extended with the DSP / A-B-loop operations the UI needs so
  no screen reaches past the interface.
- **Single tier truth.** Only `ExperienceTier`; the `isSimpleMode` key, the `setSimpleMode`
  path and the legacy `FullscreenPolicy` overload are deleted (D-1/D-2, §0). Migration is
  one-shot in DataStore (D-3).
- **Fullscreen state.** Rotation is safe by manifest `configChanges` (§7); process death is
  the gap — fullscreen, controls-locked and controls-visible state are hoisted to the
  ViewModel / saved state in Phase 2. The orientation lock input is the real video
  dimensions (`VideoSize`), never the aspect mode.
- **Back behaviour (contract).** Today Back never pops the stack: `BackHandler(enabled =
  currentDestination != homeDestination)` navigates to home (`VeloraShellScreen.kt:154-156`),
  and on the home destination the handler is disabled so system Back exits the app
  (default behaviour — this answers Gemini's open question: on home, Back exits). Phase 2
  keeps that and adds the priority order: **fullscreen exit → sheet/dialog dismiss →
  navigate home → exit app**. A sheet or dialog that does not close on Back is a defect.
- **Dialog-file deletion.** `LibraryDialogs.kt`, `VideoPlayerDialogs.kt`,
  `PlayerLyricsAndDialogs.kt` are deleted only after the 125-testTag inventory (§4.7) —
  every tag is mapped to the new component or retired explicitly.
- **Small debts carried into Phase 2 (audit §5.7, §5.12).** One shared permission-request
  helper replaces the 4 duplicated permission arrays; music quick-filter gets the same
  250 ms debounce as global search; MiniPlayerBar gains a Previous button; video speeds
  gain the 0.75× step; the video Retry action becomes a real re-load; the smart-rule
  builder stops hardcoding bitrate/min-plays to 0 (§5.9); LRC Studio stamps beyond the
  first 4 lines (§5.10). None of these is a design decision — they are recorded here so
  Phase 2 does not drop them.

## 12. Coverage map — all 120 audit controls, mapped (new)

The "no feature removed" proof. Every numbered control from audit §2 appears here with its
new home and tier. References are audit section/item numbers; "—" in the tier column means
the destination is hidden in that tier.

### 12.1 Shell (§2.1, 11 controls)

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.1.1 | Bottom NavigationBar / side NavigationRail, tier-filtered | Shell (§5.1: 4 destinations in SIMPLE, 8 otherwise) | All |
| §2.1.2 | MiniPlayerBar (progress, title, artist, elapsed, play/next, tap → Player) | Shell bottom bar, all routes (§4) | All |
| §2.1.3 | `VELORA` wordmark + tier label | Shell header | All |
| §2.1.4 | Tier cycle button (SIMPLE → STANDARD → PRO) | Shell header (stays in SIMPLE — it is the door out) | All |
| §2.1.5 | Language toggle EN / فا | Shell header | All |
| §2.1.6 | Track-count badge | Shell header (Latin digits, JetBrains Mono — R-T2) | All |
| §2.1.7 | Status banner + dismiss | Shell header | All |
| §2.1.8 | Startup permission request + auto-scan flow | Startup flow (unchanged; shared permission helper, §11) | All |
| §2.1.9 | Back-handler → home (no stack pop) | Shell (§11 contract: fullscreen → sheet/dialog → home → exit) | All |
| §2.1.10 | SIMPLE redirect to Music | Shell logic (unchanged) | All |
| §2.1.11 | RTL/LTR layout-direction switch | App-wide (R-T1) | All |

### 12.2 Library (§2.2, 17 controls) — route hidden in SIMPLE

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.2.1–4 | Auto-scan, rescan, import file, import folder | Library | STANDARD+ |
| §2.2.5–7 | Folder-path field + Add, create sandbox folder, remove-folder chips | Library | PRO |
| §2.2.8 | Scan-progress skeleton | Library (§4.9) | STANDARD+ |
| §2.2.9 | Playlists section (create/play/shuffle/delete) | Library | STANDARD+ |
| §2.2.10 | M3U8 export/import dialog | Library | STANDARD+ |
| §2.2.11 | Smart playlists + listening-stats card | Library | STANDARD+ |
| §2.2.12 | Smart-rule builder dialog | Library PRO (fix §5.9: bitrate/min-plays no longer hardcoded 0) | PRO |
| §2.2.13–14 | Filter-mode chips, sort-order chips | Library | STANDARD+ |
| §2.2.15 | Track list (tap play, favorite, add-to-queue, add-to-playlist) | Library (`VeloraTrackCard`, §4.3) | STANDARD+ |
| §2.2.16 | Empty state + scan action | Library (§4.8) | STANDARD+ |
| §2.2.17 | Playback-history section + clear-history | Library | STANDARD+ |

### 12.3 Music (§2.3, 7 controls)

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.3.1 | Play All (with track count) | Music | All |
| §2.3.2 | Shuffle All | Music | STANDARD+ |
| §2.3.3 | Quick-filter text field + clear | Music (debounced in Phase 2, §11) | STANDARD+ |
| §2.3.4 | Hi-Res (≥48 kHz) filter toggle | Music | STANDARD+ |
| §2.3.5 | Auto-scan device music | Music | All |
| §2.3.6 | Track list (4 actions) | Music (`VeloraTrackCard`; SIMPLE: tap-to-play only) | All |
| §2.3.7 | Empty state + scan action | Music (§4.8) | All |

### 12.4 Artists (§2.4, 4 controls) — route hidden in SIMPLE

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.4.1–4 | Artist selector chips, Play Artist, Shuffle Artist, track list | Artists | STANDARD+ |

### 12.5 Radio (§2.5, 5 controls) — route hidden in SIMPLE

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.5.1–5 | Genre chips, Tune Live, add to queue, custom station dialog, empty state | Radio | STANDARD+ |

### 12.6 Video (§2.6, 25 controls)

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.6.1 | Video surface | Video (`VeloraVideoSurface`, §4.11) | All |
| §2.6.2 | Subtitle overlay | Video (shown when PRO subtitles enabled) | PRO |
| §2.6.3 | Lock / unlock controls button | Video | All |
| §2.6.4 | Tap toggle; double-tap seek; vertical-drag brightness/volume | Video (tap: all tiers; double-tap seek + drags: STANDARD+; §4 R-K4) | All / STANDARD+ |
| §2.6.5 | Gesture HUD label | Video | All |
| §2.6.6 | Seek bar + elapsed/total | Video (`VeloraSeekBar`) | All |
| §2.6.7 | Play / Pause button | Video | All |
| §2.6.8 | Fullscreen button | Video (§7) | All |
| §2.6.9 | PiP button (fixed 16:9) | Video (always-on, §5.5) | All |
| §2.6.10 | Scan / pick-video button | Video | All |
| §2.6.11 | Playback-speed chips (0.5/1.0/1.25/1.5/2.0) | Video (0.75× added, §11) | PRO |
| §2.6.12 | Aspect-mode chips (FIT/FILL/GOLDEN_PHI/16:9) | Video (STANDARD, justified §5.5) | STANDARD+ |
| §2.6.13 | Subtitles button → dialog (enable, font scale, colour) | Video | PRO |
| §2.6.14 | Audio-track button → dialog | Video | PRO |
| §2.6.15 | Bookmark button ("Cinema Scene") | Video (PRO, reverted §5.5) | PRO |
| §2.6.16 | Error banner + Retry | Video (§4.10; Retry becomes a real re-load) | All |
| §2.6.17 | Video track list (4 actions; tap navigates to Video) | Video (`VeloraTrackCard`; SIMPLE: tap-to-play only) | All |
| §2.6.18 | Empty state + scan action | Video (§4.8) | All |
| §2.6.19 | Fullscreen Dialog (system bars hidden, orientation lock) | Video (§7: lock from real `VideoSize`) | All |
| §2.6.20 | Exit-fullscreen button | Fullscreen dialog | All |
| §2.6.21 | Lock/unlock toggle (top bar) | Fullscreen dialog | All |
| §2.6.22 | Fullscreen seek bar | Fullscreen dialog | All |
| §2.6.23 | Fullscreen gesture layer + HUD | Fullscreen dialog (§4 R-K4) | All / STANDARD+ |
| §2.6.24 | Fullscreen subtitle overlay | Fullscreen dialog | PRO |
| §2.6.25 | Dialog dismiss (back / outside) exits fullscreen | Fullscreen dialog (§11 back contract) | All |

### 12.7 Player & EQ (§2.7, 29 controls)

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.7.1 | Error banner + Dismiss | Player stage (§4.10) | All |
| §2.7.2 | Spectrum analyzer canvas | Player stage (§3.1c, §4.12) | All |
| §2.7.3 | Visualizer-mode selector chips | Player stage (PRO-only) | PRO |
| §2.7.4 | Cinema gestures on spectrum canvas | Player stage (double-tap seek + volume drag stay; brightness drag removed, §4 R-K4) | STANDARD+ |
| §2.7.5 | Gesture HUD label | Player stage | All |
| §2.7.6 | Seek bar + elapsed/total | Player stage | All |
| §2.7.7 | Transport: play/pause, prev, next; shuffle + repeat-cycle | Player stage (shuffle/repeat STANDARD+) | All / STANDARD+ |
| §2.7.8 | Mute toggle + volume slider | Player stage | All |
| §2.7.9 | Stop button | Player stage | STANDARD+ |
| §2.7.10 | Exact-seek dialog ("Seek ms") | Player stage | STANDARD+ |
| §2.7.11 | Favorite toggle (header) | Player stage | STANDARD+ |
| §2.7.12 | Metadata tag editor dialog | Player stage | STANDARD+ |
| §2.7.13 | EQ enable/disable switch | EQ screen | PRO |
| §2.7.14 | Flat reset button | EQ screen | PRO |
| §2.7.15 | Save-custom-preset dialog (name field) | EQ screen (local presets only) | PRO |
| §2.7.16 | Preset chips (built-in + custom) + delete on custom | EQ screen (local only — no cloud presets) | PRO |
| §2.7.17 | Ten band sliders + auto-headroom readout | EQ screen (disabled state: R-C6 tokens) | PRO |
| §2.7.18 | Preamp gain slider (−12…+12 dB) | EQ screen | PRO |
| §2.7.19 | Sub-bass boost slider (0…100%) | EQ screen | PRO |
| §2.7.20 | 3D stereo-width slider (0…200%) | EQ screen | PRO |
| §2.7.21 | A-B loop: Set A, Set B, Clear A-B | EQ screen | PRO |
| §2.7.22 | Crossfade chips (0/3/6/12 s) | EQ screen (PRO, reverted §5.5) | PRO |
| §2.7.23 | Sleep-timer chips (Off/15/30/60 min) | EQ screen (PRO, reverted §5.5) | PRO |
| §2.7.24 | Non-PRO upsell card ("Switch to Studio Pro") | EQ door (shown to STANDARD users opening the EQ door) | STANDARD |
| §2.7.25 | Synchronized lyrics viewer (+ Persian translation) | Player lyrics | STANDARD+ |
| §2.7.26 | Lyrics offset −250 ms / +250 ms | Player lyrics | STANDARD+ |
| §2.7.27 | LRC Timestamp Studio dialog | Player lyrics (fix §5.10: stamp beyond the first 4 lines) | PRO |
| §2.7.28 | Time-machine bookmarks (add + chips) | Player | PRO |
| §2.7.29 | Playback queue (tap to play, remove per item) | Player (view/add/remove — no reorder) | STANDARD+ |

### 12.8 Search (§2.8, 6 controls) — route hidden in SIMPLE

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.8.1–6 | Omnisearch field + clear, semantic chips, "valid songs only", Media Copilot, results, empty state | Search | STANDARD+ |

### 12.9 Settings (§2.9, 16 controls)

| Audit | Control | New home | Tier |
|---|---|---|---|
| §2.9.1 | Experience-tier picker (SIMPLE/STANDARD/PRO) | Settings | All |
| §2.9.2 | Language picker (English / فارسی) | Settings | All |
| §2.9.3 | Theme-palette picker (all `ThemePaletteMode` entries) | Settings (SYSTEM_DYNAMIC retired in Phase 2, R-C3) | All |
| §2.9.4 | Adaptive-audio-mode picker | Settings (all tiers — ungated in code) | All |
| §2.9.5 | Auto-scan-on-startup switch | Settings | All |
| §2.9.6 | Valid-songs-only-in-search switch | Settings | All |
| §2.9.7 | Background-video-audio switch | Settings (decided: WIRE, §7) | All |
| §2.9.8 | Low-battery DSP-saver switch | Settings | All |
| §2.9.9 | Include-demo-samples switch | Settings (all tiers — ungated in code) | All |
| §2.9.10 | Scan Device Now | Settings | All |
| §2.9.11 | Purge demo samples | Settings | All |
| §2.9.12 | Video auto-hide timeout slider (1500…15000 ms) | Settings | PRO |
| §2.9.13 | Google sign-in / sign-out (Firebase) | Settings | All |
| §2.9.14 | Export offline backup (JSON) | Settings (all tiers; the duplicate PRO row is removed, §5.5) | All |
| §2.9.15 | Restore offline backup from pasted JSON | Settings | All |
| §2.9.16 | Declarative-plugin toggles | Settings | PRO |

**Count check:** 11 (shell) + 17 (library) + 7 (music) + 4 (artists) + 5 (radio) + 25
(video) + 29 (player) + 6 (search) + 16 (settings) = **120**. Every control has a home;
nothing is dropped silently. Items marked "fix" (§5.9, §5.10, §5.12, §11 debts) keep their
home and gain a recorded fix — a fix is not a removal.

---
*Sources read for this revision: `UI-AUDIT.md` (519 lines, unchanged reference) and the
Revision 1 `DESIGN-SYSTEM.md` (454 lines) on branch `feature/ui-rebuild-phase0-1`;
Gemini review, AI Studio chat `1iF3XVNAIxzo2nSNjwlPvRL6N_32jbCly`; `ARCHITECTURE.md` /
`CLEAN-CODE-PLAN.md` (branch `docs/architecture`); HEAD `19e70bc` —
`domain/model/Track.kt:18`, `data/local/VeloraEntities.kt:39`,
`data/scanner/MediaLibraryScanner.kt:194`, `gradle/libs.versions.toml:4`,
`app/build.gradle.kts:85,142,150,155`, `domain/model/PlaylistAndSettings.kt:78-89`,
`ui/screens/VeloraShellScreen.kt:131-156,184`, `data/settings/VeloraSettingsRepository.kt:107-121,231`,
`AndroidManifest.xml:37,40`, `ui/components/VeloraVideoSurface.kt:35,72-73`,
`ui/screens/NowPlayingScreen.kt:339-356`, `app/src/test/java/com/example/ExampleRobolectricTest.kt`.
Contrast ratios computed from hex values with the WCAG 2.1 luminance formula; type-scale and
deviation percentages computed arithmetically. No Gradle run, no device, no screenshots exist
for this phase — anything marked UNTESTED stays untested until Phase 3.*
