package com.grcarmenaty.lifegame.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.data.entities.MinorQuest

/**
 * Cadence picker. Three controls in a stacked column:
 *
 *   - **Period chip** (One-off / Daily / Weekly / Monthly) — always shown.
 *   - **Count stepper** (− N +) — shown when period ≠ ONE_OFF and the
 *     weekly day-chips are not in use (with days selected the count is
 *     implicitly len(days)).
 *   - **Day chips** (Mon-Sun) — shown only when period = WEEKLY.
 *
 * The selector communicates state out via three callbacks so the host
 * can store it in whatever shape they want (parallel arrays in the
 * summoning ritual, single state object in the AddMinor dialog).
 *
 * Count is clamped to 1..9; the stepper hides the buttons at the
 * boundaries instead of greying them out so the layout stays tight.
 */
@Composable
fun CadencePicker(
    cadence: String,
    cadenceCount: Int,
    cadenceDays: Set<Int>,
    onCadenceChange: (String) -> Unit,
    onCadenceCountChange: (Int) -> Unit,
    onCadenceDaysChange: (Set<Int>) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeriodChip(
                cadence = cadence,
                enabled = enabled,
                onPick = { onCadenceChange(it) },
            )
            if (showsCountStepper(cadence, cadenceDays)) {
                CountStepper(
                    count = cadenceCount,
                    enabled = enabled,
                    onChange = onCadenceCountChange,
                )
            }
        }
        if (cadence == MinorQuest.CADENCE_WEEKLY) {
            DayChipRow(
                selected = cadenceDays,
                enabled = enabled,
                onToggle = { day ->
                    val next = if (day in cadenceDays) cadenceDays - day else cadenceDays + day
                    onCadenceDaysChange(next)
                },
            )
        }
    }
}

@Composable
private fun PeriodChip(
    cadence: String,
    enabled: Boolean,
    onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            label = { Text(MinorQuest.cadenceLabel(cadence)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MinorQuest.ALL_CADENCES.forEach { option ->
                DropdownMenuItem(
                    text = { Text(MinorQuest.cadenceLabel(option)) },
                    onClick = {
                        onPick(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CountStepper(
    count: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    val safe = count.coerceIn(1, 9)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (safe > 1) onChange(safe - 1) },
            enabled = enabled && safe > 1,
        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            text = "$safe×",
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(
            onClick = { if (safe < 9) onChange(safe + 1) },
            enabled = enabled && safe < 9,
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun DayChipRow(
    selected: Set<Int>,
    enabled: Boolean,
    onToggle: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MinorQuest.DAY_OF_WEEK_ORDER.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { if (enabled) onToggle(day) },
                enabled = enabled,
                label = { Text(MinorQuest.dayShortName(day).first().toString()) },
                colors = FilterChipDefaults.filterChipColors(),
                modifier = Modifier.padding(end = 0.dp),
            )
        }
    }
}

/**
 * Don't show the count when:
 *  - ONE_OFF — there's nothing to repeat
 *  - WEEKLY with at least one day chip on — the count is len(days)
 *    implicitly and a separate stepper would just confuse the model.
 */
private fun showsCountStepper(cadence: String, days: Set<Int>): Boolean = when {
    cadence == MinorQuest.CADENCE_ONE_OFF -> false
    cadence == MinorQuest.CADENCE_WEEKLY && days.isNotEmpty() -> false
    else -> true
}
