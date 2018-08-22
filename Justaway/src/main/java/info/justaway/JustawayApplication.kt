package info.justaway

import android.app.Application
import android.graphics.Typeface
import android.os.StrictMode
import info.justaway.model.Relationship
import info.justaway.model.UserIconManager
import info.justaway.settings.BasicSettings
import info.justaway.settings.MuteSettings
import info.justaway.util.ImageUtil

class JustawayApplication: Application() {
    companion object {
        lateinit var app: JustawayApplication
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