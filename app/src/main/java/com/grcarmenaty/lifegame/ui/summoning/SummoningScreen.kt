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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import kotlinx.coroutines.launch

@Composable
fun SummoningScreen(
    repository: PantheonRepository,
    onSummoned: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }

    var archetype by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var voice by remember { mutableStateOf(VoicePreset.GENTLE_MENTOR) }
    var majorTitle by remember { mutableStateOf("") }
    val minorTitles = remember { mutableStateListOf("", "", "") }
    var boon by remember { mutableStateOf("") }
    var summoning by remember { mutableStateOf(false) }

    val canAdvance = when (step) {
        0 -> archetype.isNotBlank()
        1 -> name.isNotBlank()
        2 -> true
        3 -> majorTitle.isNotBlank()
        4 -> minorTitles.any { it.isNotBlank() }
        5 -> boon.isNotBlank()
        else -> false
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Summoning",
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "Step ${step + 1} of 6",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

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
                    VoicePresetPicker(selected = voice, onSelect = { voice = it })
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
                    minorTitles.forEachIndexed { i, value ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { minorTitles[i] = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (i == 0) 0.dp else 8.dp),
                            placeholder = { Text("Small act ${i + 1}") },
                        )
                    }
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
                    TextButton(onClick = { step-- }, enabled = !summoning) {
                        Text("Back")
                    }
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
                                    firstMinorTitles = minorTitles
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
        Text(
            text = question,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun VoicePresetPicker(
    selected: VoicePreset,
    onSelect: (VoicePreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VoicePreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onSelect(preset) },
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "“${preset.sample}”",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
