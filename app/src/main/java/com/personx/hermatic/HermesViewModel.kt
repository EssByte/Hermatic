package com.personx.hermatic

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personx.hermatic.data.model.*
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HermesViewModel(
    private val repository: HermesRepository,
    private val securityManager: SecurityManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HermesUiState>(HermesUiState.Loading)
    val uiState: StateFlow<HermesUiState> = _uiState

    private val _isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    private val _selfDestructPeriod = MutableStateFlow(securityManager.getSelfDestructPeriod())
    val selfDestructPeriod: StateFlow<Long> = _selfDestructPeriod

    private val _systemPrompt = MutableStateFlow(securityManager.getSystemPrompt())
    val systemPrompt: StateFlow<String> = _systemPrompt

    private val _temperature = MutableStateFlow(securityManager.getTemperature())
    val temperature: StateFlow<Float> = _temperature

    private val _maxTokens = MutableStateFlow(securityManager.getMaxTokens())
    val maxTokens: StateFlow<Int> = _maxTokens

    private val _selectedModel = MutableStateFlow(securityManager.getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel

    private val _primaryColor = MutableStateFlow(securityManager.getPrimaryColor())
    val primaryColor: StateFlow<String> = _primaryColor

    private val _accentColor = MutableStateFlow(securityManager.getAccentColor())
    val accentColor: StateFlow<String> = _accentColor

    private val _isDarkMode = MutableStateFlow(securityManager.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isAutoTtsEnabled = MutableStateFlow(securityManager.isAutoTtsEnabled())
    val isAutoTtsEnabled: StateFlow<Boolean> = _isAutoTtsEnabled

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _skills = MutableStateFlow<List<SkillInfo>>(emptyList())
    val skills: StateFlow<List<SkillInfo>> = _skills

    private val _toolsets = MutableStateFlow<List<ToolsetInfo>>(emptyList())
    val toolsets: StateFlow<List<ToolsetInfo>> = _toolsets

    private val _rawDiagnostics = MutableStateFlow<Map<String, String>>(emptyMap())
    val rawDiagnostics: StateFlow<Map<String, String>> = _rawDiagnostics

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _isHermesTyping = MutableStateFlow(false)
    val isHermesTyping: StateFlow<Boolean> = _isHermesTyping

    private val _sessions = MutableStateFlow<List<String>>(listOf("default"))
    val sessions: StateFlow<List<String>> = _sessions

    private val _currentSessionId = MutableStateFlow("default")
    val currentSessionId: StateFlow<String> = _currentSessionId

    private var currentHistory = emptyList<Message>()
    private val streamingBotResponse = MutableStateFlow<String?>(null)

    init {
        checkApiKey()
        observeHistory()
        observeSessions()
        triggerSelfDestruct()
        fetchInitialData()
    }

    private fun fetchInitialData() {
        if (securityManager.getApiKey().isNullOrBlank()) return
        
        viewModelScope.launch {
            try {
                fetchModels()
                fetchSkills()
                fetchDiagnostics()
            } catch (e: Exception) {
                // Ignore silent failures
            }
        }
    }

    private suspend fun fetchModels() {
        withContext(Dispatchers.IO) {
            val models = repository.getModels()
            _availableModels.value = models
        }
    }

    private suspend fun fetchSkills() {
        withContext(Dispatchers.IO) {
            _skills.value = repository.getSkills()
            _toolsets.value = repository.getToolsets()
        }
    }

    private fun fetchDiagnostics() {
        viewModelScope.launch {
            val diag = withContext(Dispatchers.IO) {
                val d = mutableMapOf<String, String>()
                d["Capabilities"] = repository.getCapabilities()
                d["Jobs"] = repository.getJobs()
                d
            }
            _rawDiagnostics.value = diag
        }
    }

    private fun triggerSelfDestruct() {
        viewModelScope.launch {
            val period = securityManager.getSelfDestructPeriod()
            if (period > 0) {
                repository.performSelfDestruct(period)
            }
        }
    }

    private fun checkApiKey() {
        if (securityManager.getApiKey().isNullOrBlank()) {
            _uiState.value = HermesUiState.NoApiKey
        } else if (_uiState.value == HermesUiState.Loading) {
            _uiState.value = HermesUiState.Idle
        }
    }

    private fun observeHistory() {
        _currentSessionId.flatMapLatest { sessionId ->
            repository.getChatHistory(sessionId)
        }.onEach { history ->
            currentHistory = history
            updateUiState()
        }.launchIn(viewModelScope)

        streamingBotResponse.onEach {
            updateUiState()
        }.launchIn(viewModelScope)
    }
    
    private fun observeSessions() {
        repository.getSessions().onEach { sessionIds ->
            if (sessionIds.isEmpty()) {
                _sessions.value = listOf("default")
            } else {
                _sessions.value = sessionIds
            }
        }.launchIn(viewModelScope)
    }

    private fun updateUiState() {
        if (_uiState.value is HermesUiState.NoApiKey) return
        
        val historyWithStream = if (streamingBotResponse.value != null) {
            currentHistory + Message(role = "assistant", content = streamingBotResponse.value!!)
        } else {
            currentHistory
        }
        _uiState.value = HermesUiState.Chatting(historyWithStream)
    }

    fun saveConfig(key: String, url: String) {
        securityManager.saveApiKey(key)
        securityManager.saveBaseUrl(url)
        _uiState.value = HermesUiState.Idle
        checkApiKey()
        fetchInitialData()
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Testing
            val url = securityManager.getBaseUrl()
            val result = withContext(Dispatchers.IO) {
                repository.checkHealth()
            }
            result.onSuccess {
                _connectionStatus.value = ConnectionStatus.Success
            }
            result.onFailure { error ->
                val detailedError = "[URL: $url] ${error.localizedMessage ?: "Unknown error"}"
                _connectionStatus.value = ConnectionStatus.Error(detailedError)
            }
        }
    }

    fun getBaseUrl(): String = securityManager.getBaseUrl()
    fun getApiKey(): String = securityManager.getApiKey() ?: ""

    fun sendMessage(context: Context, text: String, imageUri: Uri? = null, audioFile: File? = null, transcription: String? = null) {
        viewModelScope.launch {
            try {
                _isHermesTyping.value = true
                
                val userMsg = Message(
                    role = "user", 
                    content = if (audioFile != null) transcription ?: "[VOICE_MESSAGE]" else text,
                    imageUrl = imageUri?.toString(),
                    audioUrl = audioFile?.absolutePath,
                    transcription = transcription
                )
                
                var botResponse = ""
                repository.chatStream(
                    sessionId = _currentSessionId.value,
                    messages = currentHistory + userMsg,
                    model = _selectedModel.value,
                    temperature = _temperature.value,
                    maxTokens = _maxTokens.value,
                    systemPrompt = _systemPrompt.value
                ).collect { chunk ->
                    _isHermesTyping.value = false
                    botResponse += chunk
                    streamingBotResponse.value = botResponse
                }
                
                // After streaming finished, if auto-tts is enabled, speak it
                if (_isAutoTtsEnabled.value && botResponse.isNotEmpty()) {
                    // We'll handle this in MainActivity by observing something or 
                    // just calling a speak function if we had access to VoiceManager here.
                    // Better to expose a flow of "new messages to speak".
                    _ttsEvent.emit(botResponse)
                }
                
                streamingBotResponse.value = null
            } catch (e: Exception) {
                _isHermesTyping.value = false
                streamingBotResponse.value = null
                _uiState.value = HermesUiState.Error(e.message ?: "Unknown error", currentHistory)
            }
        }
    }

    private val _ttsEvent = MutableSharedFlow<String>()
    val ttsEvent: SharedFlow<String> = _ttsEvent

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(_currentSessionId.value)
        }
    }
    
    fun startNewSession() {
        val newId = "session_${UUID.randomUUID().toString().take(8)}"
        _currentSessionId.value = newId
    }
    
    fun switchSession(id: String) {
        _currentSessionId.value = id
    }

    fun setSelfDestructPeriod(periodMs: Long) {
        securityManager.setSelfDestructPeriod(periodMs)
        _selfDestructPeriod.value = periodMs
        triggerSelfDestruct()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun setSystemPrompt(prompt: String) {
        securityManager.saveSystemPrompt(prompt)
        _systemPrompt.value = prompt
    }

    fun setTemperature(temp: Float) {
        securityManager.saveTemperature(temp)
        _temperature.value = temp
    }

    fun setMaxTokens(tokens: Int) {
        securityManager.saveMaxTokens(tokens)
        _maxTokens.value = tokens
    }

    fun setSelectedModel(model: String) {
        securityManager.saveSelectedModel(model)
        _selectedModel.value = model
    }

    fun setPrimaryColor(hex: String) {
        securityManager.savePrimaryColor(hex)
        _primaryColor.value = hex
    }

    fun setAccentColor(hex: String) {
        securityManager.saveAccentColor(hex)
        _accentColor.value = hex
    }

    fun setDarkMode(enabled: Boolean) {
        securityManager.setDarkMode(enabled)
        _isDarkMode.value = enabled
    }

    fun setAutoTtsEnabled(enabled: Boolean) {
        securityManager.setAutoTtsEnabled(enabled)
        _isAutoTtsEnabled.value = enabled
    }

    fun wipeSystem(context: Context) {
        viewModelScope.launch {
            securityManager.wipeAllData(context)
            _uiState.value = HermesUiState.NoApiKey
            currentHistory = emptyList()
            _sessions.value = listOf("default")
            _currentSessionId.value = "default"
        }
    }
}

sealed class HermesUiState {
    data object Loading : HermesUiState()
    data object NoApiKey : HermesUiState()
    data object Idle : HermesUiState()
    data class Chatting(val history: List<Message>) : HermesUiState()
    data class Error(val message: String, val history: List<Message> = emptyList()) : HermesUiState()
}

sealed class ConnectionStatus {
    data object Idle : ConnectionStatus()
    data object Testing : ConnectionStatus()
    data object Success : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
