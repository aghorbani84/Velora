package com.example.data.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.domain.model.AudioRouteInfo
import com.example.domain.model.AudioRouteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VeloraAudioDeviceManager(
    private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val _activeRoute = MutableStateFlow(inspectCurrentRoute())
    val activeRoute: StateFlow<AudioRouteInfo> = _activeRoute.asStateFlow()

    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                refreshRoute()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                refreshRoute()
            }
        }
    } else {
        null
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null && deviceCallback != null) {
            try {
                audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register audio device callback: ${e.message}")
            }
        }
    }

    fun refreshRoute() {
        _activeRoute.value = inspectCurrentRoute()
    }

    private fun inspectCurrentRoute(): AudioRouteInfo {
        if (audioManager == null) {
            return AudioRouteInfo(
                type = AudioRouteType.SPEAKER,
                deviceName = "Internal Speaker",
                recommendedProfile = "Phone Speaker Calibration"
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = try {
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            } catch (e: Exception) {
                emptyArray<AudioDeviceInfo>()
            }

            val usbDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            }
            if (usbDevice != null) {
                val name = usbDevice.productName?.toString()?.takeIf { it.isNotBlank() } ?: "USB DAC / Audio Interface"
                val maxRate = if (usbDevice.sampleRates.isNotEmpty()) usbDevice.sampleRates.maxOrNull() ?: 96000 else 96000
                return AudioRouteInfo(
                    type = AudioRouteType.USB_DAC,
                    deviceName = name,
                    isHiRes = true,
                    sampleRateHz = maxRate,
                    recommendedProfile = "Bit-Perfect Passthrough"
                )
            }

            val wiredDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            }
            if (wiredDevice != null) {
                val name = wiredDevice.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Wired 3.5mm Analog"
                val maxRate = if (wiredDevice.sampleRates.isNotEmpty()) wiredDevice.sampleRates.maxOrNull() ?: 48000 else 48000
                return AudioRouteInfo(
                    type = AudioRouteType.WIRED_HEADPHONES,
                    deviceName = name,
                    isHiRes = true,
                    sampleRateHz = maxRate,
                    recommendedProfile = "Audiophile Pure Fidelity (Flat)"
                )
            }

            val btDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            }
            if (btDevice != null) {
                val name = btDevice.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio (A2DP)"
                val maxRate = if (btDevice.sampleRates.isNotEmpty()) btDevice.sampleRates.maxOrNull() ?: 44100 else 44100
                return AudioRouteInfo(
                    type = AudioRouteType.BLUETOOTH,
                    deviceName = name,
                    isHiRes = false,
                    sampleRateHz = maxRate,
                    recommendedProfile = "Wireless Clarity & Treble Air"
                )
            }
        }

        return AudioRouteInfo(
            type = AudioRouteType.SPEAKER,
            deviceName = "Phone Speaker",
            sampleRateHz = 48000,
            recommendedProfile = "Speaker Clarity & Dynamic Limiter"
        )
    }

    companion object {
        private const val TAG = "AudioDeviceManager"
    }
}
