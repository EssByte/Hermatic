package com.personx.hermatic

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.personx.hermatic.voice.AudioPlayer
import com.personx.hermatic.voice.AudioRecorder
import com.personx.hermatic.voice.VoiceManager
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.db.HermesDatabase
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.SkillInfo
import com.personx.hermatic.data.model.ToolsetInfo
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.BiometricHelper
import com.personx.hermatic.security.SecurityManager
import com.personx.hermatic.ui.theme.HermaticTheme
import com.personx.hermatic.ui.theme.NousBlack
import com.personx.hermatic.ui.theme.toColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen(val route: String, val icon: ImageVector) {
    data object Chat : Screen("chat", Icons.AutoMirrored.Filled.Chat)
    data object Voice : Screen("voice", Icons.Default.Mic)
    data object Skills : Screen("skills", Icons.Default.Build)
    data object Settings : Screen("settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    lateinit var securityManager: SecurityManager
    lateinit var repository: HermesRepository
    lateinit var audioRecorder: AudioRecorder
    lateinit var audioPlayer: AudioPlayer
    lateinit var voiceManager: VoiceManager

    private val isAuthenticated = mutableStateOf(false)
    private val authErrorMessage = mutableStateOf<String?>(null)

    override fun onStop() {
        super.onStop()
        if (securityManager.isBiometricEnabled()) {
            isAuthenticated.value = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated.value) {
            performAuthentication()
        }
    }

    fun playAudio(url: String, onComplete: () -> Unit) {
        audioPlayer.playFile(File(url), onComplete)
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun speak(text: String) {
        if (!voiceManager.speak(text)) {
            // Try to re-initialize if it failed
            android.widget.Toast.makeText(this, "TTS engine not ready. Initializing...", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.release()
        audioPlayer.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        securityManager = SecurityManager(this)
        audioRecorder = AudioRecorder(this)
        audioPlayer = AudioPlayer(this)
        voiceManager = VoiceManager(this)
        
        val database = HermesDatabase.getDatabase(this, securityManager)
        val apiClient = ApiClient(securityManager)
        
        repository = HermesRepository(apiClient, database.chatDao(), apiClient.json)

        performAuthentication()

        enableEdgeToEdge()
        setContent {
            val viewModel: HermesViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return HermesViewModel(repository, securityManager) as T
                    }
                }
            )

            val primaryColorHex by viewModel.primaryColor.collectAsState()
            val accentColorHex by viewModel.accentColor.collectAsState()
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            
            val primaryColor = primaryColorHex.toColor()
            val accentColor = accentColorHex.toColor()

            LaunchedEffect(viewModel) {
                viewModel.ttsEvent.collect { text ->
                    voiceManager.speak(text)
                }
            }

            HermaticTheme(
                isDark = isDarkTheme,
                primaryColor = primaryColor,
                accentColor = accentColor
            ) {
                val authed by isAuthenticated
                val error by authErrorMessage
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }
                
                Box(Modifier.fillMaxSize()) {
                    if (isDarkTheme) {
                        NoisyAmbientBackground(primaryColor, accentColor)
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.White))
                    }
                    
                    if (authed) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    val items = listOf(Screen.Chat, Screen.Voice, Screen.Skills, Screen.Settings)
                                    items.forEach { screen ->
                                        NavigationBarItem(
                                            selected = currentScreen == screen,
                                            onClick = { currentScreen = screen },
                                            icon = { Icon(screen.icon, contentDescription = screen.route, modifier = Modifier.size(20.dp)) },
                                            label = { Text(screen.route.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                                when (currentScreen) {
                                    is Screen.Chat -> ChatScreen(viewModel)
                                    is Screen.Voice -> VoiceModeScreen(viewModel)
                                    is Screen.Skills -> SkillsScreen(viewModel)
                                    is Screen.Settings -> SettingsScreen(viewModel, onBack = { currentScreen = Screen.Chat })
                                }
                            }
                        }
                    } else {
                        AuthScreen(error) { performAuthentication() }
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
fun AuthScreen(error: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (error != null) {
                Text("ACCESS DENIED", color = Color.Red, fontWeight = FontWeight.Black)
                Text(error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry, shape = RectangleShape) {
                    Text("RETRY AUTHENTICATION")
                }
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("DECRYPTING...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: HermesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isHermesTyping.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "HERMATIC", 
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), 
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { viewModel.clearHistory() }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Clear Chat", 
                    tint = Color.Red.copy(alpha = 0.6f), 
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        SessionSwitcher(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSessionSelected = { viewModel.switchSession(it) },
            onNewSession = { viewModel.startNewSession() }
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            val maxBubbleWidth = if (maxWidth > 600.dp) 500.dp else maxWidth * 0.85f
            
            when (val state = uiState) {
                is HermesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is HermesUiState.NoApiKey -> SetupScreen(onSave = viewModel::saveConfig)
                else -> {
                    val history = when (state) {
                        is HermesUiState.Chatting -> state.history
                        is HermesUiState.Error -> state.history
                        else -> emptyList()
                    }
                    
                    ChatHistory(messages = history, maxBubbleWidth = maxBubbleWidth)
                    if (isTyping) {
                        Box(Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)) {
                            TypingIndicator()
                        }
                    }
                    
                    if (state is HermesUiState.Error) {
                        Text(
                            text = "ERROR: ${state.message}", 
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp).align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
        
        if (uiState !is HermesUiState.NoApiKey) {
            val context = LocalContext.current
            ChatInput(onSend = { text, uri, file, transcription -> 
                viewModel.sendMessage(context, text, uri, file, transcription) 
            })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("HERMES_THINKING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.primary, RectangleShape)
            )
        }
    }
}

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
                DiagnosticCard(key, value)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyState(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun DiagnosticCard(title: String, rawJson: String) {
    var expanded by remember { mutableStateOf(false) }
    
    // Attempt to parse simple JSON for better display
    val parsedContent = remember(rawJson) {
        try {
            if (rawJson.startsWith("{") || rawJson.startsWith("[")) {
                // Very basic formatting for the UI
                rawJson.replace("{", "{\n  ").replace("}", "\n}").replace("\",", "\",\n  ")
            } else rawJson
        } catch (_: Exception) {
            rawJson
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RectangleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SettingsEthernet, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title.uppercase(), 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Text(
                        parsedContent,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolsetCard(toolset: ToolsetInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RectangleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Column {
            Text(toolset.name.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                toolset.tools.forEach { tool ->
                    Box(Modifier.background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tool, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: SkillInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RectangleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Column {
            Text(skill.name.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (!skill.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text("ID: ${skill.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
        }
    }
}

@Composable
fun NoisyAmbientBackground(primaryColor: Color, accentColor: Color) {
    val noiseDots = remember {
        List(1000) { 
            val x = (0f..1f).random()
            val y = (0f..1f).random()
            val alpha = (0.01f..0.06f).random()
            val size = (0.5f..1.2f).random()
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
                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * 0.8f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.1f),
                radius = size.width * 0.8f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.0f, size.height * 0.5f),
                    radius = size.width * 0.9f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.0f, size.height * 0.5f),
                radius = size.width * 0.9f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.1f), Color.Transparent),
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
fun SetupScreen(onSave: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://hermes-agent.nousresearch.com/") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "INITIALIZATION REQUIRED", 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            "CONFIGURE YOUR HERMES NODE", 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
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
fun TechnicalWaveform(color: Color, modifier: Modifier = Modifier, seed: String) {
    val barCount = 32
    val heights = remember(seed) {
        val random = java.util.Random(seed.hashCode().toLong())
        List(barCount) { 0.2f + random.nextFloat() * 0.8f }
    }

    Canvas(modifier = modifier.height(24.dp)) {
        val barWidth = size.width / (barCount * 1.5f)
        val spaceWidth = barWidth * 0.5f
        
        heights.forEachIndexed { index, heightFactor ->
            val x = index * (barWidth + spaceWidth)
            val barHeight = size.height * heightFactor
            val y = (size.height - barHeight) / 2
            
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun AudioMessagePlayer(audioUrl: String, transcription: String?, isUser: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val activity = context as? MainActivity
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            IconButton(
                onClick = { 
                    if (isPlaying) {
                        activity?.stopAudio()
                        isPlaying = false
                    } else {
                        activity?.playAudio(audioUrl) { isPlaying = false }
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(36.dp).background(
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RectangleShape
                ).border(0.5.dp, if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RectangleShape)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            TechnicalWaveform(
                color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
                seed = audioUrl
            )
        }
        
        if (!transcription.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RectangleShape
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        "TRANSCRIPTION_DECODED:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = transcription.uppercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
@Composable
fun SessionSwitcher(
    sessions: List<String>,
    currentSessionId: String,
    onSessionSelected: (String) -> Unit,
    onNewSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "STREAMS:", 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(end = 8.dp)
        )
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(sessions) { id ->
                val isSelected = id == currentSessionId
                Text(
                    text = if (id == "default") "00" else id.takeLast(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onSessionSelected(id) }
                        .padding(horizontal = 4.dp)
                )
            }
        }
        IconButton(onClick = onNewSession, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Add, contentDescription = "New Session", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun ChatHistory(messages: List<Message>, maxBubbleWidth: androidx.compose.ui.unit.Dp) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val activity = context as? MainActivity
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            val isUser = message.role == "user"
            val alignment = if (isUser) Alignment.End else Alignment.Start
            
            var showMenu by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUser) "[U]" else "[H]",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    )
                }
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(max = maxBubbleWidth)
                        .border(
                            BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            RectangleShape
                        )
                        .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.1f))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { 
                                showMenu = true 
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (message.imageUrl != null) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = "Image attachment",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RectangleShape).padding(bottom = 6.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        if (message.audioUrl != null) {
                            AudioMessagePlayer(
                                audioUrl = message.audioUrl,
                                transcription = message.transcription,
                                isUser = isUser
                            )
                        } else {
                            val content = remember(message.content) { message.content }
                            Markdown(
                                content = content,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.content))
                                showMenu = false
                            }
                        )
                        if (!isUser) {
                            DropdownMenuItem(
                                text = { Text("Speak (TTS)", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    activity?.speak(message.content)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceModeScreen(viewModel: HermesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isHermesTyping.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    var transcription by remember { mutableStateOf("") }
    
    val history = (uiState as? HermesUiState.Chatting)?.history ?: emptyList()
    val lastUserMsg = history.lastOrNull { it.role == "user" }?.content ?: ""
    val lastAgentMsg = history.lastOrNull { it.role == "assistant" }?.content ?: ""

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp), // Space for nav bar and bottom spacing
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // 1. User Input (Above the button)
            val displayInput = if (isListening) transcription else lastUserMsg
            Text(
                text = displayInput.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = if (isListening) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (displayInput.isEmpty()) 0f else 1f)
            )

            Spacer(Modifier.height(48.dp))

            // 2. The Voice Button (Center piece)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                if (isListening) {
                    repeat(2) { index ->
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = index * 400),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ring_alpha"
                        )
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 2.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = index * 400),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ring_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .alpha(alpha)
                                .scale(scale)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(if (isListening) pulseScale else 1f)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            CircleShape
                        )
                        .clickable {
                            if (!isListening) {
                                activity?.voiceManager?.startListening { transcription = it }
                                isListening = true
                            } else {
                                activity?.voiceManager?.stopListening { final ->
                                    if (final.isNotBlank()) {
                                        viewModel.sendMessage(context, final)
                                    }
                                    isListening = false
                                    transcription = ""
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isListening) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // 3. Agent Response (Below the button)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isTyping) {
                    TypingIndicator()
                } else {
                    Text(
                        text = lastAgentMsg.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp,
                            letterSpacing = 1.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(if (lastAgentMsg.isEmpty()) 0f else 1f)
                    )
                }
            }
        }
    }
}

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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val primaryColorHex by viewModel.primaryColor.collectAsState()
    val accentColorHex by viewModel.accentColor.collectAsState()

    var editUrl by remember { mutableStateOf(viewModel.getBaseUrl()) }
    var editKey by remember { mutableStateOf(viewModel.getApiKey()) }
    
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "SYSTEM_CONFIG", 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "TERMINAL_PREFERENCES", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsSection("NETWORK_INTERFACE") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            label = { Text("BASE_ENDPOINT", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                            )
                        )
                        TextField(
                            value = editKey,
                            onValueChange = { editKey = it },
                            label = { Text("ACCESS_TOKEN", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                            )
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.testConnection() },
                                shape = RectangleShape,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                if (connectionStatus is ConnectionStatus.Testing) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("TEST_LINK", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Button(
                                onClick = { viewModel.saveConfig(editKey, editUrl) },
                                shape = RectangleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SYNCHRONIZE", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        ConnectionStatusIndicator(connectionStatus)
                    }
                }
            }

            item {
                SettingsSection("INTERFACE_STYLING") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("HIGH_CONTRAST_DARK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AUTO_AGENT_VOICE_REPLY", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            val isAutoTtsEnabled by viewModel.isAutoTtsEnabled.collectAsState()
                            Switch(checked = isAutoTtsEnabled, onCheckedChange = { viewModel.setAutoTtsEnabled(it) })
                        }
                        
                        Column {
                            Text("PRIMARY_HEX_VALUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            ColorPicker(selectedColor = primaryColorHex) { viewModel.setPrimaryColor(it) }
                        }
                        
                        Column {
                            Text("ACCENT_HEX_VALUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            ColorPicker(selectedColor = accentColorHex) { viewModel.setAccentColor(it) }
                        }
                    }
                }
            }

            item {
                SettingsSection("SECURITY_PROTOCOL") {
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
                                    shape = RectangleShape,
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Text(selectedModel.ifEmpty { "SELECT_MODEL" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
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
                                shape = RectangleShape,
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
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.wipeSystem(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB0000)),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("PURGE_ALL_LOCAL_STREAMS", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RectangleShape)
                .background(Color.White.copy(alpha = 0.02f))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    when (status) {
        is ConnectionStatus.Success -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("LINK_ESTABLISHED", color = Color.Green, style = MaterialTheme.typography.labelSmall)
            }
        }
        is ConnectionStatus.Error -> {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LINK_FAILURE", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
                Text(status.message, color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
        else -> {}
    }
}

@Composable
fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#FFFFFF", "#00FF00", "#0066FF", "#AA00FF", "#FF0066", "#FFFF00")
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(hex.toColor())
                    .border(
                        width = if (selectedColor == hex) 2.dp else 1.dp,
                        color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

fun Context.findActivity(): MainActivity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is MainActivity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun ChatInput(onSend: (String, Uri?, File?, String?) -> Unit) {
    var message by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Voice Recording States
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var transcription by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, recording will work on next interaction 
            // or we could trigger it here if we stored the state.
        } else {
            android.widget.Toast.makeText(context, "Mic permission required", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (selectedImageUri != null) {
            Box(
                Modifier
                    .size(100.dp)
                    .padding(bottom = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RectangleShape)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.7f), RectangleShape)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Remove", 
                        tint = Color.White, 
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RectangleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(4.dp)
        ) {
            if (isRecording) {
                Row(
                    modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recording")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red.copy(alpha = alpha), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (transcription.isNotBlank()) transcription.uppercase()
                        else if (isLocked) "RECORDING_LOCKED..." 
                        else "SLIDE_UP_TO_LOCK", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "CANCEL", 
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clickable { 
                            activity?.audioRecorder?.stop()
                            activity?.voiceManager?.stopListening {}
                            isRecording = false
                            isLocked = false
                            swipeOffset = 0f
                            transcription = ""
                        },
                        color = Color.Red.copy(alpha = 0.7f)
                    )
                }
            } else {
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate, 
                        contentDescription = "Send Image", 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { 
                        Text(
                            "INPUT_HERMES_COMMAND...", 
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            val canSend = message.isNotBlank() || selectedImageUri != null || isLocked
            val isActionable = canSend || isRecording || (message.isBlank() && selectedImageUri == null)
            val showMic = (message.isBlank() && selectedImageUri == null) || isRecording || isLocked

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .width(60.dp)
                    .height(48.dp)
                    .offset { IntOffset(0, swipeOffset.roundToInt()) }
                    .background(
                        if (isActionable) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RectangleShape
                    )
                    .pointerInput(message, selectedImageUri) {
                        if (message.isBlank() && selectedImageUri == null) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown()
                                    if (isLocked) {
                                        waitForUpOrCancellation()
                                        activity?.voiceManager?.stopListening { finalTranscription ->
                                            activity.audioRecorder.stop()
                                            val files = context.filesDir.listFiles { f -> f.name.startsWith("voice_") }
                                            val latestFile = files?.maxByOrNull { it.lastModified() }
                                            onSend("", null, latestFile, finalTranscription)
                                            isRecording = false
                                            isLocked = false
                                            transcription = ""
                                        }
                                        continue
                                    }
                                    
                                    // Start recording - check permission first
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val file = File(context.filesDir, "voice_${System.currentTimeMillis()}.mp4")
                                        activity?.audioRecorder?.start(file)
                                        transcription = ""
                                        activity?.voiceManager?.startListening { transcription = it }
                                        isRecording = true
                                        isLocked = false
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        return@awaitPointerEventScope
                                    }
                                    
                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.find { it.id == pointerId }
                                        if (change == null || change.isConsumed) break
                                        
                                        if (change.changedToUp()) {
                                            if (isRecording && !isLocked) {
                                                activity?.voiceManager?.stopListening { finalTranscription ->
                                                    activity.audioRecorder.stop()
                                                    val files = context.filesDir.listFiles { f -> f.name.startsWith("voice_") }
                                                    val latestFile = files?.maxByOrNull { it.lastModified() }
                                                    onSend("", null, latestFile, finalTranscription)
                                                    isRecording = false
                                                    swipeOffset = 0f
                                                    transcription = ""
                                                }
                                            }
                                            break
                                        } else {
                                            val dragAmount = change.position.y - change.previousPosition.y
                                            if (isRecording && !isLocked) {
                                                swipeOffset = (swipeOffset + dragAmount).coerceIn(-200f, 0f)
                                                if (swipeOffset <= -150f) {
                                                    isLocked = true
                                                    swipeOffset = 0f
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .then(
                        if (message.isNotBlank() || selectedImageUri != null) {
                            Modifier.clickable {
                                onSend(message, selectedImageUri, null, null)
                                message = ""
                                selectedImageUri = null
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (showMic) Icons.Default.Mic else Icons.Default.ArrowUpward, 
                    contentDescription = "Action", 
                    tint = if (isActionable) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        
        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width + mainAxisSpacing.roundToPx() > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacing.roundToPx()
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val height = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * crossAxisSpacing.roundToPx()
        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + mainAxisSpacing.roundToPx()
                }
                y += row.maxOf { it.height } + crossAxisSpacing.roundToPx()
            }
        }
    }
}
