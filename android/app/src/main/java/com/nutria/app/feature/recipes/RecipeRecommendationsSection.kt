package com.nutria.app.feature.recipes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.RecommendationPreferencesRepository
import com.nutria.app.core.model.ComplexityFilter
import com.nutria.app.core.model.NutritionRules
import com.nutria.app.core.model.RecommendationCategory
import com.nutria.app.core.model.RecommendationFilter
import com.nutria.app.core.model.RecommendationRules
import com.nutria.app.core.model.RecipeRecommendation
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.SavedRecipe
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.data.RecipeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@Composable
internal fun RecipeRecommendationsSection(
    savedRecipes: List<SavedRecipe>,
    recipeRepository: RecipeRepository,
    preferencesRepository: RecommendationPreferencesRepository,
) {
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.recommendations_saved)
    val saveErrorMessage = stringResource(R.string.recommendations_save_error)
    var filter by remember { mutableStateOf(RecommendationFilter()) }
    var filterError by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableIntStateOf(0) }
    val eligible = remember(savedRecipes, filter) {
        RecommendationRules.eligible(
            catalog = RecommendationCatalog.recipes,
            savedRecipes = savedRecipes,
            filter = filter,
        )
    }
    val shownRecommendations = remember(eligible, refreshCount) {
        RecommendationRules.select(eligible)
    }

    LaunchedEffect(preferencesRepository) {
        try {
            preferencesRepository.observeRecommendationFilter().collect {
                filter = it
                filterError = false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            filterError = true
        }
    }

    fun saveFilter(changed: RecommendationFilter) {
        scope.launch {
            try {
                preferencesRepository.saveRecommendationFilter(changed)
                filter = changed
                refreshCount++
                statusMessage = null
                filterError = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                filterError = true
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.recommendations_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.recommendations_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(stringResource(R.string.recommendations_category), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecommendationCategory.entries.forEach { category ->
                FilterChip(
                    selected = filter.category == category,
                    onClick = { saveFilter(filter.copy(category = category)) },
                    label = {
                        Text(
                            stringResource(
                                when (category) {
                                    RecommendationCategory.ALL -> R.string.recommendations_all
                                    RecommendationCategory.MEAL -> R.string.recommendations_meals
                                    RecommendationCategory.DINNER -> R.string.recommendations_dinners
                                },
                            ),
                        )
                    },
                )
            }
        }

        Text(stringResource(R.string.recommendations_time), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecommendationFilter.SUPPORTED_PREP_TIMES.sorted().forEach { minutes ->
                FilterChip(
                    selected = filter.maxPrepMinutes == minutes,
                    onClick = { saveFilter(filter.copy(maxPrepMinutes = minutes)) },
                    label = { Text(stringResource(R.string.recommendations_minutes, minutes)) },
                )
            }
        }

        Text(stringResource(R.string.recommendations_complexity), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ComplexityFilter.entries.forEach { complexity ->
                FilterChip(
                    selected = filter.complexity == complexity,
                    onClick = { saveFilter(filter.copy(complexity = complexity)) },
                    label = {
                        Text(
                            stringResource(
                                when (complexity) {
                                    ComplexityFilter.ALL -> R.string.recommendations_any_complexity
                                    ComplexityFilter.EASY -> R.string.recommendations_easy
                                    ComplexityFilter.MEDIUM -> R.string.recommendations_medium
                                    ComplexityFilter.ADVANCED -> R.string.recommendations_advanced
                                },
                            ),
                        )
                    },
                )
            }
        }

        OutlinedButton(
            onClick = { refreshCount++ },
            enabled = eligible.size > shownRecommendations.size,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.recommendations_refresh))
        }

        if (eligible.isEmpty()) {
            Text(
                text = stringResource(R.string.recommendations_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            shownRecommendations.forEach { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    isSaving = isSaving,
                    onSave = {
                        val nutrition = NutritionRules.calculateRecipeTotal(recommendation.ingredients)
                        val recipe = SavedRecipe(
                            id = UUID.randomUUID().toString(),
                            name = recommendation.name,
                            category = recommendation.category,
                            recommendationId = recommendation.id,
                            ingredients = recommendation.ingredients,
                            servings = 1,
                            prepMinutes = recommendation.prepMinutes,
                            complexity = recommendation.complexity,
                            instructions = recommendation.instructions,
                        )
                        if (nutrition.caloriesKcal.roundToInt() > RecommendationRules.MAX_RECOMMENDATION_CALORIES) {
                            saveFailed = true
                            return@RecommendationCard
                        }
                        scope.launch {
                            isSaving = true
                            statusMessage = null
                            try {
                                recipeRepository.saveRecipe(recipe)
                                statusMessage = savedMessage
                                saveFailed = false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                saveFailed = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                )
            }
        }

        if (filterError) {
            Text(
                text = stringResource(R.string.recommendations_preferences_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (saveFailed) {
            Text(
                text = saveErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (statusMessage != null) {
            Text(
                text = statusMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecipeRecommendation,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    var expanded by remember(recommendation.id) { mutableStateOf(false) }
    val nutrition = NutritionRules.calculateRecipeTotal(recommendation.ingredients)
    val category = stringResource(
        if (recommendation.category == RecipeCategory.DINNER) {
            R.string.recommendations_dinner_category
        } else {
            R.string.recommendations_meal_category
        },
    )
    val complexity = stringResource(
        when (recommendation.complexity) {
            RecipeComplexity.EASY -> R.string.recommendations_easy
            RecipeComplexity.MEDIUM -> R.string.recommendations_medium
            RecipeComplexity.ADVANCED -> R.string.recommendations_advanced
        },
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(recommendation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = category,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                stringResource(
                    R.string.recommendations_calories,
                    SpanishFormats.formatInteger(kotlin.math.round(nutrition.caloriesKcal).toLong()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.recipes_saved_prep,
                    recommendation.prepMinutes,
                    complexity,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(
                    R.string.recommendations_macros,
                    SpanishFormats.formatNumber(nutrition.proteinGrams),
                    SpanishFormats.formatNumber(nutrition.carbohydratesGrams),
                    SpanishFormats.formatNumber(nutrition.fatGrams),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.recommendations_details))
            }
            if (expanded) {
                recommendation.ingredients.forEach { ingredient ->
                    Text(
                        stringResource(
                            R.string.recommendations_ingredient,
                            ingredient.name,
                            SpanishFormats.formatNumber(ingredient.edibleGrams),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                recommendation.instructions.forEachIndexed { index, instruction ->
                    Text(
                        stringResource(R.string.recipes_saved_instruction, index + 1, instruction),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.recommendations_save))
            }
        }
    }
}
