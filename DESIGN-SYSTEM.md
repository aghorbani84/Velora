# VELORA Design System Specification (Phase 1)

**Brand Identity:** VELORA — *Beyond Playback.*  
**Design Archetype:** Atmospheric Minimalism & High-Craft Studio Precision (Obsidian & Saffron Dark + Daylight Ivory Light + OLED Monochrome)  
**Golden Ratio Reference:** $\varphi = 1.618033988749895$

---

## 1. Color Token System (Light, Dark & OLED High-Contrast)

Every color in the UI layer is referenced through `MaterialTheme.colorScheme` or semantic `VeloraColors` tokens in `ui/theme/Color.kt` — **never** as raw hex literals (`0xFF...`) inside screen or component files.

### 1.1 Dark Theme (`VELORA_SAFFRON` — Default Studio Dark)

| Semantic Token | Compose Token Name | Hex Value | WCAG 2.1 Contrast vs Background | APCA $L_c$ | Usage |
|---|---|---|---|---|---|
| `background` | `FazoraObsidian` | `#0B0B0D` | 1.00:1 (Base) | — | Primary app canvas |
| `surface` | `FazoraCarbon` | `#121216` | 1.08:1 | — | Standard card & container surface |
| `surfaceVariant` | `FazoraCarbonElevated` | `#19191F` | 1.18:1 | — | Elevated modal, active row, input background |
| `surfaceContainerHigh` | `VeloraCardSurface` | `#202027` | 1.29:1 | — | Highlighted cards, dialogs, bottom sheets |
| `primary` | `FazoraGold` | `#E5A817` | **9.43:1** (AAA) | $-81.4$ | Primary CTAs, active indicators, seek thumb |
| `onPrimary` | `FazoraObsidian` | `#0B0B0D` | **9.43:1** (AAA) | $+83.1$ | Text/icons on saffron buttons |
| `primaryContainer` | `FazoraGoldMuted` | `#2D2310` | 1.38:1 | — | Selected chip/row container |
| `onPrimaryContainer` | `FazoraGoldBright` | `#F5C451` | **11.92:1** (AAA) | $-91.2$ | Text on selected container (`8.64:1` vs `#2D2310`) |
| `secondary` | `VeloraChampagneGold` | `#D9BD83` | **10.65:1** (AAA) | $-86.5$ | Acoustic badges, sample-rate metadata |
| `tertiary` | `VeloraSoftGold` | `#BDA36B` | **8.12:1** (AAA) | $-73.8$ | Secondary accents, subtle badges |
| `onBackground` / `onSurface` | `FazoraIvory` | `#F5F2EB` | **17.41:1** (AAA) | $-102.6$ | Primary headlines and body text |
| `onSurfaceVariant` | `FazoraSteel` | `#A5A29B` | **7.68:1** (AAA) | $-70.4$ | Subtitles, artist/album secondary labels |
| `outline` | `FazoraCarbonBorder` | `#2A2927` | 1.48:1 | — | 1dp hairline card/container borders |
| `error` | `FazoraCrimson` | `#FF6B6B` | **6.92:1** (AA+) | $-64.1$ | Error banners, destructive actions |
| `onError` | `FazoraObsidian` | `#0B0B0D` | **6.92:1** (AA+) | $+65.8$ | Text/icon on error fills |

### 1.2 Light Theme (`VELORA_DAYLIGHT` — Warm Daylight Ivory)

