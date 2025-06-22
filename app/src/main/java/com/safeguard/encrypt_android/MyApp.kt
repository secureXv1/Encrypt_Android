package com.safeguard.endcrypt_android

import android.app.Application
import android.content.Context

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        lateinit var context: Context
            private set
    }
}
