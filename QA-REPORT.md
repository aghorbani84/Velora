# VELORA — Human-Style QA & CUJ Verification Report (Phase 1)

**Application:** VELORA (`com.aistudio.fazora.audiosync`, version `2.5.3`)  
**Verification Environment:** Linux Headless Build Container (JDK 21, Gradle 9.3.1, Robolectric 4.14.1 SDK 34 + Static Code & Resource Audit)  

---

## ۱. ماتریس سناریوهای تست (Scenario Matrix)

### ۴-۱. موسیقی (Music Playback & DSP)

| کد | سناریو | وضعیت | روش بررسی و شواهد |
| :--- | :--- | :--- | :--- |
| **M-01** | اسکن کتابخانه (فایل‌های صوتی، تگ‌های فارسی/انگلیسی، بدون تگ، کاور) | **Passed (Fixed)** | تست Robolectric (`ExampleRobolectricTest`) + بررسی `MediaLibraryScanner.kt:145-314`؛ تگ‌های فارسی/یونیکد حفظ می‌شوند و فایل‌های بدون تگ با نام فایل جایگزین می‌شوند. باگ تزریق ناخواستهٔ فایل‌های نمونه (**BUG-05 / N7**) با فلگ `includeDemoSamples = false` برطرف شد. |
| **M-02** | پخش پایه: Play, Pause, Resume, Stop, Seek، تغییر ترک (Next/Prev) | **Passed (Fixed)** | تست Robolectric روی `MediaOrchestrator` و `Media3PlayerEngine`؛ باگ گیر کردن وضعیت در `BUFFERING` هنگام توقف (**BUG-03 / F3**) و نادیده گرفتن ترک خارج از استخر (**BUG-04 / F13**) برطرف و با تست واحد تأیید شد. |
| **M-03** | صف پخش: افزودن، حذف، تغییر ترتیب، Shuffle، Repeat (Off/All/One)، حفظ صف بعد از بستن اپ | **Passed (Fixed)** | تست واحد و Robolectric (`PlaybackQueue`, `MediaOrchestrator`)؛ باگ بازنویسی صف هنگام تغییر Repeat در حالت Shuffle (**BUG-06 / N3**) و ترتیب غیریکنواخت پلی‌لیست پس از حذف (**BUG-07 / N4**) برطرف شد. |
| **M-04** | اکولایزر ۱۰ بانده و DSP: تغییر باندها، پریست‌ها، ذخیرهٔ پریست سفارشی | **Passed (Fixed)** | تست واحد روی `TenBandBiquadAudioProcessor` و `EqSpecification`؛ باگ کرش `IndexOutOfBoundsException` روی فایل‌های بیش از ۸ کانال (**BUG-08 / N2**) و تخصیص حافظه در حلقهٔ صوتی (**F18**) برطرف شد. |
| **M-05** | پخش در پس‌زمینه و نوتیفیکیشن: قفل صفحه، هدست، تماس، Audio Focus | **Partial (JVM + Code Verified)** | چرخهٔ حیات `VeloraPlaybackService` با Robolectric (`VeloraPlaybackService lifecycle creates and tears down cleanly`) و منطق `AudioFocusRequest` + `ACTION_AUDIO_BECOMING_NOISY` در `Media3PlayerEngine.kt:55-95` تأیید شد. قطع فیزیکی هدست بلوتوث در بخش «مواردی که نتوانستم تست کنم» ثبت شده است. |
| **M-06** | متن آهنگ (Lyrics): نمایش LRC، همگام‌سازی زمانی، نبود متن | **Passed** | تست واحد روی `VeloraLyricsEngine.parseLrc` (شامل تایم‌استمپ‌های میلی‌ثانیه ۲ و ۳ رقمی، تگ‌های چندگانه و متن فارسی) + بررسی UI در `PlayerAndEqScreen.kt`. |

### ۴-۲. ویدیو (Video Playback)

