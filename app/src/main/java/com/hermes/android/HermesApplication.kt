package com.hermes.android

import android.app.Application
import com.hermes.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HermesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HermesApplication)
            modules(appModule)
        }
    }
}