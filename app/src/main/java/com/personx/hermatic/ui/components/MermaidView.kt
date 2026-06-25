package com.personx.hermatic.ui.components

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MermaidView(mermaidCode: String, isDark: Boolean) {
    val theme = if (isDark) "dark" else "default"
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script type="module">
                import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
                mermaid.initialize({ startOnLoad: true, theme: '$theme' });
            </script>
            <style>
                body { margin: 0; padding: 10px; background-color: transparent; }
                .mermaid { background-color: transparent !important; }
            </style>
        </head>
        <body>
            <div class="mermaid">
                $mermaidCode
            </div>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 400.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(0)
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}
