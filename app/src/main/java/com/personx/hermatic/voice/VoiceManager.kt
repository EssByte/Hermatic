package com.personx.hermatic.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var currentTranscription = StringBuilder()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            Log.d("VoiceManager", "Attempting to initialize TTS...")
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to init TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engines = tts?.engines
            Log.d("VoiceManager", "Available TTS engines: ${engines?.joinToString { it.name }}")
            
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceManager", "System default language not supported, falling back to US English")
                tts?.setLanguage(Locale.US)
            }
            
            isTtsReady = true
            Log.d("VoiceManager", "TTS Initialized and Ready")
        } else {
            Log.e("VoiceManager", "TTS Initialization failed with status: $status")
            // On some systems (like GrapheneOS), we might need to wait or retry if no engine is active
        }
    }

    fun speak(text: String): Boolean {
        if (tts == null) {
            initializeTts()
            return false
        }
        
        return if (isTtsReady) {
            Log.d("VoiceManager", "Speaking: $text")
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HERMES_VOICE")
            result == TextToSpeech.SUCCESS
        } else {
            Log.w("VoiceManager", "TTS not ready yet")
            false
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(onPartialResult: (String) -> Unit) {
        currentTranscription.clear()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("VoiceManager", "Speech recognition error: $error")
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentTranscription.clear()
                    currentTranscription.append(matches[0])
                    onPartialResult(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onPartialResult(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening(): String {
        speechRecognizer?.stopListening()
        return currentTranscription.toString()
    }
    
    fun release() {
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
