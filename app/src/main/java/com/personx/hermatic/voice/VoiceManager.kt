package com.personx.hermatic.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var finalResultCallback: ((String) -> Unit)? = null

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
            tts?.setLanguage(Locale.getDefault())
            isTtsReady = true
            Log.d("VoiceManager", "TTS Initialized")
        }
    }

    fun speak(text: String): Boolean {
        if (tts == null) {
            initializeTts()
            return false
        }
        return if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HERMES_VOICE")
            true
        } else false
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(onPartialResult: (String) -> Unit) {
        currentTranscription.clear()
        finalResultCallback = null
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
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    finalResultCallback?.let { 
                        it.invoke(currentTranscription.toString())
                        finalResultCallback = null
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val final = if (!matches.isNullOrEmpty()) matches[0] else currentTranscription.toString()
                
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    finalResultCallback?.let {
                        it.invoke(final)
                        finalResultCallback = null
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0]
                    currentTranscription.clear()
                    currentTranscription.append(partial)
                    onPartialResult(partial)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening(onResult: (String) -> Unit) {
        finalResultCallback = onResult
        speechRecognizer?.stopListening()
        
        // Timeout to ensure callback is called if onResults doesn't fire
        Handler(Looper.getMainLooper()).postDelayed({
            finalResultCallback?.let { 
                Log.d("VoiceManager", "stopListening: Timeout reached, using partial: ${currentTranscription}")
                it.invoke(currentTranscription.toString())
                finalResultCallback = null
            }
        }, 3000)
    }
    
    fun release() {
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
