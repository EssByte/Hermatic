package com.personx.hermatic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personx.hermatic.data.model.DisplayToolCall
import com.personx.hermatic.data.model.ToolCallStatus

@Composable
fun ToolCallCard(toolCall: DisplayToolCall) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (toolCall.status) {
        ToolCallStatus.Running -> MaterialTheme.colorScheme.tertiary
        ToolCallStatus.Completed -> MaterialTheme.colorScheme.primary
        ToolCallStatus.Failed -> Color.Red.copy(alpha = 0.8f)
    }

    val statusIcon = when (toolCall.status) {
        ToolCallStatus.Running -> Icons.Default.HourglassEmpty
        ToolCallStatus.Completed -> Icons.Default.CheckCircle
        ToolCallStatus.Failed -> Icons.Default.HourglassEmpty
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    toolCall.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        "ARGS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        toolCall.arguments,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (toolCall.result != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            toolCall.result.take(500),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
