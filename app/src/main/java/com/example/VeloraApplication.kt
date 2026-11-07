package com.example

import android.app.Application
import com.example.di.veloraAppModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class VeloraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
}


