package com.grcarmenaty.lifegame.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.domain.VoicePreset

@Composable
fun VoicePresetPicker(
    selected: VoicePreset,
    onSelect: (VoicePreset) -> Unit,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
) {
    // When collapsible, default to collapsed (only the chosen preset visible).
    // When not collapsible (summoning ritual), always show every preset.
    var expanded by rememberSaveable { mutableStateOf(!collapsible) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!expanded) {
            // Collapsed: only the chosen preset is rendered, in its highlighted
            // form. The toggle below brings the full list back.
            PresetCard(preset = selected, isSelected = true, onClick = null)
        } else {
            // Expanded: every preset in its natural enum order, with the
            // current selection highlighted IN PLACE. Earlier the picker
            // re-pinned the selected card to the top, which read as the
            // list jumping back to the start each time you picked from
            // further down. Keep order stable; let the highlight move.
            VoicePreset.entries.forEach { preset ->
                val isSel = preset == selected
                PresetCard(
                    preset = preset,
                    isSelected = isSel,
                    // Always clickable — even the already-selected card —
                    // so a tap can drive navigation (the summoning ritual
                    // advances on pick, including re-picking the default).
                    onClick = {
                        onSelect(preset)
                        if (collapsible) expanded = false
                    },
                )
            }
        }

        if (collapsible) {
            Spacer(Modifier.height(2.dp))
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (expanded) "Show only selected" else "Show all archetypes")
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: VoicePreset,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surface
    val titleColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface
    val bodyColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val cardModifier = Modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "“${preset.sample}”",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = bodyColor,
            )
        }
    }
    if (onClick == null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = cardModifier,
        ) { content() }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = cardModifier,
            onClick = onClick,
        ) { content() }
    }
}
