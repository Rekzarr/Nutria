package com.nutria.app.feature.calendar

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.core.data.CalendarPreferencesRepository
import com.nutria.app.core.data.CalendarRepository
import com.nutria.app.core.data.RecipeRepository
import com.nutria.app.core.model.CalendarAssignment
import com.nutria.app.core.model.CalendarRules
import com.nutria.app.core.model.CalendarSlot
import com.nutria.app.core.model.MenuGenerationResult
import com.nutria.app.core.model.SavedRecipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MealCalendarScreen(
    calendarRepository: CalendarRepository,
    recipeRepository: RecipeRepository,
    preferencesRepository: CalendarPreferencesRepository,
    onOpenRecipes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = remember { Locale.forLanguageTag("es-ES") }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthKey) { YearMonth.parse(monthKey) }
    var recipes by remember { mutableStateOf<List<SavedRecipe>?>(null) }
    var assignments by remember { mutableStateOf<List<CalendarAssignment>>(emptyList()) }
    var breakfastEnabled by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showGenerator by remember { mutableStateOf(false) }
    var generateWeek by remember { mutableStateOf(false) }
    var selectedWeekDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var pendingAssignments by remember { mutableStateOf<List<CalendarAssignment>?>(null) }
    var pendingRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val storageErrorText = stringResource(R.string.calendar_storage_error)
    val saveErrorText = stringResource(R.string.calendar_save_error)
    val previousMonthText = stringResource(R.string.calendar_previous_month)
    val nextMonthText = stringResource(R.string.calendar_next_month)
    val daySavedText = stringResource(R.string.calendar_day_saved)
    val noRecipesText = stringResource(R.string.calendar_no_recipes)
    val missingCategoryTemplate = stringResource(R.string.calendar_missing_category)
    val weekPeriodText = stringResource(R.string.calendar_period_week_lower)
    val monthPeriodText = stringResource(R.string.calendar_period_month_lower)
    val weekSavedText = stringResource(R.string.calendar_menu_saved, weekPeriodText)
    val monthSavedText = stringResource(R.string.calendar_menu_saved, monthPeriodText)
    val weekReplaceText = stringResource(R.string.calendar_replace_confirmation, weekPeriodText)
    val monthReplaceText = stringResource(R.string.calendar_replace_confirmation, monthPeriodText)
    val slotLabels = mapOf(
        CalendarSlot.BREAKFAST to stringResource(R.string.calendar_slot_breakfast),
        CalendarSlot.MEAL to stringResource(R.string.calendar_slot_meal),
        CalendarSlot.DINNER to stringResource(R.string.calendar_slot_dinner),
    )

    LaunchedEffect(recipeRepository) {
        try {
            recipeRepository.observeRecipes().collect { recipes = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageError = storageErrorText
        }
    }

    LaunchedEffect(preferencesRepository) {
        try {
            preferencesRepository.observeBreakfastEnabled().collect { breakfastEnabled = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageError = storageErrorText
        }
    }

    LaunchedEffect(calendarRepository, month) {
        try {
            calendarRepository.observeAssignments(month.atDay(1), month.atEndOfMonth()).collect {
                assignments = it
                storageError = null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageError = storageErrorText
        }
    }

    val recipesLoaded = recipes != null
    val savedRecipes = recipes.orEmpty()
    val slots = if (breakfastEnabled) {
        listOf(CalendarSlot.BREAKFAST, CalendarSlot.MEAL, CalendarSlot.DINNER)
    } else {
        listOf(CalendarSlot.MEAL, CalendarSlot.DINNER)
    }
    val assignmentsByDate = remember(assignments, slots) {
        assignments.filter { it.slot in slots }.groupBy(CalendarAssignment::localDate)
    }
    val monthTitle = remember(month, locale) {
        month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.calendar_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { monthKey = month.minusMonths(1).toString() },
                modifier = Modifier.semantics {
                    contentDescription = previousMonthText
                },
            ) {
                Text("‹")
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = { monthKey = month.plusMonths(1).toString() },
                modifier = Modifier.semantics {
                    contentDescription = nextMonthText
                },
            ) {
                Text("›")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.calendar_add_breakfast),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = breakfastEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        try {
                            preferencesRepository.setBreakfastEnabled(enabled)
                            statusMessage = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            statusMessage = saveErrorText
                            statusIsError = true
                        }
                    }
                },
            )
        }

        if (!recipesLoaded) {
            Text(
                text = stringResource(R.string.calendar_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (savedRecipes.isEmpty()) {
            Card {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.calendar_no_recipes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.calendar_no_recipes_help),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onOpenRecipes) {
                        Text(stringResource(R.string.calendar_open_recipes))
                    }
                }
            }
        } else {
            Button(
                onClick = { showGenerator = true },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.calendar_generate_menu))
            }
            CalendarMonthGrid(
                month = month,
                assignmentsByDate = assignmentsByDate,
                locale = locale,
                onSelectDate = { selectedDate = it },
            )
            Text(
                text = stringResource(R.string.calendar_day_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (storageError != null) {
            Text(
                text = stringResource(R.string.calendar_storage_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        statusMessage?.let {
            Text(
                text = it,
                color = if (statusIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }
    }

    selectedDate?.let { date ->
        var draft by remember(date) {
            mutableStateOf(
                assignmentsByDate[date].orEmpty()
                    .associate { it.slot to it.recipeId },
            )
        }
        AlertDialog(
            onDismissRequest = { if (!isSaving) selectedDate = null },
            title = {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale))
                        .replaceFirstChar { it.titlecase(locale) },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.forEach { slot ->
                        CalendarSlotPicker(
                            slot = slot,
                            recipes = savedRecipes,
                            selectedRecipeId = draft[slot],
                            onSelect = { recipeId ->
                                draft = draft.toMutableMap().apply {
                                    if (recipeId == null) remove(slot) else put(slot, recipeId)
                                }
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                calendarRepository.replaceAssignments(
                                    fromInclusive = date,
                                    throughInclusive = date,
                                    assignments = draft.map { (slot, recipeId) ->
                                        CalendarAssignment(date, slot, recipeId)
                                    },
                                )
                                selectedDate = null
                                statusMessage = daySavedText
                                statusIsError = false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                statusMessage = saveErrorText
                                statusIsError = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.calendar_save))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { selectedDate = null },
                ) {
                    Text(stringResource(R.string.calendar_cancel))
                }
            },
        )
    }

    if (showGenerator) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showGenerator = false },
            title = { Text(stringResource(R.string.calendar_generator_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !generateWeek,
                            onClick = { generateWeek = false },
                        )
                        TextButton(onClick = { generateWeek = false }) {
                            Text(stringResource(R.string.calendar_period_month))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = generateWeek,
                            onClick = { generateWeek = true },
                        )
                        TextButton(onClick = { generateWeek = true }) {
                            Text(stringResource(R.string.calendar_period_week))
                        }
                    }
                    if (generateWeek) {
                        val weekDate = LocalDate.parse(selectedWeekDate)
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, monthIndex, day ->
                                        selectedWeekDate = LocalDate.of(
                                            year,
                                            monthIndex + 1,
                                            day,
                                        ).toString()
                                    },
                                    weekDate.year,
                                    weekDate.monthValue - 1,
                                    weekDate.dayOfMonth,
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string.calendar_week_date,
                                    weekDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)),
                                ),
                            )
                        }
                    }
                    Text(
                        text = if (generateWeek) {
                            stringResource(R.string.calendar_week_hint)
                        } else {
                            stringResource(R.string.calendar_month_hint, monthTitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val weekDate = LocalDate.parse(selectedWeekDate)
                        val targetDates = if (generateWeek) {
                            CalendarRules.weekDates(weekDate)
                        } else {
                            CalendarRules.monthDates(month)
                        }
                        val from = targetDates.first()
                        val through = targetDates.last()
                        isSaving = true
                        statusMessage = null
                        scope.launch {
                            try {
                                val existing = calendarRepository
                                    .observeAssignments(from, through)
                                    .first()
                                val result = CalendarRules.generateMenu(
                                    dates = targetDates,
                                    recipes = savedRecipes,
                                    thirdMealEnabled = breakfastEnabled,
                                    existingAssignments = existing,
                                    replacementConfirmed = false,
                                )
                                when (result) {
                                    MenuGenerationResult.NoRecipes -> {
                                        statusMessage = noRecipesText
                                        statusIsError = true
                                    }
                                    is MenuGenerationResult.MissingRequiredCategory -> {
                                        statusMessage = String.format(
                                            locale,
                                            missingCategoryTemplate,
                                            slotLabels.getValue(result.slot).lowercase(locale),
                                        )
                                        statusIsError = true
                                    }
                                    is MenuGenerationResult.ReplacementConfirmationRequired -> {
                                        val generated = CalendarRules.generateMenu(
                                            dates = targetDates,
                                            recipes = savedRecipes,
                                            thirdMealEnabled = breakfastEnabled,
                                            existingAssignments = existing,
                                            replacementConfirmed = true,
                                        ) as MenuGenerationResult.Generated
                                        pendingAssignments = generated.assignments
                                        pendingRange = from to through
                                        showGenerator = false
                                    }
                                    is MenuGenerationResult.Generated -> {
                                        calendarRepository.replaceAssignments(from, through, result.assignments)
                                        monthKey = YearMonth.from(from).toString()
                                        showGenerator = false
                                        statusMessage = if (generateWeek) weekSavedText else monthSavedText
                                        statusIsError = false
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                statusMessage = saveErrorText
                                statusIsError = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.calendar_generate))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showGenerator = false },
                ) {
                    Text(stringResource(R.string.calendar_cancel))
                }
            },
        )
    }

    val replacement = pendingAssignments
    val replacementRange = pendingRange
    if (replacement != null && replacementRange != null) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) pendingAssignments = null },
            title = { Text(stringResource(R.string.calendar_replace_title)) },
            text = {
                Text(
                    if (generateWeek) weekReplaceText else monthReplaceText,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                calendarRepository.replaceAssignments(
                                    replacementRange.first,
                                    replacementRange.second,
                                    replacement,
                                )
                                monthKey = YearMonth.from(replacementRange.first).toString()
                                pendingAssignments = null
                                pendingRange = null
                                statusMessage = if (generateWeek) weekSavedText else monthSavedText
                                statusIsError = false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                statusMessage = saveErrorText
                                statusIsError = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.calendar_replace))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        pendingAssignments = null
                        pendingRange = null
                    },
                ) {
                    Text(stringResource(R.string.calendar_cancel))
                }
            },
        )
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    assignmentsByDate: Map<LocalDate, List<CalendarAssignment>>,
    locale: Locale,
    onSelectDate: (LocalDate) -> Unit,
) {
    val weekdays = (1..7).map { day ->
        java.time.DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale)
            .replace(".", "")
            .replaceFirstChar { it.titlecase(locale) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        weekdays.forEach { weekday ->
            Text(
                text = weekday,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val cells = buildList<LocalDate?> {
        repeat(month.atDay(1).dayOfWeek.value - 1) { add(null) }
        addAll(CalendarRules.monthDates(month))
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                    } else {
                        CalendarDayCard(
                            date = date,
                            assignmentCount = assignmentsByDate[date].orEmpty().size,
                            locale = locale,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).height(52.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCard(
    date: LocalDate,
    assignmentCount: Int,
    locale: Locale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.calendar_day_accessibility,
        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)),
        assignmentCount,
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .semantics { contentDescription = description },
        colors = CardDefaults.cardColors(
            containerColor = if (date == LocalDate.now()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = if (assignmentCount == 0) " " else "● $assignmentCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CalendarSlotPicker(
    slot: CalendarSlot,
    recipes: List<SavedRecipe>,
    selectedRecipeId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = recipes.firstOrNull { it.id == selectedRecipeId }?.name
        ?: stringResource(R.string.calendar_unassigned)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(slot.labelResource()),
            style = MaterialTheme.typography.labelLarge,
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedName, maxLines = 1)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.calendar_unassigned)) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
                recipes.forEach { recipe ->
                    DropdownMenuItem(
                        text = { Text(recipe.name, maxLines = 1) },
                        onClick = {
                            onSelect(recipe.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun CalendarSlot.labelResource(): Int = when (this) {
    CalendarSlot.MEAL -> R.string.calendar_slot_meal
    CalendarSlot.DINNER -> R.string.calendar_slot_dinner
    CalendarSlot.BREAKFAST -> R.string.calendar_slot_breakfast
}
