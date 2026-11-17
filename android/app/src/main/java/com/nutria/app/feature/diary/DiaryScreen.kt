package com.nutria.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutria.app.app.SpanishFormats
import com.nutria.app.R
import com.nutria.app.core.model.FoodUnit
import com.nutria.app.core.model.MealEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCurrentDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("100") }
    var unitName by rememberSaveable { mutableStateOf(FoodUnit.GRAMS.name) }
    var gramsPerPortion by rememberSaveable { mutableStateOf("150") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbohydrates by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<MealEntry?>(null) }

    val unit = remember(unitName) { FoodUnit.valueOf(unitName) }
    val draft = FoodEntryDraft(
        name = name,
        quantity = quantity,
        unit = unit,
        gramsPerPortion = gramsPerPortion,
        caloriesPer100Grams = calories,
        proteinPer100Grams = protein,
        carbohydratesPer100Grams = carbohydrates,
        fatPer100Grams = fat,
    )
    val validation = remember(draft) { FoodEntryValidator.validate(draft) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Registra una comida para el ${SpanishFormats.formatDate(state.date)}.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Añadir comida", style = MaterialTheme.typography.titleLarge)
                IngredientSearchCard(
                    onProductSelected = { product ->
                        val selectedDraft = draft.withIngredientProduct(product)
                        name = selectedDraft.name
                        calories = selectedDraft.caloriesPer100Grams
                        protein = selectedDraft.proteinPer100Grams
                        carbohydrates = selectedDraft.carbohydratesPer100Grams
                        fat = selectedDraft.fatPer100Grams
                    },
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Alimento") },
                    singleLine = true,
                    isError = attemptedSubmit && validation.errors.containsKey("name"),
                    supportingText = {
                        if (attemptedSubmit) validation.errors["name"]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = attemptedSubmit && validation.errors.containsKey("quantity"),
                    supportingText = {
                        if (attemptedSubmit) validation.errors["quantity"]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Medida", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodUnit.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unitName = option.name },
                            label = { Text(option.label()) },
                        )
                    }
                }
                if (unit == FoodUnit.PORTIONS) {
                    OutlinedTextField(
                        value = gramsPerPortion,
                        onValueChange = { gramsPerPortion = it },
                        label = { Text("Gramos por porción") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = attemptedSubmit && validation.errors.containsKey("gramsPerPortion"),
                        supportingText = {
                            if (attemptedSubmit) validation.errors["gramsPerPortion"]?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = when (unit) {
                        FoodUnit.GRAMS -> "Los nutrientes se indican por cada 100 g."
                        FoodUnit.OUNCES -> "Las onzas se convierten con 1 oz = 28,3495 g."
                        FoodUnit.PORTIONS -> "El peso total es la cantidad de porciones por sus gramos por porción."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Valores nutricionales por 100 g", style = MaterialTheme.typography.titleMedium)
                FoodNumberField(
                    label = "Calorías (kcal)",
                    value = calories,
                    error = if (attemptedSubmit) validation.errors["calories"] else null,
                    onValueChange = { calories = it },
                )
                FoodNumberField(
                    label = "Proteínas (g)",
                    value = protein,
                    error = if (attemptedSubmit) validation.errors["protein"] else null,
                    onValueChange = { protein = it },
                )
                FoodNumberField(
                    label = "Carbohidratos (g)",
                    value = carbohydrates,
                    error = if (attemptedSubmit) validation.errors["carbohydrates"] else null,
                    onValueChange = { carbohydrates = it },
                )
                FoodNumberField(
                    label = "Grasas (g)",
                    value = fat,
                    error = if (attemptedSubmit) validation.errors["fat"] else null,
                    onValueChange = { fat = it },
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = validation.entry?.let { entry ->
                                "Vista previa: ${SpanishFormats.formatNumber(entry.edibleGrams)} gramos, " +
                                    "${SpanishFormats.formatNumber(entry.nutrients.roundedForDisplay().caloriesKcal)} kilocalorías"
                            } ?: "Completa los datos para calcular los nutrientes."
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Vista previa para tu cantidad", style = MaterialTheme.typography.labelLarge)
                        val preview = validation.entry
                        if (preview == null) {
                            Text(
                                "Completa el alimento, la cantidad y los cuatro valores nutricionales.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text("Cantidad equivalente: ${SpanishFormats.formatNumber(preview.edibleGrams)} g")
                            Text(
                                "${display(preview.nutrients.roundedForDisplay().caloriesKcal)} kcal · " +
                                    "P ${display(preview.nutrients.roundedForDisplay().proteinGrams)} g · " +
                                    "C ${display(preview.nutrients.roundedForDisplay().carbohydratesGrams)} g · " +
                                    "G ${display(preview.nutrients.roundedForDisplay().fatGrams)} g",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        attemptedSubmit = true
                        val entry = validation.entry ?: return@Button
                        scope.launch {
                            if (viewModel.addFood(entry)) {
                                name = ""
                                quantity = "100"
                                unitName = FoodUnit.GRAMS.name
                                gramsPerPortion = "150"
                                calories = ""
                                protein = ""
                                carbohydrates = ""
                                fat = ""
                                attemptedSubmit = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Añadir al diario")
                }
                StatusMessage(
                    error = state.storageError,
                    success = state.successMessage,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Historial de comidas", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Últimos 30 días. Solo puedes quitar comidas de hoy; los días anteriores son de solo lectura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val grouped = remember(state.mealHistory) {
                    state.mealHistory.groupBy { it.localDate }
                }
                val storageError = state.storageError
                if (state.isLoading) {
                    Text("Cargando el diario privado de este dispositivo…")
                } else if (storageError != null) {
                    storageError.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                } else if (grouped.isEmpty()) {
                    Text("Todavía no hay comidas registradas en los últimos 30 días.")
                } else {
                    grouped.forEach { (date, meals) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                dateLabel(date, state.date),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            meals.forEach { meal ->
                                MealHistoryEntry(
                                    meal = meal,
                                    isEditable = date == state.date,
                                    onRemove = { pendingRemoval = meal },
                                )
                            }
                            if (date != state.date) {
                                Text(
                                    "Solo lectura",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRemoval?.let { meal ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Quitar comida") },
            text = { Text("¿Quieres quitar «${meal.name}» del diario de hoy?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        scope.launch { viewModel.removeMeal(meal) }
                    },
                ) { Text("Quitar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
fun WaterLogCard(viewModel: DiaryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmClearToday by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCurrentDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val storageError = state.storageError
            Text("Agua de hoy", style = MaterialTheme.typography.titleLarge)
            when {
                state.isLoading -> Text("Cargando el diario privado…")
                storageError != null -> storageError.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Text("${SpanishFormats.formatNumber(state.waterMillilitres.toDouble())} ml")
            }
            if (!state.isLoading && storageError == null) {
                Text(
                    "Vasos completos: ${minOf(8, state.waterMillilitres / 250)} de 8 (250 ml cada uno).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "${minOf(8, state.waterMillilitres / 250)} de 8 vasos completos"
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(8) { index ->
                        Text(
                            text = if (index < state.waterMillilitres / 250) "●" else "○",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { viewModel.addWater(150) } }) {
                    Text("+150 ml")
                }
                Button(onClick = { scope.launch { viewModel.addWater(250) } }) {
                    Text("+250 ml")
                }
            }
            TextButton(
                enabled = state.canUndoWater,
                onClick = { scope.launch { viewModel.undoLatestWater() } },
                modifier = Modifier.height(48.dp),
            ) {
                Text("Deshacer última toma")
            }
            TextButton(
                enabled = state.canClearToday && !state.isLoading && storageError == null,
                onClick = { confirmClearToday = true },
                modifier = Modifier.height(48.dp),
            ) {
                Text(stringResource(R.string.diary_clear_today))
            }
            StatusMessage(error = state.storageError, success = state.successMessage)
        }
    }

    if (confirmClearToday) {
        AlertDialog(
            onDismissRequest = { confirmClearToday = false },
            title = { Text(stringResource(R.string.diary_clear_today_title)) },
            text = { Text(stringResource(R.string.diary_clear_today_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearToday = false
                        scope.launch { viewModel.clearTodayIntake() }
                    },
                ) {
                    Text(stringResource(R.string.diary_clear_today_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearToday = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun FoodNumberField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MealHistoryEntry(
    meal: MealEntry,
    isEditable: Boolean,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(meal.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (isEditable) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRemove, modifier = Modifier.height(48.dp)) {
                    Text("Quitar")
                }
            }
        }
        val displayNutrients = meal.nutrients.roundedForDisplay()
        Text(
            "${display(displayNutrients.caloriesKcal)} kcal · " +
                "P ${display(displayNutrients.proteinGrams)} g · " +
                "C ${display(displayNutrients.carbohydratesGrams)} g · " +
                "G ${display(displayNutrients.fatGrams)} g",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatusMessage(error: String?, success: String?) {
    when {
        error != null -> Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        success != null -> Text(
            success,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun FoodUnit.label(): String = when (this) {
    FoodUnit.GRAMS -> "g"
    FoodUnit.OUNCES -> "oz"
    FoodUnit.PORTIONS -> "Porción"
}

private fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoy"
    today.minusDays(1) -> "Ayer"
    else -> DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH_LOCALE)
        .format(date)
        .replaceFirstChar { it.titlecase(SPANISH_LOCALE) }
}

private fun display(value: Double): String = SpanishFormats.formatNumber(value)

private val SPANISH_LOCALE: Locale = Locale.forLanguageTag("es-ES")
