package com.grcarmenaty.lifegame.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grcarmenaty.lifegame.BuildConfig
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.notify.NotificationPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: PantheonRepository,
    onBack: () -> Unit,
) {
    val appVersion = BuildConfig.VERSION_NAME
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(repository, appVersion)
    )
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showImportConfirm by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.export { json ->
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Could not open the destination for writing.")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.import {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: throw IOException("Could not open the chosen file.")
            }
        }
    }

    LaunchedEffect(status) {
        status?.let {
            snackbar.showSnackbar(it)
            viewModel.acknowledgeStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Section(title = "Backup") {
                ActionCard(
                    title = "Export pantheon",
                    body = "Save every daemon, boon, and quest to a JSON " +
                        "file you can keep, share, or restore later.",
                    actionLabel = "Export",
                    enabled = !busy,
                    onAction = { exportLauncher.launch(defaultExportFilename()) },
                )
                ActionCard(
                    title = "Import pantheon",
                    body = "Load a backup. This **replaces** your current " +
                        "pantheon — the daemons you have now will be lost.",
                    actionLabel = "Import",
                    enabled = !busy,
                    onAction = { showImportConfirm = true },
                )
            }

            Section(title = "Notifications") {
                NotificationSettingsCard()
            }

            Section(title = "Danger") {
                ActionCard(
                    title = "Reset pantheon",
                    body = "Permanently erase every daemon, boon, and " +
                        "quest. The summoning ritual will run again on " +
                        "next launch. This cannot be undone.",
                    actionLabel = "Reset",
                    enabled = !busy,
                    destructive = true,
                    onAction = { showResetConfirm = true },
                )
            }

            Text(
                text = "lifegame v$appVersion · backup format v1",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Replace your current pantheon?") },
            text = {
                Text(
                    "Importing wipes every daemon, boon, and quest you " +
                        "have now and loads the backup in their place. " +
                        "There is no undo."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset everything?") },
            text = {
                Text(
                    "This erases every daemon, boon, and quest. The work " +
                        "they remember will be gone. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.reset()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        content()
    }
}

@Composable
private fun ActionCard(
    title: String,
    body: String,
    actionLabel: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            if (destructive) {
                OutlinedButton(
                    onClick = onAction,
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) { Text(actionLabel) }
            } else {
                Button(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun NotificationSettingsCard() {
    val context = LocalContext.current
    val prefs = remember { NotificationPrefs(context) }
    val scope = rememberCoroutineScope()

    val masterEnabled by prefs.masterEnabled.collectAsState(initial = true)
    val quietStart by prefs.quietStart.collectAsState(initial = 22)
    val quietEnd by prefs.quietEnd.collectAsState(initial = 8)

    var startText by rememberSaveable(quietStart) { mutableStateOf(quietStart.toString()) }
    var endText by rememberSaveable(quietEnd) { mutableStateOf(quietEnd.toString()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            // User denied — keep the master toggle on but warn via the
            // body copy. System notification settings still allow them
            // to opt back in.
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Daemon nudges",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Each daemon decides when you should pay attention to its " +
                            "quests, and speaks in its own voice. Rate-limited.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = { newValue ->
                        scope.launch { prefs.setMaster(newValue) }
                        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Quiet hours (no nudges)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { v -> startText = v.filter { it.isDigit() }.take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start hour (0-23)") },
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { v -> endText = v.filter { it.isDigit() }.take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("End hour (0-23)") },
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val s = startText.toIntOrNull() ?: 22
                    val e = endText.toIntOrNull() ?: 8
                    scope.launch { prefs.setQuietHours(s, e) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (startText.toIntOrNull() != null && endText.toIntOrNull() != null) &&
                    (startText.toInt() != quietStart || endText.toInt() != quietEnd),
            ) { Text("Save quiet hours") }
        }
    }
}

private fun defaultExportFilename(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return "lifegame-export-$date.json"
}
