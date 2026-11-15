package com.nutria.app.feature.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import com.nutria.app.core.data.CycleRepository
import com.nutria.app.core.model.CycleDateInput
import com.nutria.app.core.model.CycleDayEntry
import com.nutria.app.core.model.CycleEstimate
import com.nutria.app.core.model.CycleFlow
import com.nutria.app.core.model.CycleMood
import com.nutria.app.core.model.CyclePhase
import com.nutria.app.core.model.CycleRules
import com.nutria.app.core.model.CycleSettings
import com.nutria.app.core.model.PeriodRecord
import com.nutria.app.core.model.PeriodValidationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun CycleScreen(
    repository: CycleRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val storageUnavailableMessage = stringResource(R.string.cycle_storage_unavailable)
    val invalidSettingsMessage = stringResource(R.string.cycle_settings_invalid)
    val saveFailedMessage = stringResource(R.string.cycle_save_failed)
    val invalidDateMessage = stringResource(R.string.cycle_date_invalid)
    val periodValidationMessages = mapOf(
        PeriodValidationError.START_IN_FUTURE to stringResource(R.string.cycle_error_start_future),
        PeriodValidationError.END_BEFORE_START to stringResource(R.string.cycle_error_end_before_start),
        PeriodValidationError.END_IN_FUTURE to stringResource(R.string.cycle_error_end_future),
        PeriodValidationError.OVERLAPS_EXISTING_PERIOD to stringResource(R.string.cycle_error_overlap),
    )

    var settings by remember { mutableStateOf(CycleSettings()) }
    var periods by remember { mutableStateOf(emptyList<PeriodRecord>()) }
    var recentEntries by remember { mutableStateOf(emptyList<CycleDayEntry>()) }
    var storageMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsLoaded by remember { mutableStateOf(false) }
    var periodsLoaded by remember { mutableStateOf(false) }
    var entriesLoaded by remember { mutableStateOf(false) }

    // Keep reproductive-health form values out of Android saved-instance-state bundles.
    var cycleLengthText by remember { mutableStateOf(CycleSettings.DEFAULT_CYCLE_LENGTH_DAYS.toString()) }
    var periodLengthText by remember { mutableStateOf(CycleSettings.DEFAULT_PERIOD_LENGTH_DAYS.toString()) }
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var editingPeriodId by remember { mutableStateOf<String?>(null) }
    var periodToDelete by remember { mutableStateOf<PeriodRecord?>(null) }
    var journalLoaded by remember(repository, today) { mutableStateOf(false) }
    var flow by remember { mutableStateOf(CycleFlow.NONE) }
    var pain by remember { mutableStateOf(0) }
    var mood by remember { mutableStateOf(CycleMood.STABLE) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(repository, today, storageUnavailableMessage) {
        launch {
            repository.observeSettings()
                .catch {
                    storageMessage = storageUnavailableMessage
                    settingsLoaded = true
                }
                .collect { saved ->
                    settings = saved
                    cycleLengthText = saved.cycleLengthDays.toString()
                    periodLengthText = saved.periodLengthDays.toString()
                    settingsLoaded = true
                }
        }
        launch {
            repository.observePeriods()
                .catch {
                    storageMessage = storageUnavailableMessage
                    periodsLoaded = true
                }
                .collect {
                    periods = it
                    periodsLoaded = true
                }
        }
        launch {
            repository.observeRecentEntries(today, RECENT_DAYS)
                .catch {
                    storageMessage = storageUnavailableMessage
                    entriesLoaded = true
                }
                .collect { entries ->
                    recentEntries = entries
                    if (!journalLoaded) {
                        entries.firstOrNull { it.date == today }?.let { savedEntry ->
                            flow = savedEntry.flow
                            pain = savedEntry.pain
                            mood = savedEntry.mood
                            note = savedEntry.note
                        }
                        journalLoaded = true
                    }
                    entriesLoaded = true
                }
        }
    }

    val loading = !(settingsLoaded && periodsLoaded && entriesLoaded)
    val estimate = CycleRules.estimate(periods, settings, today)
    val deleteTitle = periodToDelete?.let { SpanishFormats.formatDate(it.startDate) }.orEmpty()
    val deleteConfirmation = stringResource(R.string.cycle_delete_confirmation, deleteTitle)
    val deleteAction = stringResource(R.string.cycle_delete)
    val cancelAction = stringResource(R.string.action_cancel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.cycle_screen_title), style = MaterialTheme.typography.headlineSmall)

        storageMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (loading) {
            Text(stringResource(R.string.cycle_loading))
        }

        EstimateCard(estimate = estimate, today = today)
        SettingsCard(
            cycleLengthText = cycleLengthText,
            periodLengthText = periodLengthText,
            onCycleLengthChange = { cycleLengthText = it.filter(Char::isDigit).take(2) },
            onPeriodLengthChange = { periodLengthText = it.filter(Char::isDigit).take(2) },
            onSave = {
                val cycleDays = cycleLengthText.toIntOrNull()
                val periodDays = periodLengthText.toIntOrNull()
                if (cycleDays == null || periodDays == null ||
                    !CycleRules.validateSettings(cycleDays, periodDays)
                ) {
                    storageMessage = invalidSettingsMessage
                } else {
                    scope.launch {
                        try {
                            repository.saveSettings(CycleSettings(cycleDays, periodDays))
                            storageMessage = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            storageMessage = saveFailedMessage
                        }
                    }
                }
            },
        )

        PeriodFormCard(
            startDateText = startDateText,
            endDateText = endDateText,
            isEditing = editingPeriodId != null,
            onStartDateChange = { startDateText = it.take(MAX_DATE_INPUT_LENGTH) },
            onEndDateChange = { endDateText = it.take(MAX_DATE_INPUT_LENGTH) },
            onCancelEdit = {
                editingPeriodId = null
                startDateText = ""
                endDateText = ""
            },
            onSave = {
                val startDate = CycleDateInput.parse(startDateText)
                val endDate = if (endDateText.isBlank()) null else CycleDateInput.parse(endDateText)
                if (startDate == null || (endDateText.isNotBlank() && endDate == null)) {
                    storageMessage = invalidDateMessage
                } else {
                    val periodId = editingPeriodId ?: UUID.randomUUID().toString()
                    val otherPeriods = periods.filterNot { it.id == periodId }
                    val error = CycleRules.validateNewPeriod(
                        startDate = startDate,
                        endDate = endDate,
                        today = today,
                        settings = settings,
                        existingPeriods = otherPeriods,
                    )
                    if (error != null) {
                        storageMessage = periodValidationMessages.getValue(error)
                    } else {
                        val record = PeriodRecord(periodId, startDate, endDate)
                        scope.launch {
                            try {
                                repository.savePeriod(record)
                                editingPeriodId = null
                                startDateText = ""
                                endDateText = ""
                                storageMessage = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                storageMessage = saveFailedMessage
                            }
                        }
                    }
                }
            },
        )

        PeriodHistoryCard(
            periods = periods,
            today = today,
            onEdit = { period ->
                editingPeriodId = period.id
                startDateText = SpanishFormats.formatDate(period.startDate)
                endDateText = period.endDate?.let(SpanishFormats::formatDate).orEmpty()
                storageMessage = null
            },
            onFinalize = { period ->
                val otherPeriods = periods.filterNot { it.id == period.id }
                val error = CycleRules.validateNewPeriod(
                    startDate = period.startDate,
                    endDate = today,
                    today = today,
                    settings = settings,
                    existingPeriods = otherPeriods,
                )
                if (error != null) {
                    storageMessage = periodValidationMessages.getValue(error)
                } else {
                    scope.launch {
                        try {
                            repository.savePeriod(period.copy(endDate = today))
                            storageMessage = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            storageMessage = saveFailedMessage
                        }
                    }
                }
            },
            onDelete = { periodToDelete = it },
        )

        JournalCard(
            today = today,
            flow = flow,
            pain = pain,
            mood = mood,
            note = note,
            onFlowChange = { flow = it },
            onPainChange = { pain = it },
            onMoodChange = { mood = it },
            onNoteChange = { if (it.length <= CycleDayEntry.MAX_NOTE_LENGTH) note = it },
            enabled = entriesLoaded,
            onSave = {
                scope.launch {
                    try {
                        repository.saveDayEntry(
                            CycleDayEntry(
                                date = today,
                                flow = flow,
                                pain = pain,
                                mood = mood,
                                note = CycleRules.normalizedNote(note),
                            ),
                        )
                        storageMessage = null
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        storageMessage = saveFailedMessage
                    }
                }
            },
        )

        RecentJournalCard(entries = recentEntries)
    }

    periodToDelete?.let { period ->
        AlertDialog(
            onDismissRequest = { periodToDelete = null },
            title = { Text(stringResource(R.string.cycle_delete_title)) },
            text = { Text(deleteConfirmation) },
            confirmButton = {
                TextButton(
                    onClick = {
                        periodToDelete = null
                        scope.launch {
                            try {
                                repository.deletePeriod(period.id)
                                if (editingPeriodId == period.id) {
                                    editingPeriodId = null
                                    startDateText = ""
                                    endDateText = ""
                                }
                                storageMessage = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                storageMessage = saveFailedMessage
                            }
                        }
                    },
                ) { Text(deleteAction) }
            },
            dismissButton = {
                TextButton(onClick = { periodToDelete = null }) { Text(cancelAction) }
            },
        )
    }
}

