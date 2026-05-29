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
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.ui.common.VoicePresetPicker
import kotlinx.coroutines.launch

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
    var minor1 by rememberSaveable { mutableStateOf("") }
    var minor2 by rememberSaveable { mutableStateOf("") }
    var minor3 by rememberSaveable { mutableStateOf("") }
    var boon by rememberSaveable { mutableStateOf("") }
    var summoning by rememberSaveable { mutableStateOf(false) }

    val voice = VoicePreset.fromKey(voiceKey)
    val anyMinor = minor1.isNotBlank() || minor2.isNotBlank() || minor3.isNotBlank()
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
                    question = "What are 2-3 small acts that would feed that?",
                    helper = "Each will be a minor quest. Leave any blank to skip.",
                ) {
                    OutlinedTextField(
                        value = minor1,
                        onValueChange = { minor1 = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Small act 1") },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minor2,
                        onValueChange = { minor2 = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Small act 2") },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minor3,
                        onValueChange = { minor3 = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Small act 3") },
                    )
                }
                5 -> Prompt(
                    question = "What favor will it grant you for the work?",
                    helper = "This is the boon — what you receive each time you spend a wish.",
                ) {
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
                                repository.summonDaemon(
                                    name = name.trim(),
                                    archetype = archetype.trim(),
                                    voicePreset = voice,
                                    boonText = boon.trim(),
                                    firstMajorTitle = majorTitle.trim(),
                                    firstMinorTitles = listOf(minor1, minor2, minor3)
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() },
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
