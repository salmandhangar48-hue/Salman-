package com.example.data.model

enum class SalmanVoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

enum class LanguageMode(val displayName: String, val promptDirective: String) {
    HINDI("हिंदी (Hindi)", "Respond primarily in warm, articulate, respectful Hindi (Devanagari script) with a natural male tone."),
    HINGLISH("हिंग्लिश (Hinglish)", "Respond in friendly, natural everyday Hinglish (Hindi written in Roman/Latin script, e.g., 'Haan boss, bilkul! Main aapki madad karta hoon.') with a charismatic male persona."),
    ENGLISH("English", "Respond in crisp, polite, articulate English with a courteous male JARVIS tone.")
}

data class SystemTelemetry(
    val coreStatus: String = "ONLINE",
    val quantumFrequencyHz: Int = 432,
    val neuralCoreLoad: Int = 18,
    val memoryUsageMb: Int = 248,
    val audioChannelActive: Boolean = true,
    val voiceEngineStatus: String = "Man Hindi TTS Ready",
    val modelVersion: String = "Gemini 3.5 Flash",
    val batteryLevel: Int = 94,
    val networkLatencyMs: Int = 42
)