@Composable
private fun EstimateCard(estimate: CycleEstimate, today: LocalDate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.cycle_estimate_title), style = MaterialTheme.typography.titleMedium)
            when (estimate) {
                CycleEstimate.Unknown -> {
                    Text(stringResource(R.string.cycle_unknown_state))
                    Text(stringResource(R.string.cycle_unknown_help))
                }
                is CycleEstimate.Known -> {
                    Text(stringResource(R.string.cycle_cycle_day, estimate.cycleDay))
                    Text(stringResource(estimate.phase.stringResourceId()))
                    val expectedStart = SpanishFormats.formatDate(estimate.expectedNextStart)
                    when {
                        estimate.isOngoing -> {
                            Text(stringResource(R.string.cycle_next_start_estimate, expectedStart))
                        }
                        estimate.daysLate == 1L -> {
                            Text(stringResource(R.string.cycle_one_day_late, expectedStart))
                        }
                        estimate.daysLate > 1L -> {
                            Text(stringResource(R.string.cycle_days_late, estimate.daysLate, expectedStart))
                        }
                        estimate.expectedNextStart == today -> {
                            Text(stringResource(R.string.cycle_expected_today))
                        }
                        ChronoUnit.DAYS.between(today, estimate.expectedNextStart) == 1L -> {
                            Text(stringResource(R.string.cycle_next_start_in_one_day, expectedStart))
                        }
                        else -> {
                            val daysUntil = ChronoUnit.DAYS.between(today, estimate.expectedNextStart)
                            Text(
                                stringResource(
                                    R.string.cycle_next_start_in_days,
                                    expectedStart,
                                    daysUntil,
                                ),
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.cycle_estimate_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    cycleLengthText: String,
    periodLengthText: String,
    onCycleLengthChange: (String) -> Unit,
    onPeriodLengthChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.cycle_settings_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = cycleLengthText,
                onValueChange = onCycleLengthChange,
                label = { Text(stringResource(R.string.cycle_average_length)) },
                suffix = { Text(stringResource(R.string.cycle_days_unit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = periodLengthText,
                onValueChange = onPeriodLengthChange,
                label = { Text(stringResource(R.string.cycle_average_period)) },
                suffix = { Text(stringResource(R.string.cycle_days_unit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.cycle_settings_ranges), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_save_settings))
            }
        }
    }
}

@Composable
private fun PeriodFormCard(
    startDateText: String,
    endDateText: String,
    isEditing: Boolean,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(if (isEditing) R.string.cycle_correct_period else R.string.cycle_add_period),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = startDateText,
                onValueChange = onStartDateChange,
                label = { Text(stringResource(R.string.cycle_start_date)) },
                placeholder = { Text(stringResource(R.string.cycle_date_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = endDateText,
                onValueChange = onEndDateChange,
                label = { Text(stringResource(R.string.cycle_end_date_optional)) },
                placeholder = { Text(stringResource(R.string.cycle_date_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.cycle_period_date_help), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (isEditing) R.string.cycle_save_correction else R.string.cycle_save_period))
                }
                if (isEditing) {
                    OutlinedButton(onClick = onCancelEdit, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodHistoryCard(
    periods: List<PeriodRecord>,
    today: LocalDate,
    onEdit: (PeriodRecord) -> Unit,
    onFinalize: (PeriodRecord) -> Unit,
    onDelete: (PeriodRecord) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.cycle_history_title), style = MaterialTheme.typography.titleMedium)
            if (periods.isEmpty()) {
                Text(stringResource(R.string.cycle_history_empty))
            } else {
                periods.forEach { period ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(
                                R.string.cycle_period_summary,
                                SpanishFormats.formatDate(period.startDate),
                                period.endDate?.let(SpanishFormats::formatDate)
                                    ?: stringResource(R.string.cycle_open_period),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        period.endDate?.let { end ->
                            val duration = ChronoUnit.DAYS.between(period.startDate, end) + 1
                            Text(stringResource(R.string.cycle_period_duration, duration))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onEdit(period) }) {
                                Text(stringResource(R.string.cycle_correct))
                            }
                            if (period.endDate == null) {
                                TextButton(
                                    onClick = { onFinalize(period) },
                                    enabled = period.startDate <= today,
                                ) {
                                    Text(stringResource(R.string.cycle_finalize_today))
                                }
                            }
                            TextButton(onClick = { onDelete(period) }) {
                                Text(stringResource(R.string.cycle_delete))
                            }
                        }
                    }
                    if (period != periods.last()) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalCard(
    today: LocalDate,
    flow: CycleFlow,
    pain: Int,
    mood: CycleMood,
    note: String,
    onFlowChange: (CycleFlow) -> Unit,
    onPainChange: (Int) -> Unit,
    onMoodChange: (CycleMood) -> Unit,
    onNoteChange: (String) -> Unit,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.cycle_journal_title), style = MaterialTheme.typography.titleMedium)
            Text(SpanishFormats.formatDate(today), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.cycle_flow_label))
            ChoiceList(
                values = CycleFlow.entries,
                selected = flow,
                label = { stringResource(it.stringResourceId()) },
                onSelected = onFlowChange,
                enabled = enabled,
            )
            val painDescription = stringResource(R.string.cycle_pain_label, pain)
            Text(painDescription)
            Slider(
                value = pain.toFloat(),
                onValueChange = { onPainChange(it.roundToInt()) },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = painDescription },
                enabled = enabled,
            )
            Text(stringResource(R.string.cycle_mood_label))
            ChoiceList(
                values = CycleMood.entries,
                selected = mood,
                label = { stringResource(it.stringResourceId()) },
                onSelected = onMoodChange,
                enabled = enabled,
            )
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.cycle_note_label)) },
                supportingText = {
                    Text(stringResource(R.string.cycle_note_counter, note.length, CycleDayEntry.MAX_NOTE_LENGTH))
                },
                minLines = 2,
                maxLines = 4,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.cycle_private_disclosure), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_save_today))
            }
        }
    }
}

