package com.personx.hermatic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HermesViewModel(
    private val repository: HermesRepository,
    private val securityManager: SecurityManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HermesUiState>(HermesUiState.Loading)
    val uiState: StateFlow<HermesUiState> = _uiState

    private var currentHistory = emptyList<Message>()
    private val streamingBotResponse = MutableStateFlow<String?>(null)

    init {
        checkApiKey()
        observeHistory()
    }

    private fun checkApiKey() {
        if (securityManager.getApiKey().isNullOrBlank()) {
            _uiState.value = HermesUiState.NoApiKey
        } else if (_uiState.value == HermesUiState.Loading) {
            _uiState.value = HermesUiState.Idle
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getChatHistory().collectLatest { history ->
                currentHistory = history
                updateUiState()
            }
        }
        viewModelScope.launch {
            streamingBotResponse.collectLatest { _ ->
                updateUiState()
            }
        }
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

    fun saveApiKey(key: String) {
        securityManager.saveApiKey(key)
        _uiState.value = HermesUiState.Idle
        checkApiKey()
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            try {
                val userMsg = Message(role = "user", content = text)
                // Note: repository.chatStream will save userMsg to DB, 
                // which will trigger observeHistory and update currentHistory.
                
                var botResponse = ""
                repository.chatStream(currentHistory + userMsg).collect { chunk ->
                    botResponse += chunk
                    streamingBotResponse.value = botResponse
                }
                streamingBotResponse.value = null
            } catch (e: Exception) {
                streamingBotResponse.value = null
                _uiState.value = HermesUiState.Error(e.message ?: "Unknown error", currentHistory)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
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
