package com.nutria.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.core.data.AppTheme
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.data.SettingsPreferences
import com.nutria.app.core.data.SettingsPreferencesRepository
import com.nutria.app.core.model.NutritionGoals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class ThemeChoice(
    val theme: AppTheme,
    val label: Int,
    val color: Color,
)

private val themeChoices = listOf(
    ThemeChoice(AppTheme.SAGE, R.string.settings_theme_sage, Color(0xFF537E5C)),
    ThemeChoice(AppTheme.OCEAN, R.string.settings_theme_ocean, Color(0xFF397D8A)),
    ThemeChoice(AppTheme.LAVENDER, R.string.settings_theme_lavender, Color(0xFF82659B)),
    ThemeChoice(AppTheme.CORAL, R.string.settings_theme_coral, Color(0xFFB45846)),
)

@Composable
fun SettingsScreen(
    goalsRepository: GoalsRepository,
    preferencesRepository: SettingsPreferencesRepository,
    onPreviewTheme: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var savedGoals by remember { mutableStateOf<NutritionGoals?>(null) }
    var savedPreferences by remember { mutableStateOf<SettingsPreferences?>(null) }
    var goalsLoaded by remember { mutableStateOf(false) }
    var preferencesLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var hasChanges by rememberSaveable { mutableStateOf(false) }
    var saveCompleted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var calories by rememberSaveable { mutableStateOf("") }
    var water by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbohydrates by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var selectedTheme by rememberSaveable { mutableStateOf(AppTheme.SAGE.name) }
    var serviceBaseUrl by rememberSaveable { mutableStateOf("") }
    var goalErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var serviceUrlError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(goalsRepository) {
        try {
            goalsRepository.observeGoals().collect { latest ->
                savedGoals = latest
                goalsLoaded = true
                if (!hasChanges) {
                    calories = latest.caloriesKcal.toString()
                    water = latest.waterMillilitres.toString()
                    protein = latest.proteinGrams.toPlainGoalText()
                    carbohydrates = latest.carbohydratesGrams.toPlainGoalText()
                    fat = latest.fatGrams.toPlainGoalText()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = "No se pudieron leer los objetivos de este dispositivo."
            goalsLoaded = true
        }
    }
    LaunchedEffect(preferencesRepository) {
        try {
            preferencesRepository.observeSettings().collect { latest ->
                savedPreferences = latest
                preferencesLoaded = true
                if (!hasChanges) {
                    selectedTheme = latest.theme.name
                    serviceBaseUrl = latest.serviceBaseUrl.orEmpty()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = "No se pudieron leer los ajustes de este dispositivo."
            preferencesLoaded = true
        }
    }

    val latestSavedTheme by rememberUpdatedState(savedPreferences?.theme ?: AppTheme.SAGE)
    val latestPreviewCallback by rememberUpdatedState(onPreviewTheme)
    val latestPreferencesLoaded by rememberUpdatedState(preferencesLoaded)
    DisposableEffect(Unit) {
        onDispose {
            if (!saveCompleted && latestPreferencesLoaded) {
                latestPreviewCallback(latestSavedTheme)
            }
        }
    }

    val ready = goalsLoaded && preferencesLoaded
    val selectedAppTheme = runCatching { AppTheme.valueOf(selectedTheme) }.getOrDefault(AppTheme.SAGE)
    val savedMessage = stringResource(R.string.settings_saved)
    val saveErrorMessage = stringResource(R.string.settings_save_error)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!ready) {
            Text(
                text = loadError ?: stringResource(R.string.settings_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = if (loadError == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        } else {
            loadError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingsSection(title = stringResource(R.string.settings_goals_heading)) {
                GoalTextField(
                    label = stringResource(R.string.settings_goal_calories),
                    value = calories,
                    error = goalErrors[GoalDraftValidator.CALORIES],
                    keyboardType = KeyboardType.Number,
                    onValueChange = {
                        calories = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        goalErrors = goalErrors - GoalDraftValidator.CALORIES
                    },
                )
                GoalTextField(
                    label = stringResource(R.string.settings_goal_water),
                    value = water,
                    error = goalErrors[GoalDraftValidator.WATER],
                    supportingText = stringResource(R.string.settings_goal_water_hint),
                    keyboardType = KeyboardType.Number,
                    onValueChange = {
                        water = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        goalErrors = goalErrors - GoalDraftValidator.WATER
                    },
                )
                GoalTextField(
                    label = stringResource(R.string.settings_goal_protein),
                    value = protein,
                    error = goalErrors[GoalDraftValidator.PROTEIN],
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = {
                        protein = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        goalErrors = goalErrors - GoalDraftValidator.PROTEIN
                    },
                )
                GoalTextField(
                    label = stringResource(R.string.settings_goal_carbohydrates),
                    value = carbohydrates,
                    error = goalErrors[GoalDraftValidator.CARBOHYDRATES],
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = {
                        carbohydrates = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        goalErrors = goalErrors - GoalDraftValidator.CARBOHYDRATES
                    },
                )
                GoalTextField(
                    label = stringResource(R.string.settings_goal_fat),
                    value = fat,
                    error = goalErrors[GoalDraftValidator.FAT],
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = {
                        fat = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        goalErrors = goalErrors - GoalDraftValidator.FAT
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_theme_heading)) {
                Text(
                    text = stringResource(R.string.settings_theme_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                themeChoices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAppTheme == choice.theme,
                                role = Role.RadioButton,
                                onClick = {
                                    selectedTheme = choice.theme.name
                                    hasChanges = true
                                    saveCompleted = false
                                    statusMessage = null
                                    onPreviewTheme(choice.theme)
                                },
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedAppTheme == choice.theme,
                            onClick = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Spacer(
                            Modifier
                                .size(20.dp)
                                .background(choice.color, CircleShape),
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = stringResource(choice.label),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_privacy_heading)) {
                DisclosureText(
                    title = stringResource(R.string.settings_privacy_local_title),
                    body = stringResource(R.string.settings_privacy_local_body),
                )
                DisclosureText(
                    title = stringResource(R.string.settings_privacy_health_title),
                    body = stringResource(R.string.settings_privacy_health_body),
                )
                DisclosureText(
                    title = stringResource(R.string.settings_privacy_ble_title),
                    body = stringResource(R.string.settings_privacy_ble_body),
                )
                DisclosureText(
                    title = stringResource(R.string.settings_privacy_photo_title),
                    body = stringResource(R.string.settings_privacy_photo_body),
                )
                DisclosureText(
                    title = stringResource(R.string.settings_privacy_search_title),
                    body = stringResource(R.string.settings_privacy_search_body),
                )
            }

            SettingsSection(title = stringResource(R.string.settings_service_heading)) {
                Text(
                    text = stringResource(R.string.settings_service_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = serviceBaseUrl,
                    onValueChange = {
                        serviceBaseUrl = it
                        hasChanges = true
                        saveCompleted = false
                        statusMessage = null
                        serviceUrlError = null
                    },
                    label = { Text(stringResource(R.string.settings_service_url_label)) },
                    placeholder = { Text(stringResource(R.string.settings_service_url_placeholder)) },
                    supportingText = {
                        val error = serviceUrlError
                        Text(
                            text = error ?: stringResource(R.string.settings_service_url_hint),
                            color = if (error == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    },
                    isError = serviceUrlError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = if (message == savedMessage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        savedGoals?.let { latest ->
                            calories = latest.caloriesKcal.toString()
                            water = latest.waterMillilitres.toString()
                            protein = latest.proteinGrams.toPlainGoalText()
                            carbohydrates = latest.carbohydratesGrams.toPlainGoalText()
                            fat = latest.fatGrams.toPlainGoalText()
                        }
                        savedPreferences?.let { latest ->
                            selectedTheme = latest.theme.name
                            serviceBaseUrl = latest.serviceBaseUrl.orEmpty()
                            onPreviewTheme(latest.theme)
                        }
                        goalErrors = emptyMap()
                        serviceUrlError = null
                        statusMessage = null
                        hasChanges = false
                        saveCompleted = false
                    },
                    enabled = ready && loadError == null && !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
                Button(
                    onClick = {
                        val goalResult = GoalDraftValidator.validate(
                            NutritionGoalDraft(calories, water, protein, carbohydrates, fat),
                        )
                        goalErrors = goalResult.errors
                        val urlResult = ServiceUrlValidator.validate(
                            rawValue = serviceBaseUrl,
                            allowLocalHttpForDebug = com.nutria.app.BuildConfig.DEBUG,
                        )
                        when (urlResult) {
                            is ServiceUrlValidation.Invalid -> {
                                serviceUrlError = urlResult.message
                            }
                            is ServiceUrlValidation.Valid -> {
                                serviceUrlError = null
                                if (goalResult.goals != null) {
                                    isSaving = true
                                    statusMessage = null
                                    scope.launch {
                                        try {
                                            goalsRepository.saveGoals(goalResult.goals)
                                            preferencesRepository.saveSettings(
                                                SettingsPreferences(
                                                    theme = selectedAppTheme,
                                                    serviceBaseUrl = urlResult.normalizedUrl,
                                                ),
                                            )
                                            savedGoals = goalResult.goals
                                            savedPreferences = SettingsPreferences(
                                                theme = selectedAppTheme,
                                                serviceBaseUrl = urlResult.normalizedUrl,
                                            )
                                            hasChanges = false
                                            saveCompleted = true
                                            onPreviewTheme(selectedAppTheme)
                                            statusMessage = savedMessage
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            statusMessage = saveErrorMessage
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = ready && loadError == null && !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            if (isSaving) R.string.settings_saving else R.string.settings_save,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun GoalTextField(
    label: String,
    value: String,
    error: String?,
    keyboardType: KeyboardType,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = {
            val hint = error ?: supportingText
            if (hint != null) {
                Text(
                    text = hint,
                    color = if (error == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DisclosureText(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Double.toPlainGoalText(): String = toString().removeSuffix(".0")
