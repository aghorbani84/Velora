package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Track
import com.example.domain.model.VeloraUiState
import com.example.ui.screens.QueueSectionCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w320dp-h470dp")
class QueueSectionCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tracks = List(12) { index ->
        Track(
            id = "queue-$index",
            mediaStoreId = 500L + index,
            title = "Queue Track $index",
            artist = "Queue Artist",
            album = "Queue Album",
            durationMs = 180_000L,
            uri = "/storage/emulated/0/Music/queue-$index.mp3",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO
        )
    }

    @Test
    fun `queue keeps its first row visible and scrolls independently`() {
        val state = VeloraUiState(
            queue = PlaybackQueue(tracks = tracks, currentIndex = 0)
        )

        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 200.dp)) {
                    QueueSectionCard(
                        uiState = state,
                        isPersian = false,
                        onSelectQueueTrack = {},
                        onRemoveFromQueue = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("queue_list_scroll_container").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_item_0").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_list_scroll_container").performScrollToIndex(5)
        composeRule.onNodeWithTag("queue_item_5").assertIsDisplayed()
    }
}
