package net.slashOmega.juktaway.util

import android.content.Context
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.model.AccessTokenManager
import kotlin.reflect.KProperty

/**
 * Created on 2018/11/02.
 */
class StringSharedPreference(key: String) {
    private val sharedPreferences by lazy {
        JuktawayApplication.app.getSharedPreferences(key, Context.MODE_PRIVATE)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        
    }
}