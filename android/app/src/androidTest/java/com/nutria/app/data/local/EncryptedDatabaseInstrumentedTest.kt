package com.nutria.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlatformSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun databaseIsEncryptedAndReopensWithTheSameKeystoreWrappedKey() = runBlocking {
        val databaseName = "test-${System.nanoTime()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val vault = DatabaseKeyVault(context, databaseName)
        val provider = EncryptedDatabaseProvider(context, databaseName) { FIXED_TIMESTAMP }

        try {
            val firstPassphrase = vault.getOrCreatePassphrase(databaseFile)
            val envelope = File(context.noBackupFilesDir, "$databaseName.key-envelope")
            assertTrue(envelope.exists())
            assertEquals(43, firstPassphrase.size)
            assertFalse(
                envelope.readBytes().toString(Charsets.ISO_8859_1)
                    .contains(firstPassphrase.toString(Charsets.ISO_8859_1)),
            )
            firstPassphrase.fill(0)

            val firstDatabase = provider.open()
            val firstRecord = firstDatabase.storageMetadataDao().findById()
            assertNotNull(firstRecord)
            assertEquals(FIXED_TIMESTAMP, firstRecord?.initializedAtEpochMs)
            assertEquals(LocalDatabaseMigrations.SCHEMA_VERSION, firstDatabase.openHelper.writableDatabase.version)
            provider.close()

            val fileHeader = RandomAccessFile(databaseFile, "r").use { file ->
                ByteArray(SQLITE_HEADER.size).also(file::readFully)
            }
            assertFalse("Encrypted database unexpectedly has a plaintext SQLite header", fileHeader.contentEquals(SQLITE_HEADER))

            val secondProvider = EncryptedDatabaseProvider(context, databaseName) { FIXED_TIMESTAMP + 1 }
            try {
                val reopened = secondProvider.open()
                assertEquals(FIXED_TIMESTAMP, reopened.storageMetadataDao().findById()?.initializedAtEpochMs)
            } finally {
                secondProvider.close()
            }

            val secondPassphrase = vault.getOrCreatePassphrase(databaseFile)
            try {
                val thirdPassphrase = DatabaseKeyVault(context, databaseName).getOrCreatePassphrase(databaseFile)
                try {
                    assertArrayEquals(secondPassphrase, thirdPassphrase)
                } finally {
                    thirdPassphrase.fill(0)
                }
            } finally {
                secondPassphrase.fill(0)
            }
        } finally {
            provider.close()
            cleanup(databaseName, vault, databaseFile)
        }
    }

    @Test
    fun existingDatabaseWithoutItsKeyEnvelopeFailsClosedAndRemainsUntouched() = runBlocking {
        val databaseName = "orphan-${System.nanoTime()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        val platformDatabase = PlatformSQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        platformDatabase.execSQL("CREATE TABLE preserve_me (value TEXT NOT NULL)")
        platformDatabase.execSQL("INSERT INTO preserve_me(value) VALUES ('keep')")
        platformDatabase.close()

        val vault = DatabaseKeyVault(context, databaseName)
        try {
            assertThrows(DatabaseKeyException::class.java) {
                vault.getOrCreatePassphrase(databaseFile)
            }
            assertTrue(databaseFile.exists())
            assertFalse(File(context.noBackupFilesDir, "$databaseName.key-envelope").exists())
        } finally {
            cleanup(databaseName, vault, databaseFile)
        }
    }

    private fun cleanup(databaseName: String, vault: DatabaseKeyVault, databaseFile: File) {
        context.deleteDatabase(databaseName)
        File(context.noBackupFilesDir, "$databaseName.key-envelope").delete()
        File("${context.noBackupFilesDir}/$databaseName.key-envelope.bak").delete()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(vault.keyAlias)
        }
        databaseFile.delete()
    }

    companion object {
        private const val FIXED_TIMESTAMP = 1_800_000_000_000L
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
