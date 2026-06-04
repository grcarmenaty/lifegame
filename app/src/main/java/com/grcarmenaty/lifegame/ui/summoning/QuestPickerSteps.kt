package com.grcarmenaty.lifegame.ui.summoning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.LifeTheme
import com.grcarmenaty.lifegame.domain.catalog.CatalogMinor
import com.grcarmenaty.lifegame.domain.catalog.QuestCatalog
import com.grcarmenaty.lifegame.ui.common.CadencePicker

/** Step 3: choose major quests for the theme (+ custom). */
@Composable
fun MajorPickStep(
    theme: LifeTheme?,
    selection: SummonSelection,
    onChange: (SummonSelection) -> Unit,
) {
    val catalog = QuestCatalog.majorsFor(theme)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (catalog.isNotEmpty()) {
            Text(
                "Pick the quests that fit — as many as you like. We pre-tick a " +
                    "few small acts under each; adjust them next.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            catalog.forEach { major ->
                CheckRow(
                    checked = selection.has(major.templateId),
                    title = major.title,
                    subtitle = null,
                    onToggle = { onChange(selection.toggleCatalogMajor(major)) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            if (catalog.isEmpty()) "Name a quest it will ask of you."
            else "Or write your own:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        var customTitle by rememberSaveable { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Run a 10k") },
                label = { Text("Custom quest") },
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    if (customTitle.isNotBlank()) {
                        onChange(selection.addCustomMajor(customTitle.trim()))
                        customTitle = ""
                    }
                },
                enabled = customTitle.isNotBlank(),
            ) { Text("Add") }
        }
        selection.majors.filter { it.templateId == null }.forEach { m ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("• ${m.title}", modifier = Modifier.weight(1f))
                IconButton(onClick = { onChange(selection.removeMajor(m.key)) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
    }
}

/** Step 4: per chosen major, pick its repeating + one-off acts (+ custom). */
@Composable
fun MinorPickStep(
    selection: SummonSelection,
    onChange: (SummonSelection) -> Unit,
) {
    if (selection.majors.isEmpty()) {
        Text(
            "Go back and choose at least one quest first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        selection.majors.forEach { sm ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sm.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val catalogMajor = QuestCatalog.major(sm.templateId)
                if (catalogMajor != null) {
                    SubHeader("Repeating")
                    catalogMajor.repeating.forEach { cm ->
                        CheckRow(
                            checked = cm.templateId in sm.minorTemplateIds,
                            title = cm.title,
                            subtitle = cadenceText(cm),
                            onToggle = { onChange(selection.toggleMinor(sm.key, cm.templateId)) },
                        )
                    }
                    SubHeader("One-off — harder")
                    catalogMajor.oneOff.forEach { cm ->
                        CheckRow(
                            checked = cm.templateId in sm.minorTemplateIds,
                            title = cm.title,
                            subtitle = "One-off",
                            onToggle = { onChange(selection.toggleMinor(sm.key, cm.templateId)) },
                        )
                    }
                }
                sm.customMinors.forEachIndexed { i, cmn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "• ${cmn.title} · ${MinorQuest.cadenceLabel(cmn.cadence)}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = { onChange(selection.removeCustomMinor(sm.key, i)) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
                CustomMinorAdder(onAdd = { onChange(selection.addCustomMinor(sm.key, it)) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun cadenceText(cm: CatalogMinor): String =
    MinorQuest.cadenceLongLabel(cm.cadence, cm.cadenceCount, cm.cadenceDays)

@Composable
private fun CheckRow(
    checked: Boolean,
    title: String,
    subtitle: String?,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Compact expandable to author a custom act under a major. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomMinorAdder(onAdd: (SelMinor) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) { Text("+ Add your own act") }
        return
    }
    var title by rememberSaveable { mutableStateOf("") }
    var cadence by rememberSaveable { mutableStateOf(MinorQuest.CADENCE_DAILY) }
    var count by rememberSaveable { mutableStateOf(1) }
    var daysCsv by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Act") },
            placeholder = { Text("e.g. Drink a glass of water") },
        )
        CadencePicker(
            cadence = cadence,
            cadenceCount = count,
            cadenceDays = MinorQuest.parseDaysCsv(daysCsv),
            onCadenceChange = { cadence = it },
            onCadenceCountChange = { count = it },
            onCadenceDaysChange = { daysCsv = MinorQuest.encodeDays(it) ?: "" },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = false; title = "" }) { Text("Cancel") }
            OutlinedButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            SelMinor(
                                title = title.trim(),
                                cadence = cadence,
                                cadenceCount = count,
                                cadenceDaysCsv = daysCsv,
                                weight = 1,
                            )
                        )
                        title = ""
                        expanded = false
                    }
                },
                enabled = title.isNotBlank(),
            ) { Text("Add act") }
        }
    }
}
