package com.personx.hermatic.voice

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

class AudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playFile(file: File, onComplete: () -> Unit) {
        stop()
        player = MediaPlayer.create(context, Uri.fromFile(file)).apply {
            setOnCompletionListener { onComplete() }
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
}
