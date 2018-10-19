package net.slashOmega.juktaway

import android.app.Application
import android.graphics.Typeface
import android.os.StrictMode
import net.slashOmega.juktaway.model.Relationship
import net.slashOmega.juktaway.model.UserIconManager
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.settings.MuteSettings
import net.slashOmega.juktaway.util.ImageUtil

class JuktawayApplication: Application() {
    companion object {
        lateinit var app: JuktawayApplication
        //TODO private set
        lateinit var font: Typeface
        //TODO private set
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        if (!BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        // 画像のキャッシュ,角丸の設定
        ImageUtil.init()

        // load setting files
        MuteSettings.init()
        BasicSettings.init()
        UserIconManager.warmUpUserIconMap()
        Relationship.init()

        font = Typeface.createFromAsset(assets, "fontello.ttf")

        if (BuildConfig.DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(MyUncaughtExceptionHandler(app))
    }
}