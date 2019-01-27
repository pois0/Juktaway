package net.slash_omega.juktaway

import android.content.Context
import android.graphics.Typeface
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import net.slash_omega.juktaway.model.UserIconManager
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.Core
import net.slash_omega.juktaway.twitter.currentClient

internal lateinit var app: JuktawayApplication

internal lateinit var font: Typeface

class JuktawayApplication: MultiDexApplication() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        if (!BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())


        // load setting files
        Core.initialize()
        BasicSettings.init()
        UserIconManager.warmUpUserIconMap()


        font = Typeface.createFromAsset(assets, "fontello.ttf")

        if (BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(MyUncaughtExceptionHandler(app))
    }

    override fun onTerminate() {
        currentClient.close()
        super.onTerminate()
    }
}