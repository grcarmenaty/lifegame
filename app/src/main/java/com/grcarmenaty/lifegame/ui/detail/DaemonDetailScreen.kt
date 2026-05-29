package com.grcarmenaty.lifegame.ui.detail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.ui.common.VoicePresetPicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaemonDetailScreen(
    repository: PantheonRepository,
    daemonId: Long,
    onBack: () -> Unit,
) {
    val viewModel: DaemonDetailViewModel = viewModel(
        factory = DaemonDetailViewModel.factory(repository, daemonId)
    )
    val state by viewModel.state.collectAsState()
    val saved by viewModel.saved.collectAsState()

    val daemon = state.daemon
    if (daemon == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Keyed on daemon.id so opening a different daemon resets form state.
    var name by rememberSaveable(daemon.id) { mutableStateOf(daemon.name) }
    var archetype by rememberSaveable(daemon.id) { mutableStateOf(daemon.archetype) }
    var voiceKey by rememberSaveable(daemon.id) { mutableStateOf(daemon.voicePreset) }
    var boon by rememberSaveable(daemon.id) { mutableStateOf(daemon.boonText) }
    var showVanishConfirm by rememberSaveable { mutableStateOf(false) }

    val voice = VoicePreset.fromKey(voiceKey)
    val dirty = name != daemon.name ||
        archetype != daemon.archetype ||
        voiceKey != daemon.voicePreset ||
        boon != daemon.boonText
    val canSave = dirty && name.isNotBlank() && archetype.isNotBlank() && boon.isNotBlank()

    // After save, the daemon flow re-emits with the new values, which makes
    // `dirty` flip back to false on its own. Acknowledge the saved flag so it
    // doesn't sit on the VM forever.
    LaunchedEffect(saved) { if (saved) viewModel.acknowledgeSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(daemon.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                LevelSection(
                    level = state.level,
                    progress = state.levelProgress,
                    leading = state.leadingMajor,
                )
            }

            item {
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                )
            }
            item {
                OutlinedTextField(
                    value = archetype,
                    onValueChange = { archetype = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Archetype") },
                )
            }
            item {
                VoicePresetPicker(
                    selected = voice,
                    onSelect = { voiceKey = it.name },
                )
            }
            item {
                OutlinedTextField(
                    value = boon,
                    onValueChange = { boon = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Boon") },
                )
            }
            item {
                Button(
                    onClick = { viewModel.save(name, archetype, voice, boon) },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (dirty) "Save changes" else "Saved")
                }
            }

            item { HorizontalDivider() }
            item {
                Text(
                    text = "Quest history",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            if (state.majors.isEmpty()) {
                item {
                    Text(
                        text = "No quests yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.majors, key = { it.id }) { major ->
                    MajorWithMinors(repository = repository, major = major)
                }
            }

            item { HorizontalDivider() }
            item {
                OutlinedButton(
                    onClick = { showVanishConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text("Vanish daemon")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showVanishConfirm) {
        AlertDialog(
            onDismissRequest = { showVanishConfirm = false },
            title = { Text("Vanish ${daemon.name}?") },
            text = {
                Text(
                    "This removes the daemon and all of its quests, completed " +
                        "or not. The work it remembers will be gone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showVanishConfirm = false
                    viewModel.vanish(onBack)
                }) { Text("Vanish") }
            },
            dismissButton = {
                TextButton(onClick = { showVanishConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LevelSection(
    level: Int,
    progress: Float,
    leading: LeadingMajor?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Level $level", style = MaterialTheme.typography.headlineSmall)
            if (leading != null) {
                Text(
                    text = "${leading.progress}/${leading.threshold}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = leading?.let { "toward next: ${it.title}" } ?: "no open quests",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MajorWithMinors(
    repository: PantheonRepository,
    major: MajorQuest,
) {
    val minors by remember(major.id) {
        repository.observeMinors(major.id)
    }.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = major.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (major.completed) "completed" else "${major.progressCount}/${major.thresholdCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (major.completed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (minors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                minors.forEach { minor ->
                    MinorRow(minor)
                }
            }
        }
    }
}

@Composable
private fun MinorRow(minor: MinorQuest) {
    val mark = when {
        minor.completed -> "✓"
        minor.cadence == MinorQuest.CADENCE_DAILY -> "↻"
        else -> "○"
    }
    val date = minor.lastCompletedAt?.let { dateFormatter.format(Date(it)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$mark  ${minor.title}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (date != null) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
