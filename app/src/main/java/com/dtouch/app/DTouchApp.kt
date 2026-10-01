package com.dtouch.app

import android.app.Application

class DTouchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
