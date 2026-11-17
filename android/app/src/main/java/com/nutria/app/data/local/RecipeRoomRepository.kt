package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.withTransaction
import com.nutria.app.core.data.RecipeRepository
import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.SavedRecipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.math.round

@Entity(tableName = "saved_recipes")
internal data class SavedRecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    @ColumnInfo(name = "recommendation_id") val recommendationId: String?,
    val servings: Int,
    @ColumnInfo(name = "prep_minutes") val prepMinutes: Int?,
    val complexity: String?,
    @ColumnInfo(name = "instructions", defaultValue = "''") val instructions: String,
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["recipe_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SavedRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recipe_id"])],
)
internal data class RecipeIngredientEntity(
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    val position: Int,
    val name: String,
    @ColumnInfo(name = "edible_milli_grams") val edibleMilliGrams: Long,
    @ColumnInfo(name = "calories_milli_kcal") val caloriesMilliKcal: Long,
    @ColumnInfo(name = "protein_milli_grams") val proteinMilliGrams: Long,
    @ColumnInfo(name = "carbohydrates_milli_grams") val carbohydratesMilliGrams: Long,
    @ColumnInfo(name = "fat_milli_grams") val fatMilliGrams: Long,
)

internal data class SavedRecipeWithIngredients(
    @Embedded val recipe: SavedRecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
        entity = RecipeIngredientEntity::class,
    )
    val ingredients: List<RecipeIngredientEntity>,
)

@Dao
internal interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM saved_recipes ORDER BY category, name COLLATE NOCASE")
    fun observeRecipes(): Flow<List<SavedRecipeWithIngredients>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipeIfMissing(recipe: SavedRecipeEntity)

    @Update
    suspend fun updateRecipe(recipe: SavedRecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Insert
    suspend fun saveIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM saved_recipes WHERE id = :recipeId")
    suspend fun deleteRecipe(recipeId: String)
}

internal class RecipeRoomRepository(
    private val databaseProvider: EncryptedDatabaseProvider,
) : RecipeRepository {
    override fun observeRecipes(): Flow<List<SavedRecipe>> = flow {
        emitAll(
            databaseProvider.open().recipeDao().observeRecipes().map { recipes ->
                recipes.map(SavedRecipeWithIngredients::toModel)
            },
        )
    }

    override suspend fun saveRecipe(recipe: SavedRecipe) = persistRecipe(recipe)

    override suspend fun updateRecipe(recipe: SavedRecipe) = persistRecipe(recipe)

    override suspend fun deleteRecipe(recipeId: String) {
        require(recipeId.isNotBlank())
        databaseProvider.open().recipeDao().deleteRecipe(recipeId)
    }

    private suspend fun persistRecipe(recipe: SavedRecipe) {
        require(recipe.ingredients.isNotEmpty())
        val database = databaseProvider.open()
        database.withTransaction {
            val dao = database.recipeDao()
            val entity = recipe.toEntity()
            dao.insertRecipeIfMissing(entity)
            dao.updateRecipe(entity)
            dao.deleteIngredients(recipe.id)
            dao.saveIngredients(recipe.ingredients.mapIndexed { position, ingredient ->
                ingredient.toEntity(recipe.id, position)
            })
        }
    }
}

private fun SavedRecipe.toEntity() = SavedRecipeEntity(
    id = id,
    name = name.trim(),
    category = category.name,
    recommendationId = recommendationId,
    servings = servings,
    prepMinutes = prepMinutes,
    complexity = complexity?.name,
    instructions = instructions.map(String::trim).filter(String::isNotEmpty).joinToString("\n"),
)

private fun RecipeIngredient.toEntity(recipeId: String, position: Int) = RecipeIngredientEntity(
    recipeId = recipeId,
    position = position,
    name = name.trim(),
    edibleMilliGrams = edibleGrams.toMilliUnits(),
    caloriesMilliKcal = nutritionPer100Grams.caloriesKcal.toMilliUnits(),
    proteinMilliGrams = nutritionPer100Grams.proteinGrams.toMilliUnits(),
    carbohydratesMilliGrams = nutritionPer100Grams.carbohydratesGrams.toMilliUnits(),
    fatMilliGrams = nutritionPer100Grams.fatGrams.toMilliUnits(),
)

private fun SavedRecipeWithIngredients.toModel() = SavedRecipe(
    id = recipe.id,
    name = recipe.name,
    category = RecipeCategory.valueOf(recipe.category),
    recommendationId = recipe.recommendationId,
    ingredients = ingredients.sortedBy(RecipeIngredientEntity::position).map(RecipeIngredientEntity::toModel),
    servings = recipe.servings,
    prepMinutes = recipe.prepMinutes,
    complexity = recipe.complexity?.let(RecipeComplexity::valueOf),
    instructions = recipe.instructions.lines().map(String::trim).filter(String::isNotEmpty),
)

private fun RecipeIngredientEntity.toModel() = RecipeIngredient(
    name = name,
    edibleGrams = edibleMilliGrams / 1_000.0,
    nutritionPer100Grams = Nutrients(
        caloriesKcal = caloriesMilliKcal / 1_000.0,
        proteinGrams = proteinMilliGrams / 1_000.0,
        carbohydratesGrams = carbohydratesMilliGrams / 1_000.0,
        fatGrams = fatMilliGrams / 1_000.0,
    ),
)

private fun Double.toMilliUnits(): Long {
    val scaled = this * 1_000.0
    require(scaled.isFinite() && scaled <= Long.MAX_VALUE.toDouble())
    return round(scaled).toLong()
}
