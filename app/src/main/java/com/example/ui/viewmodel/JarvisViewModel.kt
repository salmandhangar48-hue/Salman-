package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechEngine
import com.example.data.model.ChatMessage
import com.example.data.model.LanguageMode
import com.example.data.model.SalmanVoiceState
import com.example.data.model.SystemTelemetry
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val speechEngine = SpeechEngine(application)
    private val geminiClient = GeminiClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _voiceState = MutableStateFlow(SalmanVoiceState.IDLE)
    val voiceState: StateFlow<SalmanVoiceState> = _voiceState.asStateFlow()

    private val _telemetry = MutableStateFlow(SystemTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _languageMode = MutableStateFlow(LanguageMode.HINDI)
    val languageMode: StateFlow<LanguageMode> = _languageMode.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _speechPitch = MutableStateFlow(0.92f) // Male Hindi voice
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    val audioAmplitude: StateFlow<Float> = speechEngine.audioAmplitude
    val engineStatus: StateFlow<String> = speechEngine.engineStatus

    private val initialGreeting = "Hello, I am Salman! नमस्ते बॉस! मैं सलमान हूँ — आपका पर्सनल जार्विस AI डेस्कटॉप असिस्टेंट। मैं आपके हर सवाल, कोडिंग, साइंस और डेस्कटॉप कार्यों में पूरी ताकत और गर्मजोशी से मदद करने के लिए तैयार हूँ। बताइए, आज हम क्या नया करने वाले हैं?"

    init {
        setupSpeechEngineListeners()
        loadInitialGreeting()
    }

    private fun setupSpeechEngineListeners() {
        speechEngine.onTtsReady = {
            if (!_isMuted.value) {
                speakText(initialGreeting)
            }
        }

        speechEngine.onSpeechRecognized = { text ->
            if (text.isNotBlank()) {
                _liveTranscript.value = text
                sendMessage(text = text, isAudio = true)
            }
        }

        speechEngine.onSpeechStateChanged = { isListening ->
            if (isListening) {
                _voiceState.value = SalmanVoiceState.LISTENING
            } else if (_voiceState.value == SalmanVoiceState.LISTENING) {
                _voiceState.value = SalmanVoiceState.IDLE
            }
        }

        viewModelScope.launch {
            speechEngine.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _voiceState.value = SalmanVoiceState.SPEAKING
                } else if (_voiceState.value == SalmanVoiceState.SPEAKING) {
                    _voiceState.value = SalmanVoiceState.IDLE
                }
            }
        }
    }

    private fun loadInitialGreeting() {
        val introMessage = ChatMessage(
            senderName = "Salman",
            text = initialGreeting,
            isUser = false
        )
        _messages.value = listOf(introMessage)
        speakText(initialGreeting)
    }

    fun startListening() {
        _voiceState.value = SalmanVoiceState.LISTENING
        speechEngine.startListening(_languageMode.value)
    }

    fun stopListening() {
        speechEngine.stopListening()
        _voiceState.value = SalmanVoiceState.IDLE
    }

    fun toggleListening() {
        if (speechEngine.isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun sendMessage(text: String, isAudio: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        stopSpeaking()
        stopListening()

        val userMessage = ChatMessage(
            senderName = "Operator",
            text = trimmed,
            isUser = true,
            isAudioInput = isAudio
        )

        _messages.value = _messages.value + userMessage
        _voiceState.value = SalmanVoiceState.PROCESSING
        _telemetry.value = _telemetry.value.copy(
            coreStatus = "COMPUTING",
            neuralCoreLoad = (45..75).random(),
            networkLatencyMs = (28..65).random()
        )

        viewModelScope.launch {
            val result = geminiClient.generateSalmanResponse(
                prompt = trimmed,
                conversationHistory = _messages.value,
                languageMode = _languageMode.value,
                customApiKey = _customApiKey.value
            )

            val replyText = result.getOrElse {
                "माफ़ कीजिए बॉस, सिस्टम में अप्रत्याशित समस्या आई। लेकिन मैं अभी भी आपके साथ हूँ!"
            }

            val salmanMessage = ChatMessage(
                senderName = "Salman",
                text = replyText,
                isUser = false
            )

            _messages.value = _messages.value + salmanMessage
            _voiceState.value = SalmanVoiceState.IDLE
            _telemetry.value = _telemetry.value.copy(
                coreStatus = "ONLINE",
                neuralCoreLoad = (12..25).random()
            )

            if (!_isMuted.value) {
                speakText(replyText)
            }
        }
    }

    fun speakText(text: String) {
        if (_isMuted.value) return
        speechEngine.speak(text)
    }

    fun stopSpeaking() {
        speechEngine.stopSpeaking()
        if (_voiceState.value == SalmanVoiceState.SPEAKING) {
            _voiceState.value = SalmanVoiceState.IDLE
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        _languageMode.value = mode
        val confirmation = when (mode) {
            LanguageMode.HINDI -> "हिंदी मोड एक्टिवेट कर दिया गया है बॉस!"
            LanguageMode.HINGLISH -> "Hinglish mode activated, Boss! Ab mast conversation karenge."
            LanguageMode.ENGLISH -> "English protocol online. Standing by for instructions, Boss."
        }
        speakText(confirmation)
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        if (newMuted) {
            stopSpeaking()
        } else {
            speakText("ऑडियो आउटपुट अनम्यूट हो गया है।")
        }
    }

    fun updateCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        val notice = if (key.isNotBlank()) "कस्टम जेमिनी एपीआई की सफलतापूर्वक लिंक हो गई है।" else "डिफ़ॉल्ट एपीआई मोड पर स्विच किया गया।"
        speakText(notice)
    }

    fun calibrateVoice(pitch: Float, rate: Float) {
        _speechPitch.value = pitch
        _speechRate.value = rate
        speechEngine.setPitch(pitch)
        speechEngine.setRate(rate)
        speakText("वॉइस कैलिब्रेशन टेस्ट: नमस्ते बॉस! मेरी आवाज़ कैसी लग रही है?")
    }

    fun runDiagnosticProtocol() {
        val report = "डायग्नोस्टिक टेस्ट पूरा हुआ! क्वांटम न्यूरल कोर 432 हर्ट्ज़ पर पूरी तरह स्टेबल है। महिला हिंदी वॉइस सिंथेसाइज़र सक्रिय है। जेमिनी 3.5 फ़्लैश इंजन से कनेक्शन सही है। सभी डेस्कटॉप सहायक पैरामीटर सामान्य हैं।"
        sendMessage("Run full system diagnostic and check all protocols.")
    }

    fun clearChat() {
        stopSpeaking()
        stopListening()
        loadInitialGreeting()
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.destroy()
    }
}
