package com.example.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.domain.model.AppSettings
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.VideoAspectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import java.util.Locale

class VeloraSettingsRepository(
    private val context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    fileName: String = "velora_settings"
) {
    private val rawDataStoreFile: File = context.preferencesDataStoreFile(fileName)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { ex ->
            Log.e(TAG, "Corrupt DataStore preferences detected; backing up and restoring defaults", ex)
            backupCorruptFileIfPresent(rawDataStoreFile)
            val fallbackPrefs = emptyPreferences().toMutablePreferences()
            fallbackPrefs[KEY_CORRUPTION_COUNT] = 1
            fallbackPrefs
        },
        scope = scope,
        produceFile = { rawDataStoreFile }
    )

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { ex ->
            if (ex is IOException) {
                Log.e(TAG, "IOException reading DataStore; falling back to defaults", ex)
                emit(emptyPreferences())
            } else {
                throw ex
            }
        }
        .map { prefs -> mapPreferencesToSettings(prefs) }

    suspend fun getSettingsOnce(): AppSettings = settingsFlow.first()

    suspend fun updateEqEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EQ_ENABLED] = enabled }
    }

    suspend fun updateEqPresetAndGains(presetId: String, gainsDb: List<Float>) {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_EQ_PRESET] = presetId
            prefs[KEY_CUSTOM_EQ_GAINS] = normalized.joinToString(",") {
                "%.2f".format(Locale.US, it)
            }
        }
    }

    suspend fun updateQueueModes(queueMode: QueueMode, repeatMode: RepeatMode) {
        dataStore.edit { prefs ->
            prefs[KEY_QUEUE_MODE] = queueMode.name
            prefs[KEY_REPEAT_MODE] = repeatMode.name
        }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        dataStore.edit { it[KEY_PLAYBACK_SPEED] = safeSpeed }
    }

    suspend fun updateVideoAspectMode(mode: VideoAspectMode) {
        dataStore.edit { it[KEY_VIDEO_ASPECT] = mode.name }
    }

    suspend fun updateAutoHideControlsMs(timeoutMs: Long) {
        dataStore.edit { it[KEY_AUTO_HIDE_MS] = timeoutMs.coerceIn(1500L, 15000L) }
    }

    suspend fun updateAppLanguageCode(languageCode: String) {
        val normalized = if (languageCode == "fa") "fa" else "en"
        dataStore.edit { it[KEY_APP_LANGUAGE] = normalized }
    }

    suspend fun updateSimpleMode(enabled: Boolean) {
        dataStore.edit { it[KEY_SIMPLE_MODE] = enabled }
    }

    suspend fun updateVideoGestureHintSeen(seen: Boolean) {
        dataStore.edit { it[KEY_VIDEO_GESTURE_HINT_SEEN] = seen }
    }

    suspend fun updateAutoScanOnStartup(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SCAN_STARTUP] = enabled }
    }

    suspend fun updateOnlyValidMusic(enabled: Boolean) {
        dataStore.edit { it[KEY_ONLY_VALID_MUSIC] = enabled }
    }

    suspend fun updateBackgroundVideoAudio(enabled: Boolean) {
        dataStore.edit { it[KEY_BG_VIDEO_AUDIO] = enabled }
    }

    suspend fun updateLowBatteryMode(enabled: Boolean) {
        dataStore.edit { it[KEY_LOW_BATTERY_MODE] = enabled }
    }

    suspend fun updateAdaptiveAudioMode(mode: com.example.domain.model.AdaptiveAudioMode) {
        dataStore.edit { it[KEY_ADAPTIVE_AUDIO] = mode.name }
    }

    suspend fun updateThemePaletteMode(mode: com.example.domain.model.ThemePaletteMode) {
        dataStore.edit { it[KEY_THEME_PALETTE_MODE] = mode.name }
    }

    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun addAllowedFolder(folderUriOrPath: String): Boolean {
        if (!PhiDesignMetrics.isSafeMediaFolder(folderUriOrPath)) {
            Log.w(TAG, "Rejected unsafe media folder path: $folderUriOrPath")
            return false
        }
        dataStore.edit { prefs ->
            val current = prefs[KEY_ALLOWED_FOLDERS] ?: emptySet()
            prefs[KEY_ALLOWED_FOLDERS] = current + folderUriOrPath
        }
        return true
    }

    suspend fun removeAllowedFolder(folderUriOrPath: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_ALLOWED_FOLDERS] ?: emptySet()
            prefs[KEY_ALLOWED_FOLDERS] = current - folderUriOrPath
        }
    }

    suspend fun saveQueueSession(
        queueTrackIds: List<String>,
        activeTrackId: String?,
        positionMs: Long
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_QUEUE_IDS] = queueTrackIds.joinToString("|")
            if (activeTrackId != null) {
                prefs[KEY_LAST_ACTIVE_TRACK] = activeTrackId
            } else {
                prefs.remove(KEY_LAST_ACTIVE_TRACK)
            }
            prefs[KEY_LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    private fun mapPreferencesToSettings(prefs: Preferences): AppSettings {
        val eqEnabled = prefs[KEY_EQ_ENABLED] ?: true
        val presetId = prefs[KEY_ACTIVE_EQ_PRESET] ?: EqPreset.FLAT.id
        val gainsCsv = prefs[KEY_CUSTOM_EQ_GAINS]
        val gains = if (!gainsCsv.isNullOrBlank()) {
            val parsed = gainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
            if (parsed.size == EqSpecification.BAND_COUNT) {
                EqSpecification.normalizeGains(parsed)
            } else {
                EqPreset.FLAT.gainsDb
            }
        } else {
            EqPreset.FLAT.gainsDb
        }

        val queueMode = prefs[KEY_QUEUE_MODE]?.let { raw ->
            runCatching { QueueMode.valueOf(raw) }.getOrNull()
        } ?: QueueMode.NORMAL

        val repeatMode = prefs[KEY_REPEAT_MODE]?.let { raw ->
            runCatching { RepeatMode.valueOf(raw) }.getOrNull()
        } ?: RepeatMode.OFF

        val speed = (prefs[KEY_PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.25f, 3.0f)

        val aspectMode = prefs[KEY_VIDEO_ASPECT]?.let { raw ->
            runCatching { VideoAspectMode.valueOf(raw) }.getOrNull()
        } ?: VideoAspectMode.FIT

        val autoHideMs = (prefs[KEY_AUTO_HIDE_MS] ?: 3500L).coerceIn(1500L, 15000L)
        val allowedFolders = (prefs[KEY_ALLOWED_FOLDERS] ?: emptySet())
            .filter { PhiDesignMetrics.isSafeMediaFolder(it) }
            .toSet()

        val lastQueueIds = prefs[KEY_LAST_QUEUE_IDS]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return AppSettings(
            eqEnabled = eqEnabled,
            activeEqPresetId = presetId,
            customEqGainsDb = gains,
            queueMode = queueMode,
            repeatMode = repeatMode,
            defaultPlaybackSpeed = speed,
            videoAspectMode = aspectMode,
            autoHideVideoControlsMs = autoHideMs,
            allowedFolderUris = allowedFolders,
            lastQueueTrackIds = lastQueueIds,
            lastActiveTrackId = prefs[KEY_LAST_ACTIVE_TRACK],
            lastPositionMs = (prefs[KEY_LAST_POSITION_MS] ?: 0L).coerceAtLeast(0L),
            corruptionFallbackCount = prefs[KEY_CORRUPTION_COUNT] ?: 0,
            appLanguageCode = prefs[KEY_APP_LANGUAGE] ?: "en",
            isSimpleMode = prefs[KEY_SIMPLE_MODE] ?: true,
            videoGestureHintSeen = prefs[KEY_VIDEO_GESTURE_HINT_SEEN] ?: false,
            autoScanOnStartup = prefs[KEY_AUTO_SCAN_STARTUP] ?: true,
            onlyValidMusicInSearch = prefs[KEY_ONLY_VALID_MUSIC] ?: true,
            backgroundVideoAudioEnabled = prefs[KEY_BG_VIDEO_AUDIO] ?: true,
            lowBatteryModeEnabled = prefs[KEY_LOW_BATTERY_MODE] ?: false,
            adaptiveAudioMode = prefs[KEY_ADAPTIVE_AUDIO]?.let {
                runCatching { com.example.domain.model.AdaptiveAudioMode.valueOf(it) }.getOrNull()
            } ?: com.example.domain.model.AdaptiveAudioMode.ADAPTIVE_AI,
            dynamicColorEnabled = prefs[KEY_DYNAMIC_COLOR_ENABLED] ?: true,
            themePaletteMode = prefs[KEY_THEME_PALETTE_MODE]?.let {
                runCatching { com.example.domain.model.ThemePaletteMode.valueOf(it) }.getOrNull()
            } ?: com.example.domain.model.ThemePaletteMode.SYSTEM_DYNAMIC
        )
    }

    companion object {
        private const val TAG = "VeloraSettings"

        private val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val KEY_ACTIVE_EQ_PRESET = stringPreferencesKey("active_eq_preset")
        private val KEY_CUSTOM_EQ_GAINS = stringPreferencesKey("custom_eq_gains")
        private val KEY_QUEUE_MODE = stringPreferencesKey("queue_mode")
        private val KEY_REPEAT_MODE = stringPreferencesKey("repeat_mode")
        private val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val KEY_VIDEO_ASPECT = stringPreferencesKey("video_aspect")
        private val KEY_AUTO_HIDE_MS = longPreferencesKey("auto_hide_ms")
        private val KEY_ALLOWED_FOLDERS = stringSetPreferencesKey("allowed_folders")
        private val KEY_LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        private val KEY_LAST_ACTIVE_TRACK = stringPreferencesKey("last_active_track")
        private val KEY_LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        private val KEY_CORRUPTION_COUNT = intPreferencesKey("corruption_count")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language_code")
        private val KEY_SIMPLE_MODE = booleanPreferencesKey("is_simple_mode")
        private val KEY_VIDEO_GESTURE_HINT_SEEN = booleanPreferencesKey("video_gesture_hint_seen")
        private val KEY_AUTO_SCAN_STARTUP = booleanPreferencesKey("auto_scan_startup")
        private val KEY_ONLY_VALID_MUSIC = booleanPreferencesKey("only_valid_music")
        private val KEY_BG_VIDEO_AUDIO = booleanPreferencesKey("bg_video_audio")
        private val KEY_LOW_BATTERY_MODE = booleanPreferencesKey("low_battery_mode")
        private val KEY_ADAPTIVE_AUDIO = stringPreferencesKey("adaptive_audio_mode")
        private val KEY_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        private val KEY_THEME_PALETTE_MODE = stringPreferencesKey("theme_palette_mode")

        fun backupCorruptFileIfPresent(targetFile: File): File? {
            return try {
                if (targetFile.exists()) {
                    val backupFile = File(targetFile.parentFile, "${targetFile.name}.corrupt.bak")
                    targetFile.copyTo(backupFile, overwrite = true)
                    backupFile
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create corrupt settings backup", e)
                null
            }
        }
    }
}
