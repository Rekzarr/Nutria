package com.nutria.app.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate

/** Keeps the week selector, Health Connect steps, and local BLE workouts on one screen. */
@Composable
fun ActivityFeatureScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val today = LocalDate.now()
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDate = remember(selectedDateText) { LocalDate.parse(selectedDateText) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActivityWeekSelector(
            selectedDate = selectedDate,
            today = today,
            onSelectDate = { selectedDateText = it.toString() },
        )
        HealthConnectStepsFeature(
            lifecycleOwner = lifecycleOwner,
            compact = false,
            localDate = selectedDate,
        )
        WorkoutFeatureScreen(
            selectedDate = selectedDate,
        )
    }
}
