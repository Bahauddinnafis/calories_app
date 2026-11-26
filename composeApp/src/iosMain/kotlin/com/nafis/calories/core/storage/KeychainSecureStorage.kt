package com.nafis.calories.core.storage

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

@OptIn(ExperimentalSettingsImplementation::class)
class KeychainSecureStorage(service: String) : SecureStorage {

    private val keychain = KeychainSettings(service)

    override fun read(key: String): String? = keychain.getStringOrNull(key)

    override fun write(values: Map<String, String>) {
        values.forEach { (key, value) -> keychain.putString(key, value) }
    }

    override fun delete(keys: Collection<String>) {
        keys.forEach(keychain::remove)
    }
}
