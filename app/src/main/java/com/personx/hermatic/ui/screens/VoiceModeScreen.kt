package com.personx.hermatic.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personx.hermatic.HermesUiState
import com.personx.hermatic.HermesViewModel
import com.personx.hermatic.ui.components.TypingIndicator
import kotlin.math.sin

@Composable
fun VoiceModeScreen(viewModel: HermesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isHermesTyping.collectAsState()
    val isListening by viewModel.isVoiceListening.collectAsState()
    val transcription by viewModel.voiceTranscription.collectAsState()
    val rmsLevel by viewModel.voiceRmsLevel.collectAsState()

    val history = (uiState as? HermesUiState.Chatting)?.history ?: emptyList()
    val lastAgentMsg = history.lastOrNull { it.role == "assistant" }?.content ?: ""

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isListening) {
                if (transcription.isNotEmpty()) transcription.uppercase() else "LISTENING..."
            } else if (lastAgentMsg.isNotEmpty()) {
                lastAgentMsg.uppercase()
            } else {
                "TAP MIC TO TRANSMIT"
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = if (isListening) FontWeight.Light else FontWeight.Medium,
                letterSpacing = 2.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color = when {
                isListening -> primary
                isTyping -> tertiary
                else -> onSurface.copy(alpha = 0.4f)
            },
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isListening || lastAgentMsg.isNotEmpty()) 1f else 0.6f)
        )

        Spacer(Modifier.height(40.dp))

        if (isListening) {
            RmsWaveform(primary = primary, rmsLevel = rmsLevel)
        } else if (isTyping) {
            TypingWaveform(primary = tertiary)
        }

        Spacer(Modifier.height(12.dp))

        if (isListening) {
            Text(
                "RECEIVING_INPUT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = primary.copy(alpha = 0.6f)
            )
        } else if (isTyping) {
            Text(
                "AGENT_SPEAKING",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = tertiary.copy(alpha = 0.6f)
            )
        } else if (lastAgentMsg.isNotEmpty()) {
            Text(
                lastAgentMsg.uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = tertiary,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun RmsWaveform(primary: Color, rmsLevel: Float) {
    val barCount = 11
    val barHeights = (0 until barCount).map { index ->
        val phase = index.toFloat() / barCount
        val wave = (sin(phase * Math.PI * 2) * 0.3f + 0.7f).toFloat()
        (wave * rmsLevel * 0.9f + 0.1f).coerceIn(0.1f, 1f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(32.dp)
    ) {
        val barWidth = size.width / (barCount * 2.5f)
        val gap = barWidth * 1.5f
        barHeights.forEachIndexed { index, height ->
            drawRoundRect(
                color = primary,
                topLeft = Offset(
                    x = index * (barWidth + gap) + gap * 0.5f,
                    y = size.height * (1f - height) * 0.5f
                ),
                size = Size(barWidth, size.height * height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.5f)
            )
        }
    }
}

@Composable
private fun TypingWaveform(primary: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_wave")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val barCount = 5
    val barHeights = (0 until barCount).map { index ->
        val phase = index.toFloat() / barCount
        val wave = (sin((progress * 4 + phase) * Math.PI * 2) * 0.3f + 0.5f).toFloat()
        wave.coerceIn(0.15f, 1f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .height(20.dp)
    ) {
        val barWidth = size.width / (barCount * 2.5f)
        val gap = barWidth * 1.5f
        barHeights.forEachIndexed { index, height ->
            drawRoundRect(
                color = primary.copy(alpha = 0.7f),
                topLeft = Offset(
                    x = index * (barWidth + gap) + gap * 0.5f,
                    y = size.height * (1f - height) * 0.5f
                ),
                size = Size(barWidth, size.height * height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.5f)
            )
        }
    }
}
