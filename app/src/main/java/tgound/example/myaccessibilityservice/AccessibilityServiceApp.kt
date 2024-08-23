package tgound.example.myaccessibilityservice

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri

class AccessibilityServiceApp : Application() {
    companion object {
        private lateinit var instance: AccessibilityServiceApp

        fun getAppContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()

    }
}