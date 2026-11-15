package com.nutria.app.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.core.model.NutritionGoals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class GoalsRoomRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun defaultsAndSavedGoalsSurviveAnEncryptedDatabaseReopen() = runBlocking {
        val databaseName = "goals-${System.nanoTime()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val vault = DatabaseKeyVault(context, databaseName)
        val firstProvider = EncryptedDatabaseProvider(context, databaseName)
        try {
            val firstRepository = GoalsRoomRepository(firstProvider)
            assertEquals(NutritionGoals(), firstRepository.observeGoals().first())

            val updated = NutritionGoals(
                caloriesKcal = 2_150,
                waterMillilitres = 2_050,
                proteinGrams = 125.5,
                carbohydratesGrams = 230.25,
                fatGrams = 70.0,
            )
            firstRepository.saveGoals(updated)
            assertEquals(updated, firstRepository.observeGoals().first())
            firstProvider.close()

            val reopenedProvider = EncryptedDatabaseProvider(context, databaseName)
            try {
                assertEquals(
                    updated,
                    GoalsRoomRepository(reopenedProvider).observeGoals().first(),
                )
            } finally {
                reopenedProvider.close()
            }
        } finally {
            firstProvider.close()
            context.deleteDatabase(databaseName)
            File(context.noBackupFilesDir, "$databaseName.key-envelope").delete()
            File("${context.noBackupFilesDir}/$databaseName.key-envelope.bak").delete()
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(vault.keyAlias)
            }
            databaseFile.delete()
        }
    }
}
