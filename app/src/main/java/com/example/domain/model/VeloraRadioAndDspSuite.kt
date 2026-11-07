package com.example.domain.model

import java.util.Locale

enum class RadioCategory(
    val id: String,
    val labelEn: String,
    val labelFa: String
) {
    ALL("all", "All Stations", "همه ایستگاه‌ها"),
    PERSIAN("persian", "Persian & East", "فارسی و شرقی"),
    AUDIOPHILE_HIFI("hifi", "Audiophile Hi-Fi", "کیفیت استودیویی Hi-Fi"),
    CHILL_LOFI("chill", "Chill & Lo-Fi", "چیل و لوفای"),
    SYNTHWAVE_NIGHT("night", "Night Drive & Synth", "رانندگی شبانه و سینث"),
    CLASSICAL_JAZZ("classical", "Classical & Jazz", "کلاسیک و جاز"),
    CUSTOM("custom", "Custom Streams", "ایستگاه‌های شخصی")
}

data class RadioStation(
    val id: String,
    val name: String,
    val nameFa: String,
    val tagline: String,
    val streamUrl: String,
    val category: RadioCategory,
    val country: String,
    val codecLabel: String = "AAC / MP3",
    val bitrateKbps: Int = 320,
    val frequencyMhz: String = "98.5 FM",
    val isCustom: Boolean = false
) {
    fun toPlayableTrack(): Track = Track(
        id = "radio_$id",
        mediaStoreId = (id.hashCode().toLong() and 0x7FFFFFFFL) + 800_000_000L,
        title = "$name ($frequencyMhz)",
        artist = "$country • $tagline",
        album = "VELORA Live Radio ($codecLabel ${bitrateKbps}kbps)",
        durationMs = 0L,
        uri = streamUrl,
        folderPath = "/storage/emulated/0/Music/VeloraRadio",
        mediaType = MediaType.AUDIO,
        artworkUri = null,
        addedTimestampMs = System.currentTimeMillis(),
        sampleRateHz = 48_000,
        bitrateKbps = bitrateKbps
    )
}

