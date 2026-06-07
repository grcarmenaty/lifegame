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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.domain.LifeTheme

/**
 * Theme chooser that mirrors [VoicePresetPicker]'s card format: each
 * [LifeTheme] is a selectable card (title + helper), with a trailing
 * "Other" card for free-text. Selecting "Other" reports `null`; the
 * caller surfaces the free-text archetype field only in that case.
 */
@Composable
fun ThemePicker(
    selectedKey: String,
    onPick: (LifeTheme?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LifeTheme.entries.forEach { theme ->
            val isSel = theme.key.equals(selectedKey, ignoreCase = true)
            // Always clickable so a tap re-picks and advances the ritual.
            ThemeCard(
                title = theme.display,
                helper = theme.helper,
                isSelected = isSel,
                onClick = { onPick(theme) },
            )
        }
        val otherSel = selectedKey.equals("OTHER", ignoreCase = true)
        ThemeCard(
            title = "Other",
            helper = "Write your own — the daemon uses its base voice.",
            isSelected = otherSel,
            onClick = { onPick(null) },
        )
    }
}

@Composable
private fun ThemeCard(
    title: String,
    helper: String,
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

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = helper,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )
        }
    }
    if (onClick == null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        ) { content() }
    }
}
