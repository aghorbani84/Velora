package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VeloraFirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveCloudPlaylists_authenticatedUser_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = VeloraFirebaseRepository(db = firestore, auth = auth)

        val playlistId = "playlist_obsidian_flow"
        val trackIds = listOf("audio_track_1", "audio_track_2")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCloudPlaylist(
                userId = aliceUid,
                playlistId = playlistId,
                name = "Obsidian Pulse Mix",
                trackIds = trackIds
            )
        }

        val playlists = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeCloudPlaylists(aliceUid).first { it.isNotEmpty() }
        }

        assertEquals(1, playlists.size)
        val firstPlaylist = playlists.first()
        assertEquals(playlistId, firstPlaylist.id)
        assertEquals("Obsidian Pulse Mix", firstPlaylist.name)
        assertEquals(trackIds, firstPlaylist.trackIds)
        assertEquals(aliceUid, firstPlaylist.userId)
    }

    @Test
    fun crossUserAccess_aliceCannotAccessBobsPlaylists_fails() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepository = VeloraFirebaseRepository(db = firestore, auth = auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepository.saveCloudPlaylist(
                userId = bobUid,
                playlistId = "bobs_heavy_rotation",
                name = "Bob Heavy Rotation",
                trackIds = listOf("track_heavy_1")
            )
        }

        // Switch user to Alice
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepository = VeloraFirebaseRepository(db = firestore, auth = auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceRepository.saveCloudPlaylist(
                    userId = bobUid,
                    playlistId = "bobs_heavy_rotation",
                    name = "Malicious Overwrite",
                    trackIds = listOf("hacked_track")
                )
            }
            fail("Expected exception when Alice attempts to write to Bob's playlists")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("requires authenticated user") == true || e is com.google.firebase.firestore.FirebaseFirestoreException)
        }

        assertNotNull(aliceUid)
    }

    @Test
    fun saveAndObserveCloudEqPresets_authenticatedUser_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = VeloraFirebaseRepository(db = firestore, auth = auth)

        val presetId = "tape_warmth_preset"
        val gains = listOf(3.5, 2.5, 1.0, 0.0, -0.5, 0.0, 1.0, 2.0, 3.0, 4.0)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCloudEqPreset(
                userId = aliceUid,
                presetId = presetId,
                name = "Tape Warmth Master",
                gainsDb = gains
            )
        }

        val presets = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeCloudEqPresets(aliceUid).first { it.isNotEmpty() }
        }

        assertEquals(1, presets.size)
        val firstPreset = presets.first()
        assertEquals(presetId, firstPreset.id)
        assertEquals("Tape Warmth Master", firstPreset.name)
        assertEquals(gains, firstPreset.gainsDb)
        assertEquals(aliceUid, firstPreset.userId)
    }
}
