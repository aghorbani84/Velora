# تصمیم مسیر A — مرحلهٔ ۳ هیبریدی ولورا (دستور operative برای جمینای در AI Studio)

> **وضعیت: این سند جایگزین پرامپت مرحلهٔ ۳ قدیمی (`velora-hybrid-stage3-android-screens-prompt.md`) است.**
> تصمیم مالک (۲۰۲۶-۱۱-۲۰): **مسیر A** — سازگار با ریپو. تصمیم Q1 مورخ ۲۰۲۶-۱۱-۱۸ («override P6 برای لایهٔ UI — `:shared-ui` همین حالا») **باطل شد**.
> ADR-0003 حاکم است: تک‌ماژول `:app` تا P6؛ `:shared-ui` و `:desktopApp` موکول به P6.
> این دستور را در **همان چت AI Studio** بفرست که مرحله‌های ۱ و ۲ هیبریدی را داده بودی — نه چت تازه.
> ضمیمه کن: (۱) همین فایل، (۲) `velora-master-prompt-v2.md` (با اصلاحیهٔ §C.1)، (۳) `hybrid-ui-stage2-tokens-theme.md` (خروجی مرحلهٔ ۲).

---

## T — Task (وظیفه)

تو مرحلهٔ ۳ هیبریدی ولورا را در **ریپوی واقعی** انجام می‌دهی — نه در معماری فرضی مرحلهٔ ۱.

**ترتیب کار — به همین ترتیب، و تا مرحله‌ای تایید نشد سراغ بعدی نرو:**

1. **اول: ممیزی ۱۲۰ کنترل روی کد main** (فقط-خواندنی، بدون حدس): هر کنترل نقشه → فایل:خط → وضعیت (موجود / ناقص / غایب). همین را اول بده تا تایید شود. این ممیزی مبنای هر دو مسیر است و خلأ §۱۲ را بدون merge کردن PR #3 حل می‌کند.
2. **بعد: ۳الف — کاهش خلأ روی پیاده‌سازی موجود در `:app`**، یکی‌یکی با تایید بین‌شان:
   - **(الف) جابه‌جایی صف:** امروز `moveQueue|onMoveQueue|reorderQueue|queue_move` صفر نتیجه می‌دهد؛ فقط حذف و انتخاب هست. جابه‌جایی را اضافه کن. گیت «صف قابل ویرایش» الان ۵۰٪ است.
   - **(ب) سیم‌کشی `backgroundVideoAudioEnabled`:** الان فقط-نوشتنی است — در UI و repository خوانده/نوشته می‌شود (`SettingsScreen.kt:210`، `VeloraSettingsRepository.kt:259`) ولی در `data/playback`، `domain/engine` و orchestrator هیچ مصرف‌کننده‌ای ندارد. از مسیر `MediaOrchestrator ON_STOP` وصلش کن. این یکی از پنج تلهٔ شناخته‌شده است.
   - **(پ) آلبوم و پوشه در کتابخانه:** در `LibraryHubScreen` / `LibrarySubTab` فقط PLAYLISTS/MUSIC/ARTISTS/RADIO هست؛ گروه‌بندی آلبوم و مرور پوشه‌ها نیست و در NavGraph هم FOLDERS وجود ندارد. اضافه کن.

**«تمام‌شده» یعنی:** کد کامل در `:app` + ارجاع file:line برای هر ادعا + اینکه کدام فایل فعلی تغییر می‌کند و کدام کامپوننت جدید است. بدون حدس، بدون API یا تابعی که در سندها/کد نیست.

## C — Context (زمینه — چهار فرض پرامپت قدیمی که هر چهارتا غلط از آب درآمد)

**فرض ۱ — «به ریپو دسترسی نداری» — غلط.** تو ریپو را clone کرده‌ای و دسترسی GitHub داری (کارflow، لاگ‌ها، دیف‌ها). ولی Gradle/Android SDK در دسترس تو نیست، پس هر ادعای build/test را فقط `[UNVERIFIED]` بنویس.

