package com.personx.hermatic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.db.HermesDatabase
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.BiometricHelper
import com.personx.hermatic.security.SecurityManager
import com.personx.hermatic.ui.components.NoisyAmbientBackground
import com.personx.hermatic.ui.navigation.Screen
import com.personx.hermatic.ui.screens.AuthScreen
import com.personx.hermatic.ui.screens.ChatScreen
import com.personx.hermatic.ui.screens.SettingsScreen
import com.personx.hermatic.ui.screens.SkillsScreen
import com.personx.hermatic.ui.screens.VoiceModeScreen
import com.personx.hermatic.ui.theme.HermaticTheme
import com.personx.hermatic.ui.theme.toColor
import com.personx.hermatic.util.findActivity
import com.personx.hermatic.util.findActivity
import com.personx.hermatic.voice.AudioPlayer
import com.personx.hermatic.voice.AudioRecorder
import com.personx.hermatic.voice.VoiceManager
import java.io.File

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
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
                    voiceManager.speakQueue(text)
                }
            }

            HermaticTheme(
                isDark = isDarkTheme,
                primaryColor = primaryColor,
                accentColor = accentColor
            ) {
                val authed by isAuthenticated
                val error by authErrorMessage
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Voice) }

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
                                val context = LocalContext.current
                                val activity = remember(context) { context.findActivity() }
                                BottomBar(
                                    currentScreen = currentScreen,
                                    isVoiceListening = viewModel.isVoiceListening.collectAsState().value,
                                    onNavTap = { screen ->
                                        currentScreen = screen
                                        if (screen == Screen.Voice) {
                                            viewModel.setVoiceListening(false)
                                        }
                                    },
                                    onMicTap = {
                                        if (currentScreen != Screen.Voice) {
                                            currentScreen = Screen.Voice
                                        } else if (viewModel.isVoiceListening.value) {
                                            activity?.voiceManager?.stopListening { final ->
                                                if (final.isNotBlank()) {
                                                    viewModel.sendMessage(context, final)
                                                }
                                            }
                                            viewModel.setVoiceListening(false)
                                        } else {
                                            activity?.voiceManager?.stopSpeaking()
                                            viewModel.cancelSending()
                                            activity?.voiceManager?.startListening(
                                                onPartialResult = { partial ->
                                                    viewModel.setVoiceTranscription(partial)
                                                },
                                                onRmsChanged = { level ->
                                                    viewModel.setVoiceRmsLevel(level)
                                                }
                                            )
                                            viewModel.setVoiceListening(true)
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                                when (currentScreen) {
                                    Screen.Chat -> ChatScreen(viewModel)
                                    Screen.Voice -> VoiceModeScreen(viewModel)
                                    Screen.Skills -> SkillsScreen(viewModel)
                                    Screen.Settings -> SettingsScreen(viewModel, onBack = { currentScreen = Screen.Voice })
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
private fun BottomBar(
    currentScreen: Screen,
    isVoiceListening: Boolean,
    onNavTap: (Screen) -> Unit,
    onMicTap: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "CHAT",
                selected = currentScreen == Screen.Chat,
                onClick = { onNavTap(Screen.Chat) }
            )
            NavIcon(
                icon = Icons.Default.Build,
                label = "SKILLS",
                selected = currentScreen == Screen.Skills,
                onClick = { onNavTap(Screen.Skills) }
            )
            MicNavButton(
                isListening = isVoiceListening,
                onClick = onMicTap
            )
            NavIcon(
                icon = Icons.Default.Settings,
                label = "SETTINGS",
                selected = currentScreen == Screen.Settings,
                onClick = { onNavTap(Screen.Settings) }
            )
            Spacer(Modifier.width(32.dp))
        }
    }
}

@Composable
private fun NavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MicNavButton(isListening: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isListening) {
            val ringAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_alpha"
            )
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_scale"
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .alpha(ringAlpha)
                    .scale(ringScale)
                    .border(1.5.dp, primary, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(if (isListening) primary else primary.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Voice",
                tint = if (isListening) MaterialTheme.colorScheme.onPrimary else primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