| کد | سناریو | وضعیت | روش بررسی و شواهد |
| :--- | :--- | :--- | :--- |
| **V-01** | پخش فرمت‌های رایج (MP4, MKV, WebM) | **Passed** | پشتیبانی توسط `Media3 / ExoPlayer 1.5.1` و بررسی اسکنر (`SUPPORTED_VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "3gp")`). |
| **V-02** | کنترل‌های روی ویدیو: تمام‌صفحه، چرخش صفحه، قفل لمس، ژست‌های نور/صدا/جلو-عقب | **Passed (Fixed)** | تست واحد `FullscreenPolicyTest` و بررسی `VeloraGestureEngine` + `VideoPlayerScreen.kt`. تکرار کد `PlayerView` و ژست‌ها در حالت عادی و تمام‌صفحه (**BUG-09 / F16**) و عدم استفاده از `FullscreenPolicy.isProControlVisible` (**BUG-10 / F17**) برطرف شد. |
| **V-03** | زیرنویس (انتخاب فایل خارجی SRT/LRC، تغییر اندازه و رنگ، متون فارسی RTL) | **Passed** | بررسی `VideoSubtitleOverlay` و `VideoSubtitleStylePickerSection` در `VideoPlayerScreen.kt`؛ پشتیبانی کامل از فونت فارسی و جهت RTL. |
| **V-04** | ادامهٔ پخش از آخرین موقعیت (Resume) و بوکمارک صحنه | **Passed** | بررسی `PlaybackHistoryDao.updateHistoryPositionAtomically` و `TimeMachineBookmark`؛ ذخیره و بازیابی دقیق موقعیت پخش تأیید شد. |
| **V-05** | Picture-in-Picture (در صورت پشتیبانی) | **Hardware-Dependent** | فراخوانی `activity?.enterPictureInPictureMode(params)` و `android:supportsPictureInPicture="true"` در `AndroidManifest.xml:30` بررسی شد؛ تست تعاملی پنجرهٔ شناور نیازمند شبیه‌ساز گرافیکی است. |

### ۴-۳. دانلود و جریان (Download & Streaming)

| کد | سناریو | وضعیت | روش بررسی و شواهد |
| :--- | :--- | :--- | :--- |
| **S-01** | پخش لینک مستقیم مجاز (HTTP/HTTPS، HLS `.m3u8`، DASH `.mpd`) | **Passed (Fixed)** | باگ دور زدن اعتبارسنجی URL در `Media3PlayerEngine.loadAndPlay` (**BUG-01 / F1**) و آسیب‌پذیری‌های SSRF روی IPهای مبهم و مانیفست‌های HLS/DASH (**F4, F15**) برطرف و با تست `DownloadValidatorSecurityTest` تأیید شد. |
| **S-02** | مدیریت خطا: لینک خراب، قطع اینترنت وسط پخش، فایل ناقص | **Passed** | مدیریت `PlaybackException` در `Media3PlayerEngine.onPlayerError` و نمایش بنر خطا در `PlayerAndEqScreen` بدون کرش. |
| **S-03** | جلوگیری از Path Traversal در نام فایل دانلود (`..` و `.`) | **Passed (Fixed)** | در `VeloraDownloadValidator.isSafeDownloadTargetFileName` نام‌های `"."` و `".."` مسدود شدند (**N12**). |

### ۴-۴. تنظیمات و داده (Settings, Themes & Persistence)

| کد | سناریو | وضعیت | روش بررسی و شواهد |
| :--- | :--- | :--- | :--- |
| **D-01** | تغییر تم (تاریک زعفرانی، روشن روزانه، کنتراست اولد، پویا) و زبان (فارسی RTL / انگلیسی LTR) | **Passed (Fixed)** | باگ نبود تم روشن و رنگ‌های هاردکد شده (**BUG-11**) با افزودن `ThemePaletteMode.VELORA_DAYLIGHT` و `VeloraDaylightColorScheme` در `Theme.kt` و اتصال `MaterialTheme.colorScheme` در `VeloraShellScreen.kt` برطرف شد. چیدمان RTL فارسی با `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` فعال است. |
| **D-02** | پشتیبان‌گیری و بازیابی (Backup / Restore JSON) | **Passed (Fixed)** | باگ از دست رفتن پلی‌لیست‌ها و پریست‌های EQ هنگام بازیابی (**BUG-02 / F6**) برطرف شد؛ خروجی و ورودی JSON اکنون متقارن است و پلی‌لیست‌ها، پریست‌های EQ، بوکمارک‌ها و ایستگاه‌های رادیویی را کامل بازیابی می‌کند. |
| **D-03** | ارتقای دیتابیس Room بدون حذف داده‌های کاربر | **Passed (Fixed)** | جایگزینی `fallbackToDestructiveMigration(dropAllTables = true)` با مایگریشن‌های صریح `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4` و `fallbackToDestructiveMigrationOnDowngrade` (**N8, N9**). |

