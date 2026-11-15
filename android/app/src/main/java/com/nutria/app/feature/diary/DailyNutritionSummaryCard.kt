package com.nutria.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
                    )
                    Text(
                        text = stringResource(
                            R.string.home_nutrition_remaining,
                            SpanishFormats.formatNumber(summary.remainingCaloriesKcal),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_protein),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.proteinGrams),
                            SpanishFormats.formatNumber(goals.proteinGrams),
                        ),
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_carbohydrates),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.carbohydratesGrams),
                            SpanishFormats.formatNumber(goals.carbohydratesGrams),
                        ),
                    )
                    NutritionMetricRow(
                        label = stringResource(R.string.home_nutrition_fat),
                        value = stringResource(
                            R.string.home_nutrition_gram_values,
                            SpanishFormats.formatNumber(summary.consumed.fatGrams),
                            SpanishFormats.formatNumber(goals.fatGrams),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
