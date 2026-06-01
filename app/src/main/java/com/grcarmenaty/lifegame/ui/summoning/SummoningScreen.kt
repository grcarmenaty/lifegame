package com.grcarmenaty.lifegame.ui.summoning

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.ui.common.CadencePicker
import com.grcarmenaty.lifegame.ui.common.VoicePresetPicker
import kotlinx.coroutines.launch

private const val MAX_MINOR_SLOTS = 7
private const val INITIAL_MINOR_SLOTS = 3

@OptIn(ExperimentalMaterial3Api::class)
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
    var name by rememberSaveable { mutableStateOf("") }
    var voiceKey by rememberSaveable { mutableStateOf(VoicePreset.GENTLE_MENTOR.name) }
    var majorTitle by rememberSaveable { mutableStateOf("") }
    var boon by rememberSaveable { mutableStateOf("") }
    var summoning by rememberSaveable { mutableStateOf(false) }

    // Up to MAX_MINOR_SLOTS slots; only the first slotCount are rendered.
    // Storing all slots regardless of visibility means a user who removes
    // a slot and re-adds it gets their text back.
    val minorTitleStates: List<MutableState<String>> = List(MAX_MINOR_SLOTS) { i ->
        rememberSaveable(key = "minor_title_$i") { mutableStateOf("") }
    }
    val minorCadenceStates: List<MutableState<String>> = List(MAX_MINOR_SLOTS) { i ->
        rememberSaveable(key = "minor_cadence_$i") { mutableStateOf(MinorQuest.CADENCE_ONE_OFF) }
    }
    // v0.0.12: per-slot count + days. Saveable as Int + CSV-string.
    val minorCountStates: List<MutableState<Int>> = List(MAX_MINOR_SLOTS) { i ->
        rememberSaveable(key = "minor_count_$i") { mutableStateOf(1) }
    }
    val minorDaysStates: List<MutableState<String>> = List(MAX_MINOR_SLOTS) { i ->
        rememberSaveable(key = "minor_days_$i") { mutableStateOf("") }
    }
    var slotCount by rememberSaveable { mutableStateOf(INITIAL_MINOR_SLOTS) }

    val voice = VoicePreset.fromKey(voiceKey)
    val anyMinor = (0 until slotCount).any { minorTitleStates[it].value.isNotBlank() }
    val canAdvance = when (step) {
        0 -> archetype.isNotBlank()
        1 -> name.isNotBlank()
        2 -> true
        3 -> majorTitle.isNotBlank()
        4 -> anyMinor
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
                    helper = "One line. The thing you want this daemon to speak for.",
                ) {
                    OutlinedTextField(
                        value = archetype,
                        onValueChange = { archetype = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. my body, my craft, my finances") },
                    )
                }
                1 -> Prompt(
                    question = "Give it a name.",
                    helper = "Anything you like. It will speak to you under this name.",
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Athleta, Sage, Hearth") },
                    )
                }
                2 -> Prompt(
                    question = "How does it speak?",
                    helper = "Pick a voice. You can override individual lines later.",
                ) {
                    VoicePresetPicker(selected = voice, onSelect = { voiceKey = it.name })
                }
                3 -> Prompt(
                    question = "What's one thing it wants from you in the next month?",
                    helper = "This becomes its first major quest.",
                ) {
                    OutlinedTextField(
                        value = majorTitle,
                        onValueChange = { majorTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Run a 10k") },
                    )
                }
                4 -> Prompt(
                    question = "What small acts will feed that?",
                    helper = "Each becomes a minor quest. Set how often it should come " +
                        "back — once, n times a day/week/month, or weekly on specific days.",
                ) {
                    (0 until slotCount).forEach { i ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minorTitleStates[i].value,
                                onValueChange = { minorTitleStates[i].value = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Small act ${i + 1}") },
                            )
                            CadencePicker(
                                cadence = minorCadenceStates[i].value,
                                cadenceCount = minorCountStates[i].value,
                                cadenceDays = MinorQuest.parseDaysCsv(minorDaysStates[i].value),
                                onCadenceChange = { minorCadenceStates[i].value = it },
                                onCadenceCountChange = { minorCountStates[i].value = it },
                                onCadenceDaysChange = {
                                    minorDaysStates[i].value = MinorQuest.encodeDays(it) ?: ""
                                },
                                enabled = !summoning,
                            )
                        }
                        if (i < slotCount - 1) Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (slotCount > 1) slotCount-- },
                            enabled = slotCount > 1 && !summoning,
                            modifier = Modifier.weight(1f),
                        ) { Text("− Remove") }
                        OutlinedButton(
                            onClick = { if (slotCount < MAX_MINOR_SLOTS) slotCount++ },
                            enabled = slotCount < MAX_MINOR_SLOTS && !summoning,
                            modifier = Modifier.weight(1f),
                        ) { Text("+ Add slot") }
                    }
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
                                val visibleMinors = (0 until slotCount).mapNotNull { i ->
                                    val title = minorTitleStates[i].value.trim()
                                    if (title.isBlank()) null else PantheonRepository.NewMinorSpec(
                                        title = title,
                                        cadence = minorCadenceStates[i].value,
                                        cadenceCount = minorCountStates[i].value,
                                        cadenceDays = MinorQuest.parseDaysCsv(minorDaysStates[i].value),
                                    )
                                }
                                repository.summonDaemon(
                                    name = name.trim(),
                                    archetype = archetype.trim(),
                                    voicePreset = voice,
                                    boonText = boon.trim(),
                                    firstMajorTitle = majorTitle.trim(),
                                    firstMinors = visibleMinors,
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