@Composable
private fun <T> ChoiceList(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun RecentJournalCard(entries: List<CycleDayEntry>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.cycle_recent_title), style = MaterialTheme.typography.titleMedium)
            if (entries.isEmpty()) {
                Text(stringResource(R.string.cycle_recent_empty))
            } else {
                entries.take(RECENT_DAYS).forEach { entry ->
                    Text(
                        stringResource(
                            R.string.cycle_journal_summary,
                            SpanishFormats.formatDate(entry.date),
                            stringResource(entry.flow.stringResourceId()),
                            entry.pain,
                            stringResource(entry.mood.stringResourceId()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (entry.note.isNotBlank()) {
                        Text(entry.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun CyclePhase.stringResourceId(): Int = when (this) {
    CyclePhase.MENSTRUAL -> R.string.cycle_phase_menstrual
    CyclePhase.FOLLICULAR -> R.string.cycle_phase_follicular
    CyclePhase.OVULATORY_WINDOW -> R.string.cycle_phase_ovulatory
    CyclePhase.LUTEAL -> R.string.cycle_phase_luteal
    CyclePhase.POSSIBLY_LATE -> R.string.cycle_phase_late
}

private fun CycleFlow.stringResourceId(): Int = when (this) {
    CycleFlow.NONE -> R.string.cycle_flow_none
    CycleFlow.SPOTTING -> R.string.cycle_flow_spotting
    CycleFlow.LIGHT -> R.string.cycle_flow_light
    CycleFlow.MEDIUM -> R.string.cycle_flow_medium
    CycleFlow.HEAVY -> R.string.cycle_flow_heavy
}

private fun CycleMood.stringResourceId(): Int = when (this) {
    CycleMood.GOOD -> R.string.cycle_mood_good
    CycleMood.STABLE -> R.string.cycle_mood_stable
    CycleMood.TIRED -> R.string.cycle_mood_tired
    CycleMood.SENSITIVE -> R.string.cycle_mood_sensitive
    CycleMood.IRRITABLE -> R.string.cycle_mood_irritable
}

private const val RECENT_DAYS = 7
private const val MAX_DATE_INPUT_LENGTH = 10
