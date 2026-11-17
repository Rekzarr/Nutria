package com.nutria.app.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.SavedRecipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecipeRoomRepositoryInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun recipesAndIngredientsPersistInEncryptedRoomAndDeleteTogether() = runBlocking {
        val databaseName = "recipes-${UUID.randomUUID()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val provider = EncryptedDatabaseProvider(context, databaseName)
        val repository = RecipeRoomRepository(provider)
        val recipe = SavedRecipe(
            id = "recipe-1",
            name = "Avena",
            category = RecipeCategory.DINNER,
            recommendationId = "recommendation-1",
            ingredients = listOf(
                RecipeIngredient(
                    name = "Avena",
                    edibleGrams = 75.5,
                    nutritionPer100Grams = Nutrients(380.0, 13.0, 68.0, 7.0),
                ),
                RecipeIngredient(
                    name = "Leche",
                    edibleGrams = 200.0,
                    nutritionPer100Grams = Nutrients(50.0, 3.5, 5.0, 2.0),
                ),
            ),
            servings = 2,
            prepMinutes = 25,
            complexity = RecipeComplexity.MEDIUM,
            instructions = listOf("Mezclar", "Nota: reservar un poco de leche", "Calentar"),
        )

        try {
            repository.saveRecipe(recipe)
            provider.close()

            val reopenedProvider = EncryptedDatabaseProvider(context, databaseName)
            try {
                val reopened = RecipeRoomRepository(reopenedProvider)
                assertEquals(recipe, reopened.observeRecipes().first().single())

                val updated = recipe.copy(
                    name = "Avena actualizada",
                    ingredients = listOf(recipe.ingredients.first()),
                )
                reopened.updateRecipe(updated)
                assertEquals(updated, reopened.observeRecipes().first().single())

                reopened.deleteRecipe(recipe.id)
                assertEquals(emptyList<SavedRecipe>(), reopened.observeRecipes().first())
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