**فرض ۲ — «صفحه‌ها ساخته نشده‌اند» — غلط.** صفحه‌ها روی main هستند — کامیت `b032e8f` («update»، ۱۹ نوامبر، مستقیم روی main، ۲٬۴۹۱ خط افزوده) شامل این‌هاست:
- مرحلهٔ ۲ (توکن/تم): `theme/Color.kt:90` با VeloraAlphaTokens دقیقاً 0.14/0.45/0.60؛ `ThemePaletteMode.VELORA_SAFFRON / VELORA_DAYLIGHT / HIGH_CONTRAST_MONOCHROME / SYSTEM_DYNAMIC`؛ `Type.kt` با Vazirmatn/SpaceGrotesk/Inter/JetBrainsMono؛ PhiTokens.
- ۳الف Now Playing: `NowPlayingScreen.kt` (۶۲۵ خط) با `open_queue_sheet_button:554`، `open_lyrics_sheet_button:561`، `player_favorite_button:380`، `open_exact_seek_button:620`، سقف استیج `PhiTokens.ARTWORK_STAGE_MAX_FRACTION = 0.618f:409`.
- ۳ب Library: `LibraryHubScreen.kt` + `LibrarySubTab.kt` (۴ تب: Collections/Music/Artists/Radio).
- ۳پ Home: `HomeScreen.kt` (۴۳۹ خط) با `home_shelf_top_mixes`، `home_quick_actions_row`.
- ۳ت Search: debounce از قبل هست — `VeloraViewModel.kt:101` با `.debounce { if blank 0L else 250L }`.
- ۳ث Video: `video_lock_controls_button:260`، `video_pip_button` (`VideoPlayerDialogs:117`)، سرعت 0.5/1/1.25/1.5/2.0f (`:132`)، زیرنویس کامل.
- مرحلهٔ ۴ شل دسکتاپ (پرامپت قدیمی گفت «اینجا ساخته نمی‌شود»): `VeloraDesktopShellComponents.kt` (۵۰۷ خط) + `veloraKeyboardShortcutAction.kt`.
- ناوبری: `NavGraph.kt:104` با `visibleForTier` دقیقاً SIMPLE=۴ / STANDARD·PRO=۵ و home = MUSIC در SIMPLE.
- `HYBRID-UI-INTEGRATION-REVIEW.md` همین‌ها را به‌عنوان «Stages 2–5» با `Gates: 8/8 passed` ثبت کرده.
- **مسیر فایل‌ها:** صفحه‌ها زیر `app/src/main/java/com/example/ui/screens/` هستند (زیردایرکتوری `screens/`)، نه مستقیم زیر `ui/`؛ تم زیر `ui/theme/` است.

**فرض ۳ — «بساز در `commonMain` زیر `:shared-ui`» — غلط و غیرقابل‌اجرا.** در ریپو هیچ `commonMain`، هیچ `:shared-ui`، هیچ `:desktopApp`، هیچ plugin مربوط به Jetpack-CMP و هیچ `androidResources { enable = true }` وجود ندارد. ADR-0003 (`docs/decisions/0003-desktop-playback-backend-and-hybrid-ui-review.md`) عیناً می‌گوید: «Active Build Strategy: Keep single-module `:app` for live Android builds… Use the migration templates below only when entering Phase P6» و «DO NOT apply until Phase P6» — و `include(":shared-ui")` فقط به‌صورت تمپلیت داخل ADR نوشته شده، نه در `settings.gradle.kts`. اگر الان کد commonMain تولید کنی، نه با ADR-0003 سازگار است و نه اصلاً در این پروژه compile می‌شود.

**فرض ۴ — «اتصال فقط از مسیر `VeloraUiBridgeAdapter`» — غلط.** این نام در کل ریپو، تمام برنچ‌ها و تمام اسناد صفر بار می‌آید. تنها پل واقعی که روی main هست `FazoraUiState = typealias` به `VeloraUiState` (`VeloraUiStateAssembler.kt:47`) و `PlayerEngine` (`domain/engine`) است. از همان‌ها استفاده کن؛ آداپتور تازه اختراع نکن.

**نسخه‌ها — یک pin که هنوز اعمال نشده:** CMP 1.10.3 در ADR-0003 پین شده ✓، ولی ریپو همچنان `composeBom = 2024.09.00` و Material3 از طریق BOM است (`gradle/libs.versions.toml:5`). Material3 1.9.0 stable و Coil 3.6.3 در toml نیستند — این‌ها pinهای تصمیم‌گرفته‌شده‌اند که هنوز در build اعمال نشده‌اند؛ ادعا نکن اعمال شده‌اند، به‌عنوان «pending» گزارششان کن.

