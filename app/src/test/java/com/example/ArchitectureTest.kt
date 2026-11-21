package com.example

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-2 — Architecture tests (CLEAN-CODE-PLAN.md, ARCHITECTURE.md §4).
 *
 * Scans the main source tree and enforces the layer rules:
 * - `ui.*` may not import `data.*`, `androidx.media3.*` or ExoPlayer.
 * - `domain.*` may not import `android.*`, `androidx.*` or `data.*`
 *   (pure Kotlin + kotlinx.coroutines only).
 * - `data.*` may not import `ui.*`.
 * - No file-level mutable global caches (`@Volatile` + `var` at file scope).
 * - No `as? Media3PlayerEngine` downcasts (the engine must be reached through
 *   the `PlayerEngine` port; P2-1/P2-2 extend the port instead).
 *
 * The current code VIOLATES these rules in known places (the plan's P1/P2
 * items exist precisely to remove them). Those violations are recorded in the
 * baseline allowlists below, keyed by exact file + import (layer) or by exact
 * per-file count (caches, downcasts). The test fails when:
 * - a violation appears that is NOT in the baseline (a new violation), or
 * - a baseline entry is no longer observed (a violation was fixed — remove
 *   the entry instead of leaving a stale allowlist behind).
 *
 * Deliberately NOT enforced here: `runCatching` without `onFailure`
 * (ARCHITECTURE.md §4). That needs data-flow analysis, not an import scan;
 * the detekt `SwallowedException` / `TooGenericExceptionCaught` rules (active
 * since 9c06551) are the enforcement path for it. A detekt custom rule is a
 * later-phase item, not a P0 scanner hack.
 *
 * Files under `com.example.di`, `com.example.orchestrator` and the
 * `com.example` root package are the composition root (DI module, ports,
 * Application) and are exempt from the layer rules — they are allowed to
 * wire concrete implementations together.
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArchitectureTest {

    data class Violation(val file: String, val detail: String, val rule: String)

    companion object {
        val IMPORT_REGEX = Regex("""^\s*import\s+([\w.]+)""")
        val DOWNCAST_REGEX = Regex("""as\??\s*Media3PlayerEngine\b""")

        const val RULE_UI_LAYER = "ui must not import data/Media3/ExoPlayer"
        const val RULE_DOMAIN_PURE = "domain must stay pure Kotlin (no android/androidx/data)"
        const val RULE_DATA_LAYER = "data must not depend on ui"
        const val RULE_NO_GLOBAL_CACHE = "no file-level mutable global caches"
        const val RULE_NO_DOWNCAST = "no as? ConcreteEngine downcasts"

        /**
         * Baseline layer violations, exact (file, import) pairs, each with the
         * clean-code item that removes it. Verified against main 19e70bc.
         */
        val ALLOWED_LAYER: Set<Pair<String, String>> = setOf(
            // P2-2: the video surface must be rebuilt on a PlayerEngine port.
            "com/example/ui/components/VeloraVideoSurface.kt" to "androidx.media3.common.util.UnstableApi",
            "com/example/ui/components/VeloraVideoSurface.kt" to "androidx.media3.ui.AspectRatioFrameLayout",
            "com/example/ui/components/VeloraVideoSurface.kt" to "androidx.media3.ui.PlayerView",
            "com/example/ui/components/VeloraVideoSurface.kt" to "com.example.data.playback.Media3PlayerEngine",
            // P2-1: DSP/AB-loop calls must move onto PlayerEngine ports.
            "com/example/ui/viewmodel/VeloraRadioAndDspExtensions.kt" to "com.example.data.playback.Media3PlayerEngine",
            // P2-1: ViewModel must not reach the concrete engine or Firebase repo.
            "com/example/ui/viewmodel/VeloraUiStateAssembler.kt" to "com.example.data.firebase.VeloraFirebaseRepository",
            "com/example/ui/viewmodel/VeloraUiStateAssembler.kt" to "com.example.data.playback.Media3PlayerEngine",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "androidx.media3.exoplayer.ExoPlayer",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "com.example.data.audio.VeloraAudioDeviceManager",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "com.example.data.firebase.VeloraFirebaseRepository",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "com.example.data.playback.Media3PlayerEngine",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "com.example.data.repository.VeloraLibraryRepository",
            "com/example/ui/viewmodel/VeloraViewModel.kt" to "com.example.data.settings.VeloraSettingsRepository",
            // P2-3: the orchestrator must depend on domain ports, not data classes.
            "com/example/domain/engine/MediaOrchestrator.kt" to "android.util.Log",
            "com/example/domain/engine/MediaOrchestrator.kt" to "com.example.data.repository.VeloraLibraryRepository",
            "com/example/domain/engine/MediaOrchestrator.kt" to "com.example.data.settings.VeloraSettingsRepository"
        )

        /** Baseline file-level @Volatile caches, exact per-file counts (P1-3). */
        val ALLOWED_VOLATILE_CACHES: Map<String, Int> = mapOf(
            "com/example/domain/engine/MediaOrchestrator.kt" to 3,
            "com/example/ui/viewmodel/VeloraUiStateAssembler.kt" to 1
        )

        /** Baseline Media3PlayerEngine downcasts, exact per-file counts (P2-1/P2-2). */
        val ALLOWED_DOWNCASTS: Map<String, Int> = mapOf(
            "com/example/data/playback/VeloraPlaybackService.kt" to 1,
            "com/example/ui/components/VeloraVideoSurface.kt" to 1,
            "com/example/ui/viewmodel/VeloraRadioAndDspExtensions.kt" to 6,
            "com/example/ui/viewmodel/VeloraUiStateAssembler.kt" to 1,
            "com/example/ui/viewmodel/VeloraViewModel.kt" to 2
        )

        /** Pure rule engine: layer violations for one file. */
        fun layerViolations(relativePath: String, imports: List<String>): List<Violation> {
            val violations = mutableListOf<Violation>()
            val isUi = relativePath.startsWith("com/example/ui/")
            val isDomain = relativePath.startsWith("com/example/domain/")
            val isData = relativePath.startsWith("com/example/data/")
            for (imp in imports) {
                when {
                    isUi && (imp.startsWith("com.example.data.") ||
                        imp.startsWith("androidx.media3.") ||
                        imp.startsWith("com.google.android.exoplayer2.")) ->
                        violations += Violation(relativePath, imp, RULE_UI_LAYER)
                    isDomain && (imp.startsWith("android.") ||
                        imp.startsWith("androidx.") ||
                        imp.startsWith("com.example.data.")) ->
                        violations += Violation(relativePath, imp, RULE_DOMAIN_PURE)
                    isData && imp.startsWith("com.example.ui.") ->
                        violations += Violation(relativePath, imp, RULE_DATA_LAYER)
                }
            }
            return violations
        }

        /** Pure rule engine: file-scope global caches + concrete-engine downcasts. */
        fun globalViolations(relativePath: String, lines: List<String>): List<Violation> {
            val violations = mutableListOf<Violation>()
            for (i in lines.indices) {
                val line = lines[i]
                if (line.trim() == "@Volatile" && line.firstOrNull() == '@') {
                    val next = lines.subList(i + 1, minOf(i + 3, lines.size))
                        .firstOrNull { it.isNotBlank() }.orEmpty()
                    if (next.startsWith("private var ") ||
                        next.startsWith("internal var ") ||
                        next.startsWith("var ")) {
                        violations += Violation(
                            relativePath,
                            "file-level @Volatile ${next.trim().take(60)} (line ${i + 1})",
                            RULE_NO_GLOBAL_CACHE
                        )
                    }
                }
                if (DOWNCAST_REGEX.containsMatchIn(line)) {
                    violations += Violation(
                        relativePath,
                        "downcast at line ${i + 1}: ${line.trim().take(80)}",
                        RULE_NO_DOWNCAST
                    )
                }
            }
            return violations
        }
    }

    private fun sourceRoot(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { it.isDirectory }
            ?: throw AssertionError(
                "main source root not found from working dir ${File(".").absolutePath}; tried $candidates"
            )
    }

    @Test
    fun `no new architecture violations beyond the P0 baseline`() {
        val root = sourceRoot()
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("expected main Kotlin sources under $root", files.isNotEmpty())

        val layerFound = mutableListOf<Violation>()
        val volatileByFile = mutableMapOf<String, Int>()
        val downcastByFile = mutableMapOf<String, Int>()
        for (file in files) {
            val rel = file.relativeTo(root).path.replace('\\', '/')
            val lines = file.readLines()
            val imports = lines.mapNotNull { IMPORT_REGEX.find(it)?.groupValues?.get(1) }
            layerFound += layerViolations(rel, imports)
            for (v in globalViolations(rel, lines)) {
                when (v.rule) {
                    RULE_NO_GLOBAL_CACHE -> volatileByFile[rel] = (volatileByFile[rel] ?: 0) + 1
                    RULE_NO_DOWNCAST -> downcastByFile[rel] = (downcastByFile[rel] ?: 0) + 1
                }
            }
        }

        val unexpectedLayer = layerFound.filter { (it.file to it.detail) !in ALLOWED_LAYER }
        assertTrue(
            "NEW layer violations — add to the baseline only with a P-item and a plan to remove them:\n" +
                unexpectedLayer.joinToString("\n") { "${it.file}: ${it.detail} [${it.rule}]" },
            unexpectedLayer.isEmpty()
        )
        val staleLayer = ALLOWED_LAYER - layerFound.map { it.file to it.detail }.toSet()
        assertTrue(
            "STALE baseline entries — the violation is fixed, remove it from ALLOWED_LAYER: $staleLayer",
            staleLayer.isEmpty()
        )
        assertEquals(
            "file-level @Volatile cache counts changed — update ALLOWED_VOLATILE_CACHES only with a P-item",
            ALLOWED_VOLATILE_CACHES,
            volatileByFile
        )
        assertEquals(
            "Media3PlayerEngine downcast counts changed — update ALLOWED_DOWNCASTS only with a P-item",
            ALLOWED_DOWNCASTS,
            downcastByFile
        )
    }

    // --- Self-tests: prove the scanner fails on a deliberate violation. ---

    @Test
    fun `scanner flags a deliberate ui-to-data violation`() {
        val violations = layerViolations(
            "com/example/ui/screens/FakeScreen.kt",
            listOf(
                "com.example.data.repository.VeloraLibraryRepository",
                "androidx.compose.runtime.Composable"
            )
        )
        assertEquals(1, violations.size)
        assertTrue(violations[0].detail.contains("VeloraLibraryRepository"))
    }

    @Test
    fun `scanner flags a deliberate domain-to-android violation`() {
        val violations = layerViolations(
            "com/example/domain/engine/FakeEngine.kt",
            listOf("android.util.Log", "kotlinx.coroutines.flow.Flow")
        )
        assertEquals(1, violations.size)
        assertEquals(RULE_DOMAIN_PURE, violations[0].rule)
    }

    @Test
    fun `scanner passes clean files`() {
        assertTrue(
            layerViolations(
                "com/example/domain/model/FakeModel.kt",
                listOf("kotlinx.coroutines.flow.StateFlow")
            ).isEmpty()
        )
        assertTrue(
            globalViolations(
                "com/example/domain/model/FakeModel.kt",
                listOf("data class Fake(val x: Int)")
            ).isEmpty()
        )
    }

    @Test
    fun `scanner flags a deliberate downcast and file-level volatile cache`() {
        val violations = globalViolations(
            "com/example/ui/viewmodel/FakeViewModel.kt",
            listOf(
                "@Volatile",
                "private var cachedThing: String? = null",
                "",
                "val engine = (playerEngine as? Media3PlayerEngine)?.exoPlayer"
            )
        )
        assertEquals(2, violations.size)
        assertTrue(violations.any { it.rule == RULE_NO_GLOBAL_CACHE })
        assertTrue(violations.any { it.rule == RULE_NO_DOWNCAST })
    }
}
