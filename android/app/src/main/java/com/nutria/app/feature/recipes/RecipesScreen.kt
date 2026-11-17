package com.nutria.app.feature.recipes

import android.content.ContentResolver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.RecipeRepository
import com.nutria.app.core.data.RecommendationPreferencesRepository
import com.nutria.app.core.model.NutritionRules
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.SavedRecipe
import com.nutria.app.feature.diary.DiaryViewModel
import com.nutria.app.feature.diary.IngredientSearchCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RecipesScreen(
    contentResolver: ContentResolver,
    repository: RecipeRepository,
    recommendationPreferencesRepository: RecommendationPreferencesRepository,
    diaryViewModel: DiaryViewModel?,
) {
    var loadError by remember(repository) { mutableStateOf<String?>(null) }
    val recipesFlow = remember(repository) {
        repository.observeRecipes().catch { failure ->
            if (failure is CancellationException) throw failure
            loadError = failure.message ?: "No se pudieron leer las recetas guardadas."
            emit(emptyList())
        }
    }
    val recipes by recipesFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(RecipeFormDraft()) }
    var editingRecipeId by remember { mutableStateOf<String?>(null) }
    var activeIngredientIndex by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<SavedRecipe?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var importStatusIsError by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    val validation = remember(draft) { RecipeFormValidator.validate(draft) }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importStatusMessage = null
            scope.launch {
                isImporting = true
                try {
                    val importedRecipe = withContext(Dispatchers.IO) {
                        val input = contentResolver.openInputStream(uri)
                            ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado.")
                        input.use(RecipeImportParser::parse)
                    }
                    if (recipes.orEmpty().any { it.name.equals(importedRecipe.name, ignoreCase = true) }) {
                        throw IllegalArgumentException(
                            "Ya existe una receta con el nombre «${importedRecipe.name}». No se ha importado otra copia.",
                        )
                    }
                    repository.saveRecipe(importedRecipe)
                    importStatusMessage = "Receta «${importedRecipe.name}» importada en este dispositivo."
                    importStatusIsError = false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    importStatusMessage = failure.message ?: "No se pudo importar la receta."
                    importStatusIsError = true
                } finally {
                    isImporting = false
                }
            }
        }
    }

    LaunchedEffect(recipes) {
        if (loadError == null) statusMessage = null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.recipes_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.recipes_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { importPicker.launch(arrayOf("application/json")) },
            enabled = recipes != null && !isImporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (isImporting) R.string.recipes_importing else R.string.recipes_import_button))
        }
        Text(
            text = stringResource(R.string.recipes_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        importStatusMessage?.let {
            Text(
                text = it,
                color = if (importStatusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        recipes?.let {
            RecipeRecommendationsSection(
                savedRecipes = it,
                recipeRepository = repository,
                preferencesRepository = recommendationPreferencesRepository,
            )
        }
        RecipeFormCard(
            draft = draft,
            validation = validation,
            editing = editingRecipeId != null,
            activeIngredientIndex = activeIngredientIndex,
            isSaving = isSaving,
            onDraftChange = {
                draft = it
                statusMessage = null
            },
            onActiveIngredientChange = { activeIngredientIndex = it },
            onSave = {
                val recipe = validation.recipe ?: return@RecipeFormCard
                scope.launch {
                    isSaving = true
                    statusMessage = null
                    try {
                        if (editingRecipeId == null) {
                            repository.saveRecipe(recipe)
                            statusMessage = "Receta guardada."
                        } else {
                            repository.updateRecipe(recipe.copy(id = editingRecipeId!!))
                            statusMessage = "Receta actualizada."
                        }
                        draft = RecipeFormDraft()
                        editingRecipeId = null
                        activeIngredientIndex = 0
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        statusMessage = failure.message
                            ?: "No se pudo guardar la receta. No se ha confirmado el cambio."
                    } finally {
                        isSaving = false
                    }
                }
            },
            onCancelEdit = {
                draft = RecipeFormDraft()
                editingRecipeId = null
                activeIngredientIndex = 0
                statusMessage = null
            },
        )
        loadError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        when {
            recipes == null -> Text(stringResource(R.string.recipes_loading))
            recipes.isNullOrEmpty() -> Text(stringResource(R.string.recipes_empty))
            else -> {
                Text(stringResource(R.string.recipes_saved_heading), style = MaterialTheme.typography.titleLarge)
                RecipeCategory.entries.forEach { category ->
                    val matchingRecipes = requireNotNull(recipes).filter { it.category == category }
                    Text(
                        text = stringResource(
                            if (category == RecipeCategory.MEAL) {
                                R.string.recipes_meals_heading
                            } else {
                                R.string.recipes_dinners_heading
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (matchingRecipes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.recipes_category_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        matchingRecipes.forEach { recipe ->
                            SavedRecipeCard(
                                recipe = recipe,
                                canAddToDiary = diaryViewModel != null,
                                onEdit = {
                                    draft = recipe.toRecipeFormDraft()
                                    editingRecipeId = recipe.id
                                    activeIngredientIndex = 0
                                    statusMessage = null
                                },
                                onDelete = { pendingDelete = recipe },
                                onAddToDiary = {
                                    if (diaryViewModel == null) return@SavedRecipeCard
                                    scope.launch {
                                    val added = try {
                                            diaryViewModel.addRecipeServing(
                                                recipe.name,
                                                NutritionRules.calculatePerServing(
                                                    NutritionRules.calculateRecipeTotal(recipe.ingredients),
                                                    recipe.servings,
                                                ),
                                            )
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            false
                                        }
                                        statusMessage = if (added) {
                                            "Una ración de ${recipe.name} se añadió al diario."
                                        } else {
                                            "No se pudo añadir la ración al diario."
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.recipes_delete_title)) },
            text = { Text(stringResource(R.string.recipes_delete_confirmation, recipe.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.deleteRecipe(recipe.id)
                                statusMessage = "Receta eliminada."
                                if (editingRecipeId == recipe.id) {
                                    draft = RecipeFormDraft()
                                    editingRecipeId = null
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                statusMessage = failure.message
                                    ?: "No se pudo eliminar la receta."
                            } finally {
                                pendingDelete = null
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.recipes_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.recipes_cancel))
                }
            },
        )
    }
}

@Composable
private fun RecipeFormCard(
    draft: RecipeFormDraft,
    validation: RecipeFormValidation,
    editing: Boolean,
    activeIngredientIndex: Int,
    isSaving: Boolean,
    onDraftChange: (RecipeFormDraft) -> Unit,
    onActiveIngredientChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    val savedRecipe = validation.recipe
    val nutrition = savedRecipe?.let { NutritionRules.calculateRecipeTotal(it.ingredients) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (editing) R.string.recipes_edit_heading else R.string.recipes_new_heading),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = {
                    onDraftChange(draft.copy(name = it.take(RecipeFormValidator.MAX_RECIPE_NAME_LENGTH)))
                },
                label = { Text(stringResource(R.string.recipes_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.recipes_category_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.category == RecipeCategory.MEAL,
                    onClick = { onDraftChange(draft.copy(category = RecipeCategory.MEAL)) },
                    label = { Text(stringResource(R.string.recipes_meal_category)) },
                )
                FilterChip(
                    selected = draft.category == RecipeCategory.DINNER,
                    onClick = { onDraftChange(draft.copy(category = RecipeCategory.DINNER)) },
                    label = { Text(stringResource(R.string.recipes_dinner_category)) },
                )
            }
            RecipeNumericField(
                value = draft.servings,
                label = stringResource(R.string.recipes_servings_label),
                keyboardType = KeyboardType.Number,
                onValueChange = { onDraftChange(draft.copy(servings = it)) },
            )
            RecipeNumericField(
                value = draft.prepMinutes,
                label = stringResource(R.string.recipes_prep_label),
                keyboardType = KeyboardType.Number,
                onValueChange = { onDraftChange(draft.copy(prepMinutes = it)) },
            )
            Text(stringResource(R.string.recipes_complexity_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RecipeComplexity.entries.forEach { complexity ->
                    FilterChip(
                        selected = draft.complexity == complexity,
                        onClick = {
                            onDraftChange(
                                draft.copy(complexity = if (draft.complexity == complexity) null else complexity),
                            )
                        },
                        label = {
                            Text(
                                stringResource(
                                    when (complexity) {
                                        RecipeComplexity.EASY -> R.string.recipes_easy
                                        RecipeComplexity.MEDIUM -> R.string.recipes_medium
                                        RecipeComplexity.ADVANCED -> R.string.recipes_advanced
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Text(stringResource(R.string.recipes_ingredients_heading), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.recipes_ingredient_search_hint, activeIngredientIndex + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IngredientSearchCard(
                onProductSelected = { product ->
                    val current = draft.ingredients.getOrNull(activeIngredientIndex) ?: return@IngredientSearchCard
                    onDraftChange(
                        draft.updateIngredient(activeIngredientIndex) {
                            it.copy(
                                name = product.name,
                                caloriesPer100Grams = product.caloriesPer100g.toString(),
                                proteinPer100Grams = product.proteinPer100g.toString(),
                                carbohydratesPer100Grams = product.carbsPer100g.toString(),
                                fatPer100Grams = product.fatPer100g.toString(),
                            )
                        },
                    )
                },
            )
            draft.ingredients.forEachIndexed { index, ingredient ->
                IngredientDraftCard(
                    index = index,
                    ingredient = ingredient,
                    selected = index == activeIngredientIndex,
                    canRemove = draft.ingredients.size > 1,
                    onSelect = { onActiveIngredientChange(index) },
                    onChange = { changed -> onDraftChange(draft.updateIngredient(index) { changed }) },
                    onRemove = {
                        val remaining = draft.ingredients.filterIndexed { ingredientIndex, _ -> ingredientIndex != index }
                        onDraftChange(draft.copy(ingredients = remaining))
                        onActiveIngredientChange((activeIngredientIndex - if (index < activeIngredientIndex) 1 else 0)
                            .coerceIn(0, remaining.lastIndex))
                    },
                )
            }
            TextButton(
                onClick = {
                    val added = draft.ingredients + RecipeIngredientDraft()
                    onDraftChange(draft.copy(ingredients = added))
                    onActiveIngredientChange(added.lastIndex)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.recipes_add_ingredient))
            }
            Text(
                text = stringResource(R.string.recipes_preparation_heading),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = draft.instructions,
                onValueChange = { onDraftChange(draft.copy(instructions = it)) },
                label = { Text(stringResource(R.string.recipes_instructions_label)) },
                supportingText = { Text(stringResource(R.string.recipes_instructions_hint)) },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            if (nutrition != null) {
                val perServing = NutritionRules.calculatePerServing(nutrition, requireNotNull(savedRecipe).servings)
                Text(
                    text = stringResource(
                        R.string.recipes_nutrition_summary,
                        SpanishFormats.formatNumber(nutrition.caloriesKcal),
                        SpanishFormats.formatNumber(nutrition.proteinGrams),
                        SpanishFormats.formatNumber(nutrition.carbohydratesGrams),
                        SpanishFormats.formatNumber(nutrition.fatGrams),
                        SpanishFormats.formatNumber(perServing.caloriesKcal),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            validation.errors.distinct().forEach { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onSave,
                enabled = validation.isValid && !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(if (editing) R.string.recipes_update_button else R.string.recipes_save_button),
                )
            }
            if (editing) {
                TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipes_cancel_edit))
                }
            }
        }
    }
}

@Composable
private fun IngredientDraftCard(
    index: Int,
    ingredient: RecipeIngredientDraft,
    selected: Boolean,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onChange: (RecipeIngredientDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.recipes_ingredient_number, index + 1),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onSelect, enabled = !selected) {
                    Text(stringResource(R.string.recipes_select_ingredient))
                }
                TextButton(onClick = onRemove, enabled = canRemove) {
                    Text(stringResource(R.string.recipes_remove_ingredient))
                }
            }
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onChange(ingredient.copy(name = it.take(60))) },
                label = { Text(stringResource(R.string.recipes_ingredient_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            RecipeNumericField(
                value = ingredient.edibleGrams,
                label = stringResource(R.string.recipes_ingredient_grams),
                keyboardType = KeyboardType.Decimal,
                onValueChange = { onChange(ingredient.copy(edibleGrams = it)) },
            )
            RecipeNumericField(
                value = ingredient.caloriesPer100Grams,
                label = stringResource(R.string.recipes_calories_per100),
                keyboardType = KeyboardType.Decimal,
                onValueChange = { onChange(ingredient.copy(caloriesPer100Grams = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeNumericField(
                    value = ingredient.proteinPer100Grams,
                    label = stringResource(R.string.recipes_protein_per100),
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = { onChange(ingredient.copy(proteinPer100Grams = it)) },
                    modifier = Modifier.weight(1f),
                )
                RecipeNumericField(
                    value = ingredient.carbohydratesPer100Grams,
                    label = stringResource(R.string.recipes_carbs_per100),
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = { onChange(ingredient.copy(carbohydratesPer100Grams = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            RecipeNumericField(
                value = ingredient.fatPer100Grams,
                label = stringResource(R.string.recipes_fat_per100),
                keyboardType = KeyboardType.Decimal,
                onValueChange = { onChange(ingredient.copy(fatPer100Grams = it)) },
            )
        }
    }
}

@Composable
private fun RecipeNumericField(
    value: String,
    label: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SavedRecipeCard(
    recipe: SavedRecipe,
    canAddToDiary: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToDiary: () -> Unit,
) {
    val total = recipe.ingredients.takeIf(List<RecipeIngredient>::isNotEmpty)
        ?.let(NutritionRules::calculateRecipeTotal)
    val perServing = total?.let { NutritionRules.calculatePerServing(it, recipe.servings) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium)
            perServing?.let {
                Text(
                    text = stringResource(
                        R.string.recipes_saved_nutrition,
                        SpanishFormats.formatNumber(it.caloriesKcal),
                        SpanishFormats.formatNumber(it.proteinGrams),
                        SpanishFormats.formatNumber(it.carbohydratesGrams),
                        SpanishFormats.formatNumber(it.fatGrams),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            recipe.prepMinutes?.let {
                Text(
                    text = stringResource(
                        R.string.recipes_saved_prep,
                        it,
                        recipe.complexity?.let { value -> stringResource(complexityStringResource(value)) } ?: "—",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            recipe.ingredients.forEach { ingredient ->
                Text(
                    text = stringResource(
                        R.string.recipes_saved_ingredient,
                        ingredient.name,
                        SpanishFormats.formatNumber(ingredient.edibleGrams),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            recipe.instructions.forEachIndexed { index, instruction ->
                Text(
                    text = stringResource(R.string.recipes_saved_instruction, index + 1, instruction),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onAddToDiary,
                enabled = canAddToDiary && perServing != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.recipes_add_to_diary))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipes_edit_button))
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipes_delete))
                }
            }
        }
    }
}

private fun complexityStringResource(complexity: RecipeComplexity): Int = when (complexity) {
    RecipeComplexity.EASY -> R.string.recipes_easy
    RecipeComplexity.MEDIUM -> R.string.recipes_medium
    RecipeComplexity.ADVANCED -> R.string.recipes_advanced
}

private fun RecipeFormDraft.updateIngredient(
    index: Int,
    transform: (RecipeIngredientDraft) -> RecipeIngredientDraft,
) = copy(
    ingredients = ingredients.mapIndexed { ingredientIndex, ingredient ->
        if (ingredientIndex == index) transform(ingredient) else ingredient
    },
)
