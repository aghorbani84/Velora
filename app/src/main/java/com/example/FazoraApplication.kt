package com.example

import android.app.Application
import com.example.di.fazoraAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class FazoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@FazoraApplication)
                modules(fazoraAppModule)
            }
        }
    }
}
