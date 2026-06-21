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
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HermesUiState>(HermesUiState.Loading)
    val uiState: StateFlow<HermesUiState> = _uiState

    private var currentHistory = emptyList<Message>()

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
                if (_uiState.value !is HermesUiState.NoApiKey) {
                    _uiState.value = HermesUiState.Chatting(history)
                }
            }
        }
    }

    fun saveApiKey(key: String) {
        securityManager.saveApiKey(key)
        _uiState.value = HermesUiState.Idle
        checkApiKey()
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            // We don't manually add to currentHistory here because the observer will pick it up
            // when it's saved to the database in the repository.
            // But we can show a temporary state if needed.
            try {
                repository.chat(currentHistory + Message(role = "user", content = text))
            } catch (e: Exception) {
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
