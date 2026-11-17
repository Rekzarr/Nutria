package com.nutria.app.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutria.app.R
import com.nutria.app.app.SpanishFormats
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun ActivityWeekSelector(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = ActivityWeekRules.weekStart(selectedDate)
    val previousDate = ActivityWeekRules.adjacentWeekSelection(selectedDate, -1, today)
    val nextDate = ActivityWeekRules.adjacentWeekSelection(selectedDate, 1, today)
    val locale = Locale.forLanguageTag("es-ES")
    val previousWeekLabel = stringResource(R.string.activity_previous_week)
    val nextWeekLabel = stringResource(R.string.activity_next_week)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_history_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { previousDate?.let(onSelectDate) },
                    enabled = previousDate != null,
                    modifier = Modifier.semantics {
                        contentDescription = previousWeekLabel
                    },
                ) {
                    Text("‹", fontSize = 28.sp)
                }
                Text(
                    text = stringResource(
                        R.string.activity_week_range,
                        SpanishFormats.formatDate(weekStart),
                        SpanishFormats.formatDate(weekStart.plusDays(6)),
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(
                    onClick = { nextDate?.let(onSelectDate) },
                    enabled = nextDate != null,
                    modifier = Modifier.semantics {
                        contentDescription = nextWeekLabel
                    },
                ) {
                    Text("›", fontSize = 28.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ActivityWeekRules.datesInWeek(selectedDate).forEach { date ->
                    val enabled = ActivityWeekRules.isSelectable(date, today)
                    val isSelected = date == selectedDate
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                    val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
                    val dayDescription = stringResource(
                        R.string.activity_week_day_description,
                        dayName,
                        date.dayOfMonth,
                        monthName,
                    )
                    val dayColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val dayModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        )
                        .then(
                            if (enabled) {
                                Modifier.clickable(role = Role.Button) { onSelectDate(date) }
                            } else {
                                Modifier
                            },
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = dayDescription
                            role = Role.Button
                            selected = isSelected
                            if (!enabled) disabled()
                        }

                    Column(
                        modifier = dayModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                            color = dayColor,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = dayColor,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}
