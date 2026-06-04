package com.grcarmenaty.lifegame.ui.summoning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.domain.BoonSuggestions
import com.grcarmenaty.lifegame.domain.DaemonFaceCatalog
import com.grcarmenaty.lifegame.domain.DaemonFaceSuggestions
import com.grcarmenaty.lifegame.domain.DaemonNameSuggestions
import com.grcarmenaty.lifegame.domain.LifeTheme
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.ui.common.FacePicker
import com.grcarmenaty.lifegame.ui.common.ThemePicker
import com.grcarmenaty.lifegame.ui.common.VoicePresetPicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SummoningScreen(
    repository: PantheonRepository,
    onSummoned: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // rememberSaveable so configuration changes (rotation, dark-mode switch,
    // process death) don't wipe the ritual mid-flow.
    var step by rememberSaveable { mutableStateOf(0) }
    var archetype by rememberSaveable { mutableStateOf("") }
    // v0.0.12: theme key. Empty = nothing picked yet; "OTHER" = the
    // user explicitly chose Other (free text). Any LifeTheme.key
    // means the daemon will draw from that themed corpus.
    var themeKey by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // v0.0.13: the user-chosen face's stable name. Null until they tap
    // one in the chooser; on summon we resolve null to the deterministic
    // default so what the preview showed is what the daemon keeps.
    var faceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var voiceKey by rememberSaveable { mutableStateOf(VoicePreset.GENTLE_MENTOR.name) }
    var boon by rememberSaveable { mutableStateOf("") }
    var summoning by rememberSaveable { mutableStateOf(false) }

    // v0.0.14: the picked majors + their minors (library or custom),
    // saveable as a single JSON blob so the ritual survives rotation.
    var selection by rememberSaveable(stateSaver = SummonSelectionSaver) {
        mutableStateOf(SummonSelection())
    }

    val voice = VoicePreset.fromKey(voiceKey)
    val canAdvance = when (step) {
        0 -> archetype.isNotBlank()
        1 -> true
        2 -> name.isNotBlank()
        3 -> selection.majors.isNotEmpty()
        4 -> selection.majors.all { it.minorCount > 0 }
        5 -> boon.isNotBlank()
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summoning") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Step ${step + 1} of 6",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (step) {
                0 -> Prompt(
                    question = "What part of your life is asking to be heard?",
                    helper = "Pick a theme — the daemon's dialogue is tuned to it. " +
                        "Or pick \"Other\" and write your own.",
                ) {
                    ThemePicker(
                        selectedKey = themeKey,
                        onPick = { picked ->
                            themeKey = picked?.key ?: "OTHER"
                            // Themed daemons take the theme's archetype text
                            // silently; only "Other" asks for free text.
                            archetype = picked?.archetypeText ?: ""
                        },
                    )
                    if (themeKey == "OTHER") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = archetype,
                            onValueChange = { archetype = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. my body, my craft, my finances") },
                            label = { Text("What it represents") },
                        )
                    }
                }
                1 -> Prompt(
                    question = "How does it speak?",
                    helper = "Pick a voice. You can override individual lines later.",
                ) {
                    VoicePresetPicker(selected = voice, onSelect = { voiceKey = it.name })
                }
                2 -> Prompt(
                    question = "Give it a name and a face.",
                    helper = "Tap a name suggestion or write your own, then pick a face.",
                ) {
                    val pickedTheme = LifeTheme.fromKey(themeKey)
                    val names = DaemonNameSuggestions.forPair(voice, pickedTheme)
                    // No daemon id yet at summoning; derive a stable variant
                    // from the chosen pair so the default stays consistent.
                    val faceSeed = (voice.name + (pickedTheme?.key ?: "OTHER"))
                        .hashCode().toLong()
                    val selectedRes = DaemonFaceCatalog.resForName(faceKey)
                        ?: DaemonFaceSuggestions.faceFor(voice, pickedTheme, faceSeed)
                    if (names.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            names.forEach { suggestion ->
                                OutlinedButton(onClick = { name = suggestion }) {
                                    Text(suggestion)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Anything you like.") },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Face",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FacePicker(
                        preset = voice,
                        theme = pickedTheme,
                        selectedRes = selectedRes,
                        onSelect = { faceName, _ -> faceKey = faceName },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                3 -> Prompt(
                    question = "What will it ask of you?",
                    helper = "Choose its major quests — as many as you want — or write " +
                        "your own.",
                ) {
                    MajorPickStep(
                        theme = LifeTheme.fromKey(themeKey),
                        selection = selection,
                        onChange = { selection = it },
                    )
                }
                4 -> Prompt(
                    question = "What small acts will feed those?",
                    helper = "Pick the minor quests under each. Repeating ones return on " +
                        "their own rhythm; one-off ones are the harder milestones.",
                ) {
                    MinorPickStep(
                        selection = selection,
                        onChange = { selection = it },
                    )
                }
                5 -> Prompt(
                    question = "What favor will it grant you for the work?",
                    helper = "This is the boon — what you receive each time you spend a wish.",
                ) {
                    // Per-archetype "stay small" advice in the daemon's
                    // own voice, not product tone. Council outcome:
                    // Ally polish + user decision (5).
                    Text(
                        text = "“${voice.staySmallBoonAdvice}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    val boonIdeas = BoonSuggestions.forTheme(LifeTheme.fromKey(themeKey))
                    if (boonIdeas.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            boonIdeas.forEach { idea ->
                                OutlinedButton(onClick = { boon = idea }) { Text(idea) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = boon,
                        onValueChange = { boon = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. A guilt-free rest day") },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (step > 0) {
                    TextButton(onClick = { step-- }, enabled = !summoning) { Text("Back") }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else if (!summoning) {
                            summoning = true
                            scope.launch {
                                val majorSpecs = selection.toMajorSpecs()
                                // Resolve the face: the user's explicit pick,
                                // or the deterministic default the preview
                                // showed, so the daemon keeps what was seen.
                                val pickedTheme = LifeTheme.fromKey(themeKey)
                                val resolvedFace = faceKey ?: run {
                                    val seed = (voice.name + (pickedTheme?.key ?: "OTHER"))
                                        .hashCode().toLong()
                                    DaemonFaceCatalog.nameForRes(
                                        DaemonFaceSuggestions.faceFor(voice, pickedTheme, seed)
                                    )
                                }
                                repository.summonDaemon(
                                    name = name.trim(),
                                    archetype = archetype.trim(),
                                    voicePreset = voice,
                                    boonText = boon.trim(),
                                    majors = majorSpecs,
                                    // Persist the chosen LifeTheme key, or
                                    // null when "Other" was picked.
                                    theme = pickedTheme?.key,
                                    face = resolvedFace,
                                )
                                onSummoned()
                            }
                        }
                    },
                    enabled = canAdvance && !summoning,
                ) {
                    Text(if (step < 5) "Next" else "Summon")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Prompt(
    question: String,
    helper: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = question, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = helper,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

