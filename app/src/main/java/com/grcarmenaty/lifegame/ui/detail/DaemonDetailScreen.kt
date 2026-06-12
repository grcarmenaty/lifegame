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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.EpicChapter
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.DaemonFaceCatalog
import com.grcarmenaty.lifegame.domain.DaemonFaceSuggestions
import com.grcarmenaty.lifegame.domain.LifeTheme
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.domain.catalog.QuestCatalog
import com.grcarmenaty.lifegame.ui.common.FacePicker
import com.grcarmenaty.lifegame.ui.common.VoicePresetPicker
import com.grcarmenaty.lifegame.ui.summoning.MajorPickStep
import com.grcarmenaty.lifegame.ui.summoning.MinorPickStep
import com.grcarmenaty.lifegame.ui.summoning.SummonSelection
import com.grcarmenaty.lifegame.ui.summoning.SummonSelectionSaver
import com.grcarmenaty.lifegame.ui.summoning.toMajorSpecs
import androidx.compose.foundation.layout.heightIn
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
    val deletePreview by viewModel.deletePreview.collectAsState()
    val apotheosis by viewModel.apotheosis.collectAsState()

    val daemon = state.daemon
    if (daemon == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var name by rememberSaveable(daemon.id) { mutableStateOf(daemon.name) }
    var archetype by rememberSaveable(daemon.id) { mutableStateOf(daemon.archetype) }
    var voiceKey by rememberSaveable(daemon.id) { mutableStateOf(daemon.voicePreset) }
    // Null = inherit the deterministic default; a name = explicit pick.
    var faceKey by rememberSaveable(daemon.id) { mutableStateOf(daemon.face) }

    var showVanishConfirm by rememberSaveable { mutableStateOf(false) }
    var showAddBoon by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteBoonId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddMajor by rememberSaveable { mutableStateOf(false) }
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var editMajorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editMinorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addingMinorForMajor by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingChapterForMajor by rememberSaveable { mutableStateOf<String?>(null) }

    val voice = VoicePreset.fromKey(voiceKey)
    val theme = LifeTheme.fromKey(daemon.theme)
    val selectedFaceRes = DaemonFaceCatalog.resForName(faceKey)
        ?: DaemonFaceSuggestions.faceFor(voice, theme, daemon.id)
    val dirty = name != daemon.name ||
        archetype != daemon.archetype ||
        voiceKey != daemon.voicePreset ||
        faceKey != daemon.face
    val canSave = dirty && name.isNotBlank() && archetype.isNotBlank()

    LaunchedEffect(saved) { if (saved) viewModel.acknowledgeSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(selectedFaceRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(daemon.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        // Column + verticalScroll: the screen has at most ~25 items in
        // real use, so Lazy isn't earning its keep — and a regular
        // scrolling Column eliminates LazyColumn-specific bugs around
        // item composition timing and focused-TextField + scroll
        // interaction. Minors come from the VM state, not per-item
        // Flow subscriptions opened inside MajorCard.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            LevelSection(
                level = state.level,
                progress = state.levelProgress,
                attentionPoints = state.attentionPoints,
                atMaxLevel = state.level >= com.grcarmenaty.lifegame.domain.attention.AttentionMath.MAX_LEVEL,
            )

            Text("Edit", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
            )
            OutlinedTextField(
                value = archetype,
                onValueChange = { archetype = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Archetype") },
            )
            VoicePresetPicker(
                selected = voice,
                onSelect = { voiceKey = it.name },
                collapsible = true,
            )
            Text(
                text = "Face",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FacePicker(
                preset = voice,
                theme = theme,
                selectedRes = selectedFaceRes,
                onSelect = { faceName, _ -> faceKey = faceName },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.save(name, archetype, voice, faceKey) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (dirty) "Save changes" else "Saved") }

            HorizontalDivider()
            TuningSection(state = state, viewModel = viewModel)

            HorizontalDivider()
            SectionHeader(
                title = "Boons",
                actionLabel = "+ Boon",
                onAction = { showAddBoon = true },
            )
            if (state.boons.isEmpty()) {
                Text(
                    text = "No boons yet — the daemon has nothing to grant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.boons.forEach { boon ->
                    BoonRow(
                        boon = boon,
                        onDelete = { pendingDeleteBoonId = boon.id },
                    )
                }
            }

            HorizontalDivider()
            SectionHeader(
                title = "Quest history",
                actionLabel = "+ Major",
                onAction = { showAddMajor = true },
                secondaryLabel = if (QuestCatalog.majorsFor(theme).isNotEmpty()) "+ Library" else null,
                onSecondary = { showLibrary = true },
            )
            if (state.majors.isEmpty()) {
                Text(
                    text = "No quests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.majors.forEach { major ->
                    MajorCard(
                        major = major,
                        minors = state.minorsFor(major.id),
                        grantsBoonText = state.boonTextFor(major.wishBoonId),
                        onAddMinor = { addingMinorForMajor = major.id },
                        onEditMajor = { editMajorId = major.id },
                        onDeleteMajor = { viewModel.requestDeleteMajor(major.id, major.title) },
                        onEditMinor = { editMinorId = it },
                        onDeleteMinor = { viewModel.deleteMinor(it) },
                        onCompleteMajor = { viewModel.completeMajor(major.id) },
                        onReopenMajor = { viewModel.reopenMajor(major.id) },
                    )
                }
            }

            HorizontalDivider()
            ScriptureSection(chapters = state.epicChapters)

            HorizontalDivider()
            OutlinedButton(
                onClick = { showVanishConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
            ) { Text("Vanish daemon") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showVanishConfirm) {
        AlertDialog(
            onDismissRequest = { showVanishConfirm = false },
            title = { Text("Vanish ${daemon.name}?") },
            text = {
                Text(
                    "This removes the daemon, all of its boons, and all of " +
                        "its quests — completed or not. The work it remembers " +
                        "will be gone."
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

    if (showAddBoon) {
        AddBoonDialog(
            onDismiss = { showAddBoon = false },
            onAdd = { text, count ->
                viewModel.addBoon(text, count)
                showAddBoon = false
            },
        )
    }

    pendingDeleteBoonId?.let { id ->
        val boon = state.boons.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingDeleteBoonId = null },
            title = { Text("Delete this boon?") },
            text = {
                Text(
                    text = boon?.let { "“${it.text}” — ${it.count} available." }
                        ?: "Delete this boon?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBoon(id)
                    pendingDeleteBoonId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBoonId = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddMajor) {
        AddMajorDialog(
            onDismiss = { showAddMajor = false },
            onAdd = { title, threshold ->
                viewModel.addMajor(title, threshold)
                showAddMajor = false
            },
        )
    }

    if (showLibrary) {
        LibraryAddDialog(
            theme = theme,
            onDismiss = { showLibrary = false },
            onConfirm = { specs ->
                viewModel.addMajorsFromLibrary(specs)
                showLibrary = false
            },
        )
    }

    editMajorId?.let { id ->
        state.majors.firstOrNull { it.id == id }?.let { major ->
            EditMajorDialog(
                major = major,
                onDismiss = { editMajorId = null },
                onSave = { title, phrase, threshold ->
                    viewModel.editMajor(id, title, phrase, threshold)
                    editMajorId = null
                },
            )
        }
    }

    editMinorId?.let { id ->
        val minor = state.majors.firstNotNullOfOrNull { m ->
            state.minorsFor(m.id).firstOrNull { it.id == id }
        }
        if (minor != null) {
            EditMinorDialog(
                minor = minor,
                onDismiss = { editMinorId = null },
                onSave = { title, cadence, count, days, weight, phrase ->
                    viewModel.editMinor(id, title, cadence, count, days, weight, phrase)
                    editMinorId = null
                },
            )
        }
    }

    addingMinorForMajor?.let { majorId ->
        AddMinorDialog(
            onDismiss = { addingMinorForMajor = null },
            onAdd = { title, cadence, cadenceCount, cadenceDays, weight ->
                viewModel.addMinor(majorId, title, cadence, cadenceCount, cadenceDays, weight)
                addingMinorForMajor = null
            },
        )
    }

    deletePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteMajor,
            title = { Text("Delete “${preview.title}”?") },
            text = {
                Text(
                    if (preview.totalMinors == 0)
                        "No minors recorded under this quest."
                    else
                        "${preview.completedMinors} of ${preview.totalMinors} minors complete. " +
                            "The major and all its minors will be removed."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteMajor) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteMajor) { Text("Cancel") }
            }
        )
    }

    apotheosis?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissApotheosis,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissApotheosis()
                    pendingChapterForMajor = event.completedMajorTitle
                }) { Text("Write a chapter") }
            },
            dismissButton = {
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
                    Text(
                        text = "Write a chapter to keep this in the daemon's scripture, " +
                            "or just continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        )
    }

    pendingChapterForMajor?.let { majorTitle ->
        WriteChapterDialog(
            majorTitle = majorTitle,
            onDismiss = { pendingChapterForMajor = null },
            onSave = { text ->
                viewModel.addEpicChapter(text)
                pendingChapterForMajor = null
            },
        )
    }
}

// ---- Sub-composables ----

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun LevelSection(
    level: Int,
    progress: Float,
    attentionPoints: Int,
    atMaxLevel: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Level $level", style = MaterialTheme.typography.headlineSmall)
            if (atMaxLevel) {
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (atMaxLevel)
                "$attentionPoints attention · the buffer fills"
            else
                "$attentionPoints attention",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BoonRow(boon: Boon, onDelete: () -> Unit) {
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
                Text(text = boon.text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${boon.count} available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete boon")
            }
        }
    }
}

@Composable
private fun MajorCard(
    major: MajorQuest,
    minors: List<MinorQuest>,
    grantsBoonText: String?,
    onAddMinor: () -> Unit,
    onEditMajor: () -> Unit,
    onDeleteMajor: () -> Unit,
    onEditMinor: (Long) -> Unit,
    onDeleteMinor: (Long) -> Unit,
    onCompleteMajor: () -> Unit,
    onReopenMajor: () -> Unit,
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = major.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (major.completed)
                            "completed"
                        else
                            "${major.progressCount} / ${major.thresholdCount} contributions",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (major.completed)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditMajor) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit major quest")
                }
                IconButton(onClick = onDeleteMajor) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete major quest")
                }
            }
            if (!major.completed) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = {
                        (major.progressCount.toFloat() / major.thresholdCount.coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!grantsBoonText.isNullOrBlank()) {
                Text(
                    text = "→ grants “$grantsBoonText”",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (minors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                minors.forEach { minor ->
                    MinorRow(
                        minor = minor,
                        onEdit = { onEditMinor(minor.id) },
                        onDelete = { onDeleteMinor(minor.id) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!major.completed) {
                    TextButton(onClick = onAddMinor) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.padding(end = 4.dp))
                        Text("Add minor")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onCompleteMajor) {
                        Text("Mark complete")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onReopenMajor) {
                        Text("Reopen")
                    }
                }
            }
        }
    }
}

@Composable
private fun MinorRow(minor: MinorQuest, onEdit: () -> Unit, onDelete: () -> Unit) {
    val mark = when {
        minor.completed -> "✓"
        minor.cadence == MinorQuest.CADENCE_ONE_OFF -> "○"
        else -> "↻"
    }
    val date = minor.lastCompletedAt?.let { dateFormatter.format(Date(it)) }
    val weightSuffix = if (minor.weight > 1) "  +${minor.weight}" else ""
    val cadenceLabel = MinorQuest.cadenceLongLabel(
        cadence = minor.cadence,
        count = minor.cadenceCount,
        days = minor.parsedCadenceDays(),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$mark  ${minor.title}$weightSuffix",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (date != null) "$cadenceLabel  ·  $date" else cadenceLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit minor")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete minor")
        }
    }
}

