package com.suseoaa.projectoaa.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OaaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}