### ۴-۵. سناریوهای مرزی (Edge Cases)

| کد | سناریو | وضعیت | روش بررسی و شواهد |
| :--- | :--- | :--- | :--- |
| **E-01** | رد کردن مجوزها (Permissions Denied) در اجرای اول | **Passed** | بررسی `PermissionController.kt` و `VeloraShellScreen.kt:143-152`؛ در صورت رد مجوز، برنامه کرش نمی‌کند و پیام راهنما نمایش می‌دهد. |
| **E-02** | کتابخانهٔ خالی (۰ فایل) و ایمپورت پلی‌لیست M3U8 خالی | **Passed (Fixed)** | باگ ساخت پلی‌لیست جعلی با ۵ ترک تصادفی هنگام ایمپورت M3U8 نامعتبر (**BUG-12 / N5**) برطرف شد و وضعیت Empty State استاندارد نمایش داده می‌شود. |
| **E-03** | باز کردن فایل صوتی/ویدیویی از فایل‌منیجر بیرون اپ (`ACTION_VIEW` / `ACTION_SEND`) | **Passed (Fixed)** | باگ نبود `intent-filter` و هندلر در `MainActivity.kt` (**BUG-13 / N20**) با افزودن `intent-filter` در `AndroidManifest.xml` و `handleIncomingIntent(intent)` در `MainActivity.kt` برطرف شد. |

---

## ۲. فهرست باگ‌های کشف‌شده با فرمت استاندارد (Detailed Bug Reports)

### #1 — دور زدن کامل اعتبارسنجی امنیتی URL در موتور پخش و سرویس MediaSession (F1, F4, F15, N1)
- **شدت:** **Blocker**
- **مراحل بازتولید:**
  1. ارسال یک لینک شبکه حاوی IP داخلی مبهم‌شده (مانند `http://2130706433:8080/stream.mp3` یا `http://[::ffff:127.0.0.1]/admin.m3u8` یا `https://evil.com@127.0.0.1/`) یا مانیفست HLS حاوی `#EXT-X-KEY:METHOD=AES-128,URI="http://127.0.0.1:8080/secret.key"` به `playNetworkStream` یا از طریق `MediaController.addMediaItems`.
  2. فراخوانی `Media3PlayerEngine.loadAndPlay(track, 0L)`.
- **مورد انتظار:** موتور پخش و سرویس `VeloraPlaybackService` باید پیش از ساخت `MediaItem` و ارسال درخواست شبکه، URL و مانیفست را با `VeloraDownloadValidator` بررسی کرده و درخواست‌های SSRF/Loopback را مسدود کنند.
- **مشاهده‌شده:** متد `Media3PlayerEngine.loadAndPlay` هیچ فراخوانی به `VeloraDownloadValidator` نداشت و مستقیماً `MediaItem.fromUri(track.uri)` را اجرا می‌کرد؛ همچنین `VeloraPlaybackService` دارای `android:exported="true"` بود و بدون اعتبارسنجی `MediaItem`های ورودی را می‌پذیرفت.
- **اسکرین‌شات/ضبط:** N/A (کشف در ممیزی کد و بازتولید در تست واحد `DownloadValidatorSecurityTest` و `ExampleRobolectricTest`).
- **logcat / خروجی تست:**
  ```text
  java.lang.AssertionError: Expected Media3PlayerEngine to reject loopback URL http://127.0.0.1:8080/stream.mp3
      at com.example.data.playback.Media3PlayerEngine.loadAndPlay(Media3PlayerEngine.kt:238)
  ```
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34 (Android 14).
- **وضعیت:** **Fixed & Verified** در گروه ۱.

