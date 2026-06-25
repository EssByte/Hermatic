package com.personx.hermatic.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personx.hermatic.ConnectionStatus
import com.personx.hermatic.HermesViewModel
import com.personx.hermatic.ui.components.DiagnosticCard
import com.personx.hermatic.ui.components.EmptyState
import com.personx.hermatic.ui.components.SectionHeader
import com.personx.hermatic.ui.components.SkillCard
import com.personx.hermatic.ui.components.ToolsetCard
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(viewModel: HermesViewModel) {
    val skills by viewModel.skills.collectAsState()
    val toolsets by viewModel.toolsets.collectAsState()
    val rawDiagnostics by viewModel.rawDiagnostics.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "CAPABILITIES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "SYSTEM_OPERATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(onClick = { viewModel.testConnection() }, modifier = Modifier.size(28.dp)) {
                if (connectionStatus is ConnectionStatus.Testing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionHeader("ACTIVE TOOLSETS", Icons.Default.Build)
            }
            if (toolsets.isEmpty()) {
                item { EmptyState("No toolsets detected.") }
            }
            items(toolsets) { toolset ->
                ToolsetCard(toolset)
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("DETECTED SKILLS", Icons.Default.Bolt)
            }
            if (skills.isEmpty()) {
                item { EmptyState("No individual skills detected.") }
            }
            items(skills) { skill ->
                SkillCard(skill)
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("SYSTEM DIAGNOSTICS", Icons.Default.Dns)
            }
            items(rawDiagnostics.toList()) { (key, value) ->
                val parsed = parseJsonDiagnostic(value)
                DiagnosticCard(key, value, parsed)
            }
        }
    }
}

data class ParsedEntry(val key: String, val value: String)

private fun JsonElement.toDisplayString(): String = when (this) {
    is JsonPrimitive -> content
    is JsonObject -> entries.joinToString("\n") { (k, v) -> "$k: ${v.toDisplayString()}" }
    is kotlinx.serialization.json.JsonArray -> joinToString("\n") { it.toDisplayString() }
}

fun parseJsonDiagnostic(rawJson: String): List<ParsedEntry> {
    return try {
        val element = Json.Default.parseToJsonElement(rawJson)
        when (element) {
            is JsonObject -> element.entries.map { (k, v) ->
                ParsedEntry(k, v.toDisplayString())
            }
            is kotlinx.serialization.json.JsonArray -> element.mapIndexed { i, v ->
                ParsedEntry("#$i", v.toDisplayString())
            }
            is JsonPrimitive -> listOf(ParsedEntry("value", element.content))
        }
    } catch (_: Exception) {
        if (rawJson.isNotBlank()) listOf(ParsedEntry("raw", rawJson)) else emptyList()
    }
}
