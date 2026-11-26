package com.nafis.calories.data.remote.session

import com.nafis.calories.core.storage.SecureStorage
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.concurrent.Volatile

class SessionStore(private val storage: SecureStorage) {

    private val lock = SynchronizedObject()

    @Volatile
    var accessToken: String? = storage.read(KEY_ACCESS_TOKEN)
        private set

    @Volatile
    var refreshToken: String? = storage.read(KEY_REFRESH_TOKEN)
        private set

    @Volatile
    var userId: String? = storage.read(KEY_USER_ID)
        private set

    val hasSession: Boolean get() = !accessToken.isNullOrBlank()

    fun save(userId: String, accessToken: String, refreshToken: String?) = synchronized(lock) {
        storage.write(
            buildMap {
                put(KEY_USER_ID, userId)
                put(KEY_ACCESS_TOKEN, accessToken)
                refreshToken?.let { put(KEY_REFRESH_TOKEN, it) }
            }
        )
        this.userId = userId
        this.accessToken = accessToken
        if (refreshToken != null) this.refreshToken = refreshToken
    }

    fun updateTokens(accessToken: String, refreshToken: String?) = synchronized(lock) {
        storage.write(
            buildMap {
                put(KEY_ACCESS_TOKEN, accessToken)
                refreshToken?.let { put(KEY_REFRESH_TOKEN, it) }
            }
        )
        this.accessToken = accessToken
        if (refreshToken != null) this.refreshToken = refreshToken
    }

    /** Drops the tokens so the next sync signs in anonymously again; the user id is kept. */
    fun clearSession() = synchronized(lock) {
        storage.delete(listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN))
        accessToken = null
        refreshToken = null
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
