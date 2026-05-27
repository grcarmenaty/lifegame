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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grcarmenaty.lifegame.domain.PantheonRepository

@Composable
fun DailyScreen(
    repository: PantheonRepository,
    viewModel: DailyViewModel = viewModel(
        factory = DailyViewModel.factory(repository)
    ),
) {
    val state by viewModel.state.collectAsState()
    val apotheosis by viewModel.apotheosis.collectAsState()
    val boonGranted by viewModel.boonGranted.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Today",
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(16.dp))

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
                    items(state.daemons, key = { it.daemon.id }) { block ->
                        DaemonBlock(
                            block = block,
                            onComplete = viewModel::completeMinor,
                            onSpendWish = { viewModel.spendWish(block.daemon.id) },
                        )
                    }
                }
            }
        }
    }

    apotheosis?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissApotheosis,
            confirmButton = {
                TextButton(onClick = viewModel::dismissApotheosis) {
                    Text("Continue")
                }
            },
            title = {
                Text(
                    text = "${event.daemonName} — level ${event.newLevel}",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "“${event.voicePreset.apotheosis(event.daemonId)}”",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "“${event.completedMajorTitle}” is complete.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "A wish has been granted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
private fun DaemonBlock(
    block: DaemonDailyBlock,
    onComplete: (Long) -> Unit,
    onSpendWish: () -> Unit,
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
                if (block.daemon.wishesAvailable > 0) {
                    AssistChip(
                        onClick = onSpendWish,
                        label = { Text("${block.daemon.wishesAvailable} wish") },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "“${block.greeting}”",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            if (block.openMinors.isEmpty()) {
                Text(
                    text = "Nothing asked of you today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block.openMinors.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            onClick = { onComplete(entry.minor.id) },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = entry.minor.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = entry.parentMajorTitle,
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
