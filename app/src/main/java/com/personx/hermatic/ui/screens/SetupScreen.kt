package com.personx.hermatic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(onSave: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://hermes-agent.nousresearch.com/") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "INITIALIZATION REQUIRED",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            "CONFIGURE YOUR HERMES NODE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("BASE_URL", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start))
        TextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text("https://your-hermes-server.com/", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            "Use http://10.0.2.2:PORT/ for local development in emulator.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("API_KEY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start))
        TextField(
            value = key,
            onValueChange = { key = it },
            placeholder = { Text("Enter your API key", color = MaterialTheme.colorScheme.outline) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSave(key, url) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SAVE & INITIALIZE", fontWeight = FontWeight.Bold)
        }
    }
}
