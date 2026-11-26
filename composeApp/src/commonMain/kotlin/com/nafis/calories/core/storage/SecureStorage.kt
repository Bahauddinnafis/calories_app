package com.nafis.calories.core.storage

interface SecureStorage {
    fun read(key: String): String?
    fun write(values: Map<String, String>)
    fun delete(keys: Collection<String>)
}
