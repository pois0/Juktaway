package net.slashOmega.juktaway.util

import android.content.Context
import net.slashOmega.juktaway.JuktawayApplication
import net.slashOmega.juktaway.model.AccessTokenManager
import kotlin.reflect.KProperty

/**
 * Created on 2018/11/02.
 */
class SharedPreference<T>(prefName: String, private val key: String, private val default: T) {
    private val pref by lazy {
        JuktawayApplication.app.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
            = pref.run {
        when (default) {
            is Int -> getInt(key, default)
            is Long -> getLong(key, default)
            is Float -> getFloat(key, default)
            is String -> getString(key, default)
            is Boolean -> getBoolean(key, default)
            else -> throw IllegalArgumentException()
        } as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        pref.edit().run {
            when (default) {
                is Int -> putInt(key, value as Int)
                is Long -> putLong(key, value as Long)
                is Float -> putFloat(key, value as Float)
                is String -> putString(key, value as String)
                is Boolean -> putBoolean(key, value as Boolean)
                else -> throw IllegalArgumentException()
            }
        }.apply()
    }
}