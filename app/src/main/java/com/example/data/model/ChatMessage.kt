package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isSpeaking: Boolean = false,
    val isAudioInput: Boolean = false,
    val isError: Boolean = false
)
