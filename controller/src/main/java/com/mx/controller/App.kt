package com.mx.controller

import android.app.Application
import com.mx.common.CrashLogManager

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogManager.getInstance().init(this)
    }
}