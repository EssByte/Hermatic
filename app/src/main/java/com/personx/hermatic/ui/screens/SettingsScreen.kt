package com.personx.hermatic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personx.hermatic.HermesViewModel
import com.personx.hermatic.ui.components.ColorPicker
import com.personx.hermatic.ui.components.ConnectionStatusIndicator
import com.personx.hermatic.ui.components.SettingsSection
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HermesViewModel, onBack: () -> Unit) {
    val currentPeriod by viewModel.selfDestructPeriod.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val systemPrompt by viewModel.systemPrompt.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val primaryColor by viewModel.primaryColor.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val isAutoTtsEnabled by viewModel.isAutoTtsEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val healthDetailed by viewModel.healthDetailed.collectAsState()
    val context = LocalContext.current

    var apiKey by remember { mutableStateOf(viewModel.getApiKey()) }
    var baseUrl by remember { mutableStateOf(viewModel.getBaseUrl()) }
    var showApiKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("CONFIGURATION", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(28.dp))
            }
        }

        item {
            SettingsSection("CONNECTION") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("BASE_URL", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("https://your-hermes-server.com/") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API_KEY", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showApiKey, onCheckedChange = { showApiKey = it })
                        Text("Show API Key", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.saveConfig(apiKey, baseUrl) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    ConnectionStatusIndicator(connectionStatus)
                    if (connectionStatus is com.personx.hermatic.ConnectionStatus.Testing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = { viewModel.testConnection() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("TEST_CONNECTION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (healthDetailed != null) {
                        val hd = healthDetailed!!
                        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("STATUS: ${hd.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            hd.active_sessions?.let { Text("ACTIVE_SESSIONS: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                            hd.running_agents?.let { Text("RUNNING_AGENTS: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
                            hd.resource_usage?.let { Text("RESOURCE_USAGE: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("APPEARANCE") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DARK_MODE", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("PRIMARY_COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    ColorPicker(selectedColor = primaryColor, onColorSelected = { viewModel.setPrimaryColor(it) })
                    Spacer(Modifier.height(8.dp))
                    Text("ACCENT_COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    ColorPicker(selectedColor = accentColor, onColorSelected = { viewModel.setAccentColor(it) })
                }
            }
        }

        item {
            SettingsSection("AUDIO") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AUTO_TTS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isAutoTtsEnabled, onCheckedChange = { viewModel.setAutoTtsEnabled(it) })
                }
            }
        }

        item {
            SettingsSection("SECURITY") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                        Text("BIOMETRIC_ENCRYPTION_LAYER", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("DATA_PERSISTENCE_TTL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    val options = listOf(
                        "VOLATILE" to 0L,
                        "60_MINUTES" to 3600_000L,
                        "24_HOURS" to 86400_000L,
                        "7_DAYS" to 604800_000L
                    )
                    options.forEach { (label, period) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp).clickable { viewModel.setSelfDestructPeriod(period) }) {
                            RadioButton(selected = currentPeriod == period, onClick = { viewModel.setSelfDestructPeriod(period) })
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        item {
            SettingsSection("NEURAL_ENGINE_PARAMS") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("MODEL_IDENTIFIER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Text(selectedModel.ifEmpty { "SELECT_MODEL" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.id, style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            viewModel.setSelectedModel(model.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text("ENTROPY_FACTOR (TEMPERATURE): ${String.format(Locale.US, "%.2f", temperature)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Slider(value = temperature, onValueChange = { viewModel.setTemperature(it) }, valueRange = 0f..1f)
                    }

                    Column {
                        Text("MAX_TOKEN_CAPACITY: $maxTokens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        Slider(value = maxTokens.toFloat(), onValueChange = { viewModel.setMaxTokens(it.toInt()) }, valueRange = 256f..4096f)
                    }

                    Column {
                        Text("SYSTEM_INSTRUCTION_OVERRIDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        TextField(
                            value = systemPrompt,
                            onValueChange = { viewModel.setSystemPrompt(it) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                            minLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        item {
            SettingsSection("SCHEDULED_JOBS") {
                var jobPrompt by remember { mutableStateOf("") }
                var jobId by remember { mutableStateOf("") }
                var jobUpdatePrompt by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jobPrompt,
                        onValueChange = { jobPrompt = it },
                        label = { Text("NEW_JOB_PROMPT", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. Summarize today's logs") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Button(
                        onClick = {
                            if (jobPrompt.isNotBlank()) {
                                viewModel.createJob(jobPrompt)
                                jobPrompt = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("CREATE_JOB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    OutlinedTextField(
                        value = jobId,
                        onValueChange = { jobId = it },
                        label = { Text("JOB_ID", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("Paste job ID from diagnostics") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = { if (jobId.isNotBlank()) viewModel.deleteJob(jobId) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Text("DELETE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        Button(onClick = { if (jobId.isNotBlank()) viewModel.pauseJob(jobId) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Text("PAUSE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        Button(onClick = { if (jobId.isNotBlank()) viewModel.resumeJob(jobId) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Text("RESUME", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        Button(onClick = { if (jobId.isNotBlank()) viewModel.triggerJob(jobId) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Text("RUN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                    Button(onClick = { viewModel.fetchDiagnostics() }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("REFRESH_DIAGNOSTICS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            SettingsSection("SESSION_MANAGEMENT") {
                var sessionRenameId by remember { mutableStateOf("") }
                var sessionNewTitle by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sessionRenameId,
                        onValueChange = { sessionRenameId = it },
                        label = { Text("SESSION_ID", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("Session UUID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    OutlinedTextField(
                        value = sessionNewTitle,
                        onValueChange = { sessionNewTitle = it },
                        label = { Text("NEW_TITLE", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("Friendly name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (sessionRenameId.isNotBlank() && sessionNewTitle.isNotBlank()) viewModel.renameServerSession(sessionRenameId, sessionNewTitle) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                        ) { Text("RENAME", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) }
                        Button(
                            onClick = { if (sessionRenameId.isNotBlank()) viewModel.deleteServerSession(sessionRenameId) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB0000))
                        ) { Text("DELETE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color.White) }
                    }
                }
            }
        }

        item {
            SettingsSection("RESPONSES_API") {
                var responseInput by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = responseInput,
                        onValueChange = { responseInput = it },
                        label = { Text("INPUT", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. What files are in my project?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Button(
                        onClick = {
                            if (responseInput.isNotBlank()) {
                                viewModel.createResponse(responseInput)
                                responseInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("SEND_RESPONSE_REQUEST", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.wipeSystem(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB0000)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("PURGE_ALL_LOCAL_STREAMS", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