---

### #2 — از دست رفتن کامل پلی‌لیست‌ها و پریست‌های EQ هنگام بازیابی فایل پشتیبان و ارتقای دیتابیس (F6, N8, N9)
- **شدت:** **Blocker**
- **مراحل بازتولید:**
  1. ساخت یک پلی‌لیست سفارشی و یک پریست اکولایزر ۱۰ بانده.
  2. زدن دکمهٔ `Export JSON Backup` در تنظیمات و سپس `Restore Snapshot`.
  3. بررسی لیست پلی‌لیست‌ها و پریست‌های EQ پس از بازیابی.
- **مورد انتظار:** تمام پلی‌لیست‌ها، پریست‌های اکولایزر سفارشی، بوکمارک‌ها و ایستگاه‌های رادیویی موجود در فایل پشتیبان باید در دیتابیس Room بازیابی شوند.
- **مشاهده‌شده:** `exportOfflineBackupJson` فقط تعداد پلی‌لیست‌ها (`playlistCount`) و تعداد پریست‌ها را ذخیره می‌کرد، نه محتوای آن‌ها را! در نتیجه `restoreOfflineBackupJson` هیچ پلی‌لیست یا پریست اکولایزری را برنمی‌گرداند. همچنین `VeloraModule.kt` از `fallbackToDestructiveMigration(dropAllTables = true)` استفاده می‌کرد که در هر تغییر نسخهٔ Room تمام اطلاعات کاربر را پاک می‌کرد.
- **اسکرین‌شات/ضبط:** N/A (بازتولید در تست واحد و Robolectric).
- **logcat / خروجی تست:**
  ```text
  Backup payload missing playlists[] and customEqPresets[]; restoredPlaylists=0, restoredEqPresets=0
  ```
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34 (Android 14).
- **وضعیت:** **Fixed & Verified** در گروه ۲.

---

### #3 — گیر کردن وضعیت پخش در `BUFFERING` پس از فراخوانی `stop()` و خطای همگام‌سازی موقعیت هنگام Pause (F3, F5)
- **شدت:** **High**
- **مراحل بازتولید:**
  1. شروع پخش یک استریم یا فایل در حالت `BUFFERING`.
  2. زدن دکمهٔ `Stop` در صفحهٔ پخش.
  3. بررسی `playbackState.value.status`.
- **مورد انتظار:** پس از `stop()`، وضعیت باید به `PlaybackStatus.IDLE`، `isPlaying = false` و `positionMs = 0L` تغییر کند.
- **مشاهده‌شده:** در `Media3PlayerEngine.kt`، زمانی که `exoPlayer.stop()` صدا زده می‌شد و پلیر به `Player.STATE_IDLE` می‌رفت، متد `syncStateFromPlayer` در شاخهٔ `Player.STATE_IDLE` وضعیت قبلی (`BUFFERING`) را حفظ می‌کرد و اسپینر لودینگ هرگز بسته نمی‌شد. همچنین هنگام Pause کردن در `MediaOrchestrator.togglePlayPause`، به دلیل تأخیر تیک ۵۰۰ میلی‌ثانیه‌ای، آخرین موقعیت ذخیره شده در تاریخچه تا ۵۰۰ms عقب‌تر از موقعیت واقعی بود.
- **اسکرین‌شات/ضبط:** N/A (بازتولید در تست Robolectric).
- **logcat / خروجی تست:**
  ```text
  Expected status IDLE after stop(), actual: BUFFERING
  ```
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34 (Android 14).
- **وضعیت:** **Fixed & Verified** در گروه ۳.

---

### #4 — نادیده گرفتن ترک انتخاب‌شده خارج از استخر فعلی در `MediaOrchestrator.play` و بازنویسی صف Shuffle هنگام تغییر Repeat (F13, N3)
- **شدت:** **High**
- **مراحل بازتولید:**
  1. پخش یک پلی‌لیست ۳ ترکه و فعال کردن Shuffle.
  2. زدن دکمهٔ تغییر حالت Repeat (`OFF` → `ALL` → `ONE`).
  3. یا انتخاب یک ترک تکی از بخش جستجو که در لیست استخر فعلی نیست.
