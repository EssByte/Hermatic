package com.personx.hermatic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.repository.HermesRepository
import com.personx.hermatic.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HermesViewModel(
    private val repository: HermesRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HermesUiState>(HermesUiState.Loading)
    val uiState: StateFlow<HermesUiState> = _uiState

    private val history = mutableListOf<Message>()

    init {
        checkApiKey()
    }

    private fun checkApiKey() {
        if (securityManager.getApiKey().isNullOrBlank()) {
            _uiState.value = HermesUiState.NoApiKey
        } else {
            _uiState.value = HermesUiState.Idle
        }
    }

    fun saveApiKey(key: String) {
        securityManager.saveApiKey(key)
        _uiState.value = HermesUiState.Idle
    }

    fun sendMessage(text: String) {
        val userMsg = Message(role = "user", content = text)
        history.add(userMsg)
        _uiState.value = HermesUiState.Chatting(history.toList())
        
        viewModelScope.launch {
            try {
                val response = repository.chat(history)
                val botMsg = Message(role = "assistant", content = response)
                history.add(botMsg)
                _uiState.value = HermesUiState.Chatting(history.toList())
            } catch (e: Exception) {
                _uiState.value = HermesUiState.Error(e.message ?: "Unknown error", history.toList())
            }
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
