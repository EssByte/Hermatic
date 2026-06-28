package com.personx.hermatic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personx.hermatic.HermesUiState
import com.personx.hermatic.HermesViewModel
import com.personx.hermatic.ui.components.ChatInput
import com.personx.hermatic.ui.components.SessionSwitcher
import com.personx.hermatic.ui.components.TypingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: HermesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isHermesTyping.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val sessionTitles = remember(sessions) {
        sessions.associateWith { viewModel.getSessionTitle(it) ?: it.take(8).uppercase() }
    }

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
            sessionTitles = sessionTitles,
            onSessionSelected = { viewModel.switchSession(it) },
            onNewSession = { viewModel.startNewSession() },
            onRenameSession = { id, title -> viewModel.renameSession(id, title) },
            onDeleteSession = { viewModel.deleteLocalSession(it) }
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

                    ChatHistory(
                        messages = history,
                        maxBubbleWidth = maxBubbleWidth,
                        onDeleteMessage = { viewModel.deleteMessage(it) },
                        onEditMessage = { id, content -> viewModel.editMessage(id, content) }
                    )
                    if (isTyping) {
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TypingIndicator()
                            IconButton(
                                onClick = { viewModel.cancelSending() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(start = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
            ChatInput(onSend = { text, uri ->
                viewModel.sendMessage(context, text, uri)
            })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
