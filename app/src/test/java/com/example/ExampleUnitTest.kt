package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.example.data.playback.TenBandBiquadAudioProcessor
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaType
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.ui.theme.HEX_CARBON
import com.example.ui.theme.HEX_GOLD
import com.example.ui.theme.HEX_IVORY
import com.example.ui.theme.HEX_OBSIDIAN
import com.example.ui.theme.HEX_STEEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class ExampleUnitTest {

    private val sampleTracks: List<Track> = (0 until 6).map { idx ->
        Track(
            id = "track_$idx",
            mediaStoreId = 1000L + idx,
            title = "Track $idx",
            artist = "Artist ${idx % 2}",
            album = "Album $idx",
            durationMs = 180_000L,
            uri = "/storage/emulated/0/Music/track_$idx.wav",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO
        )
    }

    @Test
    fun `queue shuffle actually permutes track order and visits every track`() {
        val random = Random(42L)
        val queue = PlaybackQueue(mode = QueueMode.SHUFFLE)
            .setQueue(sampleTracks, startIndex = 0, random = random)

        assertEquals(QueueMode.SHUFFLE, queue.mode)
        assertEquals(sampleTracks.size, queue.activeOrder.size)
        // Must contain all indices 0..5 exactly once
        assertEquals((0 until sampleTracks.size).toSet(), queue.activeOrder.toSet())
        // Must not be identical to natural ascending order 0,1,2,3,4,5
        assertNotEquals(sampleTracks.indices.toList(), queue.activeOrder)
    }

    @Test
    fun `queue RepeatMode OFF stops at end of queue and does not wrap to 0`() {
        var queue = PlaybackQueue(mode = QueueMode.NORMAL, repeatMode = RepeatMode.OFF)
            .setQueue(sampleTracks.take(3), startIndex = 0)

        val step1 = queue.moveNext()
        assertFalse(step1.shouldStopPlayback)
        assertEquals(1, step1.queue.currentIndex)
        queue = step1.queue

        val step2 = queue.moveNext()
        assertFalse(step2.shouldStopPlayback)
        assertEquals(2, step2.queue.currentIndex)
        queue = step2.queue

        // At last track (index 2) with RepeatMode.OFF: must stop and NOT wrap to 0
        val endStep = queue.moveNext()
        assertTrue(endStep.shouldStopPlayback)
        assertNull(endStep.nextTrack)
        assertEquals(2, endStep.queue.currentIndex)
    }

    @Test
    fun `queue RepeatMode ALL wraps from end of queue to first track in activeOrder`() {
        val queueAtEnd = PlaybackQueue(mode = QueueMode.NORMAL, repeatMode = RepeatMode.ALL)
            .setQueue(sampleTracks.take(3), startIndex = 2)

        val wrapped = queueAtEnd.moveNext()
        assertFalse(wrapped.shouldStopPlayback)
        assertEquals(0, wrapped.queue.currentIndex)
        assertEquals("track_0", wrapped.nextTrack?.id)
    }

    @Test
    fun `queue RepeatMode ONE repeats current track and requests seek to start`() {
        val queue = PlaybackQueue(mode = QueueMode.NORMAL, repeatMode = RepeatMode.ONE)
            .setQueue(sampleTracks.take(3), startIndex = 1)

        val nextResult = queue.moveNext()
        assertFalse(nextResult.shouldStopPlayback)
        assertTrue(nextResult.seekToStartOnly)
        assertEquals(1, nextResult.queue.currentIndex)
        assertEquals("track_1", nextResult.nextTrack?.id)
    }

    @Test
    fun `clampSeekPosition clamps both negative low bound and exceeding high bound`() {
        assertEquals(0L, PlaybackQueue.clampSeekPosition(-5000L, 120_000L))
        assertEquals(45_000L, PlaybackQueue.clampSeekPosition(45_000L, 120_000L))
        assertEquals(120_000L, PlaybackQueue.clampSeekPosition(250_000L, 120_000L))
        assertEquals(0L, PlaybackQueue.clampSeekPosition(10_000L, 0L))
    }

    @Test
    fun `10-band biquad DSP measurably boosts and cuts target frequency RMS power`() {
        val sampleRate = 44100
        val numSamples = sampleRate // 1.0 second of 1000 Hz sine wave at -12 dBFS (amplitude 0.25)
        val input1kHz = FloatArray(numSamples) { n ->
            (0.25 * sin(2.0 * PI * 1000.0 * n / sampleRate)).toFloat()
        }

        val flatOutput = BiquadEqDsp.processMonoSamples(input1kHz, sampleRate, EqPreset.FLAT.gainsDb)
        val flatRmsDb = BiquadEqDsp.computeRmsDb(flatOutput)

        // Apply Vocal preset (+5.5 dB at 1kHz, +4.0 dB at 500Hz, +4.5 dB at 2kHz)
        val vocalOutput = BiquadEqDsp.processMonoSamples(input1kHz, sampleRate, EqPreset.VOCAL.gainsDb)
        val vocalRmsDb = BiquadEqDsp.computeRmsDb(vocalOutput)

        // Apply -10 dB cut at 1kHz (band index 5)
        val cutGains = MutableList(EqSpecification.BAND_COUNT) { 0f }.apply { this[5] = -10.0f }
        val cutOutput = BiquadEqDsp.processMonoSamples(input1kHz, sampleRate, cutGains)
        val cutRmsDb = BiquadEqDsp.computeRmsDb(cutOutput)

        assertTrue("Vocal preset must boost 1kHz RMS by > 4 dB (flat=$flatRmsDb, vocal=$vocalRmsDb)", vocalRmsDb - flatRmsDb > 4.0)
        assertTrue("1kHz -10dB cut must attenuate 1kHz RMS by > 6 dB (flat=$flatRmsDb, cut=$cutRmsDb)", flatRmsDb - cutRmsDb > 6.0)
    }

    @Test
    fun `TenBandBiquadAudioProcessor modifies PCM 16-bit audio buffer when EQ preset is active`() {
        val processor = TenBandBiquadAudioProcessor()
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outFormat = processor.configure(format)
        assertEquals(44100, outFormat.sampleRate)
        assertTrue(processor.isActive())

        processor.setEnabled(true)
        processor.setBandGains(EqPreset.BASS_BOOST.gainsDb)

        val frames = 512
        val inputBuf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.nativeOrder())
        val originalSamples = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 62.5 * i / 44100.0) * 10000.0).toInt().toShort()
            originalSamples[i * 2] = s
            originalSamples[i * 2 + 1] = s
            inputBuf.putShort(s)
            inputBuf.putShort(s)
        }
        inputBuf.flip()

        processor.queueInput(inputBuf)
        val outBuf = processor.getOutput()
        assertTrue(outBuf.remaining() > 0)

        var differences = 0
        var idx = 0
        while (outBuf.remaining() >= 2) {
            val processed = outBuf.short
            if (abs(processed - originalSamples[idx]) > 50) {
                differences++
            }
            idx++
        }
        assertTrue("Expected BiquadAudioProcessor to alter 62.5Hz samples under BassBoost", differences > 100)
    }

    @Test
    fun `built-in EQ presets all have distinct 10-band gain profiles`() {
        val presets = EqPreset.BUILT_IN_PRESETS
        assertEquals(6, presets.size)
        val distinctGainVectors = presets.map { it.gainsDb }.toSet()
        assertEquals(presets.size, distinctGainVectors.size)

        val hwMapped = BiquadEqDsp.mapTenBandsToHardwareMillibels(EqPreset.BASS_BOOST.gainsDb, 5)
        assertEquals(5, hwMapped.size)
        assertTrue("First hardware band should be boosted (> +600 mB)", hwMapped[0] > 600)
    }

    @Test
    fun `media folder security validator blocks forbidden system and credential paths`() {
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/home"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/home/"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/home/user/.ssh"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/home/user/.ssh/id_rsa"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/tmp"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/tmp/media"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("C:/"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("D:\\"))
        assertFalse(PhiDesignMetrics.isSafeMediaFolder("/"))

        assertTrue(PhiDesignMetrics.isSafeMediaFolder("/storage/emulated/0/Music"))
        assertTrue(PhiDesignMetrics.isSafeMediaFolder("content://com.android.externalstorage.documents/tree/primary%3AMusic"))
    }

    @Test
    fun `WCAG 2_1 AA contrast ratios and Phi metrics meet specification`() {
        val ivoryOnObsidian = PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_OBSIDIAN)
        val ivoryOnCarbon = PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_CARBON)
        val goldOnObsidian = PhiDesignMetrics.contrastRatio(HEX_GOLD, HEX_OBSIDIAN)
        val steelOnCarbon = PhiDesignMetrics.contrastRatio(HEX_STEEL, HEX_CARBON)

        assertTrue("Ivory on Obsidian ($ivoryOnObsidian) must be >= 4.5", ivoryOnObsidian >= 4.5)
        assertTrue("Ivory on Carbon ($ivoryOnCarbon) must be >= 4.5", ivoryOnCarbon >= 4.5)
        assertTrue("Gold on Obsidian ($goldOnObsidian) must be >= 4.5", goldOnObsidian >= 4.5)
        assertTrue("Steel on Carbon ($steelOnCarbon) must be >= 4.5", steelOnCarbon >= 4.5)

        assertEquals(listOf(8, 13, 21, 34, 55), PhiDesignMetrics.FIBONACCI_SPACING_DP)
        assertEquals(48, PhiDesignMetrics.MIN_TOUCH_TARGET_DP)
        assertEquals(1.0f, PhiDesignMetrics.PRIMARY_SPLIT_RATIO + PhiDesignMetrics.SECONDARY_SPLIT_RATIO, 0.001f)
    }

    @Test
    fun `Goertzel 10-band spectrum analyzer and waveform downsampler accurately track frequency and amplitude`() {
        val sampleRate = 44100
        val samples1kHz = FloatArray(512) { n ->
            (0.8 * sin(2.0 * PI * 1000.0 * n / sampleRate)).toFloat()
        }

        val spectrumDb = BiquadEqDsp.computeTenBandSpectrumDb(samples1kHz, sampleRate)
        assertEquals(EqSpecification.BAND_COUNT, spectrumDb.size)

        // Band index 5 is 1000 Hz; it should have the highest spectral energy
        val peakBandIndex = spectrumDb.indices.maxByOrNull { spectrumDb[it] } ?: -1
        assertEquals(5, peakBandIndex)
        assertTrue(
            "1kHz band (${spectrumDb[5]} dB) must exceed 125Hz band (${spectrumDb[2]} dB) by > 15 dB",
            spectrumDb[5] - spectrumDb[2] > 15f
        )

        val waveform = BiquadEqDsp.downsampleWaveform(samples1kHz, EqSpecification.WAVEFORM_POINT_COUNT)
        assertEquals(EqSpecification.WAVEFORM_POINT_COUNT, waveform.size)
        assertTrue("Downsampled waveform should contain non-zero peaks", waveform.any { abs(it) > 0.3f })
    }

    @Test
    fun `VeloraLyricsAndSubtitleEngine parses LRC timestamps and resolves active synchronized line with offset`() {
        val rawLrc = """
            [00:01.00] First line in English
            [00:05.50] خط دوم به زبان فارسی
            [00:10.00] 第三行の日本語テキスト
        """.trimIndent()

        val parsed = com.example.domain.model.VeloraLyricsAndSubtitleEngine.parseLrcOrPlain(rawLrc)
        assertEquals(3, parsed.size)
        assertEquals(1_000L, parsed[0].timestampMs)
        assertEquals(5_500L, parsed[1].timestampMs)

        val activeAt6s = com.example.domain.model.VeloraLyricsAndSubtitleEngine.findActiveLineIndex(
            lines = parsed,
            positionMs = 6_000L,
            offsetMs = 0L
        )
        assertEquals(1, activeAt6s)

        // With -1000ms offset, effective position is 5000ms -> line 0 is active
        val activeWithNegativeOffset = com.example.domain.model.VeloraLyricsAndSubtitleEngine.findActiveLineIndex(
            lines = parsed,
            positionMs = 6_000L,
            offsetMs = -1_000L
        )
        assertEquals(0, activeWithNegativeOffset)
    }

    @Test
    fun `VeloraOmniSearchEngine ranks prefix, fuzzy typo-tolerant, and semantic queries accurately`() {
        val tracks = listOf(
            sampleTracks[0].copy(
                title = "Saffron Nocturne",
                artist = "Fazora Ensemble",
                uri = "/storage/Music/nocturne.flac",
                isFavorite = true
            ),
            sampleTracks[1].copy(
                title = "Golden Horizon",
                artist = "Acoustic Trio",
                uri = "/storage/Music/horizon.mp3",
                isFavorite = false
            )
        )

        // Fuzzy query with typo "Safron" (missing one 'f')
        val fuzzyMatches = com.example.domain.model.VeloraOmniSearchEngine.searchWithSemanticAndFuzzyRanking(
            query = "Safron",
            allTracks = tracks,
            history = emptyList()
        )
        assertTrue("Expected fuzzy match for 'Safron' -> 'Saffron Nocturne'", fuzzyMatches.isNotEmpty())
        assertEquals(tracks[0].id, fuzzyMatches.first().track.id)

        // Semantic query "favorite"
        val favMatches = com.example.domain.model.VeloraOmniSearchEngine.searchWithSemanticAndFuzzyRanking(
            query = "favorite",
            allTracks = tracks,
            history = emptyList()
        )
        assertEquals(1, favMatches.size)
        assertEquals(tracks[0].id, favMatches.first().track.id)
    }

    @Test
    fun `VeloraSmartPlaylistEngine and VeloraCompatibilityMatrix generate mixes and validate streams`() {
        val tracks = sampleTracks.mapIndexed { idx, track ->
            track.copy(isFavorite = idx % 2 == 0)
        }
        val history = listOf(
            com.example.domain.model.PlaybackHistoryEntry(
                track = tracks[0],
                lastPlayedAtMs = 1_700_000_000_000L,
                lastPositionMs = 45_000L,
                playCount = 4,
                completedCount = 2
            )
        )

        val smartMixes = com.example.domain.model.VeloraSmartPlaylistEngine.generateSmartPlaylists(tracks, history)
        assertTrue("Expected smart mixes to be generated", smartMixes.size >= 3)
        assertTrue(smartMixes.any { it.category == com.example.domain.model.SmartPlaylistCategory.DAILY_SAFFRON_MIX })
        assertTrue(smartMixes.any { it.category == com.example.domain.model.SmartPlaylistCategory.CONTINUE_LISTENING })
        assertTrue(smartMixes.any { it.category == com.example.domain.model.SmartPlaylistCategory.FAVORITE_COLLECTION })

        assertTrue(
            com.example.domain.model.VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(
                "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
            )
        )
        assertFalse(
            com.example.domain.model.VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(
                "file:///etc/passwd"
            )
        )
    }

    @Test
    fun `VeloraSubtitleExporter, DownloadValidator, MediaCopilot, and PersonalMediaGraph enforce specification contracts`() {
        val sampleLyrics = com.example.domain.model.VeloraLyricsAndSubtitleEngine.resolveAutomaticLyricsForTrack(sampleTracks[0])
        val srt = com.example.domain.model.VeloraSubtitleExporter.exportToSrt(sampleLyrics)
        val vtt = com.example.domain.model.VeloraSubtitleExporter.exportToWebVtt(sampleLyrics)

        assertTrue("SRT must contain timestamp arrow", srt.contains("00:00:00,000 -->"))
        assertTrue("WebVTT must start with WEBVTT header", vtt.startsWith("WEBVTT"))
        assertTrue("WebVTT must contain dot millisecond separator", vtt.contains("00:00:00.000 -->"))

        // Magnet & download path-traversal validator
        assertTrue(
            com.example.domain.model.VeloraDownloadValidator.isValidAuthorizedMagnetUri(
                "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=CreativeCommonsAudio"
            )
        )
        assertFalse(com.example.domain.model.VeloraDownloadValidator.isValidAuthorizedMagnetUri("http://example.com/bad"))
        assertTrue(com.example.domain.model.VeloraDownloadValidator.isSafeDownloadTargetFileName("album/track_01.flac"))
        assertFalse(com.example.domain.model.VeloraDownloadValidator.isSafeDownloadTargetFileName("../../etc/passwd"))
        assertFalse(com.example.domain.model.VeloraDownloadValidator.isSafeDownloadTargetFileName("/root/.ssh/authorized_keys"))

        // Media Copilot intent routing
        val srtIntent = com.example.domain.model.VeloraMediaCopilot.interpretCommand("export srt subtitles")
        assertEquals(com.example.domain.model.CopilotCommandIntent.EXPORT_SUBTITLES_SRT, srtIntent.intent)

        val resumeIntent = com.example.domain.model.VeloraMediaCopilot.interpretCommand("ادامه آخرین قطعه ناتمام")
        assertEquals(com.example.domain.model.CopilotCommandIntent.RESUME_LAST_SESSION, resumeIntent.intent)

        // Personal Media Graph & Platform Matrix
        val graph = com.example.domain.model.VeloraPersonalMediaGraph.buildGraphSummary(sampleTracks)
        assertTrue(graph.totalNodes > sampleTracks.size)
        assertEquals(sampleTracks.size * 3, graph.totalEdges)
        assertTrue(com.example.domain.model.VeloraPlatformMatrix.CAPABILITY_MATRIX.isNotEmpty())
    }

    @Test
    fun `VeloraRadioCatalog, StudioDspState, and VeloraBackupExporter support live radio filtering and DSP state`() {
        val allStations = com.example.domain.model.VeloraRadioCatalog.BUILT_IN_STATIONS
        assertTrue("Built-in radio catalog must contain stations", allStations.size >= 10)

        val persianStations = com.example.domain.model.VeloraRadioCatalog.filterStations(
            allStations = allStations,
            category = com.example.domain.model.RadioCategory.PERSIAN,
            query = ""
        )
        assertTrue("Persian category must return stations", persianStations.isNotEmpty())

        val customStation = com.example.domain.model.VeloraRadioCatalog.createCustomStation(
            name = "Tehran Night FM",
            streamUrl = "https://stream.radioparadise.com/mp3-192",
            genreOrCountry = "Persian / Synth"
        )
        assertTrue(customStation != null)
        assertEquals("radio_${customStation!!.id}", customStation.toPlayableTrack().id)

        // Unsafe localhost SSRF radio stream must be rejected
        val unsafeStation = com.example.domain.model.VeloraRadioCatalog.createCustomStation(
            name = "Localhost Bad",
            streamUrl = "http://127.0.0.1:8080/stream.mp3"
        )
        assertNull(unsafeStation)

        // StudioDspState A-B loop & sleep timer formatting
        val dsp = com.example.domain.model.StudioDspState(
            preampGainDb = 3.5f,
            bassBoostPercent = 65,
            stereoWidthPercent = 40,
            crossfadeSeconds = 4,
            loopStartMs = 10_000L,
            loopEndMs = 25_000L,
            sleepTimerRemainingSec = 95,
            sleepTimerLabel = "15m"
        )
        assertTrue(dsp.isAbLoopActive)
        assertEquals("01:35", dsp.formattedSleepTimer)

        val backupJson = com.example.domain.model.VeloraBackupExporter.exportBackupJson(
            playlists = emptyList(),
            eqPresets = com.example.domain.model.EqPreset.BUILT_IN_PRESETS,
            bookmarks = emptyList(),
            customStations = listOf(customStation),
            dspState = dsp
        )
        assertTrue(backupJson.contains("\"product\": \"VELORA\""))
        assertTrue(backupJson.contains("\"customRadioStations\": 1"))
    }
}

