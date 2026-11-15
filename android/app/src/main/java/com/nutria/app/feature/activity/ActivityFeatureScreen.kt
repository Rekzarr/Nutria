package com.nutria.app.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Separate Health Connect and BLE cards so the two data sources stay explicit. */
@Composable
fun ActivityFeatureScreen(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HealthConnectStepsFeature(
            lifecycleOwner = lifecycleOwner,
            compact = false,
        )
        WorkoutFeatureScreen()
    }
}