// ---- Add dialogs ----

@Composable
private fun AddBoonDialog(
    onDismiss: () -> Unit,
    onAdd: (text: String, count: Int) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var countText by rememberSaveable { mutableStateOf("0") }
    val count = countText.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Author a new boon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Boon text") },
                    placeholder = { Text("e.g. A guilt-free rest day") },
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { v -> countText = v.filter { it.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Initial count") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text, count) },
                enabled = text.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LibraryAddDialog(
    theme: LifeTheme?,
    onDismiss: () -> Unit,
    onConfirm: (List<PantheonRepository.NewMajorSpec>) -> Unit,
) {
    var selection by rememberSaveable(stateSaver = SummonSelectionSaver) {
        mutableStateOf(SummonSelection())
    }
    val canAdd = selection.majors.isNotEmpty() && selection.majors.all { it.minorCount > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selection.toMajorSpecs()) }, enabled = canAdd) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add from library") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MajorPickStep(theme = theme, selection = selection, onChange = { selection = it })
                if (selection.majors.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Small acts", style = MaterialTheme.typography.titleMedium)
                    MinorPickStep(selection = selection, onChange = { selection = it })
                }
            }
        },
    )
}

@Composable
private fun EditMajorDialog(
    major: MajorQuest,
    onDismiss: () -> Unit,
    onSave: (title: String, phrase: String, threshold: Int?) -> Unit,
) {
    var title by rememberSaveable(major.id) { mutableStateOf(major.title) }
    val initialPhrase = major.fragmentOverride
        ?: QuestCatalog.majorFragment(major.templateId).orEmpty()
    var phrase by rememberSaveable(major.id) { mutableStateOf(initialPhrase) }
    var thresholdText by rememberSaveable(major.id) {
        mutableStateOf(major.thresholdCount.toString())
    }
    val threshold = thresholdText.toIntOrNull()?.coerceIn(1, 999)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit quest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { v -> thresholdText = v.filter { it.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Goal (weight of small acts to fill the bar)") },
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Completion phrase") },
                    placeholder = { Text("e.g. the ten kilometres are yours") },
                )
                Text(
                    "The daemon quotes this in its own voice when you close the quest. " +
                        "Keep it short and close to the original so it still reads naturally.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, phrase, threshold) },
                enabled = title.isNotBlank() && threshold != null,
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMinorDialog(
    minor: MinorQuest,
    onDismiss: () -> Unit,
    onSave: (title: String, cadence: String, count: Int, days: Set<Int>, weight: Int, phrase: String) -> Unit,
) {
    var title by rememberSaveable(minor.id) { mutableStateOf(minor.title) }
    var cadence by rememberSaveable(minor.id) { mutableStateOf(minor.cadence) }
    var count by rememberSaveable(minor.id) { mutableStateOf(minor.cadenceCount) }
    var daysCsv by rememberSaveable(minor.id) { mutableStateOf(minor.cadenceDays ?: "") }
    var weightText by rememberSaveable(minor.id) { mutableStateOf(minor.weight.toString()) }
    val initialPhrase = minor.fragmentOverride
        ?: QuestCatalog.minorFragment(minor.templateId).orEmpty()
    var phrase by rememberSaveable(minor.id) { mutableStateOf(initialPhrase) }
    val weight = weightText.toIntOrNull()?.coerceIn(1, 9) ?: 1
    val days = MinorQuest.parseDaysCsv(daysCsv)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit act") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                )
                Text(
                    text = "Cadence",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                com.grcarmenaty.lifegame.ui.common.CadencePicker(
                    cadence = cadence,
                    cadenceCount = count,
                    cadenceDays = days,
                    onCadenceChange = { cadence = it },
                    onCadenceCountChange = { count = it },
                    onCadenceDaysChange = { daysCsv = MinorQuest.encodeDays(it) ?: "" },
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { v -> weightText = v.filter { it.isDigit() }.take(1) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Weight (1–9)") },
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Completion phrase") },
                    placeholder = { Text("e.g. the run is logged") },
                )
                Text(
                    "Quoted in the daemon's voice when you complete it. Keep it short and " +
                        "close to the original.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, cadence, count, days, weight, phrase) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddMajorDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, threshold: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var thresholdText by rememberSaveable { mutableStateOf("3") }
    val threshold = thresholdText.toIntOrNull()?.coerceIn(1, 999)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a major quest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { v -> thresholdText = v.filter { it.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Goal (weight of small acts to fill the bar)") },
                )
                Text(
                    text = "Small acts under this quest add their weight toward the goal. " +
                        "The bar shows how close it is; you close the quest yourself.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, threshold ?: 3) },
                enabled = title.isNotBlank() && threshold != null,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMinorDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, cadence: String, cadenceCount: Int, cadenceDays: Set<Int>, weight: Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var cadence by rememberSaveable { mutableStateOf(MinorQuest.CADENCE_ONE_OFF) }
    var cadenceCount by rememberSaveable { mutableStateOf(1) }
    var cadenceDaysCsv by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("1") }
    val weight = weightText.toIntOrNull()?.coerceIn(1, 9) ?: 1
    val days = MinorQuest.parseDaysCsv(cadenceDaysCsv)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a minor quest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                )
                Text(
                    text = "Cadence",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                com.grcarmenaty.lifegame.ui.common.CadencePicker(
                    cadence = cadence,
                    cadenceCount = cadenceCount,
                    cadenceDays = days,
                    onCadenceChange = { cadence = it },
                    onCadenceCountChange = { cadenceCount = it },
                    onCadenceDaysChange = { cadenceDaysCsv = MinorQuest.encodeDays(it) ?: "" },
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { v -> weightText = v.filter { it.isDigit() }.take(1) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Weight (1–9, how much this advances the major)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, cadence, cadenceCount, days, weight) },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---- v0.0.10 sections ----

@Composable
private fun TuningSection(
    state: DetailState,
    viewModel: DaemonDetailViewModel,
) {
    val resolved = state.resolvedConfig ?: return
    val ds = state.daemonState ?: return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tuning", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Defaults come from the archetype. Override per-daemon below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Notifications switch (wires the existing setNotificationsEnabled
        // method — UI was missing since v0.0.7).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Daemon nudges", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Allow this daemon to send notifications.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ds.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled,
            )
        }

        // Decay-disabled toggle (user kill switch from council outcome #1).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Pause level decay", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "When on, attention never drops from neglect.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ds.decayDisabled,
                onCheckedChange = viewModel::setDecayDisabled,
            )
        }

        // Decay rate override.
        var decayText by rememberSaveable(ds.daemonId) {
            mutableStateOf(ds.attentionDecayPerDay?.toString() ?: "")
        }
        var graceText by rememberSaveable(ds.daemonId) {
            mutableStateOf(ds.attentionDecayGraceDays?.toString() ?: "")
        }
        var minorsBoonText by rememberSaveable(ds.daemonId) {
            mutableStateOf(ds.minorsPerBoonAccrual?.toString() ?: "")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = decayText,
                onValueChange = { v -> decayText = v.filter { it.isDigit() }.take(2) },
                modifier = Modifier.weight(1f),
                label = { Text("Decay / day") },
                placeholder = { Text("default ${resolved.decayPerDay}") },
            )
            OutlinedTextField(
                value = graceText,
                onValueChange = { v -> graceText = v.filter { it.isDigit() }.take(2) },
                modifier = Modifier.weight(1f),
                label = { Text("Grace (days)") },
                placeholder = { Text("default ${resolved.decayGraceDays}") },
            )
        }
        OutlinedTextField(
            value = minorsBoonText,
            onValueChange = { v -> minorsBoonText = v.filter { it.isDigit() }.take(2) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Minors per boon (currently ${resolved.minorsPerBoonAccrual})") },
            placeholder = { Text("default ${resolved.minorsPerBoonAccrual}") },
        )
        Button(
            onClick = {
                viewModel.setDecayOverride(decayText.toIntOrNull(), graceText.toIntOrNull())
                viewModel.setMinorsPerBoonOverride(minorsBoonText.toIntOrNull())
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save tuning") }
    }
}

@Composable
private fun ScriptureSection(chapters: List<EpicChapter>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Scripture", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) "Hide"
                    else if (chapters.isEmpty()) "Empty"
                    else "Show ${chapters.size}"
                )
            }
        }
        if (expanded) {
            if (chapters.isEmpty()) {
                Text(
                    "Nothing written yet. Close a major quest, and the daemon will offer " +
                        "you a moment to inscribe what it meant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                chapters.forEach { chapter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Chapter ${chapter.position + 1}  ·  ${dateFormatter.format(Date(chapter.createdAt))}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = chapter.text,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteChapterDialog(
    majorTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    // Ally polish: pre-fill scaffolding so this is "edit" not "blank page".
    val today = remember(majorTitle) { dateFormatter.format(Date()) }
    var text by rememberSaveable(majorTitle) {
        mutableStateOf("$today — closed “$majorTitle.” ")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to scripture") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A line or two about what this closure means in the daemon's story.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) { Text("Save chapter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        }
    )
}

private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
