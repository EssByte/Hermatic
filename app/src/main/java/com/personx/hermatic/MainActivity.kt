package com.personx.hermatic

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
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
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.db.HermesDatabase
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.SkillInfo
import com.personx.hermatic.data.model.ToolsetInfo
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.BiometricHelper
import com.personx.hermatic.security.SecurityManager
import com.personx.hermatic.ui.theme.*
import com.mikepenz.markdown.m3.Markdown
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val icon: ImageVector) {
    data object Chat : Screen("chat", Icons.AutoMirrored.Filled.Chat)
    data object Skills : Screen("skills", Icons.Default.Build)
    data object Settings : Screen("settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    private lateinit var securityManager: SecurityManager
    private lateinit var repository: HermesRepository

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
                            bottomBar = {
                                NavigationBar(
                                    containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RectangleShape)
                                ) {
                                    val items = listOf(Screen.Chat, Screen.Skills, Screen.Settings)
                                    items.forEach { screen ->
                                        NavigationBarItem(
                                            selected = currentScreen == screen,
                                            onClick = { currentScreen = screen },
                                            icon = { Icon(screen.icon, contentDescription = screen.route) },
                                            label = { Text(screen.route.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        ) { padding ->
                            Box(Modifier.padding(padding)) {
                                when (currentScreen) {
                                    is Screen.Chat -> ChatScreen(viewModel)
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
        CenterAlignedTopAppBar(
            title = { 
                Text("HERMATIC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) 
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                IconButton(
                    onClick = { viewModel.startNewSession() },
                    modifier = Modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.primary, RectangleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Session", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            items(sessions) { id ->
                val isSelected = id == currentSessionId
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RectangleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { viewModel.switchSession(id) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (id == "default") "MAIN" else id.takeLast(4).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

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
            ChatInput(onSend = { text, uri -> viewModel.sendMessage(context, text, uri) })
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

@Composable
fun SkillsScreen(viewModel: HermesViewModel) {
    val skills by viewModel.skills.collectAsState()
    val toolsets by viewModel.toolsets.collectAsState()
    val rawDiagnostics by viewModel.rawDiagnostics.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AGENT CAPABILITIES", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("SYSTEM SKILLS, TOOLSETS AND ENDPOINTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        
        Spacer(Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Spacer(Modifier.height(16.dp))
                SectionHeader("DETECTED SKILLS", Icons.Default.Bolt)
            }
            if (skills.isEmpty()) {
                item { EmptyState("No individual skills detected.") }
            }
            items(skills) { skill ->
                SkillCard(skill)
            }

            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("SYSTEM DIAGNOSTICS (RAW)", Icons.Default.Dns)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RectangleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)) {
                    Text(
                        rawJson,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
fun ChatHistory(messages: List<Message>, maxBubbleWidth: androidx.compose.ui.unit.Dp) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            val isUser = message.role == "user"
            val alignment = if (isUser) Alignment.End else Alignment.Start
            
            var showMenu by remember { mutableStateOf(false) }

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
                        .widthIn(max = maxBubbleWidth)
                        .border(
                            BorderStroke(1.dp, if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            RectangleShape
                        )
                        .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.2f))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showMenu = true }
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        if (message.imageUrl != null) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = "Image attachment",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RectangleShape).padding(bottom = 8.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        val content = remember(message.content) { message.content }
                        Markdown(content = content)
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
                    }
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val primaryColorHex by viewModel.primaryColor.collectAsState()
    val accentColorHex by viewModel.accentColor.collectAsState()

    var editUrl by remember { mutableStateOf(viewModel.getBaseUrl()) }
    var editKey by remember { mutableStateOf(viewModel.getApiKey()) }
    
    val context = LocalContext.current

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
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("FAILURE", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .background(Color.Red.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    else -> {}
                }

                Spacer(Modifier.height(24.dp))
                Text("THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                }
                
                Text("Theme Color (Primary)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                ColorPicker(selectedColor = primaryColorHex) { viewModel.setPrimaryColor(it) }
                
                Text("Accent Color", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                ColorPicker(selectedColor = accentColorHex) { viewModel.setAccentColor(it) }

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
            val options = listOf(
                "DISABLED" to 0L,
                "1 HOUR" to 3600_000L,
                "24 HOURS" to 86400_000L,
                "7 DAYS" to 604800_000L
            )
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
                Button(
                    onClick = { viewModel.wipeSystem(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("WIPE ALL DATA", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
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

@Composable
fun ChatInput(onSend: (String, Uri?) -> Unit) {
    var message by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (selectedImageUri != null) {
            Box(Modifier.size(80.dp).padding(bottom = 8.dp)) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RectangleShape).border(1.dp, MaterialTheme.colorScheme.primary),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.Black, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Send Image", tint = MaterialTheme.colorScheme.primary)
            }
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
                    if (message.isNotBlank() || selectedImageUri != null) {
                        onSend(message, selectedImageUri)
                        message = ""
                        selectedImageUri = null
                    }
                },
                shape = RectangleShape,
                modifier = Modifier.height(56.dp)
            ) {
                Text("SEND")
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