| Semantic Token | Compose Token Name | Hex Value | WCAG 2.1 Contrast vs Background | Usage |
|---|---|---|---|---|
| `background` | `VeloraDaylightIvory` | `#FAF7F0` | 1.00:1 (Base) | Warm ivory canvas |
| `surface` | `VeloraDaylightSurface` | `#F2EDE2` | 1.09:1 | Card surface |
| `surfaceVariant` | `VeloraDaylightElevated` | `#E6DFD1` | 1.21:1 | Input/chip surface |
| `primary` | `VeloraDaylightSaffron` | `#8C5800` | **5.84:1** (AA+) | Accessible deep saffron primary |
| `onPrimary` | `VeloraDaylightIvory` | `#FAF7F0` | **5.84:1** (AA+) | Text on primary button |
| `onBackground` / `onSurface` | `VeloraDaylightInk` | `#161512` | **16.35:1** (AAA) | Primary ink text |
| `onSurfaceVariant` | `VeloraDaylightMutedInk` | `#4E4B44` | **7.82:1** (AAA) | Secondary text |
| `outline` | `VeloraDaylightBorder` | `#CFC7B8` | 1.54:1 | Subtle card border |
| `error` | `VeloraDaylightError` | `#B3261E` | **6.48:1** (AA+) | Error state |

### 1.3 OLED High-Contrast (`HIGH_CONTRAST_MONOCHROME`)
- Pure `#000000` background, `#FFFFFF` primary & text (`21.0:1` contrast), `#D4D4D8` secondary (`14.1:1` contrast).

---

## 2. Typography Scale ($\varphi$-Ratio & Bilingual Font Stack)

### 2.1 Font Families (Locally Bundled in `app/src/main/res/font/`)
- **Persian (`appLanguageCode == "fa"`, RTL):** `VazirmatnFamily` (`R.font.vazirmatn`) — authentic geometric Persian/Arabic variable typeface with balanced x-height and native Persian numerals/glyphs.
- **English Headlines (`appLanguageCode == "en"`, LTR):** `SpaceGroteskFamily` (`R.font.space_grotesk`) — technical studio display sans.
- **English Body & UI Controls:** `InterFamily` (`R.font.inter`) — high-legibility UI sans.
- **Timecodes, Frequency Readouts & Sample Rates:** `JetBrainsMonoFamily` (`R.font.jetbrains_mono`) — tabular monospace numerals (`00:00`, `+3.5 dB`, `96.0 kHz`) so rapidly ticking counters never jitter horizontally.

### 2.2 Modular $\varphi$ Scale ($16 \times \varphi^{n/3}$)

| M3 Role | Formula Step ($n$) | Exact $\varphi$ Value | Applied Token Size | Line Height | Weight | Primary Usage |
|---|---|---|---|---|---|---|
| `labelSmall` | $n = -2$ | $16 \times \varphi^{-2/3} = 11.60\text{sp}$ | **`11.5.sp`** | `16.sp` | Medium (500) | Codec badges, frequency ticks |
| `labelMedium` | $n = -1$ | $16 \times \varphi^{-1/3} = 13.62\text{sp}$ | **`12.5.sp`** | `18.sp` | Medium (500) | Timecodes (`JetBrainsMono`), secondary metadata |
| `bodySmall` | $n = -1$ | $16 \times \varphi^{-1/3} = 13.62\text{sp}$ | **`13.5.sp`** | `20.sp` | Normal (400) | Helper descriptions, subtitle captions |
| `labelLarge` | $n = -0.5$ | $16 \times \varphi^{-1/6} = 14.76\text{sp}$ | **`14.sp`** | `20.sp` | SemiBold (600) | Section eyebrows, button & chip labels |
| `bodyMedium` | $n = 0$ | $16 \times \varphi^{0} = 16.00\text{sp}$ | **`15.sp`** | `22.sp` | Normal (400) | Track list metadata, settings rows |
| `bodyLarge` | $n = 0$ | $16 \times \varphi^{0} = 16.00\text{sp}$ | **`16.sp`** | `24.sp` | Normal (400) | Primary body copy, lyrics lines |
| `titleMedium` | $n = +0.5$ | $16 \times \varphi^{1/6} = 17.34\text{sp}$ | **`17.sp`** | `24.sp` | SemiBold (600) | Track titles, card headers |
| `headlineMedium` | $n = +1$ | $16 \times \varphi^{1/3} = 18.80\text{sp}$ | **`20.sp`** | `28.sp` | Bold (700) | Section titles, dialog headers |
| `headlineLarge` | $n = +2$ | $16 \times \varphi^{2/3} = 22.08\text{sp}$ | **`26.sp`** | `34.sp` | Bold (700) | Screen hero titles, Now Playing title |
| `displayLarge` | $n = +3$ | $16 \times \varphi^{1} = 25.89\text{sp}$ | **`34.sp`** | `42.sp` | Bold (700) | Tablet/Expanded stage display |

