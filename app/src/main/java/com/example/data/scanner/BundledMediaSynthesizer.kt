package com.example.data.scanner

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.domain.model.MediaType
import com.example.domain.model.Track
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object BundledMediaSynthesizer {

    private const val TAG = "BundledMediaSynth"
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
            album = "Phi Reference Masters",
            durationSeconds = 12,
            frequenciesHz = listOf(62.5, 125.0, 500.0, 1000.0, 4000.0, 8000.0),
            modRateHz = 1.618
        ),
        SynthSpec(
            fileName = "fazora_golden_harmonic.wav",
            id = "studio_master_2",
            mediaStoreId = 900_002L,
            title = "Golden Ratio Overtone Suite",
            artist = "Fazora Acoustic Lab",
            album = "Phi Reference Masters",
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

        val videoFile = File(studioDir, "fazora_motion_precision.mp4")
        val mp4Created = if (!videoFile.exists() || videoFile.length() < 512L) {
            generatePlayableMp4Video(videoFile)
        } else {
            true
        }

        val effectiveVideoUri = if (mp4Created && videoFile.exists() && videoFile.length() > 512L) {
            videoFile.absolutePath
        } else {
            val fallbackVideoAudioFile = File(studioDir, "fazora_motion_precision_stream.wav")
            if (!fallbackVideoAudioFile.exists() || fallbackVideoAudioFile.length() < 1024L) {
                writeStereoWavFile(
                    targetFile = fallbackVideoAudioFile,
                    durationSeconds = 10,
                    frequenciesHz = listOf(146.83, 220.0, 293.66, 440.0, 880.0),
                    modRateHz = 1.618
                )
            }
            fallbackVideoAudioFile.absolutePath
        }

        results.add(
            Track(
                id = "studio_video_1",
                mediaStoreId = 900_101L,
                title = "Fazora Motion Calibration Reel (1080p Phi Frame)",
                artist = "Fazora Visual Lab",
                album = "Motion Calibration",
                durationMs = 10_000L,
                uri = effectiveVideoUri,
                folderPath = studioDir.absolutePath,
                mediaType = MediaType.VIDEO,
                artworkUri = null,
                addedTimestampMs = System.currentTimeMillis(),
                sampleRateHz = SAMPLE_RATE,
                bitrateKbps = 2400
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

    private fun generatePlayableMp4Video(outputFile: File): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        return try {
            val width = 320
            val height = 192
            val mime = MediaFormat.MIMETYPE_VIDEO_AVC
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 400_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec = MediaCodec.createEncoderByType(mime)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false
            val totalFrames = 150
            val frameSize = width * height * 3 / 2
            val frameData = ByteArray(frameSize)

            for (frameIdx in 0..totalFrames) {
                val inputIndex = codec.dequeueInputBuffer(10_000L)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    inputBuffer?.clear()
                    if (frameIdx == totalFrames) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            (frameIdx * 1_000_000L) / 15L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    } else {
                        populateYuv420Frame(frameData, width, height, frameIdx)
                        inputBuffer?.put(frameData)
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            frameSize,
                            (frameIdx * 1_000_000L) / 15L,
                            0
                        )
                    }
                }

                var draining = true
                while (draining) {
                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            draining = writeSampleAndCheckContinue(codec, muxer, bufferInfo, outIndex, trackIndex, muxerStarted)
                        }
                    }
                }
            }
            if (muxerStarted) {
                muxer.stop()
            }
            true
        } catch (e: Throwable) {
            Log.d(TAG, "Hardware H.264 encoder unavailable; using WAV stream fallback: ${e.message}")
            false
        } finally {
            runCatching { codec?.release() }
            runCatching { muxer?.release() }
        }
    }

    private fun writeSampleAndCheckContinue(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        outIndex: Int,
        trackIndex: Int,
        muxerStarted: Boolean
    ): Boolean {
        val encodedData = codec.getOutputBuffer(outIndex)
        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
            encodedData.position(bufferInfo.offset)
            encodedData.limit(bufferInfo.offset + bufferInfo.size)
            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
        }
        codec.releaseOutputBuffer(outIndex, false)
        return (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0
    }

    private fun populateYuv420Frame(frameData: ByteArray, width: Int, height: Int, frameIdx: Int) {
        val yPlaneSize = width * height
        val barX = (frameIdx * 4) % width
        for (y in 0 until height) {
            for (x in 0 until width) {
                val isBar = kotlin.math.abs(x - barX) < 18
                frameData[y * width + x] = if (isBar) 195.toByte() else 24.toByte()
            }
        }
        for (uv in yPlaneSize until frameData.size) {
            frameData[uv] = 128.toByte()
        }
    }
}
