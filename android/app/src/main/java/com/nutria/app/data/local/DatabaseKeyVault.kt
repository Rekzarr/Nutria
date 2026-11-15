package com.nutria.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores a random SQLCipher passphrase only as AES-GCM ciphertext under noBackupFilesDir.
 * The AES key is non-exportable and protected by Android Keystore.
 */
internal class DatabaseKeyVault(
    context: Context,
    private val databaseName: String,
) {
    private val appContext = context.applicationContext
    private val envelopeFile = File(appContext.noBackupFilesDir, "$databaseName.key-envelope")
    internal val keyAlias = "$KEY_ALIAS_PREFIX.$databaseName.v1"
    private val atomicEnvelope = AtomicFile(envelopeFile)

    /**
     * Returns the same key for the life of the database. Missing key material is only
     * initialized on a genuinely empty install; existing database files are never replaced.
     */
    fun getOrCreatePassphrase(databaseFile: File): ByteArray = synchronized(processLock) {
        val hasDatabaseState = databaseStateFiles(databaseFile).any(File::exists)
        val envelopeExists = envelopeFile.exists() || File("${envelopeFile.path}.bak").exists()
        val keyStore = loadKeyStore()
        val wrappingKeyExists = keyStore.containsAlias(keyAlias)

        if (envelopeExists) {
            if (!wrappingKeyExists) {
                throw DatabaseKeyException()
            }
            return@synchronized try {
                decryptEnvelope(atomicEnvelope.openRead().use { it.readBytes() }, getWrappingKey())
            } catch (failure: DatabaseKeyException) {
                throw failure
            } catch (failure: Exception) {
                throw DatabaseKeyException(failure)
            }
        }

        if (hasDatabaseState) {
            throw DatabaseKeyException()
        }

        // A crash between Keystore-key creation and atomic envelope persistence leaves no
        // database (the database is opened only after this method returns). That orphaned
        // alias can safely be replaced; an existing DB above always fails closed.
        if (wrappingKeyExists) {
            keyStore.deleteEntry(keyAlias)
        }
        createWrappingKey()
        val passphrase = createRandomPassphrase()
        try {
            val encryptedEnvelope = encryptEnvelope(passphrase, getWrappingKey())
            writeEnvelope(encryptedEnvelope)
            passphrase
        } catch (failure: Exception) {
            passphrase.fill(0)
            throw DatabaseKeyException(failure)
        }
    }

    private fun databaseStateFiles(databaseFile: File): List<File> = listOf(
        databaseFile,
        File("${databaseFile.path}-wal"),
        File("${databaseFile.path}-shm"),
        File("${databaseFile.path}-journal"),
    )

    private fun loadKeyStore(): KeyStore = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (failure: Exception) {
        throw DatabaseKeyException(failure)
    }

    private fun getWrappingKey(): SecretKey {
        val keyStore = loadKeyStore()
        return keyStore.getKey(keyAlias, null) as? SecretKey ?: throw DatabaseKeyException()
    }

    private fun createWrappingKey() {
        try {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generator.generateKey()
        } catch (failure: Exception) {
            throw DatabaseKeyException(failure)
        }
    }

    private fun createRandomPassphrase(): ByteArray {
        val entropy = ByteArray(PASSPHRASE_ENTROPY_BYTES)
        return try {
            SecureRandom().nextBytes(entropy)
            Base64.encodeToString(entropy, Base64.NO_WRAP or Base64.NO_PADDING)
                .toByteArray(StandardCharsets.US_ASCII)
        } finally {
            entropy.fill(0)
        }
    }

    private fun encryptEnvelope(passphrase: ByteArray, wrappingKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        cipher.updateAAD(HEADER)
        val encrypted = cipher.doFinal(passphrase)
        return ByteArrayOutputStream(HEADER.size + cipher.iv.size + encrypted.size).use { output ->
            output.write(HEADER)
            output.write(cipher.iv)
            output.write(encrypted)
            output.toByteArray()
        }.also {
            encrypted.fill(0)
        }
    }

    private fun decryptEnvelope(envelope: ByteArray, wrappingKey: SecretKey): ByteArray {
        try {
            if (envelope.size != ENVELOPE_SIZE || !envelope.copyOfRange(0, HEADER.size).contentEquals(HEADER)) {
                throw DatabaseKeyException()
            }
            val nonce = envelope.copyOfRange(HEADER.size, HEADER.size + NONCE_BYTES)
            val encrypted = envelope.copyOfRange(HEADER.size + NONCE_BYTES, envelope.size)
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_BITS, nonce))
                cipher.updateAAD(HEADER)
                return cipher.doFinal(encrypted)
            } finally {
                nonce.fill(0)
                encrypted.fill(0)
            }
        } finally {
            envelope.fill(0)
        }
    }

    private fun writeEnvelope(envelope: ByteArray) {
        var stream: java.io.FileOutputStream? = null
        try {
            stream = atomicEnvelope.startWrite()
            stream.write(envelope)
            stream.fd.sync()
            atomicEnvelope.finishWrite(stream)
            stream = null
        } catch (failure: IOException) {
            stream?.let(atomicEnvelope::failWrite)
            throw failure
        } finally {
            envelope.fill(0)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "com.nutria.app.database"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val PASSPHRASE_ENTROPY_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private val HEADER = byteArrayOf(0x4e, 0x54, 0x4b, 0x59, 0x01)
        private const val ENVELOPE_SIZE = 5 + NONCE_BYTES + 43 + 16
        private val processLock = Any()
    }
}

internal class DatabaseKeyException(cause: Throwable? = null) : IllegalStateException(
    "No se pudo recuperar la clave cifrada de Nutria. No se han borrado ni recreado tus datos; " +
        "sin la clave original no es posible recuperar este almacenamiento local.",
    cause,
)
