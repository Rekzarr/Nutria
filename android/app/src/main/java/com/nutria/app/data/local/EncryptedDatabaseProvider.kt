package com.nutria.app.data.local

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * App-scoped owner of the encrypted Room database. Keep one instance in app composition
 * and call [open] from any coroutine; initialization runs on Dispatchers.IO.
 */
internal class EncryptedDatabaseProvider(
    context: Context,
    private val databaseName: String = DEFAULT_DATABASE_NAME,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val databaseFile: File = appContext.getDatabasePath(databaseName)
    private val keyVault = DatabaseKeyVault(appContext, databaseName)
    private val mutex = Mutex()

    private var database: NutriaDatabase? = null
    private var activePassphrase: ByteArray? = null

    suspend fun open(): NutriaDatabase = mutex.withLock {
        database ?: withContext(Dispatchers.IO) {
            openAndInitialize()
        }
    }

    suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) {
            database?.close()
            database = null
            activePassphrase?.fill(0)
            activePassphrase = null
        }
    }

    private suspend fun openAndInitialize(): NutriaDatabase {
        val passphrase = keyVault.getOrCreatePassphrase(databaseFile)
        var candidate: NutriaDatabase? = null
        var success = false
        try {
            SqlCipherRuntime.load()
            candidate = Room.databaseBuilder(appContext, NutriaDatabase::class.java, databaseName)
                .openHelperFactory(SupportOpenHelperFactory(passphrase, null, true))
                .addMigrations(*LocalDatabaseMigrations.all)
                .build()

            // Force SQLCipher authentication and Room migrations now, so a wrong/missing
            // key or unsupported schema is reported here instead of as a later data loss.
            candidate.openHelper.writableDatabase
            val metadataDao = candidate.storageMetadataDao()
            metadataDao.insertIfMissing(StorageMetadataEntity(initializedAtEpochMs = clockMillis()))
            check(metadataDao.findById() != null) { "Storage metadata bootstrap failed" }

            activePassphrase = passphrase
            database = candidate
            success = true
            return candidate
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: LinkageError) {
            throw DatabaseOpenException(failure)
        } catch (failure: Exception) {
            throw DatabaseOpenException(failure)
        } finally {
            if (!success) {
                candidate?.close()
                passphrase.fill(0)
            }
        }
    }

    private object SqlCipherRuntime {
        @Volatile
        private var loaded = false

        fun load() {
            if (loaded) return
            synchronized(this) {
                if (!loaded) {
                    System.loadLibrary("sqlcipher")
                    loaded = true
                }
            }
        }
    }

    companion object {
        const val DEFAULT_DATABASE_NAME = "nutria.db"
    }
}

internal class DatabaseOpenException(cause: Throwable) : IllegalStateException(
    "No se pudo abrir el almacenamiento cifrado de Nutria. No se ha eliminado ni recreado " +
        "ningún dato; no hay recuperación automática si la clave local se perdió.",
    cause,
)