**ضمیمه‌ها — کجا هستند:** `velora-master-prompt-v2.md` و `hybrid-ui-stage2-tokens-theme.md` در ریپو نیستند؛ همین‌جا ضمیمه شده‌اند و جانشین رسمی‌شان `HYBRID-UI-INTEGRATION-REVIEW.md` + ADR-0003 است. نقشهٔ ۱۲۰ کنترل (§۱۲) فقط روی برنچ `feature/ui-rebuild-phase0-1` (PR #3، mergeنشده، docs-only) است — **از روی همان برنچ بخوانش؛ merge نکن.** CIـه main از قبل قرمز است (قدم ۷) و ربطی به PR #3 ندارد.

**تله‌های شناخته‌شده (از یافته‌های RE — همچنان معتبر):**
- تیک ۱۰ هرتزی که کل UiState را بازسازی می‌کند — موقعیت پخش فقط از state باریک می‌آید.
- `backgroundVideoAudioEnabled` فقط-نوشتنی (بالا، مورد ب) — اگر به آن دست زدی، سیم‌کشی‌اش باید درست باشد.
- قانون Simple-Mode مرده در کد قدیمی — سه سطح SIMPLE/STANDARD/PRO واقعاً پیاده و تست‌پذیرند.
- سیک‌بار در فارسی LTR اجباری؛ ریاضیات ژئومتری سیک درست باشد.

## R — References (منابع)

1. **ریپوی واقعی روی main** — تنها مرجع کد. هر ادعا file:line می‌خواهد؛ یافته بدون ارجاع، حدس است.
2. **`HYBRID-UI-INTEGRATION-REVIEW.md` + ADR-0003** — جانشین رسمی ضمیمه‌های گمشده؛ ADR-0003 مرجع معماری است.
3. **خروجی مرحلهٔ ۲ (`hybrid-ui-stage2-tokens-theme.md`)** — توکن‌ها؛ و `velora-master-prompt-v2.md` (با اصلاحیهٔ §C.1 — مسیر A).
4. **`DESIGN-SYSTEM.md` §۱۲ روی برنچ `feature/ui-rebuild-phase0-1`** — نقشهٔ ۱۲۰ کنترل؛ از روی برنچ بخوان، merge نکن.
5. **پروتوتایپ هیبریدی** — مرجع بصری رفتارها؛ اگر با کد تناقض داشت، تناقض را با ارجاع بنویس و بپرس.

## E — Evaluate (معیار قبول)

**ممیزی ۱۲۰ کنترل:**
- [ ] جدول کامل: کنترل → فایل:خط → وضعیت (موجود / ناقص / غایب)؛ هر مورد غایب یا ناقص با دلیل.
- [ ] سه خلأ واقعی بالا (صف، `backgroundVideoAudioEnabled`، آلبوم/پوشه) با ارجاع کد تأیید شده‌اند.

**۳الف (هر زیرمورد جداگانه):**
- [ ] کد کامل در `:app`؛ هیچ فایل `commonMain` / `:shared-ui` ساخته نشده؛ هیچ `expect/actual` نوشته نشده.
- [ ] صف: جابه‌جایی کار می‌کند (علاوه بر حذف/انتخاب)؛ گیت «صف قابل ویرایش» ۱۰۰٪.
- [ ] `backgroundVideoAudioEnabled`: مصرف‌کنندهٔ واقعی در مسیر پخش دارد (MediaOrchestrator ON_STOP)؛ دیگر فقط-نوشتنی نیست.
- [ ] کتابخانه: گروه‌بندی آلبوم + مرور پوشه‌ها؛ FOLDERS در ناوبری.
- [ ] همهٔ رنگ/فونت/فاصله از توکن‌های مرحلهٔ ۲؛ صفر مقدار هاردکدشده؛ RTL فارسی کامل.
- [ ] هر ادعای build/test با `[UNVERIFIED]` مشخص شده (Gradle/SDK نداری).

## I — Iterate (مراحل تحویل — به همین ترتیب؛ تا مرحله‌ای تایید نشد، سراغ بعدی نرو)

**گام ۰ — ممیزی ۱۲۰ کنترل:** جدول کامل + سه خلأ تأییدشده. همین را اول بده تا تایید شود.

**گام ۱ — (الف) جابه‌جایی صف:** کد کامل + فایل‌های تغییریافته.

**گام ۲ — (ب) سیم‌کشی `backgroundVideoAudioEnabled`:** کد کامل + مسیر مصرف.

**گام ۳ — (پ) آلبوم/پوشه در کتابخانه:** کد کامل + ناوبری FOLDERS.

**قوانین کار:**
- تو به Gradle و دیوایس دسترسی نداری — **هرگز ادعا نکن چیزی را تست یا اجرا کرده‌ای.**
- اگر بین کد، پروتوتایپ و سندها تناقض دیدی، هر دو طرف را با ارجاع بنویس و بپرس؛ یکی را بی‌صدا انتخاب نکن.
- اگر سوالی داری که جوابش در سندها/کد نیست، همان‌جا بپرس و ادامه نده؛ حدس را به‌عنوان تصمیم جا نزن.
- خروجی هر گام را جداگانه و کامل بده؛ خلاصهٔ مبهم نده.
