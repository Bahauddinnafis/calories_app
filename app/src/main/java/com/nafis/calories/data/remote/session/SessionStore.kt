package com.nafis.calories.data.remote.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SessionStore {

    private const val TAG = "SessionStore"
    private const val PREFS_NAME = "cloud_session"
    private const val KEY_ALIAS = "calories_session_key"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    private lateinit var prefs: SharedPreferences

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedAccessToken = decrypt(prefs.getString(KEY_ACCESS_TOKEN, null))
        cachedRefreshToken = decrypt(prefs.getString(KEY_REFRESH_TOKEN, null))
    }

    val isInitialized: Boolean get() = ::prefs.isInitialized

    val accessToken: String? get() = cachedAccessToken

    val refreshToken: String? get() = cachedRefreshToken

    val userId: String? get() = if (isInitialized) decrypt(prefs.getString(KEY_USER_ID, null)) else null

    val hasSession: Boolean get() = !cachedAccessToken.isNullOrBlank()

    fun save(userId: String, accessToken: String, refreshToken: String?) {
        prefs.edit {
            putString(KEY_USER_ID, encrypt(userId))
            putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
        }
        cachedAccessToken = accessToken
        if (refreshToken != null) cachedRefreshToken = refreshToken
    }

    /** Replaces the tokens after a refresh; the user stays the same. */
    @Synchronized
    fun updateTokens(accessToken: String, refreshToken: String?) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
        }
        cachedAccessToken = accessToken
        if (refreshToken != null) cachedRefreshToken = refreshToken
    }

    /** Drops the session so the next sync signs in anonymously again. */
    @Synchronized
    fun clearSession() {
        if (!isInitialized) return
        prefs.edit {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
        }
        cachedAccessToken = null
        cachedRefreshToken = null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH)
            )
            String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
        } catch (e: Exception) {
            // Key lost (e.g. restored backup): drop the unreadable session and sign in again.
            Log.w(TAG, "Stored session could not be decrypted; starting a new one", e)
            null
        }
    }
}