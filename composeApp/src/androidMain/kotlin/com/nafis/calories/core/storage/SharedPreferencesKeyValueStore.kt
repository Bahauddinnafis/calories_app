package com.nafis.calories.core.storage

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun getStringSet(key: String): Set<String>? = prefs.getStringSet(key, null)?.toSet()

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        PreferencesEditor(editor).block()
        editor.apply()
    }

    private class PreferencesEditor(private val editor: SharedPreferences.Editor) : KeyValueStore.Editor {
        override fun putString(key: String, value: String?) {
            editor.putString(key, value)
        }

        override fun putInt(key: String, value: Int) {
            editor.putInt(key, value)
        }

        override fun putLong(key: String, value: Long) {
            editor.putLong(key, value)
        }

        override fun putFloat(key: String, value: Float) {
            editor.putFloat(key, value)
        }

        override fun putBoolean(key: String, value: Boolean) {
            editor.putBoolean(key, value)
        }

        override fun putStringSet(key: String, value: Set<String>) {
            editor.putStringSet(key, value)
        }

        override fun remove(key: String) {
            editor.remove(key)
        }
    }
}

class SharedPreferencesStoreFactory(context: Context) : KeyValueStoreFactory {

    private val appContext = context.applicationContext

    override fun create(name: String): KeyValueStore =
        SharedPreferencesKeyValueStore(appContext.getSharedPreferences(name, Context.MODE_PRIVATE))
}
