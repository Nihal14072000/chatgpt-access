package com.poultrycrm.android.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the current authorization header in memory and persists it encrypted
 * with an Android Keystore AES-GCM key. The password itself is never stored.
 */
class ApiSession(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("poultry_crm_session", Context.MODE_PRIVATE)
    private val authorization = AtomicReference<String?>(decrypt(preferences.getString(KEY_AUTH, null)))

    fun header(): String? = authorization.get()

    @Synchronized
    fun start(header: String) {
        val encrypted = encrypt(header)
        preferences.edit().putString(KEY_AUTH, encrypted).commit()
        authorization.set(header)
    }

    @Synchronized
    fun clear() {
        authorization.set(null)
        preferences.edit().remove(KEY_AUTH).commit()
    }

    fun isAuthenticated(): Boolean = authorization.get() != null

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return "$iv:$ciphertext"
    }

    private fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return try {
            val parts = value.split(":", limit = 2)
            require(parts.size == 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            preferences.edit().remove(KEY_AUTH).apply()
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_AUTH = "encrypted_authorization"
        const val KEY_ALIAS = "poultry_crm_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
