package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

enum class VeloraKeyboardShortcutAction {
    TOGGLE_PLAY_PAUSE,
    SEEK_FORWARD_5S,
    SEEK_BACKWARD_5S,
    VOLUME_UP,
    VOLUME_DOWN,
    TOGGLE_MUTE,
    TOGGLE_VIDEO_FULLSCREEN
}

/**
 * Pure key-event mapper for hardware keyboard playback shortcuts so behavior is deterministic
 * and unit-testable without requiring a physical Android device or Desktop JVM window.
 */
fun resolveVeloraKeyboardShortcut(event: KeyEvent): VeloraKeyboardShortcutAction? {
    if (event.type != KeyEventType.KeyDown) return null
    if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return null
    return when (event.key) {
        Key.Spacebar -> VeloraKeyboardShortcutAction.TOGGLE_PLAY_PAUSE
        Key.DirectionRight -> VeloraKeyboardShortcutAction.SEEK_FORWARD_5S
        Key.DirectionLeft -> VeloraKeyboardShortcutAction.SEEK_BACKWARD_5S
        Key.DirectionUp -> VeloraKeyboardShortcutAction.VOLUME_UP
        Key.DirectionDown -> VeloraKeyboardShortcutAction.VOLUME_DOWN
        Key.M -> VeloraKeyboardShortcutAction.TOGGLE_MUTE
        Key.F -> VeloraKeyboardShortcutAction.TOGGLE_VIDEO_FULLSCREEN
        else -> null
    }
}

fun Modifier.veloraKeyboardShortcuts(
    onAction: (VeloraKeyboardShortcutAction) -> Unit
): Modifier = onKeyEvent { event ->
    val action = resolveVeloraKeyboardShortcut(event)
    if (action != null) {
        onAction(action)
        true
    } else {
        false
    }
}
