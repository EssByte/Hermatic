package com.personx.hermatic.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ChatInput(onSend: (String, Uri?) -> Unit) {
    var message by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (selectedImageUri != null) {
            Box(
                Modifier
                    .size(100.dp)
                    .padding(bottom = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(4.dp)
        ) {
            IconButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = "Send Image",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            TextField(
                value = message,
                onValueChange = { message = it },
                placeholder = {
                    Text(
                        "INPUT_HERMES_COMMAND...",
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            val canSend = message.isNotBlank() || selectedImageUri != null

            IconButton(
                onClick = {
                    if (canSend) {
                        onSend(message, selectedImageUri)
                        message = ""
                        selectedImageUri = null
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
