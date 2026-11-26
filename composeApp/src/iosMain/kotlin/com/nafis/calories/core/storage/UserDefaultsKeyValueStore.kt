package com.nafis.calories.core.storage

import platform.Foundation.NSUserDefaults

class UserDefaultsKeyValueStore(private val defaults: NSUserDefaults) : KeyValueStore {

    override fun contains(key: String): Boolean = defaults.objectForKey(key) != null

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun getInt(key: String, default: Int): Int =
        if (contains(key)) defaults.integerForKey(key).toInt() else default

    override fun getLong(key: String, default: Long): Long =
        if (contains(key)) defaults.integerForKey(key) else default

    override fun getFloat(key: String, default: Float): Float =
        if (contains(key)) defaults.floatForKey(key) else default

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (contains(key)) defaults.boolForKey(key) else default

    override fun getStringSet(key: String): Set<String>? =
        defaults.arrayForKey(key)?.filterIsInstance<String>()?.toSet()

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        DefaultsEditor(defaults).block()
    }

    private class DefaultsEditor(private val defaults: NSUserDefaults) : KeyValueStore.Editor {
        override fun putString(key: String, value: String?) {
            if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
        }

        override fun putInt(key: String, value: Int) {
            defaults.setInteger(value.toLong(), forKey = key)
        }

        override fun putLong(key: String, value: Long) {
            defaults.setInteger(value, forKey = key)
        }

        override fun putFloat(key: String, value: Float) {
            defaults.setFloat(value, forKey = key)
        }

        override fun putBoolean(key: String, value: Boolean) {
            defaults.setBool(value, forKey = key)
        }

        override fun putStringSet(key: String, value: Set<String>) {
            defaults.setObject(value.toList(), forKey = key)
        }

        override fun remove(key: String) {
            defaults.removeObjectForKey(key)
        }
    }
}

class UserDefaultsStoreFactory : KeyValueStoreFactory {
    override fun create(name: String): KeyValueStore =
        UserDefaultsKeyValueStore(NSUserDefaults(suiteName = name))
}
