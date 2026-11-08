package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralized permission controller for Android 8.0–16 (API 26–36) media and notification permissions.
 */
object PermissionController {

    /**
     * Returns the runtime permissions required to scan local audio and video libraries
     * based on the device's Android SDK level.
     */
    fun requiredMediaPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Returns true if at least one required media read permission is currently granted.
     */
    fun hasMediaLibraryPermission(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
        val required = requiredMediaPermissions(sdkInt)
        return required.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns true if POST_NOTIFICATIONS is granted (or not required on API < 33).
     */
    fun hasNotificationPermission(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
