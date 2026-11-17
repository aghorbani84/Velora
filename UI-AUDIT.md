# VELORA UI/UX Audit & Feature Inventory (Phase 0)

- **Repository:** `aghorbani84/Velora` (`:app` single-module Android project)
- **Base Commit Reference:** `8d13bfb5e4466023615aa7f1c2568daacfd52efb` (with all 54 `RE-REPORT.md` backend/security/DSP fixes applied; note: workspace is an exported AI Studio container without a `.git` directory, so local branch creation is tracked logically in `UI-REBUILD-LOG.md`)
- **Execution Environment Disclosure (Hard Rule #3 — No Fabrication):**
  - **Available & Verified Locally:** Gradle 9.3.1, JDK 21, Kotlin 2.2.10, Detekt 1.23.7, JUnit 4, Robolectric 4.14.1 (`SDK 34` local JVM Compose & Android framework tests), `compile_applet` (`assembleDebug`).
  - **Not Available in Headless Container:** Physical Android device or attached ADB / Streaming Emulator for live PNG frame capture (`screenshots/before/` and `screenshots/after/`). Per Hard Rule #3, no screenshot files are fabricated; instead, UI composition trees, semantics, and `testTag` visibility are verified via automated Robolectric Compose tests and static Detekt audits.

---

## 1. Complete Screen & Navigation Inventory (Before Wipe)

| # | Route ID (`VeloraScreen`) | Old Source File(s) | Line Count | Primary Role | Key Issues Identified in Old UI |
|---|---|---|---|---|---|
| 1 | `library` | `LibraryAndMusicScreens.kt` + `Phase2Components.kt` | 1,767 + 703 | Local media index, folder allowlist, playlists, smart playlists, listening stats recap, playback history | Giant 1,767-line monolith; mixed hard-coded `.dp`/`.sp` literals (`10.dp`, `12.dp`, `14.dp`, `16.dp`, `4.dp`, `6.dp`); cluttered hero banners with competing CTAs; no loading skeleton; only binary `isSimpleMode` instead of 3-tier `SIMPLE` / `STANDARD` / `PRO`. |
| 2 | `music` | `LibraryAndMusicScreens.kt` | (shared in 1,767) | Audio collection browser, quick filter, Hi-Res filter, play/shuffle all | Embedded inside `LibraryAndMusicScreens.kt`; duplicated permission launcher logic; non-tokenized `58.dp` custom canvas and `12.dp` radii. |
| 3 | `artists` | `LibraryAndMusicScreens.kt` | (shared in 1,767) | Master-detail artist browser (`Compact` chips vs `Expanded` two-pane) | Embedded inside `LibraryAndMusicScreens.kt`; uses `14.dp`, `36.dp`, `10.dp` off-scale dimensions. |
| 4 | `radio` | `RadioAndDspComponents.kt` | 729 | Live HTTP/HLS/ICY stream tuner, custom station dialog, genre chips | Bundled together with `StudioDspRackPanel` in a single 729-line file; raw color/radius literals (`12.dp`, `14.dp`, `44.dp`). |
| 5 | `video` | `VideoPlayerScreen.kt` + `VeloraVideoSurface.kt` + `Phase2Components.kt` | 1,150+ | Hardware SurfaceView video playback, gestures, fullscreen dialog, aspect ratio, speed, subtitles, audio tracks | Over 1,150 lines; duplicated gesture detectors between inline surface and fullscreen dialog; subtitle styling mixed in `Phase2Components.kt`. |
| 6 | `search` | `SearchScreen.kt` | 278 | OmniSearch (300ms debounce), semantic mood chips, Media Copilot command bar | Hard-coded `14.dp` and `16.dp` corner radii; English/Persian copy mixed in headers; no dedicated designed no-result component token usage. |
| 7 | `player` | `PlayerAndEqScreen.kt` + `RadioAndDspComponents.kt` + `Phase2Components.kt` | 1,635 | Now Playing transport deck, 10-band Biquad EQ, Studio DSP rack, synchronized lyrics, LRC studio, queue, tag editor | 1,635-line monolith; 500ms playback position ticks recompose the entire `PlayerAndEqScreen` via `FazoraUiState`; EQ and Studio DSP crammed into the same scrolling screen as basic Now Playing. |
| 8 | `settings` | `SettingsScreen.kt` | ~950 | Experience mode, theme palette, language (`en`/`fa`), audio route, smart rules, folder allowlist, backup/restore, Firebase cloud sync | Binary `isSimpleMode` toggle instead of 3-tier (`SIMPLE`, `STANDARD`, `PRO`); Persian font lacked bundled `Vazirmatn` `.ttf`; sprawling single file. |

---

## 2. Complete Feature, Control, Dialog & `testTag` Inventory (Zero-Loss Baseline)

Every feature, user action, dialog, gesture, and `testTag` below exists in the old UI and **must be 100% preserved** in the rebuilt modular UI:

### 2.1 App Shell (`VeloraShellScreen.kt` & `NavGraph.kt`)
- **Top Status Header:**
  - App brand title (`VELORA`) & subtitle
  - Experience Tier quick switcher (`shell_mode_toggle_button`, plus `tier_selector_SIMPLE`, `tier_selector_STANDARD`, `tier_selector_PRO`)
  - Language quick toggle `EN` / `فا` (`shell_language_toggle_button`)
  - Track count badge (`shell_track_count_badge`)
- **Adaptive Navigation (`NavigationBar` on `<600dp` Compact, `NavigationRail` on `>=600dp` Medium/Expanded):**
  - `nav_library`, `nav_music`, `nav_radio`, `nav_artists`, `nav_video`, `nav_search`, `nav_player`, `nav_settings` (in `SIMPLE` tier, streamlined to core tabs while keeping all routes reachable)
- **Persistent Mini-Player Bar (`mini_player_bar`):**
  - Tap bar to open `player` (`NowPlaying`)
  - Play/Pause button (`mini_player_play_pause`)
  - Skip Next button (`mini_player_next`)
  - Narrow-state progress indicator

### 2.2 Library, Playlists, Folders & History (`LibraryScreen`)
- **Media Scanning & Device Storage Import:**
  - Auto-scan device storage with runtime permission request (`library_auto_scan_button`)
  - Manual scan trigger (`scan_library_button`)
  - SAF single-file import (`import_device_file_button`)
  - SAF directory tree import (`import_device_folder_button`)
  - Manual safe folder path input (`folder_path_input`) + add button (`add_folder_button`) + remove folder chip (`remove_folder_<hash>`)
- **Playlists & M3U8 Management:**
  - New playlist name input (`new_playlist_name_input`) + create button (`create_playlist_button`)
  - Playlist cards (`playlist_card_<id>`), play playlist (`play_playlist_<id>`), delete playlist (`delete_playlist_<id>`)
  - M3U8 Import/Export Dialog (`open_m3u8_dialog_button`, `m3u8_manager_dialog`, `export_m3u8_button`, `import_m3u8_button`, `m3u8_text_input`)
- **Smart Playlists & Listening Analytics:**
  - Smart playlist cards (`smart_playlist_<id>`)
  - Custom Smart Rule Builder Dialog (`open_smart_rule_builder_button`, `smart_rule_builder_dialog`, `smart_rule_name_input`, `smart_rule_genre_input`, `smart_rule_min_sr_input`, `smart_rule_favorites_checkbox`, `run_smart_rule_button`)
  - Listening Stats Recap Card (`listening_stats_recap_card`)
- **Filtering, Sorting & Track List:**
  - Filter modes (`filter_mode_ALL`, `filter_mode_FAVORITES_ONLY`, `filter_mode_HI_RES_ONLY`)
  - Sort orders (`sort_order_TITLE_ASC`, `sort_order_ARTIST_ASC`, `sort_order_DURATION_DESC`, `sort_order_RECENTLY_ADDED`)
  - Track row item (`track_row_<id>`), favorite toggle (`favorite_track_<id>`), add to queue (`queue_track_<id>`), add to playlist menu (`playlist_menu_track_<id>`)
- **Offline Playback History (Media Time Machine):**
  - History section (`playback_history_section`), history card (`history_card_<id>`), resume playback (`resume_history_<id>`), clear history (`clear_playback_history_button`)

### 2.3 Music & Artists Browser (`MusicScreen` & `ArtistScreen`)
- **Music Collection (`music_screen`):**
  - Play All (`music_play_all_button`), Shuffle All (`music_shuffle_all_button`), Auto-scan (`music_auto_scan_button`), Quick filter text field, Hi-Res filter toggle
- **Artists Browser (`artist_screen`):**
  - Artist selector chips/list (`artist_chip_<hash>`), Play Artist (`artist_play_button`), Shuffle Artist (`artist_shuffle_button`), Artist track list

### 2.4 Music Player (Now Playing) (`NowPlayingScreen`)
- **Artwork & Live 10-Band Goertzel Spectrum (`SpectrumAnalyzerCanvas`):**
  - Displays track title, artist, album, format badge, sample rate, bit depth, channel count
  - Shared gesture surface (`veloraMediaGestures`): double-tap left/right (±10s seek), vertical drag (volume), horizontal drag (scrub)
- **Narrow-State Seek Bar & Exact Seek:**
  - Interactive seek slider (`player_seek_slider`)
  - Exact seek dialog trigger (`open_exact_seek_button`, `exact_seek_input`, `confirm_exact_seek_button`)
- **Main Transport Deck:**
  - Shuffle toggle (`transport_shuffle_button`) — `STANDARD` / `PRO`
  - Previous (`transport_prev_button`) — all tiers
  - Play / Pause (`transport_play_pause_button`) — all tiers
  - Next (`transport_next_button`) — all tiers
  - Repeat cycle (`transport_repeat_button`) — `STANDARD` / `PRO`
  - Favorite toggle (`player_favorite_button`) & Metadata Tag Editor dialog (`open_tag_editor_button`, `tag_edit_title_input`, `tag_edit_artist_input`, `tag_edit_album_input`, `confirm_save_tags_button`)
- **Queue & Up Next:**
  - Queue list (`queue_list_section`), jump to queue item (`queue_item_<index>`), remove from queue (`remove_queue_item_<index>`)
- **Synchronized Lyrics & Bookmarks (`synchronized_lyrics_card`):**
  - Multi-script synchronized lyrics (`lyric_line_<index>`), offset `-250ms` (`lyrics_offset_minus_button`) / `+250ms` (`lyrics_offset_plus_button`)
  - Time Machine Bookmarks (`add_time_machine_bookmark_button`, `bookmark_chip_<id>`) — `PRO`
  - Interactive LRC Timestamp Studio Dialog (`open_lrc_timestamp_studio_button`, `lrc_timestamp_studio_dialog`, `stamp_lrc_line_<index>`, `export_lrc_button`) — `PRO`

### 2.5 Equalizer & Studio DSP (`EqualizerAndDspScreen` — strictly **`PRO`** tier)
- **10-Band Biquad Equalizer (`eq_rack_card`):**
  - EQ master enable switch (`eq_enable_switch`)
  - Reset Flat (`eq_reset_flat_button`), Save Custom Preset Dialog (`eq_save_preset_button`, `eq_preset_name_input`, `confirm_save_eq_preset_button`)
  - Preset chips (`eq_preset_chip_<name>`) + delete custom preset
  - 10 ISO band sliders 31Hz–16kHz (`eq_band_slider_0` .. `eq_band_slider_9`) and dB readouts (`eq_band_value_0` .. `eq_band_value_9`)
- **Studio DSP Rack (`studio_dsp_rack_card`):**
  - Preamp gain slider (`dsp_preamp_slider`), Sub-Bass Harmonic Boost slider (`dsp_bass_boost_slider`), 3D Stereo Width slider (`dsp_stereo_width_slider`)
  - Crossfade duration chips (`dsp_crossfade_<ms>`)
  - Playback speed chips (`dsp_speed_<val>`)
  - A-B Precision Loop controls (`dsp_ab_set_a_button`, `dsp_ab_set_b_button`, `dsp_ab_clear_button`)
  - Sleep Timer chips (`dsp_sleep_timer_<mins>`)

### 2.6 Video Player (`VideoPlayerScreen`)
- **Hardware Video Surface (`VeloraVideoSurface`):**
  - Encapsulates Media3 `ExoPlayer` inside `ui/components/VeloraVideoSurface.kt` (no `ExoPlayer` passed as screen parameter)
  - Uses the **single shared** `Modifier.veloraMediaGestures` for double-tap ±10s seek, left-side vertical drag (brightness), right-side vertical drag (volume), horizontal drag (timeline scrub), single tap (toggle controls)
- **Tier-Gated Video Controls (`FullscreenPolicy`):**
  - All tiers (`SIMPLE`, `STANDARD`, `PRO`): Play/Pause (`video_play_pause_button`), Seek bar (`video_seek_slider`), Fullscreen toggle (`video_fullscreen_button`), Picture-in-Picture (`video_pip_button`), Pick device video (`pick_device_video_button`), Controls Lock toggle (`video_lock_controls_button` / `video_unlock_controls_button`)
  - `PRO` tier only: Playback Speed (`video_speed_button` / `video_speed_chip_<speed>`), Aspect Ratio (`video_aspect_chip_<mode>`), Subtitles & Style Picker (`video_subtitle_button`, `subtitle_style_picker_section`), Audio Track selector (`video_audio_track_button`), Scene Bookmark (`video_bookmark_button`)

### 2.7 OmniSearch & Live Radio (`SearchScreen` & `RadioScreen`)
- **OmniSearch (`search_screen`):**
  - Debounced search input (`omnisearch_query_input`), Clear button (`omnisearch_clear_button`)
  - Semantic mood/context chips (`semantic_chip_<index>`)
  - Media Copilot natural-language command bar (`copilot_command_input`, `copilot_execute_button`)
  - Categorized results: Audio, Video, Artists, Playlists, Lyrics, and Designed Empty/No-Result state
- **Live Radio (`radio_screen`):**
  - Genre filter chips (`radio_genre_chip_<genre>`), Station cards (`radio_station_card_<id>`), Add Custom Stream Dialog (`add_custom_radio_button`, `custom_radio_name_input`, `custom_radio_url_input`, `confirm_add_radio_button`)

### 2.8 Settings (`SettingsScreen`)
- **Experience Tier Selector (3 Tiers — Instant Switch, No Restart):**
  - `SIMPLE` (`tier_option_SIMPLE`), `STANDARD` (`tier_option_STANDARD`), `PRO` (`tier_option_PRO`) + legacy `settings_simple_mode_switch` compatibility
- **Theme & Appearance:**
  - Palette mode selector: `SYSTEM_DYNAMIC`, `VELORA_SAFFRON`, `VELORA_DAYLIGHT`, `HIGH_CONTRAST_MONOCHROME` (`theme_palette_<mode>`)
  - Dynamic color switch (`settings_dynamic_color_switch`)
  - Language selector: English (`language_option_en`) / فارسی (`language_option_fa`)
- **Audio, Video & Power Preferences:**
  - Adaptive Audio Mode (`adaptive_audio_<mode>`), Auto-scan on startup (`settings_auto_scan_switch`), Only valid music in search (`settings_valid_music_switch`), Background video audio (`settings_bg_video_audio_switch`), Low battery mode (`settings_low_battery_switch`), Video controls auto-hide timeout slider
- **Data, Backup/Restore, Cloud Sync & Diagnostics:**
  - Include bundled demo samples toggle (`settings_demo_samples_switch`) + Purge demo samples (`purge_demo_samples_button`)
  - JSON Backup export (`export_backup_button`) / import (`import_backup_button`)
  - Firebase Cloud Continuity Sign-in / Sync (`firebase_sync_card`)

---

## 3. UI ↔ Logic Boundary & Narrow State Architecture

1. **What `VeloraViewModel` Exposes to the UI:**
   - `uiState: StateFlow<VeloraUiState>` (`FazoraUiState`): Structural UI state (tracks, playlists, settings, active track metadata, EQ/DSP configuration, search results, radio stations). **Does NOT re-emit on every 500ms position tick when only playback position changes.**
   - `playbackPosition: StateFlow<PlaybackPositionState>`: Dedicated narrow state flow containing `(positionMs: Long, durationMs: Long, isPlaying: Boolean, bufferedPercentage: Int)` so only `VeloraSeekBar` and timecode labels recompose during playback ticks.
2. **Encapsulation of Media3 `ExoPlayer`:**
   - Screen composables (`VideoPlayerScreen`, `NowPlayingScreen`, etc.) **never** accept `ExoPlayer` in their parameter lists.
   - `VeloraVideoSurface` binds the surface internally via `PlayerEngine.attachVideoSurfaceView(SurfaceView)`.
