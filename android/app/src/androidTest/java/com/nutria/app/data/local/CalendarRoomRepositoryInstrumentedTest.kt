package com.nutria.app.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.core.model.CalendarAssignment
import com.nutria.app.core.model.CalendarSlot
import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.SavedRecipe
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
class CalendarRoomRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun replacementIsRangeBoundAndDeletingRecipeCascadesItsAssignments() = runBlocking {
        val databaseName = "calendar-${UUID.randomUUID()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val provider = EncryptedDatabaseProvider(context, databaseName)
        val recipes = RecipeRoomRepository(provider)
        val calendar = CalendarRoomRepository(provider)
        val meal = recipe("meal")
        val dinner = recipe("dinner", RecipeCategory.DINNER)
        val outsideDate = LocalDate.of(2026, 12, 1)
        val targetDate = LocalDate.of(2026, 12, 2)

        try {
            recipes.saveRecipe(meal)
            recipes.saveRecipe(dinner)
            calendar.saveAssignment(CalendarAssignment(outsideDate, CalendarSlot.MEAL, meal.id))
            calendar.saveAssignment(CalendarAssignment(targetDate, CalendarSlot.MEAL, meal.id))
            recipes.updateRecipe(meal.copy(name = "Receta actualizada"))
            assertEquals(
                listOf(CalendarAssignment(outsideDate, CalendarSlot.MEAL, meal.id)),
                calendar.observeAssignments(outsideDate, outsideDate).first(),
            )

            calendar.replaceAssignments(
                fromInclusive = targetDate,
                throughInclusive = targetDate,
                assignments = listOf(CalendarAssignment(targetDate, CalendarSlot.DINNER, dinner.id)),
            )

            assertEquals(
                listOf(CalendarAssignment(outsideDate, CalendarSlot.MEAL, meal.id)),
                calendar.observeAssignments(outsideDate, outsideDate).first(),
            )
            assertEquals(
                listOf(CalendarAssignment(targetDate, CalendarSlot.DINNER, dinner.id)),
                calendar.observeAssignments(targetDate, targetDate).first(),
            )

            recipes.deleteRecipe(meal.id)
            assertEquals(
                listOf(CalendarAssignment(targetDate, CalendarSlot.DINNER, dinner.id)),
                calendar.observeAssignments(outsideDate, targetDate).first(),
            )
        } finally {
            provider.close()
            cleanup(databaseName, databaseFile)
        }
    }

    private fun recipe(
        id: String,
        category: RecipeCategory = RecipeCategory.MEAL,
    ) = SavedRecipe(
        id = id,
        name = "Receta $id",
        category = category,
        ingredients = listOf(
            RecipeIngredient(
                name = "Ingrediente",
                edibleGrams = 100.0,
                nutritionPer100Grams = Nutrients(100.0, 5.0, 15.0, 2.0),
            ),
        ),
    )

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
