package com.example

import android.app.Application
import android.os.StrictMode
import com.example.di.veloraAppModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class VeloraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            configureDebugStrictMode()
        }
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        }
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@VeloraApplication)
                modules(veloraAppModule)
            }
        }
    }

    private fun configureDebugStrictMode() {
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }
}
