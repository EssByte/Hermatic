package com.personx.hermatic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.db.HermesDatabase
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.BiometricHelper
import com.personx.hermatic.security.SecurityManager
import com.personx.hermatic.ui.theme.HermaticTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var repository: HermesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        securityManager = SecurityManager(this)
        
        val database = HermesDatabase.getDatabase(this, securityManager)
        val apiClient = ApiClient(securityManager)
        
        repository = HermesRepository(apiClient.hermesApi, database.chatDao(), apiClient.json)

        val biometricHelper = BiometricHelper(this)
        val isAuthenticated = mutableStateOf(false)

        biometricHelper.authenticate(
            onSuccess = { isAuthenticated.value = true },
            onError = { /* Handle error */ }
        )

        enableEdgeToEdge()
        setContent {
            HermaticTheme {
                if (isAuthenticated.value) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        val viewModel: HermesViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return HermesViewModel(repository, securityManager) as T
                                }
                            }
                        )
                        
                        Scaffold(
                            modifier = Modifier.padding(innerPadding),
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("Hermatic") },
                                    actions = {
                                        var showSettings by remember { mutableStateOf(false) }
                                        
                                        IconButton(onClick = { showSettings = true }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                        IconButton(onClick = { viewModel.clearHistory() }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                                        }
                                        
                                        if (showSettings) {
                                            SettingsDialog(
                                                viewModel = viewModel,
                                                onDismiss = { showSettings = false }
                                            )
                                        }
                                    }
                                )
                            }
                        ) { chatPadding ->
                            HermesApp(viewModel, Modifier.padding(chatPadding))
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Authentication required")
                    }
                }
            }
        }
    }
}

@Composable
fun HermesApp(viewModel: HermesViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val state = uiState) {
            is HermesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is HermesUiState.NoApiKey -> ApiKeyScreen(onSave = viewModel::saveApiKey)
            else -> {
                val history = when (state) {
                    is HermesUiState.Chatting -> state.history
                    is HermesUiState.Error -> state.history
                    else -> emptyList()
                }
                ChatHistory(
                    messages = history,
                    modifier = Modifier.weight(1f)
                )
                if (state is HermesUiState.Error) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                ChatInput(onSend = viewModel::sendMessage)
            }
        }
    }
}

@Composable
fun ChatHistory(messages: List<Message>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(messages) { message ->
            val isUser = message.role == "user"
            val alignment = if (isUser) Alignment.End else Alignment.Start
            val color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .padding(12.dp)
                ) {
                    Text(text = message.content)
                }
            }
        }
    }
}

@Composable
fun ApiKeyScreen(onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter Hermes API Key", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSave(key) }) {
            Text("Save")
        }
    }
}

@Composable
fun SettingsDialog(viewModel: HermesViewModel, onDismiss: () -> Unit) {
    val currentPeriod = viewModel.getSelfDestructPeriod()
    val options = listOf(
        "Disabled" to 0L,
        "1 Hour" to 3600_000L,
        "24 Hours" to 86400_000L,
        "7 Days" to 604800_000L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("Self-Destruct Messages", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { (label, period) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        RadioButton(
                            selected = currentPeriod == period,
                            onClick = { 
                                viewModel.setSelfDestructPeriod(period)
                            }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ChatInput(onSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Type a message...") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
            if (message.isNotBlank()) {
                onSend(message)
                message = ""
            }
        }) {
            Text("Send")
        }
    }
}