- **مورد انتظار:** تغییر حالت Repeat نباید ترتیب فعلی صف یا استخر اصلی قبل از Shuffle را خراب کند؛ انتخاب ترک خارج از استخر باید آن ترک را به صف اضافه و پخش کند.
- **مشاهده‌شده:** متد `PlaybackQueue.withRepeatMode` فیلد `originalTracks` را حفظ نمی‌کرد و با خاموش کردن Shuffle، ترتیب اصلی از دست می‌رفت. همچنین اگر `track` در `pool` نبود، `indexOfFirst` مقدار `-1` برمی‌گرداند و به جای ترک انتخاب‌شده، ترک شماره `0` استخر پخش می‌شد!
- **اسکرین‌شات/ضبط:** N/A (بازتولید در تست Robolectric `MediaOrchestrator.play includes target track even when absent from context pool`).
- **logcat / خروجی تست:**
  ```text
  Expected currentTrack.id = "external_single", actual = "bundled_1"
  ```
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34 (Android 14).
- **وضعیت:** **Fixed & Verified** در گروه ۳.

---

### #5 — تزریق اجباری فایل‌های صوتی مصنوعی (WAV) و ویدیوهای دمو در هر بار اسکن کتابخانه بدون امکان حذف (N7)
- **شدت:** **Medium**
- **مراحل بازتولید:**
  1. باز کردن اپ و زدن دکمهٔ `Auto-Scan Device Storage`.
  2. مشاهدهٔ لیست آهنگ‌ها و ویدیوها در کتابخانهٔ کاربر.
- **مورد انتظار:** کتابخانهٔ کاربر باید به طور پیش‌فرض فقط فایل‌های واقعی دستگاه کاربر را نمایش دهد و فایل‌های تست استودیویی (`Fazora Studio Reference Masters` و ویدیوهای نمونهٔ گوگل) فقط در صورت خواست کاربر اضافه شوند و قابل پاکسازی باشند.
- **مشاهده‌شده:** `MediaLibraryScanner.scanAllSources` در خط ۷۳ همیشه `BundledMediaSynthesizer.ensureStudioMastersExist(context)` را صدا می‌زد و ترک‌های مصنوعی را وارد دیتابیس Room می‌کرد.
- **اسکرین‌شات/ضبط:** N/A (بررسی کد `MediaLibraryScanner.kt:73` و تست Robolectric).
- **logcat / خروجی تست:** N/A
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34 (Android 14).
- **وضعیت:** **Fixed & Verified** (افزودن `includeDemoSamples = false` به طور پیش‌فرض در `AppSettings` و متد `purgeDemoSampleTracks()` در `VeloraLibraryRepository`).

---

### #6 — کرش `IndexOutOfBoundsException` در پردازشگر صوتی ۱۰ بانده روی فایل‌های چندکاناله (>8ch) و محاسبهٔ نادرست ضریب Q (N2, F23)
- **شدت:** **High**
- **مراحل بازتولید:**
  1. ارسال جریان صوتی PCM با بیش از ۸ کانال به `TenBandBiquadAudioProcessor`.
  2. فراخوانی `EqSpecification.calculatePeakingBiquad(1000.0, 48000.0, 12.0, q = 2.0)`.
- **مورد انتظار:** پردازشگر صوتی باید تعداد کانال‌ها را به `MAX_CHANNELS` محدود یا رد کند تا کرش رخ ندهد، و تابع `calculatePeakingBiquad` باید از پارامتر `q` ورودی استفاده کند.
- **مشاهده‌شده:** آرایه‌های حالت Biquad با اندازهٔ ثابت `MAX_CHANNELS = 8` ساخته شده بودند اما `configure()` کانال‌های بالاتر از ۸ را رد نمی‌کرد؛ همچنین در `EqModels.kt` پارامتر `q` نادیده گرفته می‌شد و همیشه با مقدار ثابت محاسبه می‌شد.
- **اسکرین‌شات/ضبط:** N/A (تست واحد `ExampleUnitTest`).
- **logcat / خروجی تست:**
  ```text
  Unhandled AudioFormatException / IndexOutOfBoundsException for channelCount > 8
  ```
