package com.example

import android.app.Application
import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.example.domain.model.PhiDesignMetrics
import com.example.ui.components.VeloraKeyboardShortcutAction
import com.example.ui.components.resolveVeloraKeyboardShortcut
import com.example.ui.theme.HEX_CARBON
import com.example.ui.theme.HEX_MUTED_TEXT
import com.example.ui.theme.HEX_OBSIDIAN
import com.example.ui.theme.VeloraAlphaTokens
import com.example.ui.theme.mixedScriptText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class HybridUiTokensAndShortcutsTest {

    @Test
    fun `upgraded muted text color meets WCAG AA contrast and R-C4 uses exactly 3 alphas`() {
        assertTrue(
            "Muted text #8F8B83 must pass 4.5:1 on Obsidian #0B0B0D",
            PhiDesignMetrics.meetsWcagAa(HEX_MUTED_TEXT, HEX_OBSIDIAN, 4.5)
        )
        assertTrue(
            "Muted text #8F8B83 must pass 4.5:1 on Carbon #121216",
            PhiDesignMetrics.meetsWcagAa(HEX_MUTED_TEXT, HEX_CARBON, 4.5)
        )
        assertEquals(0.14f, VeloraAlphaTokens.ERROR_SURFACE_ALPHA, 0.001f)
        assertEquals(0.45f, VeloraAlphaTokens.ACCENT_BORDER_ALPHA, 0.001f)
        assertEquals(0.60f, VeloraAlphaTokens.SCRIM_BACKGROUND_ALPHA, 0.001f)
    }

    @Test
    fun `mixedScriptText isolates embedded Latin runs inside Persian RTL strings`() {
        val pureEnglish = "Obsidian Nocturne FLAC"
        assertEquals(pureEnglish, mixedScriptText(pureEnglish))

        val bilingual = mixedScriptText("آلبوم Velora 24-bit استودیو")
        assertTrue(bilingual.contains('\u2066'))
        assertTrue(bilingual.contains('\u2069'))
        assertTrue(bilingual.contains("Velora 24-bit"))
    }

    @Test
    fun `hardware keyboard shortcut mapper handles Space, arrows, M, and F cleanly`() {
        fun keyDown(nativeCode: Int, metaState: Int = 0): KeyEvent {
            return KeyEvent(
                NativeKeyEvent(
                    0L,
                    0L,
                    NativeKeyEvent.ACTION_DOWN,
                    nativeCode,
                    0,
                    metaState
                )
            )
        }

        assertEquals(
            VeloraKeyboardShortcutAction.TOGGLE_PLAY_PAUSE,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_SPACE))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.SEEK_FORWARD_5S,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_DPAD_RIGHT))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.SEEK_BACKWARD_5S,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_DPAD_LEFT))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.VOLUME_UP,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_DPAD_UP))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.VOLUME_DOWN,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_DPAD_DOWN))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.TOGGLE_MUTE,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_M))
        )
        assertEquals(
            VeloraKeyboardShortcutAction.TOGGLE_VIDEO_FULLSCREEN,
            resolveVeloraKeyboardShortcut(keyDown(NativeKeyEvent.KEYCODE_F))
        )
        // Modified chords (Ctrl+Space) must not hijack global shortcuts
        assertNull(
            resolveVeloraKeyboardShortcut(
                keyDown(NativeKeyEvent.KEYCODE_SPACE, NativeKeyEvent.META_CTRL_ON)
            )
        )
        assertEquals(KeyEventType.KeyDown, keyDown(NativeKeyEvent.KEYCODE_SPACE).type)
    }
}