data class StudioDspState(
    val preampGainDb: Float = 0f,
    val bassBoostPercent: Int = 0,
    val stereoWidthPercent: Int = 0,
    val crossfadeSeconds: Int = 0,
    val loopStartMs: Long? = null,
    val loopEndMs: Long? = null,
    val sleepTimerRemainingSec: Int = 0,
    val sleepTimerLabel: String = "Off"
) {
    val isAbLoopActive: Boolean
        get() = loopStartMs != null && loopEndMs != null && loopEndMs > loopStartMs

    val formattedSleepTimer: String
        get() {
            if (sleepTimerRemainingSec <= 0) return "Off"
            val mins = sleepTimerRemainingSec / 60
            val secs = sleepTimerRemainingSec % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
}

object VeloraRadioCatalog {

    val BUILT_IN_STATIONS: List<RadioStation> = listOf(
        RadioStation(
            id = "persian_farda_live",
            name = "Radio Farda Live",
            nameFa = "رادیو فردا (پخش زنده ۲۴ ساعته)",
            tagline = "Persian Contemporary Music, Culture & Live Broadcast",
            streamUrl = "https://n09.radiojar.com/cp13r2cpn3quv",
            category = RadioCategory.PERSIAN,
            country = "IR / Global",
            codecLabel = "MP3 Live",
            bitrateKbps = 128,
            frequencyMhz = "98.5 FM"
        ),
        RadioStation(
            id = "persian_oriental_lounge",
            name = "SomaFM Suburbs of Goa (Eastern & Persian Fusion)",
            nameFa = "موسیقی تلفیقی شرقی و سنتی",
            tagline = "Asian & Eastern Traditional Beats, Sitar, Santoor & Lounge",
            streamUrl = "https://ice1.somafm.com/suburbsofgoa-128-mp3",
            category = RadioCategory.PERSIAN,
            country = "Eastern Fusion",
            codecLabel = "MP3 Stereo",
            bitrateKbps = 128,
            frequencyMhz = "94.2 FM"
        ),
        RadioStation(
            id = "persian_desert_acoustic",
            name = "Radio Paradise Global & Silk Road Mix",
            nameFa = "رادیو جاده ابریشم و موسیقی ملل",
            tagline = "Listener-Supported Global Eclectic & Middle Eastern Gems",
            streamUrl = "https://stream.radioparadise.com/global-320",
            category = RadioCategory.PERSIAN,
            country = "Global / Silk Road",
            codecLabel = "AAC Hi-Fi",
            bitrateKbps = 320,
            frequencyMhz = "101.7 FM"
        ),
        RadioStation(
            id = "rp_main_320",
            name = "Radio Paradise Main Mix (320k Hi-Fi)",
            nameFa = "رادیو پارادایس (میکس اصلی استودیویی)",
            tagline = "Audiophile Eclectic Rock, Indie, Electronica & World",
            streamUrl = "https://stream.radioparadise.com/aac-320",
            category = RadioCategory.AUDIOPHILE_HIFI,
            country = "USA (California)",
            codecLabel = "AAC Reference",
            bitrateKbps = 320,
            frequencyMhz = "99.1 FM"
        ),
        RadioStation(
            id = "rp_mellow_320",
            name = "Radio Paradise Mellow Mix (320k Hi-Fi)",
            nameFa = "رادیو پارادایس آرام و آکوستیک",
            tagline = "Warm Acoustic, Ambient & Audiophile Vocal Ballads",
            streamUrl = "https://stream.radioparadise.com/mellow-320",
            category = RadioCategory.AUDIOPHILE_HIFI,
            country = "USA (California)",
            codecLabel = "AAC Reference",
            bitrateKbps = 320,
            frequencyMhz = "99.5 FM"
        ),
        RadioStation(
            id = "rp_rock_320",
            name = "Radio Paradise Rock Mix (320k Hi-Fi)",
            nameFa = "رادیو پارادایس راک استودیویی",
            tagline = "Classic & Modern Progressive Rock in High Dynamic Range",
            streamUrl = "https://stream.radioparadise.com/rock-320",
            category = RadioCategory.AUDIOPHILE_HIFI,
            country = "USA (California)",
            codecLabel = "AAC Reference",
            bitrateKbps = 320,
            frequencyMhz = "100.3 FM"
        ),
        RadioStation(
            id = "somafm_groove_salad",
            name = "SomaFM Groove Salad",
            nameFa = "سوما اف‌ام — چیل‌اوت و امبینت",
            tagline = "A Nicely Chilled Plate of Ambient/Downtempo Beats",
            streamUrl = "https://ice1.somafm.com/groovesalad-256-mp3",
            category = RadioCategory.CHILL_LOFI,
            country = "USA (San Francisco)",
            codecLabel = "MP3 Master",
            bitrateKbps = 256,
            frequencyMhz = "102.5 FM"
        ),
        RadioStation(
            id = "somafm_drone_zone",
            name = "SomaFM Drone Zone",
            nameFa = "سوما اف‌ام — فضای عمیق و مدیتیشن",
            tagline = "Atmospheric Textures with Minimal Beats for Deep Focus",
            streamUrl = "https://ice1.somafm.com/dronezone-256-mp3",
            category = RadioCategory.CHILL_LOFI,
            country = "USA (San Francisco)",
            codecLabel = "MP3 Master",
            bitrateKbps = 256,
            frequencyMhz = "103.1 FM"
        ),
        RadioStation(
            id = "somafm_lush",
            name = "SomaFM Lush Vocals",
            nameFa = "سوما اف‌ام — وکال آرام و رویایی",
            tagline = "Sensuous and Mellow Female Vocals with Electronic Influence",
            streamUrl = "https://ice1.somafm.com/lush-128-mp3",
            category = RadioCategory.CHILL_LOFI,
            country = "USA (San Francisco)",
            codecLabel = "MP3 Stereo",
            bitrateKbps = 128,
            frequencyMhz = "103.9 FM"
        ),
        RadioStation(
            id = "nightride_fm",
            name = "Nightride FM — Synthwave & Cyberpunk",
            nameFa = "نایت‌راید اف‌ام — رانندگی شبانه و سینث‌ویو",
            tagline = "24/7 Retrowave, Outrun, Darksynth & Midnight Drive",
            streamUrl = "https://stream.nightride.fm/nightride.m4a",
            category = RadioCategory.SYNTHWAVE_NIGHT,
            country = "Global Cyber",
            codecLabel = "AAC+ Studio",
            bitrateKbps = 256,
            frequencyMhz = "88.4 FM"
        ),
        RadioStation(
            id = "somafm_defcon",
            name = "SomaFM DEF CON Radio",
            nameFa = "سوما اف‌ام — موسیقی برنامه‌نویسی و سایبری",
            tagline = "Music for Hacking, Coding, Engineering & Late-Night Focus",
            streamUrl = "https://ice1.somafm.com/defcon-256-mp3",
            category = RadioCategory.SYNTHWAVE_NIGHT,
            country = "USA (Las Vegas)",
            codecLabel = "MP3 Master",
            bitrateKbps = 256,
            frequencyMhz = "91.6 FM"
        ),
        RadioStation(
            id = "somafm_secret_agent",
            name = "SomaFM Secret Agent",
            nameFa = "سوما اف‌ام — موسیقی سینمایی و جاسوسی",
            tagline = "The Soundtrack for Your Stylish, Mysterious, Dangerous Life",
            streamUrl = "https://ice1.somafm.com/secretagent-128-mp3",
            category = RadioCategory.SYNTHWAVE_NIGHT,
            country = "USA (San Francisco)",
            codecLabel = "MP3 Stereo",
            bitrateKbps = 128,
            frequencyMhz = "104.7 FM"
        ),
        RadioStation(
            id = "swiss_jazz",
            name = "Radio Swiss Jazz",
            nameFa = "رادیو سوئیس جاز (کیفیت مرجع)",
            tagline = "24/7 Pure Jazz, Soul & Blues Without Commercials",
            streamUrl = "https://stream.srg-ssr.ch/m/rsj/mp3_128",
            category = RadioCategory.CLASSICAL_JAZZ,
            country = "Switzerland",
            codecLabel = "MP3 Broadcast",
            bitrateKbps = 128,
            frequencyMhz = "96.8 FM"
        ),
        RadioStation(
            id = "swiss_classic",
            name = "Radio Swiss Classic",
            nameFa = "رادیو سوئیس کلاسیک (ارکسترال)",
            tagline = "Symphonic Masterpieces & Chamber Music 24/7",
            streamUrl = "https://stream.srg-ssr.ch/m/rsc_de/mp3_128",
            category = RadioCategory.CLASSICAL_JAZZ,
            country = "Switzerland",
            codecLabel = "MP3 Broadcast",
            bitrateKbps = 128,
            frequencyMhz = "97.4 FM"
        )
    )

    fun filterStations(
        allStations: List<RadioStation>,
        category: RadioCategory,
        query: String
    ): List<RadioStation> {
        val byCat = if (category == RadioCategory.ALL) {
            allStations
        } else {
            allStations.filter { it.category == category }
        }
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isEmpty()) return byCat
        return byCat.filter { station ->
            station.name.lowercase(Locale.ROOT).contains(cleanQuery) ||
                station.nameFa.contains(cleanQuery) ||
                station.tagline.lowercase(Locale.ROOT).contains(cleanQuery) ||
                station.country.lowercase(Locale.ROOT).contains(cleanQuery) ||
                station.frequencyMhz.lowercase(Locale.ROOT).contains(cleanQuery)
        }
    }

    fun createCustomStation(
        name: String,
        streamUrl: String,
        genreOrCountry: String = "Custom Live Stream"
    ): RadioStation? {
        val cleanUrl = streamUrl.trim()
        if (!VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(cleanUrl)) return null
        val cleanName = name.trim().ifBlank { "Custom Radio Station" }
        val hash = kotlin.math.abs(cleanUrl.hashCode())
        val freq = String.format(Locale.US, "%d.%d FM", 88 + (hash % 20), (hash / 20) % 10)
        return RadioStation(
            id = "custom_$hash",
            name = cleanName,
            nameFa = cleanName,
            tagline = genreOrCountry.trim().ifBlank { "User Custom Radio Stream" },
            streamUrl = cleanUrl,
            category = RadioCategory.CUSTOM,
            country = "Custom",
            codecLabel = "Live Stream",
            bitrateKbps = 256,
            frequencyMhz = freq,
            isCustom = true
        )
    }
}

