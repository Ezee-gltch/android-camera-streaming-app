package com.camerastreamingapp.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CredentialEncryptor {
    private val keyStoreProvider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val alias = "camera_credentials_key"

    fun encrypt(plainText: String): String? = runCatching {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plainText.toByteArray())
        val payload = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        Base64.encodeToString(payload, Base64.NO_WRAP)
    }.getOrNull()

    fun decrypt(cipherText: String): String? = runCatching {
        val payload = Base64.decode(cipherText, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(payload)
        val ivSize = buffer.int
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted))
    }.getOrNull()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(keyStoreProvider).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }
}