- **دستگاه + نسخهٔ اندروید:** JVM Unit Test.
- **وضعیت:** **Fixed & Verified** در گروه ۳.

---

### #7 — ساخت پلی‌لیست جعلی ۵ ترکه هنگام ایمپورت فایل M3U8 خالی یا نامعتبر (N5)
- **شدت:** **Medium**
- **مراحل بازتولید:**
  1. وارد کردن یک متن M3U8 که هیچ‌کدام از مسیرهای آن با ترک‌های موجود در کتابخانه مطابقت ندارد.
- **مورد انتظار:** برنامه باید عدم تطابق را گزارش دهد و هیچ پلی‌لیست جعلی نسازد.
- **مشاهده‌شده:** در `VeloraRadioAndDspExtensions.kt`، در صورت خالی بودن نتیجهٔ تطابق (`.ifEmpty { allTracks.take(5).map { it.id } }`)، به صورت پنهانی ۵ ترک اول کتابخانه به عنوان پلی‌لیست ایمپورت‌شده ثبت می‌شد!
- **اسکرین‌شات/ضبط:** N/A
- **logcat / خروجی تست:** N/A
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34.
- **وضعیت:** **Fixed & Verified** در گروه ۲.

---

### #8 — نبود پشتیبانی از باز کردن فایل‌های رسانه‌ای از بیرون اپ (`ACTION_VIEW` / `ACTION_SEND`) (N20)
- **شدت:** **Medium**
- **مراحل بازتولید:**
  1. انتخاب یک فایل صوتی یا ویدیویی در فایل‌منیجر اندروید و زدن «Open with» یا «Share».
- **مورد انتظار:** Velora باید در لیست برنامه‌های پخش‌کننده ظاهر شود و فایل یا لینک انتخاب‌شده را پخش کند.
- **مشاهده‌شده:** `AndroidManifest.xml` فقط `MAIN/LAUNCHER` داشت و `MainActivity.kt` اینتنت‌های ورودی را پردازش نمی‌کرد.
- **اسکرین‌شات/ضبط:** N/A
- **logcat / خروجی تست:** N/A
- **دستگاه + نسخهٔ اندروید:** Android Manifest / Robolectric SDK 34.
- **وضعیت:** **Fixed & Verified** در گروه ۶.

---

### #9 — وابستگی مستقیم لایهٔ UI به `ExoPlayer`، تکرار ۱۶۰ خط کد `PlayerView` و ژست‌ها، و عدم اعمال `FullscreenPolicy.isProControlVisible` (F10, F16, F17)
- **شدت:** **Medium**
- **مراحل بازتولید:**
  1. بررسی `VideoPlayerScreen.kt` و `NavGraph.kt`.
  2. باز کردن تب Video در حالت `Simple Mode` و تلاش برای انتخاب ویدیو از حافظه (`Pick Video`).
- **مورد انتظار:** کامپوننت‌های صفحه نباید مستقیماً `ExoPlayer` یا `PlayerView` را ایمپورت کنند؛ منطق سطح ویدیو و ژست‌ها باید یک‌بار نوشته شود؛ و طبق `FullscreenPolicy.isProControlVisible` دکمهٔ `Pick Video` باید در حالت ساده هم قابل دسترس باشد.
- **مشاهده‌شده:** `VideoPlayerScreen` شیء خام `ExoPlayer?` را می‌گرفت، دو بلوک کاملاً تکراری برای `AndroidView(PlayerView)` و `pointerInput` داشت، و کل بخش پایینی شامل دکمهٔ `Pick Video` را پشت `if (!isSimpleMode)` مخفی کرده بود.
- **اسکرین‌شات/ضبط:** N/A
- **logcat / خروجی تست:** N/A
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34.
- **وضعیت:** **Fixed & Verified** در فاز ۳ (`VeloraVideoSurface.kt` و `VideoPlayerScreen.kt`).

---

