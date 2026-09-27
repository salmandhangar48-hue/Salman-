package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechEngine(private val context: Context) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0.1f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _engineStatus = MutableStateFlow("Initializing Audio Neural Link...")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechStateChanged: ((Boolean) -> Unit)? = null

    private var speechRate = 1.0f
    private var speechPitch = 0.92f // Natural warm male voice pitch
    var onTtsReady: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val waveSimulationRunnable = object : Runnable {
        private var phase = 0f
        override fun run() {
            if (_isSpeaking.value) {
                phase += 0.35f
                val base = kotlin.math.sin(phase) * 0.4f + 0.5f
                val noise = ((System.currentTimeMillis() % 17) / 17f) * 0.3f
                _audioAmplitude.value = (base + noise).coerceIn(0.15f, 1.0f)
                mainHandler.postDelayed(this, 60)
            } else if (!_isListening.value) {
                _audioAmplitude.value = 0.08f
            }
        }
    }

    init {
        initializeRecognizer()
        initializeTTS()
    }

    private fun initializeRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@SpeechEngine)
                    }
                } else {
                    _engineStatus.value = "Speech recognition unavailable on this device."
                }
            } catch (e: Exception) {
                Log.e("SpeechEngine", "Error initializing recognizer: ${e.message}")
            }
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            configureMaleHindiVoice()
            _engineStatus.value = "Man Hindi Voice Engine Online"
            mainHandler.post {
                onTtsReady?.invoke()
            }
        } else {
            _engineStatus.value = "TTS Initialization issue ($status)"
        }
    }

    fun configureMaleHindiVoice() {
        val tts = textToSpeech ?: return
        try {
            val hindiLocale = Locale.forLanguageTag("hi-IN")
            val langResult = tts.setLanguage(hindiLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.forLanguageTag("en-IN"))
            }

            // Seek male voice
            val voices = tts.voices
            var maleFound = false
            if (voices != null) {
                for (v in voices) {
                    val name = v.name.lowercase()
                    val isHindi = v.locale.language.equals("hi", ignoreCase = true) || v.locale.country.equals("IN", ignoreCase = true)
                    val isMale = (name.contains("male") || name.contains("man") || name.contains("boy") || name.contains("m0")) && !name.contains("female")
                    if (isHindi && isMale) {
                        tts.voice = v
                        maleFound = true
                        break
                    }
                }
            }

            tts.setSpeechRate(speechRate)
            tts.setPitch(if (maleFound) speechPitch else speechPitch * 0.95f)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    mainHandler.post(waveSimulationRunnable)
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    mainHandler.removeCallbacks(waveSimulationRunnable)
                    _audioAmplitude.value = 0.08f
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    mainHandler.removeCallbacks(waveSimulationRunnable)
                    _audioAmplitude.value = 0.08f
                }
            })
        } catch (e: Exception) {
            Log.e("SpeechEngine", "Error setting up voice: ${e.message}")
        }
    }

    fun startListening(languageMode: LanguageMode = LanguageMode.HINDI) {
        if (_isListening.value) return
        stopSpeaking()

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initializeRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    when (languageMode) {
                        LanguageMode.HINDI -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                        }
                        LanguageMode.HINGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN"))
                        }
                        LanguageMode.ENGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        }
                    }
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                onSpeechStateChanged?.invoke(true)
            } catch (e: Exception) {
                Log.e("SpeechEngine", "Failed to start listening: ${e.message}")
                _isListening.value = false
                onSpeechStateChanged?.invoke(false)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                onSpeechStateChanged?.invoke(false)
            } catch (e: Exception) {
                Log.e("SpeechEngine", "Failed to stop listening: ${e.message}")
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized || textToSpeech == null) {
            initializeTTS()
        }

        // Clean markdown symbols (e.g. *, #, _) for fluent spoken voice
        val cleanSpeechText = text
            .replace(Regex("[*#_`~>|\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanSpeechText.isEmpty()) return

        stopListening()
        val utteranceId = "salman_voice_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        textToSpeech?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        mainHandler.removeCallbacks(waveSimulationRunnable)
        _audioAmplitude.value = 0.08f
    }

    fun setPitch(pitch: Float) {
        this.speechPitch = pitch.coerceIn(0.7f, 1.8f)
        textToSpeech?.setPitch(this.speechPitch)
    }

    fun setRate(rate: Float) {
        this.speechRate = rate.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(this.speechRate)
    }

    // RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _engineStatus.value = "Salman is listening to your voice..."
    }

    override fun onBeginningOfSpeech() {
        _engineStatus.value = "Receiving audio stream..."
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Convert dB (-2 to 10 typical) to 0.0 - 1.0 range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
        _audioAmplitude.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        onSpeechStateChanged?.invoke(false)
        _engineStatus.value = "Processing audio stream..."
    }

    override fun onError(error: Int) {
        _isListening.value = false
        onSpeechStateChanged?.invoke(false)
        _audioAmplitude.value = 0.08f
        val msg = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
            else -> "Speech recognition event ($error)"
        }
        _engineStatus.value = msg
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        onSpeechStateChanged?.invoke(false)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val topResult = matches?.firstOrNull()?.trim()
        if (!topResult.isNullOrEmpty()) {
            _lastRecognizedText.value = topResult
            onSpeechRecognized?.invoke(topResult)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrEmpty()) {
            _lastRecognizedText.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            mainHandler.removeCallbacks(waveSimulationRunnable)
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e("SpeechEngine", "Error destroying speech engine: ${e.message}")
        }
    }
}