---

## 3. Spacing, Corner Radius, Elevation & Touch Targets (`PhiTokens`)

All layout spacing, padding, and corner radii come exclusively from `PhiTokens` (`ui/theme/Theme.kt`):

### 3.1 Fibonacci Spacing Scale
- `PhiTokens.Space4 = 4.dp` (micro inline icon-to-label gap; Fibonacci $F_4 = 3 \to 4\text{dp}$ pixel-grid aligned)
- `PhiTokens.Space8 = 8.dp` ($F_6$: compact element gap, chip row spacing)
- `PhiTokens.Space13 = 13.dp` ($F_7$: card internal padding, list item spacing)
- `PhiTokens.Space21 = 21.dp` ($F_8$: screen edge padding, major section spacing)
- `PhiTokens.Space34 = 34.dp` ($F_9$: hero section separation, large icon sizing)
- `PhiTokens.Space55 = 55.dp` ($F_{10}$: primary transport FAB, empty-state hero container)

### 3.2 Corner Radius Scale
- `PhiTokens.Radius8 = 8.dp` (small badges, format pills)
- `PhiTokens.Radius13 = 13.dp` (buttons, chips, text fields, standard track rows)
- `PhiTokens.Radius21 = 21.dp` (primary cards, dialogs, sheets, video frame)
- `PhiTokens.Radius34 = 34.dp` (pill buttons, transport deck container)

### 3.3 Golden Ratio Split Weights
- `PhiTokens.PRIMARY_WEIGHT = 0.618f` ($1 / \varphi$)
- `PhiTokens.SECONDARY_WEIGHT = 0.382f` ($1 - 1 / \varphi$)

### 3.4 Documented Deviations from $\varphi$ / Fibonacci (With Exact Percentages & Rationale)

| Metric | Pure $\varphi$ / Fibonacci Value | Applied Token Value | Exact Deviation % | Engineering & Accessibility Rationale |
|---|---|---|---|---|
| `PhiTokens.MinTouchTarget` | `55.dp` ($F_{10}$) or `34.dp` ($F_9$) | **`48.dp`** | **$-12.73\%$** vs `55.dp` (`+41.18%` vs `34.dp`) | Android Accessibility Scanner & Material 3 `minimumInteractiveComponentSize()` mandate `48.dp × 48.dp` (exceeding Apple HIG `44pt`). `34.dp` fails touch accessibility; `55.dp` causes horizontal overflow on 360dp compact screens with 5 transport buttons. |
| `PhiTokens.Space4` | `5.dp` ($F_5$) | **`4.dp`** | **$-20.00\%$** vs `5.dp` | Standard Android 4dp baseline sub-grid alignment for tight badge/caption vertical rhythm on 2x/3x DPI displays. |
| `PhiTokens.BorderHairline` | `1.618.dp` | **`1.dp`** | **$-38.20\%$** vs $\varphi\text{dp}$ | Crisp 1dp hairline borders avoid sub-pixel anti-aliasing blur on OLED panels. |
| Video Player Viewport | $1.618:1$ (`GOLDEN_PHI`) | **`16:9` (`1.778:1`) / Native `FIT`** | **$+9.87\%$** vs $\varphi$ | Standard cinema/broadcast video streams are mastered at 16:9 or 21:9; cropping to $\varphi$ by default would cut off subtitles or framing (`GOLDEN_PHI` remains an optional Pro aspect mode). |

---

## 4. Shared Component Catalog (`ui/components/`)

All screens must compose their UI from these 8 standardized components rather than reinventing local styling:

1. **`VeloraPrimaryButton` & `VeloraSecondaryButton` (`VeloraSharedComponents.kt`)**
   - Tokenized `Radius13` shape, `MinTouchTarget` (`48.dp`), `MaterialTheme.colorScheme.primary` / `outline` styling, icon + label support, mandatory `testTag`.
