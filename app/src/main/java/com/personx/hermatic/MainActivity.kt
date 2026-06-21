package com.personx.hermatic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.db.HermesDatabase
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.BiometricHelper
import com.personx.hermatic.security.SecurityManager
import com.personx.hermatic.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    data object Chat : Screen()
    data object Settings : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var repository: HermesRepository

    private val isAuthenticated = mutableStateOf(false)
    private val authErrorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        securityManager = SecurityManager(this)
        
        val database = HermesDatabase.getDatabase(this, securityManager)
        val apiClient = ApiClient(securityManager)
        
        repository = HermesRepository(apiClient, database.chatDao(), apiClient.json)

        performAuthentication()

        enableEdgeToEdge()
        setContent {
            HermaticTheme {
                val authed by isAuthenticated
                val error by authErrorMessage
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }
                
                Box(Modifier.fillMaxSize()) {
                    NoisyAmbientBackground()
                    
                    if (authed) {
                        val viewModel: HermesViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return HermesViewModel(repository, securityManager) as T
                                }
                            }
                        )

                        when (currentScreen) {
                            is Screen.Chat -> {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = Color.Transparent,
                                    topBar = {
                                        CenterAlignedTopAppBar(
                                            title = { 
                                                Text(
                                                    "HERMATIC", 
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Black
                                                ) 
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                                            actions = {
                                                IconButton(onClick = { currentScreen = Screen.Settings }) {
                                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { viewModel.clearHistory() }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red)
                                                }
                                            }
                                        )
                                    }
                                ) { chatPadding ->
                                    HermesApp(viewModel, Modifier.padding(chatPadding))
                                }
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.Chat }
                                )
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (error != null) {
                                    Text("Authentication Error: $error", color = Color.Red)
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { performAuthentication() }, shape = RectangleShape) {
                                        Text("RETRY AUTHENTICATION")
                                    }
                                } else {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun performAuthentication() {
        val biometricHelper = BiometricHelper(this)
        authErrorMessage.value = null

        if (securityManager.isBiometricEnabled() && biometricHelper.canAuthenticate()) {
            biometricHelper.authenticate(
                onSuccess = { isAuthenticated.value = true },
                onError = { message -> authErrorMessage.value = message }
            )
        } else {
            isAuthenticated.value = true
        }
    }
}

@Composable
fun NoisyAmbientBackground() {
    val noiseDots = remember {
        List(1500) {
            val x = (0f..1f).random()
            val y = (0f..1f).random()
            val alpha = (0.01f..0.08f).random()
            val size = (0.5f..1.5f).random()
            Triple(androidx.compose.ui.geometry.Offset(x, y), alpha, size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NousBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobGreen, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 0.8f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                radius = size.width * 0.8f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobBlue, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.0f, size.height * 0.5f),
                    radius = size.width * 0.9f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.0f, size.height * 0.5f),
                radius = size.width * 0.9f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobPurple, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.9f),
                    radius = size.width * 0.7f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.9f),
                radius = size.width * 0.7f
            )

            noiseDots.forEach { (offset, alpha, radius) ->
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x = offset.x * size.width,
                        y = offset.y * size.height
                    )
                )
            }
        }
    }
}

private fun ClosedRange<Float>.random() =
    (kotlin.random.Random.nextFloat() * (endInclusive - start)) + start

@Composable
fun HermesApp(viewModel: HermesViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (val state = uiState) {
            is HermesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is HermesUiState.NoApiKey -> SetupScreen(onSave = viewModel::saveConfig)
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
                    Text(
                        text = "ERROR: ${state.message}", 
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                ChatInput(onSend = viewModel::sendMessage)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SetupScreen(onSave: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://hermes-agent.nousresearch.com/") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "INITIALIZATION REQUIRED", 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "CONFIGURE YOUR HERMES NODE", 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("BASE_URL", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start))
        TextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text("https://your-hermes-server.com/", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            "Use http://10.0.2.2:PORT/ for local development in emulator.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("API_KEY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start))
        TextField(
            value = key,
            onValueChange = { key = it },
            placeholder = { Text("Enter your API key", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onSave(key, url) },
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("SAVE & INITIALIZE", fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun ChatHistory(messages: List<Message>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            val isUser = message.role == "user"
            val alignment = if (isUser) Alignment.End else Alignment.Start
            
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUser) "[USER]" else "[HERMES]",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(max = 300.dp)
                        .border(
                            BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            RectangleShape
                        )
                        .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

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
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var editUrl by remember { mutableStateOf(viewModel.getBaseUrl()) }
    var editKey by remember { mutableStateOf(viewModel.getApiKey()) }

    val options = listOf(
        "DISABLED" to 0L,
        "1 HOUR" to 3600_000L,
        "24 HOURS" to 86400_000L,
        "7 DAYS" to 604800_000L
    )

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SYSTEM CONFIGURATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("CONNECTIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                TextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text("BASE_URL", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RectangleShape,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                TextField(
                    value = editKey,
                    onValueChange = { editKey = it },
                    label = { Text("API_KEY", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RectangleShape,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(
                        onClick = { viewModel.testConnection() },
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TEST CONNECTION", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.saveConfig(editKey, editUrl) },
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("APPLY CHANGES", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                    }
                }

                when (connectionStatus) {
                    is ConnectionStatus.Testing -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Pinging node...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ConnectionStatus.Success -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Node reachable", color = Color.Green, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ConnectionStatus.Error -> {
                        val msg = (connectionStatus as ConnectionStatus.Error).message
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Failure: $msg", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    else -> {}
                }

                Spacer(Modifier.height(24.dp))
                Text("SECURITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Checkbox(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                    Text("Enable Biometric Lock", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }
                
                Spacer(Modifier.height(24.dp))
                Text("PERSONA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                TextField(
                    value = systemPrompt,
                    onValueChange = { viewModel.setSystemPrompt(it) },
                    placeholder = { Text("System instructions...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RectangleShape,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(24.dp))
                Text("MODEL CONTROL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedModel.ifEmpty { "Select Model" }, style = MaterialTheme.typography.bodySmall)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
                        if (availableModels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("hermes-agent", style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    viewModel.setSelectedModel("hermes-agent")
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("TEMPERATURE: ${String.format(Locale.US, "%.1f", temperature)}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = temperature,
                    onValueChange = { viewModel.setTemperature(it) },
                    valueRange = 0f..1f,
                    steps = 10
                )

                Spacer(Modifier.height(8.dp))
                Text("MAX TOKENS: $maxTokens", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                    valueRange = 256f..4096f,
                    steps = 15
                )

                Spacer(Modifier.height(24.dp))
                Text("RETENTION POLICY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(options) { (label, period) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    RadioButton(
                        selected = currentPeriod == period,
                        onClick = { viewModel.setSelfDestructPeriod(period) }
                    )
                    Text(
                        text = label, 
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ChatInput(onSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("INPUT_MESSAGE_PROMPT...", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.weight(1f),
            shape = RectangleShape,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (message.isNotBlank()) {
                    onSend(message)
                    message = ""
                }
            },
            shape = RectangleShape,
            modifier = Modifier.height(56.dp)
        ) {
            Text("SEND")
        }
    }
}
