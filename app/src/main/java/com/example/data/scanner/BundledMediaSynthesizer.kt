package com.example.data.scanner

import android.content.Context
import com.example.domain.model.MediaType
import com.example.domain.model.Track
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object BundledMediaSynthesizer {

    private const val SAMPLE_RATE = 44100

    data class SynthSpec(
        val fileName: String,
        val id: String,
        val mediaStoreId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val frequenciesHz: List<Double>,
        val modRateHz: Double
    )

    private val AUDIO_SPECS = listOf(
        SynthSpec(
            fileName = "fazora_obsidian_pulse.wav",
            id = "studio_master_1",
            mediaStoreId = 900_001L,
            title = "Obsidian Pulse (62Hz + 1kHz + 8kHz Reference)",
            artist = "Fazora Acoustic Lab",
            album = "Velora Studio Masters",
            durationSeconds = 12,
            frequenciesHz = listOf(62.5, 125.0, 500.0, 1000.0, 4000.0, 8000.0),
            modRateHz = 1.618
        ),
        SynthSpec(
            fileName = "fazora_golden_harmonic.wav",
            id = "studio_master_2",
            mediaStoreId = 900_002L,
            title = "Velora Harmonic Acoustic Suite",
            artist = "Fazora Acoustic Lab",
            album = "Velora Studio Masters",
            durationSeconds = 15,
            frequenciesHz = listOf(110.0, 178.0, 288.0, 466.0, 754.0, 1220.0),
            modRateHz = 0.618
        ),
        SynthSpec(
            fileName = "fazora_velvet_nocturne.wav",
            id = "studio_master_3",
            mediaStoreId = 900_003L,
            title = "Carbon & Ivory Nocturne",
            artist = "Abolfazl Studio Ensemble",
            album = "Sound. Motion. Precision.",
            durationSeconds = 14,
            frequenciesHz = listOf(82.41, 164.81, 246.94, 329.63, 659.25, 2637.0),
            modRateHz = 1.0
        ),
        SynthSpec(
            fileName = "fazora_sub_bass_telemetry.wav",
            id = "studio_master_4",
            mediaStoreId = 900_004L,
            title = "Sub-Bass & Treble Air Sweep",
            artist = "Abolfazl Studio Ensemble",
            album = "Sound. Motion. Precision.",
            durationSeconds = 10,
            frequenciesHz = listOf(31.25, 62.5, 250.0, 2000.0, 8000.0, 14000.0),
            modRateHz = 2.2
        )
    )

    fun ensureStudioMastersExist(context: Context): List<Track> {
        val studioDir = File(context.filesDir, "fazora_studio_masters").apply { mkdirs() }
        val results = mutableListOf<Track>()

        for (spec in AUDIO_SPECS) {
            val targetFile = File(studioDir, spec.fileName)
            if (!targetFile.exists() || targetFile.length() < 1024L) {
                writeStereoWavFile(targetFile, spec.durationSeconds, spec.frequenciesHz, spec.modRateHz)
            }
            results.add(
                Track(
                    id = spec.id,
                    mediaStoreId = spec.mediaStoreId,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = spec.durationSeconds * 1000L,
                    uri = targetFile.absolutePath,
                    folderPath = studioDir.absolutePath,
                    mediaType = MediaType.AUDIO,
                    artworkUri = null,
                    addedTimestampMs = targetFile.lastModified(),
                    sampleRateHz = SAMPLE_RATE,
                    bitrateKbps = 1411
                )
            )
        }

        results.add(
            Track(
                id = "studio_video_1",
                mediaStoreId = 900_101L,
                title = "Big Buck Bunny (1080p Cinema Demo)",
                artist = "Blender Foundation",
                album = "Open Cinema Showcase",
                durationMs = 596_000L,
                uri = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
                folderPath = studioDir.absolutePath,
                mediaType = MediaType.VIDEO,
                artworkUri = null,
                addedTimestampMs = System.currentTimeMillis(),
                sampleRateHz = SAMPLE_RATE,
                bitrateKbps = 2400
            )
        )

        results.add(
            Track(
                id = "studio_video_2",
                mediaStoreId = 900_102L,
                title = "Tears of Steel (Sci-Fi 4K / 1080p Reel)",
                artist = "Mango Open Movie",
                album = "VFX Master Collection",
                durationMs = 734_000L,
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                folderPath = studioDir.absolutePath,
                mediaType = MediaType.VIDEO,
                artworkUri = null,
                addedTimestampMs = System.currentTimeMillis(),
                sampleRateHz = SAMPLE_RATE,
                bitrateKbps = 4800
            )
        )

        return results
    }

    fun writeStereoWavFile(
        targetFile: File,
        durationSeconds: Int,
        frequenciesHz: List<Double>,
        modRateHz: Double
    ) {
        val numChannels = 2
        val bitsPerSample = 16
        val totalFrames = SAMPLE_RATE * durationSeconds.coerceIn(1, 30)
        val byteRate = SAMPLE_RATE * numChannels * (bitsPerSample / 8)
        val dataSize = totalFrames * numChannels * (bitsPerSample / 8)
        val riffChunkSize = 36 + dataSize

        FileOutputStream(targetFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(riffChunkSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(numChannels.toShort())
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort((numChannels * (bitsPerSample / 8)).toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())

            val chunkFrames = 2048
            val pcmBuffer = ByteBuffer.allocate(chunkFrames * 4).order(ByteOrder.LITTLE_ENDIAN)
            val freqCount = frequenciesHz.size.coerceAtLeast(1)
            var frameIdx = 0

            while (frameIdx < totalFrames) {
                pcmBuffer.clear()
                val framesThisBatch = (totalFrames - frameIdx).coerceAtMost(chunkFrames)
                for (i in 0 until framesThisBatch) {
                    val t = (frameIdx + i).toDouble() / SAMPLE_RATE.toDouble()
                    val envelope = 0.65 + 0.35 * sin(2.0 * PI * modRateHz * t)
                    var leftSum = 0.0
                    var rightSum = 0.0
                    for ((idx, freq) in frequenciesHz.withIndex()) {
                        val phaseOffset = idx * 0.45
                        leftSum += sin(2.0 * PI * freq * t + phaseOffset)
                        rightSum += sin(2.0 * PI * freq * t + phaseOffset + 0.3)
                    }
                    val leftNorm = (leftSum / freqCount) * envelope * 0.75
                    val rightNorm = (rightSum / freqCount) * envelope * 0.75
                    val leftShort = (leftNorm.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    val rightShort = (rightNorm.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    pcmBuffer.putShort(leftShort)
                    pcmBuffer.putShort(rightShort)
                }
                fos.write(pcmBuffer.array(), 0, framesThisBatch * 4)
                frameIdx += framesThisBatch
            }
        }
    }
}