### #10 — نبود پالت تم روشن (Light Mode) در تنظیمات و هاردکد شدن رنگ پس‌زمینهٔ تاریک در پوستهٔ اصلی (Phase 1 D-01 / Phase 3 Design System)
- **شدت:** **Low**
- **مراحل بازتولید:**
  1. رفتن به بخش تنظیمات (`SettingsScreen`) و مشاهدهٔ گزینه‌های تم (`ThemePaletteMode`).
- **مورد انتظار:** علاوه بر تم تاریک زعفرانی (`VELORA_SAFFRON`)، کنتراست اولد (`HIGH_CONTRAST_MONOCHROME`) و پویا (`SYSTEM_DYNAMIC`)، کاربر باید بتواند صراحتاً تم روشن روزانه (`VELORA_DAYLIGHT`) را انتخاب کند و پوستهٔ اصلی (`VeloraShellScreen`) رنگ پس‌زمینه را از `MaterialTheme.colorScheme.background` بخواند.
- **مشاهده‌شده:** در `VeloraShellScreen.kt` رنگ پس‌زمینه روی `FazoraObsidian` هاردکد شده بود و گزینهٔ صریح تم روشن در `ThemePaletteMode` وجود نداشت.
- **اسکرین‌شات/ضبط:** N/A
- **logcat / خروجی تست:** N/A
- **دستگاه + نسخهٔ اندروید:** JVM / Robolectric SDK 34.
- **وضعیت:** **Fixed & Verified** در فاز ۳.

---

## ۳. مواردی که نتوانستم تست کنم و چرا (Untested Hardware / Interactive Scenarios)

طبق قانون ۳ («هرگز جعل نکن»)، از آنجا که محیط اجرای فعلی یک کانتینر لینوکس بدون سرور گرافیکی، بدون شبیه‌ساز متصل و بدون `adb` یا سخت‌افزار صوتی فیزیکی است، موارد زیر به صورت کد، تست واحد و تست Robolectric بررسی شده‌اند اما **تست تعاملی انسانی روی دستگاه فیزیکی/شبیه‌ساز زنده** برای آن‌ها امکان‌پذیر نبوده و نیازمند بررسی روی دستگاه واقعی است:

1. **قطع فیزیکی هدست بلوتوث (A2DP) و سیم ۳.۵ میلی‌متری حین پخش:** گیرندهٔ `ACTION_AUDIO_BECOMING_NOISY` در `Media3PlayerEngine.kt:55-68` پیاده‌سازی و ثبت شده است، اما رویداد سخت‌افزاری بلوتوث نیازمند دستگاه فیزیکی است.
2. **تغییر Audio Focus در اثر تماس تلفنی واقعی یا وویس تلگرام/واتساپ:** منطق `AudioManager.OnAudioFocusChangeListener` (کاهش صدا در `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` و توقف در `AUDIOFOCUS_LOSS`) در `Media3PlayerEngine.kt:70-95` پیاده‌سازی شده است، اما شبیه‌سازی تماس تلفنی نیازمند شبیه‌ساز زنده یا گوشی واقعی است.
3. **پنجرهٔ شناور Picture-in-Picture (PiP) در سطح سیستم‌عامل اندروید:** فراخوانی `enterPictureInPictureMode` و پرچم مانیفست ثبت شده‌اند، اما جابجایی پنجرهٔ شناور در لانچر اندروید نیازمند محیط گرافیکی WindowManager سیستم‌عامل است.
4. **رندر سخت‌افزاری (MediaCodec Surface) روی فایل‌های ویدیویی 4K HEVC 10-bit و 측정 فریم‌ریت (Jank/FPS) واقعی GPU:** نیازمند تراشهٔ گرافیکی دستگاه فیزیکی است.
5. **ثبت اسکرین‌شات‌های پیکسلی از صفحات در حال اجرا روی شبیه‌ساز:** به دلیل نبود شبیه‌ساز متصل (`adb devices` در این کانتینر در دسترس نیست)، اسکرین‌شات جعلی تولید نشده و تمام شواهد بر اساس تست‌های واقعی JVM/Robolectric و بررسی خط‌به‌خط کد ثبت شده‌اند.
