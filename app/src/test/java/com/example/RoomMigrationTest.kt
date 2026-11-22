package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VeloraDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-3 — Room migration EXECUTION tests v1→v2→v3→v4 (CLEAN-CODE-PLAN.md).
 *
 * Before this file, only "migrations are registered" was asserted
 * (ExampleRobolectricTest: "migrations are registered" — ALL_MIGRATIONS.size == 3).
 * These tests execute the REAL `Migration.migrate()` SQL from
 * [VeloraDatabase] against a database created with the genuine v1 schema, then
 * assert the tables, indices and rows survive each step.
 *
 * Method: a file database is created at version 1 with hand-written v1 DDL
 * (matching the v1 entities column-for-column), sample rows are inserted, the
 * file is reopened at version 4 with an `onUpgrade` that runs the real
 * migration objects in order, and the result is checked with `sqlite_master`
 * / `PRAGMA` queries plus row counts. No `MigrationTestHelper` is used: it
 * needs generated schema JSONs, and enabling them (`exportSchema = true` +
 * the `room.schemaLocation` KSP argument) CRASHED KSP in CI run #65, so that
 * wiring was reverted — the raw-SQL approach here is the method, not a
 * stopgap. Room's own schema validation is therefore NOT exercised here —
 * only the migration SQL and data survival.
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    // ---------- v1 schema (hand-written, matches the v1 entities) ----------

    private fun createV1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tracks` (" +
                "`id` TEXT NOT NULL, `mediaStoreId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, `uri` TEXT NOT NULL, " +
                "`folderPath` TEXT NOT NULL, `mediaType` TEXT NOT NULL, " +
                "`artworkUri` TEXT, `addedTimestampMs` INTEGER NOT NULL, " +
                "`sampleRateHz` INTEGER NOT NULL, `bitrateKbps` INTEGER NOT NULL, " +
                "`isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracks_uri` ON `tracks` (`uri`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_title` ON `tracks` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_artist` ON `tracks` (`artist`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_album` ON `tracks` (`album`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_mediaStoreId` ON `tracks` (`mediaStoreId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAtMs` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_tracks` (" +
                "`playlistId` INTEGER NOT NULL, `trackId` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `trackId`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_tracks_trackId` ON `playlist_tracks` (`trackId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)")
    }

    private fun insertV1SampleRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO tracks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "mig_audio_1", 901L, "Migration Song", "Old Artist", "Old Album",
                200_000L, "content://media/audio/901", "/Music", "AUDIO",
                null, 1_700_000_000_000L, 44_100, 320, 1
            )
        )
        db.execSQL(
            "INSERT INTO tracks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "mig_video_1", 902L, "Migration Clip", "Old Director", "Old Visuals",
                600_000L, "content://media/video/902", "/Movies", "VIDEO",
                null, 1_700_000_100_000L, 48_000, 4800, 0
            )
        )
        db.execSQL("INSERT INTO playlists (name, createdAtMs) VALUES (?, ?)", arrayOf<Any?>("Old Favourites", 1_700_000_000_000L))
        db.execSQL("INSERT INTO playlist_tracks VALUES (?, ?, ?)", arrayOf<Any?>(1L, "mig_audio_1", 0))
        db.execSQL("INSERT INTO playlist_tracks VALUES (?, ?, ?)", arrayOf<Any?>(1L, "mig_video_1", 1))
    }

    // ---------- helpers ----------

    private fun openHelper(
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit = {},
        onUpgrade: (SupportSQLiteDatabase, Int, Int) -> Unit = { _, _, _ -> }
    ): SupportSQLiteOpenHelper {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                    onUpgrade(db, oldVersion, newVersion)
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf<Any?>(name)
        ).use { it.count == 1 }

    private fun indexExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf<Any?>(name)
        ).use { it.count == 1 }

    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM `$table`").use {
            it.moveToFirst()
            it.getInt(0)
        }

    private fun trackTitle(db: SupportSQLiteDatabase, id: String): String? =
        db.query("SELECT title FROM tracks WHERE id=?", arrayOf<Any?>(id)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    /** Creates a v1 database with sample rows and returns it closed. */
    private fun buildV1Database() {
        openHelper(version = 1, onCreate = ::createV1Schema).use { helper ->
            val db = helper.writableDatabase
            insertV1SampleRows(db)
            assertEquals(2, rowCount(db, "tracks"))
            assertEquals(1, rowCount(db, "playlists"))
            assertEquals(2, rowCount(db, "playlist_tracks"))
        }
    }

    /** Reopens the file at version 4, running the real migrations in order. */
    private fun migrateToV4(): SupportSQLiteOpenHelper {
        val helper = openHelper(version = 4) { db, oldVersion, _ ->
            var v = oldVersion
            for (migration in VeloraDatabase.ALL_MIGRATIONS) {
                if (migration.startVersion == v) {
                    migration.migrate(db)
                    v = migration.endVersion
                }
            }
            check(v == 4) { "migration chain stopped at v$v (started at v$oldVersion)" }
        }
        // Opening forces onUpgrade to run.
        helper.writableDatabase
        return helper
    }

    // ---------- tests ----------

    @Test
    fun `migration chain is contiguous from v1 to v4`() {
        val migrations = VeloraDatabase.ALL_MIGRATIONS
        assertEquals(3, migrations.size)
        assertEquals(listOf(1, 2, 3), migrations.map { it.startVersion })
        assertEquals(listOf(2, 3, 4), migrations.map { it.endVersion })
    }

    @Test
    fun `migrate 1 to 2 creates playback_history and keeps v1 rows`() {
        buildV1Database()
        openHelper(version = 2) { db, _, _ -> VeloraDatabase.MIGRATION_1_2.migrate(db) }.use { helper ->
            val db = helper.writableDatabase
            assertTrue(tableExists(db, "playback_history"))
            assertTrue(indexExists(db, "index_playback_history_lastPlayedAtMs"))
            assertTrue(indexExists(db, "index_playback_history_playCount"))
            assertEquals(2, rowCount(db, "tracks"))
            assertEquals("Migration Song", trackTitle(db, "mig_audio_1"))
            assertEquals(2, rowCount(db, "playlist_tracks"))
        }
    }

    @Test
    fun `migrate 2 to 3 creates custom_eq_presets`() {
        // Isolated 2->3: build v1, run 1->2, then run 2->3 on the same file.
        buildV1Database()
        openHelper(version = 3) { db, oldVersion, _ ->
            if (oldVersion == 1) VeloraDatabase.MIGRATION_1_2.migrate(db)
            VeloraDatabase.MIGRATION_2_3.migrate(db)
        }.use { helper ->
            val db = helper.writableDatabase
            assertTrue(tableExists(db, "custom_eq_presets"))
            db.execSQL(
                "INSERT INTO custom_eq_presets VALUES (?, ?, ?, ?)",
                arrayOf<Any?>("preset_warm", "Warm Test", "0.0,1.0", 1_700_000_000_000L)
            )
            assertEquals(1, rowCount(db, "custom_eq_presets"))
            assertEquals(2, rowCount(db, "tracks"))
        }
    }

    @Test
    fun `migrate 3 to 4 adds the track indices`() {
        buildV1Database()
        migrateToV4().use { helper ->
            val db = helper.writableDatabase
            assertTrue(indexExists(db, "index_tracks_mediaStoreId"))
            assertTrue(indexExists(db, "index_tracks_mediaType"))
            assertTrue(indexExists(db, "index_tracks_isFavorite"))
            assertTrue(indexExists(db, "index_tracks_folderPath"))
            // The pre-existing v1 indices survive the drop-and-recreate.
            assertTrue(indexExists(db, "index_tracks_uri"))
            assertTrue(indexExists(db, "index_tracks_title"))
        }
    }

    @Test
    fun `full chain 1 to 4 keeps all data including rows added mid-migration`() {
        buildV1Database()
        // Row added at v2 (after MIGRATION_1_2) and at v3 (after MIGRATION_2_3)
        // must both survive the final step.
        openHelper(version = 2) { db, _, _ -> VeloraDatabase.MIGRATION_1_2.migrate(db) }.use { helper ->
            val db = helper.writableDatabase
            // playback_history (v2 schema): trackId, lastPlayedAtMs,
            // lastPositionMs, playCount, completedCount.
            db.execSQL(
                "INSERT INTO playback_history VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>( "mig_audio_1", 1_700_000_200_000L, 42_000L, 3, 1)
            )
        }
        openHelper(version = 3) { db, _, _ -> VeloraDatabase.MIGRATION_2_3.migrate(db) }.use { helper ->
            val db = helper.writableDatabase
            // custom_eq_presets (v3 schema): id, name, gainsCsv, createdAtMs.
            db.execSQL(
                "INSERT INTO custom_eq_presets VALUES (?, ?, ?, ?)",
                arrayOf<Any?>("preset_survivor", "Survivor", "1.0,0.0", 1_700_000_000_000L)
            )
        }
        migrateToV4().use { helper ->
            val db = helper.writableDatabase
            assertEquals(2, rowCount(db, "tracks"))
            assertEquals("Migration Song", trackTitle(db, "mig_audio_1"))
            assertEquals("Migration Clip", trackTitle(db, "mig_video_1"))
            assertEquals(1, rowCount(db, "playlists"))
            assertEquals(2, rowCount(db, "playlist_tracks"))
            assertEquals(1, rowCount(db, "playback_history"))
            assertEquals(1, rowCount(db, "custom_eq_presets"))
            db.query("SELECT playCount FROM playback_history WHERE trackId='mig_audio_1'").use {
                assertTrue(it.moveToFirst())
                assertEquals(3, it.getInt(0))
            }
        }
    }

    @Test
    fun `migrations are idempotent on the already-migrated schema`() {
        buildV1Database()
        migrateToV4().use { }
        // Re-running the migration SQL on the v4 schema must not throw
        // (every statement uses IF NOT EXISTS / IF EXISTS).
        openHelper(version = 4) { _, _, _ -> }.use { helper ->
            val db = helper.writableDatabase
            VeloraDatabase.MIGRATION_1_2.migrate(db)
            VeloraDatabase.MIGRATION_2_3.migrate(db)
            VeloraDatabase.MIGRATION_3_4.migrate(db)
            assertEquals(2, rowCount(db, "tracks"))
        }
    }
}
