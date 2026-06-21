package com.personx.hermatic

import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
                Box(Modifier.fillMaxSize()) {
                    NoisyAmbientBackground()
                    
                    if (isAuthenticated.value) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent
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
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color.Transparent
                                        ),
                                        actions = {
                                            var showSettings by remember { mutableStateOf(false) }
                                            
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.clearHistory() }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red)
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
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoisyAmbientBackground() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NousBlack)
    ) {
        // Blurred Blobs
        Box(
            modifier = Modifier
                .offset(x = screenWidth * 0.5f, y = screenHeight * 0.1f)
                .size(300.dp)
                .blur(100.dp)
                .background(BlobGreen, RoundedCornerShape(150.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = screenHeight * 0.4f)
                .size(350.dp)
                .blur(120.dp)
                .background(BlobBlue, RoundedCornerShape(175.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = screenWidth * 0.2f, y = screenHeight * 0.7f)
                .size(280.dp)
                .blur(90.dp)
                .background(BlobPurple, RoundedCornerShape(140.dp))
        )

        // Noise Overlay
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.45f)) {
            val count = 5000
            for (i in 0 until count) {
                drawCircle(
                    color = Color.White.copy(alpha = (0.01f..0.08f).random()),
                    radius = (0.5f..1.5f).random().dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x = (0f..size.width).random(),
                        y = (0f..size.height).random()
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
fun ChatHistory(messages: List<Message>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    
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
                Text(
                    text = if (isUser) "[USER]" else "[HERMES]",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
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

@Composable
fun ApiKeyScreen(onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "INITIALIZATION REQUIRED", 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            "PROVIDE ACCESS KEY TO CONTINUE", 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextField(
            value = key,
            onValueChange = { key = it },
            placeholder = { Text("API_KEY_HERE", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(key) },
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("AUTHORIZE", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsDialog(viewModel: HermesViewModel, onDismiss: () -> Unit) {
    val currentPeriod = viewModel.getSelfDestructPeriod()
    val options = listOf(
        "DISABLED" to 0L,
        "1 HOUR" to 3600_000L,
        "24 HOURS" to 86400_000L,
        "7 DAYS" to 604800_000L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text("SYSTEM CONFIGURATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black) 
        },
        text = {
            Column {
                Text("RETENTION POLICY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
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
                            onClick = { viewModel.setSelfDestructPeriod(period) }
                        )
                        Text(
                            text = label, 
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text("CLOSE", color = MaterialTheme.colorScheme.primary) 
            }
        }
    )
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
