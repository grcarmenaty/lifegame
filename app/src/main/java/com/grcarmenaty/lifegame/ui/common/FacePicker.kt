package com.grcarmenaty.lifegame.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.domain.DaemonFaceCatalog
import com.grcarmenaty.lifegame.domain.LifeTheme
import com.grcarmenaty.lifegame.domain.VoicePreset
import androidx.compose.foundation.clickable

/**
 * Horizontal chooser of daemon faces. The three faces for the daemon's
 * own (archetype, theme) lead the row, followed by the archetype's
 * other faces, then every remaining face — see
 * [DaemonFaceCatalog.orderedFor]. A [LazyRow] keeps this cheap even
 * though the full set is ~630 vectors: only visible items compose, and
 * a horizontal list nests safely inside the verticalScroll screens that
 * host it.
 *
 * [selectedRes] is the currently-highlighted drawable; [onSelect]
 * reports both the stable face name (for persistence) and the resId.
 */
@Composable
fun FacePicker(
    preset: VoicePreset,
    theme: LifeTheme?,
    selectedRes: Int,
    onSelect: (faceName: String, res: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val faces = DaemonFaceCatalog.orderedFor(preset, theme)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(faces, key = { it }) { res ->
            val isSelected = res == selectedRes
            val border = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outlineVariant
            val container = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
            val tint = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(container)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(DaemonFaceCatalog.nameForRes(res) ?: "", res) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(res),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
