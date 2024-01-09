package com.sst.sstdevicestream

import android.app.Application
import com.sst.sstdevicestream.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DeviceStreamApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@DeviceStreamApplication)
            koin.loadModules(
                listOf(appModule)
            )
        }
    }
}