package com.nutria.app.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.core.model.CycleDayEntry
import com.nutria.app.core.model.CycleFlow
import com.nutria.app.core.model.CycleMood
import com.nutria.app.core.model.CycleSettings
import com.nutria.app.core.model.PeriodRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CycleRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cycleRecordsAndPrivateNotePersistInEncryptedRoom() = runBlocking {
        val databaseName = "cycle-${UUID.randomUUID()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val provider = EncryptedDatabaseProvider(context, databaseName)
        val repository = CycleRoomRepository(provider)
        val today = LocalDate.of(2026, 11, 14)

        try {
            assertEquals(CycleSettings(), repository.observeSettings().first())
            repository.saveSettings(CycleSettings(cycleLengthDays = 30, periodLengthDays = 4))
            repository.savePeriod(PeriodRecord("older", today.minusDays(40), today.minusDays(37)))
            repository.savePeriod(PeriodRecord("newer", today.minusDays(10)))
            repository.saveDayEntry(
                CycleDayEntry(
                    date = today,
                    flow = CycleFlow.LIGHT,
                    pain = 3,
                    mood = CycleMood.TIRED,
                    note = "  registro privado  ",
                ),
            )

            provider.close()
            val reopenedProvider = EncryptedDatabaseProvider(context, databaseName)
            try {
                val reopened = CycleRoomRepository(reopenedProvider)
                assertEquals(CycleSettings(30, 4), reopened.observeSettings().first())
                assertEquals(
                    listOf("newer", "older"),
                    reopened.observePeriods().first().map { it.id },
                )
                assertEquals(
                    CycleDayEntry(today, CycleFlow.LIGHT, 3, CycleMood.TIRED, "registro privado"),
                    reopened.observeRecentEntries(today, 7).first().single(),
                )
            } finally {
                reopenedProvider.close()
            }
        } finally {
            provider.close()
            cleanup(databaseName, databaseFile)
        }
    }

    private fun cleanup(databaseName: String, databaseFile: File) {
        context.deleteDatabase(databaseName)
        File(context.noBackupFilesDir, "$databaseName.key-envelope").delete()
        File("${context.noBackupFilesDir}/$databaseName.key-envelope.bak").delete()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(DatabaseKeyVault(context, databaseName).keyAlias)
        }
        databaseFile.delete()
    }
}
