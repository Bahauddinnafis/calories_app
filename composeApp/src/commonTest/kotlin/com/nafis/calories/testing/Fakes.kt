package com.nafis.calories.testing

import com.nafis.calories.core.storage.KeyValueStore
import com.nafis.calories.core.storage.KeyValueStoreFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class FakeKeyValueStore : KeyValueStore {

    val values = mutableMapOf<String, Any>()

    override fun contains(key: String): Boolean = key in values
    override fun getString(key: String): String? = values[key] as? String
    override fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default
    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default
    override fun getFloat(key: String, default: Float): Float = values[key] as? Float ?: default
    override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String): Set<String>? = values[key] as? Set<String>

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String?) {
                if (value == null) values.remove(key) else values[key] = value
            }
            override fun putInt(key: String, value: Int) { values[key] = value }
            override fun putLong(key: String, value: Long) { values[key] = value }
            override fun putFloat(key: String, value: Float) { values[key] = value }
            override fun putBoolean(key: String, value: Boolean) { values[key] = value }
            override fun putStringSet(key: String, value: Set<String>) { values[key] = value }
            override fun remove(key: String) { values.remove(key) }
        }.block()
    }
}

class FakeKeyValueStoreFactory : KeyValueStoreFactory {

    private val stores = mutableMapOf<String, FakeKeyValueStore>()

    fun store(name: String): FakeKeyValueStore = stores.getOrPut(name) { FakeKeyValueStore() }

    override fun create(name: String): KeyValueStore = store(name)
}

class TestClock(var current: Instant = Instant.fromEpochMilliseconds(1_764_000_000_000)) : Clock {
    override fun now(): Instant = current

    fun advanceBy(duration: Duration) {
        current += duration
    }
}