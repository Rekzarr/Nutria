package com.nutria.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.model.NutritionGoals
import com.nutria.app.core.model.summarizeDailyNutrition

@Composable
fun DailyNutritionSummaryCard(
    viewModel: DiaryViewModel,
    goalsRepository: GoalsRepository,
    modifier: Modifier = Modifier,
) {
    val diary by viewModel.state.collectAsState()
    val goalsFlow = remember(goalsRepository) { goalsRepository.observeGoals() }
    val goals by goalsFlow.collectAsState(initial = NutritionGoals())

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_nutrition_title),
                style = MaterialTheme.typography.titleLarge,
            )
            when {
                diary.isLoading -> Text(stringResource(R.string.home_nutrition_loading))
                diary.storageError != null -> Text(
                    text = diary.storageError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
                else -> {
                    val summary = summarizeDailyNutrition(diary.todayMeals, goals)
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_calories),
                        value = stringResource(
                            R.string.home_nutrition_calorie_values,
                            SpanishFormats.formatNumber(summary.consumed.caloriesKcal),
                            SpanishFormats.formatNumber(goals.caloriesKcal.toDouble()),
                        ),
                        progress = summary.consumed.caloriesKcal / goals.caloriesKcal,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_nutrition_remaining,
                            SpanishFormats.formatNumber(summary.remainingCaloriesKcal),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = when (diary.careStreakDays) {
                            0 -> stringResource(R.string.home_streak_zero)
                            1 -> stringResource(R.string.home_streak_one)
                            else -> stringResource(R.string.home_streak_many, diary.careStreakDays)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_water),
                        value = stringResource(
                            R.string.home_water_values,
                            SpanishFormats.formatNumber(diary.waterMillilitres.toDouble()),
                            SpanishFormats.formatNumber(goals.waterMillilitres.toDouble()),
                        ),
                        progress = diary.waterMillilitres.toDouble() / goals.waterMillilitres,
                    )
                    Text(
                        text = stringResource(R.string.home_meal_count, diary.todayMeals.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (diary.todayMeals.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_meals_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        diary.todayMeals.forEach { meal ->
                            Text(
                                text = stringResource(
                                    R.string.home_meal_summary,
                                    meal.name,
                                    SpanishFormats.formatNumber(meal.nutrients.caloriesKcal),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_protein),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.proteinGrams),
                            SpanishFormats.formatNumber(goals.proteinGrams),
                        ),
                        progress = summary.consumed.proteinGrams / goals.proteinGrams,
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_carbohydrates),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.carbohydratesGrams),
                            SpanishFormats.formatNumber(goals.carbohydratesGrams),
                        ),
                        progress = summary.consumed.carbohydratesGrams / goals.carbohydratesGrams,
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_fat),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.fatGrams),
                            SpanishFormats.formatNumber(goals.fatGrams),
                        ),
                        progress = summary.consumed.fatGrams / goals.fatGrams,
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionMetricRow(
    label: String,
    value: String,
    progress: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { progress.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
