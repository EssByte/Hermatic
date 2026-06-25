package com.personx.hermatic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personx.hermatic.ConnectionStatus

@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    when (status) {
        is ConnectionStatus.Success -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("LINK_ESTABLISHED", color = Color.Green, style = MaterialTheme.typography.labelSmall)
            }
        }
        is ConnectionStatus.Error -> {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LINK_FAILURE", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
                Text(status.message, color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
        else -> {}
    }
}
