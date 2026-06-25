package com.personx.hermatic.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personx.hermatic.HermesUiState
import com.personx.hermatic.HermesViewModel
import com.personx.hermatic.ui.components.TypingIndicator
import com.personx.hermatic.util.findActivity
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceModeScreen(viewModel: HermesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isHermesTyping.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    var transcription by remember { mutableStateOf("") }

    val history = (uiState as? HermesUiState.Chatting)?.history ?: emptyList()
    val lastAgentMsg = history.lastOrNull { it.role == "assistant" }?.content ?: ""

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val infiniteTransition = rememberInfiniteTransition(label = "voice")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                progress = (progress + 0.03f) % 1f
                kotlinx.coroutines.delay(50)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                text = if (isListening) {
                    if (transcription.isNotEmpty()) transcription.uppercase() else "LISTENING..."
                } else if (lastAgentMsg.isNotEmpty()) {
                    lastAgentMsg.uppercase()
                } else {
                    "TAP TO TRANSMIT"
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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                if (isListening) {
                    repeat(2) { index ->
                        val ringAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = index * 400),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ring_alpha_$index"
                        )
                        val ringScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 2.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = index * 400),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ring_scale_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .alpha(ringAlpha)
                                .scale(ringScale)
                                .border(1.dp, primary, CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(if (isListening) pulseScale else 1f)
                        .background(
                            if (isListening) primary
                            else onSurface.copy(alpha = 0.05f),
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
                        tint = if (isListening) MaterialTheme.colorScheme.onPrimary else primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (isListening) {
                val barCount = 11
                val barHeights = (0 until barCount).map { index ->
                    val phase = index.toFloat() / barCount
                    val raw = sin(progress * Math.PI * 4 + phase * Math.PI * 2)
                    val dynamic = raw.toFloat() * 0.5f + 0.5f
                    (dynamic * 0.8f + 0.2f).coerceIn(0.15f, 1f)
                }

                val barColor = primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(32.dp)
                ) {
                    val barWidth = size.width / (barCount * 2.5f)
                    val gap = barWidth * 1.5f
                    barHeights.forEachIndexed { index, height ->
                        drawRoundRect(
                            color = barColor,
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
                TypingIndicator()
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
}
