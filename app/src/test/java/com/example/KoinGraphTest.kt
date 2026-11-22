package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.di.veloraAppModule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-1 — Koin DI graph verification (CLEAN-CODE-PLAN.md).
 *
 * `checkModules()` instantiates every definition in [veloraAppModule] and fails
 * if any constructor dependency is missing, circular, or throws at creation
 * time. This is the guard that must stay green before any refactor moves a
 * constructor parameter (P1/P2 rewire the ViewModel, the orchestrator and the
 * engine, so a broken graph must fail here first, not at runtime).
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 *
 * Notes on what is constructed here (all verified against the module source,
 * di/VeloraModule.kt:17-74):
 * - Room database is built from the real file name ("velora.db") inside the
 *   Robolectric application sandbox; no production database is touched.
 * - [com.example.data.firebase.VeloraFirebaseRepository] is built through its
 *   `initSafe*` constructors, which return null when Firebase is not
 *   initialised (no google-services.json in CI) instead of throwing.
 * - [com.example.ui.viewmodel.VeloraViewModel] is constructed; its init block
 *   only launches coroutines on Dispatchers.Main.immediate, which stay pending
 *   under Robolectric's paused main looper and never execute here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinGraphTest {

    @Test
    fun `koin graph verifies - every definition resolves`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        koinApplication {
            androidContext(context)
            modules(veloraAppModule)
        }.checkModules()
    }
}
