package com.personx.hermatic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.DisplayToolCall
import com.personx.hermatic.data.model.ToolCallStatus
import com.personx.hermatic.ui.components.MermaidView
import com.personx.hermatic.ui.components.ToolCallCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistory(
    messages: List<Message>,
    maxBubbleWidth: Dp,
    onDeleteMessage: (Long) -> Unit = { _ -> },
    onEditMessage: (Long, String) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { "${it.role}_${it.timestamp}_${it.content.hashCode()}_${it.hashCode()}" }) { message ->
            val isUser = message.role == "user"
            val bubbleColor = if (isUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
            val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val shape = if (isUser) {
                RoundedCornerShape(12.dp, 4.dp, 12.dp, 12.dp)
            } else {
                RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .align(if (isUser) Alignment.TopEnd else Alignment.TopStart)
                        .padding(start = if (isUser) 48.dp else 0.dp, end = if (isUser) 0.dp else 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isUser) "YOU" else "HERMES",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = timeFormat.format(Date(message.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(max = maxBubbleWidth)
                            .clip(shape)
                            .background(bubbleColor)
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
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = 8.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (message.content.isNotBlank()) {
                                Markdown(
                                    content = message.content,
                                    colors = DefaultMarkdownColors(
                                        text = textColor,
                                        codeBackground = Color.Black.copy(alpha = if (isDark) 0.5f else 0.1f),
                                        inlineCodeBackground = Color.Black.copy(alpha = if (isDark) 0.5f else 0.1f),
                                        dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                        tableBackground = Color.Transparent
                                    ),
                                    components = markdownComponents(
                                        codeBlock = { model ->
                                            val code = model.content
                                            if (code.trim().startsWith("graph") || code.trim().startsWith("sequenceDiagram")) {
                                                MermaidView(code, isDark)
                                            } else {
                                                Box(Modifier.fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)).padding(8.dp)) {
                                                    Text(
                                                        code,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    )
                                )
                            }

                            message.tool_calls?.let { calls ->
                                val results = message.toolResults ?: emptyMap()
                                calls.forEach { tc ->
                                    val displayTool = DisplayToolCall(
                                        callId = tc.id,
                                        name = tc.function.name,
                                        arguments = tc.function.arguments,
                                        result = results[tc.id],
                                        status = if (results.containsKey(tc.id)) ToolCallStatus.Completed else ToolCallStatus.Running
                                    )
                                    ToolCallCard(displayTool)
                                }
                            }
                        }

                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)) },
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red.copy(alpha = 0.8f)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.8f)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteMessage(message.dbId)
                                }
                            )
                        }
                    }

                    if (showEditDialog) {
                        EditMessageDialog(
                            currentContent = message.content,
                            onDismiss = { showEditDialog = false },
                            onSave = { newContent ->
                                onEditMessage(message.dbId, newContent)
                                showEditDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMessageDialog(currentContent: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(currentContent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDIT_MESSAGE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("SAVE", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
