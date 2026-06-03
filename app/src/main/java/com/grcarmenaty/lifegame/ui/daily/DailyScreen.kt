package com.grcarmenaty.lifegame.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.domain.DaemonFaceSuggestions
import com.grcarmenaty.lifegame.domain.PantheonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    repository: PantheonRepository,
    onAddDaemon: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DailyViewModel = viewModel(
        factory = DailyViewModel.factory(repository)
    ),
) {
    val state by viewModel.state.collectAsState()
    val apotheosis by viewModel.apotheosis.collectAsState()
    val boonGranted by viewModel.boonGranted.collectAsState()
    val picker by viewModel.spendPicker.collectAsState()

    // Snackbar host for quest-specific completion lines.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.completionLine.collect { line ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("“$line”")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onAddDaemon) {
                        Icon(Icons.Default.Add, contentDescription = "Summon another daemon")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            if (state.daemons.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "The pantheon is quiet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    if (state.pendingLevelUps.isNotEmpty()) {
                        item {
                            LevelUpBanner(
                                pending = state.pendingLevelUps,
                                onOpen = { id ->
                                    viewModel.acknowledgeLevelUp(id)
                                    onOpenDetail(id)
                                },
                                onAcknowledge = viewModel::acknowledgeLevelUp,
                            )
                        }
                    }
                    items(state.daemons, key = { it.daemon.id }) { block ->
                        DaemonBlock(
                            block = block,
                            onComplete = viewModel::completeMinor,
                            onOpenPicker = { viewModel.openSpendPicker(block.daemon.id) },
                            onOpenDetail = { onOpenDetail(block.daemon.id) },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    apotheosis?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissApotheosis,
            confirmButton = {
                TextButton(onClick = viewModel::dismissApotheosis) { Text("Continue") }
            },
            title = {
                Text(
                    text = "${event.daemonName} — level ${event.newLevel}",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quest-specific line for library majors, else the
                    // engine line, else the voice preset template.
                    val voiceText = event.questLine
                        ?: event.engineLine
                        ?: event.voicePreset.apotheosis(event.daemonId)
                    Text(
                        text = "“$voiceText”",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "“${event.completedMajorTitle}” is complete.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        )
    }

    picker?.let { pickerState ->
        AlertDialog(
            onDismissRequest = viewModel::cancelSpendPicker,
            confirmButton = {
                TextButton(onClick = viewModel::cancelSpendPicker) { Text("Cancel") }
            },
            title = { Text("Spend a wish — ${pickerState.daemonName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pick what to claim:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    pickerState.boons.forEach { boon ->
                        BoonPickerRow(boon = boon, onSpend = { viewModel.confirmSpend(boon.id) })
                    }
                }
            }
        )
    }

    boonGranted?.let { boon ->
        AlertDialog(
            onDismissRequest = viewModel::dismissBoon,
            confirmButton = {
                TextButton(onClick = viewModel::dismissBoon) { Text("Accept") }
            },
            title = { Text("A boon is granted.") },
            text = { Text(text = "“$boon”", style = MaterialTheme.typography.bodyLarge) }
        )
    }
}

@Composable
private fun LevelUpBanner(
    pending: List<PendingLevelUp>,
    onOpen: (Long) -> Unit,
    onAcknowledge: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (pending.size == 1)
                    "${pending.first().daemonName} has grown."
                else
                    "${pending.size} relationships have grown.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open the daemon to add a new boon or grow one already there. " +
                    "Larger relationships earn larger favors.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            pending.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${p.daemonName} — level ${p.newLevel}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onOpen(p.daemonId) }) { Text("Open") }
                    TextButton(onClick = { onAcknowledge(p.daemonId) }) { Text("Dismiss") }
                }
            }
        }
    }
}

@Composable
private fun BoonPickerRow(boon: Boon, onSpend: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = boon.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${boon.count} available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSpend) { Text("Spend") }
        }
    }
}

@Composable
private fun DaemonBlock(
    block: DaemonDailyBlock,
    onComplete: (Long) -> Unit,
    onOpenPicker: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    painter = painterResource(DaemonFaceSuggestions.faceForDaemon(block.daemon)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.daemon.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "level ${block.level} · ${block.daemon.archetype}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (block.totalWishes > 0) {
                    AssistChip(
                        onClick = onOpenPicker,
                        label = { Text("${block.totalWishes} wish") },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onOpenDetail) {
                    Icon(Icons.Default.Tune, contentDescription = "Daemon details")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { block.levelProgress },
                    modifier = Modifier.weight(1f),
                )
                if (block.atMaxLevel) {
                    // Ally polish: shimmer pip at level 4 makes the
                    // decay buffer visible — continued work above the
                    // cap reads as relationship investment rather than
                    // hidden hoarding.
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .width(10.dp)
                            .padding(0.dp),
                    ) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (block.atMaxLevel)
                    "level ${block.level} · ${block.attentionPoints} attention"
                else
                    "${block.attentionPoints} attention",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "“${block.greeting}”",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            if (block.majorGroups.isEmpty()) {
                Text(
                    text = "Nothing asked of you today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    block.majorGroups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = group.majorTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            group.openMinors.forEach { minor ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    onClick = { onComplete(minor.id) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = minor.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (minor.cadence != com.grcarmenaty.lifegame.data.entities.MinorQuest.CADENCE_ONE_OFF) {
                                            val effective = minor.effectiveCount()
                                            val progress = if (effective > 1)
                                                "  (${minor.completionsThisWindow}/$effective)" else ""
                                            Text(
                                                text = com.grcarmenaty.lifegame.data.entities.MinorQuest
                                                    .cadenceLongLabel(
                                                        minor.cadence,
                                                        minor.cadenceCount,
                                                        minor.parsedCadenceDays(),
                                                    ).lowercase() + progress,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
