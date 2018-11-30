package net.slashOmega.juktaway.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.JuktawayApplication
import kotlin.reflect.KProperty

/**
 * Created on 2018/11/02.
 */

@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
open class SharedPreference<T>(prefName: String, private val key: String, private val default: T) {
    private val pref by lazy {
        JuktawayApplication.app.getSharedPreferences(prefName, Context.MODE_PRIVATE)!!
    }

    private var raw: T

    init {
        raw = pref.run {
            when (default) {
                is Int -> getInt(key, default)
                is Long -> getLong(key, default)
                is Float -> getFloat(key, default)
                is String -> getString(key, default)
                is Boolean -> getBoolean(key, default)
                else -> throw IllegalTypeException()
            } as T
        }
        if (default !is Int && default !is Long && default !is Float && default !is String && default !is Boolean)
            throw IllegalTypeException()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = raw

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        raw = value
        GlobalScope.launch(Dispatchers.Default) {
            pref.edit().run {
                when (default) {
                    is Int -> putInt(key, value as Int)
                    is Long -> putLong(key, value as Long)
                    is Float -> putFloat(key, value as Float)
                    is String -> putString(key, value as String)
                    is Boolean -> putBoolean(key, value as Boolean)
                    else -> throw IllegalTypeException()
                }
            }.apply()
        }
    }

    class IllegalTypeException: IllegalStateException()
}