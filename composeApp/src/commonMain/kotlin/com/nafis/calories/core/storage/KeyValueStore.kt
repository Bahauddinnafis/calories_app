package com.nafis.calories.core.storage

interface KeyValueStore {
    fun contains(key: String): Boolean
    fun getString(key: String): String?
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getFloat(key: String, default: Float): Float
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getStringSet(key: String): Set<String>?
    fun edit(block: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String?)
        fun putInt(key: String, value: Int)
        fun putLong(key: String, value: Long)
        fun putFloat(key: String, value: Float)
        fun putBoolean(key: String, value: Boolean)
        fun putStringSet(key: String, value: Set<String>)
        fun remove(key: String)
    }
}

fun interface KeyValueStoreFactory {
    fun create(name: String): KeyValueStore
}
