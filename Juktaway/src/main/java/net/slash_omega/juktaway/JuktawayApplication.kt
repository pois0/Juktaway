package net.slash_omega.juktaway

import android.app.Application
import android.graphics.Typeface
import android.os.StrictMode
import net.slash_omega.juktaway.model.UserIconManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.Core
import net.slash_omega.juktaway.twitter.currentClient

internal lateinit var app: JuktawayApplication

internal lateinit var font: Typeface

class JuktawayApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        if (!BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())


        // load setting files
        Core.initialize()
        BasicSettings.init()
        UserIconManager.warmUpUserIconMap()
    }

    override fun onTerminate() {
        currentClient.close()
        super.onTerminate()
    }
}