object VeloraBackupExporter {

    fun exportBackupJson(
        playlists: List<Playlist>,
        eqPresets: List<EqPreset>,
        bookmarks: List<TimeMachineBookmark>,
        customStations: List<RadioStation>,
        dspState: StudioDspState
    ): String {
        val customPresets = eqPresets.filter { !it.isBuiltIn }
        return buildString {
            appendLine("{")
            appendLine("  \"product\": \"VELORA\",")
            appendLine("  \"schemaVersion\": 2,")
            appendLine("  \"exportedAtMs\": ${System.currentTimeMillis()},")
            appendLine("  \"dspConfig\": {")
            appendLine("    \"preampGainDb\": ${dspState.preampGainDb},")
            appendLine("    \"bassBoostPercent\": ${dspState.bassBoostPercent},")
            appendLine("    \"stereoWidthPercent\": ${dspState.stereoWidthPercent},")
            appendLine("    \"crossfadeSeconds\": ${dspState.crossfadeSeconds}")
            appendLine("  },")
            appendLine("  \"counts\": {")
            appendLine("    \"playlists\": ${playlists.size},")
            appendLine("    \"customEqPresets\": ${customPresets.size},")
            appendLine("    \"bookmarks\": ${bookmarks.size},")
            appendLine("    \"customRadioStations\": ${customStations.size}")
            appendLine("  }")
            append("}")
        }
    }
}
