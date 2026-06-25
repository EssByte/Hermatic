package com.personx.hermatic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.personx.hermatic.ui.theme.NousBlack
import com.personx.hermatic.ui.theme.toColor
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
                                    Screen.Chat -> ChatScreen(viewModel)
                                    Screen.Voice -> VoiceModeScreen(viewModel)
                                    Screen.Skills -> SkillsScreen(viewModel)
                                    Screen.Settings -> SettingsScreen(viewModel, onBack = { currentScreen = Screen.Chat })
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