2. **`VeloraSectionCard` (`VeloraSharedComponents.kt`)**
   - Tokenized `Radius21` surface card with `1.dp` `outline` border, `Space21` internal padding, optional eyebrow + title + action slot.
3. **`TrackRowItem` (`VeloraSharedComponents.kt`)**
   - Standardized media row for Library, Music, Artists, Search, and Playlists: 48dp artwork/format badge, title, artist/album subtitle, Hi-Res/sample-rate pill, favorite heart button (`48.dp` target), queue button, and playlist menu.
4. **`VeloraSeekBar` (`VeloraSharedComponents.kt`)**
   - Narrow-state timeline scrubber consuming `PlaybackPositionState(positionMs, durationMs)` with tabular `JetBrainsMono` elapsed/total timecodes and optional A-B loop markers.
5. **`VeloraTransportControls` (`VeloraSharedComponents.kt`)**
   - Unified transport bar supporting `SIMPLE` (Previous, Play/Pause, Next) and `STANDARD`/`PRO` (+ Shuffle, Repeat, Exact Seek, Favorite) modes with `48.dp` minimum touch targets.
6. **`VeloraEmptyState` & `VeloraLoadingSkeleton` & `VeloraErrorBanner` (`VeloraSharedComponents.kt`)**
   - Designed empty state with icon, title, subtitle, and primary CTA button; animated shimmer/pulse loading skeleton for scanning/indexing states; accessible high-contrast error banner with retry/dismiss action.
7. **`MiniPlayerBar` (`VeloraSharedComponents.kt`)**
   - Persistent bottom bar above navigation with narrow progress bar, track info, Play/Pause (`mini_player_play_pause`), and Next (`mini_player_next`).
8. **`VeloraVideoSurface` + `Modifier.veloraMediaGestures` (`VeloraVideoSurface.kt`)**
   - Encapsulates Media3 `SurfaceView` binding and provides the **single shared gesture detector** used across both Music Player and Video Player (double-tap left/right ±10s seek, vertical drag volume/brightness, horizontal drag timeline scrub, single tap toggle).

---

## 5. Three Experience Tiers (`ExperienceTier`)

Controlled via `AppSettings.experienceTier` (`SIMPLE`, `STANDARD`, `PRO`) and switchable **instantly without restart** from both the top shell bar and Settings:

| Capability / Component | `SIMPLE` Tier | `STANDARD` Tier (Default) | `PRO` Tier (Studio) |
|---|---|---|---|
| Play / Pause / Next / Previous / Seek / Volume | **Visible** | **Visible** | **Visible** |
| Library Track List & Auto-Scan / Pick Media | **Visible** | **Visible** | **Visible** |
| Video Fullscreen, PiP, Controls Lock | **Visible** | **Visible** | **Visible** |
| Shuffle, Repeat, Queue Management, Favorites | Hidden | **Visible** | **Visible** |
| Playlists, M3U8, Artists, OmniSearch, Live Radio | Hidden (or simplified) | **Visible** | **Visible** |
| Synchronized Lyrics Viewer | Hidden | **Visible** | **Visible** |
| **10-Band Biquad EQ & Live Transfer Curve (`eq_rack_card`)** | **Absent from Composition** | **Absent from Composition** | **Visible** |
| **Studio DSP Rack (`studio_dsp_rack_card`: Preamp, Bass, 3D, Crossfade, A-B Loop, Sleep Timer)** | **Absent from Composition** | **Absent from Composition** | **Visible** |
| **LRC Timestamp Studio & Time Machine Bookmarks** | **Absent from Composition** | **Absent from Composition** | **Visible** |
| **Video Speed, Aspect Ratio, Subtitles, Audio Tracks, Bookmarks** | **Absent from Composition** | **Absent from Composition** | **Visible** |
| **Smart Rule Builder & Advanced Folder Allowlist** | **Absent from Composition** | **Absent from Composition** | **Visible** |
