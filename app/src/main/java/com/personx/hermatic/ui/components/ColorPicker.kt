package com.personx.hermatic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personx.hermatic.ui.theme.toColor

@Composable
fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#FFFFFF", "#00FF00", "#0066FF", "#AA00FF", "#FF0066", "#FFFF00")
    Row(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(hex.toColor())
                    .border(
                        width = if (selectedColor == hex) 2.dp else 1.dp,
                        color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}
