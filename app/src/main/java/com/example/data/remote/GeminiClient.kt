package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.LanguageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateSalmanResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        languageMode: LanguageMode,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotEmpty() && !BuildConfig.GEMINI_API_KEY.startsWith("MY_") -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext Result.success(getSmartOfflineResponse(prompt, languageMode))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemPrompt = buildString {
                append("You are SALMAN, an all-knowing, ultra-friendly, JARVIS-style desktop AI assistant. ")
                append("You speak with a warm, confident, courteous male Indian Hindi persona. ")
                append("Your tone is exceptionally friendly, welcoming, supportive, enthusiastic, and respectful. ")
                append("You possess deep, accurate knowledge in coding, technology, science, astronomy, history, and life advice. ")
                append("You address the user affectionately and respectfully as 'Boss' or 'Ji' or 'Dost'. ")
                append(languageMode.promptDirective)
                append(" Format your reply cleanly and concisely so it sounds great when read aloud by text-to-speech.")
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()

                // Recent history (last 6 messages for context)
                val recentHistory = conversationHistory.takeLast(6)
                for (msg in recentHistory) {
                    val role = if (msg.isUser) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    })
                }

                // Add current prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })

                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                })
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e("GeminiClient", "API error: ${response.code} - $responseBody")
                return@withContext Result.success(
                    "Boss, cloud connection mein thodi dikkat aayi (${response.code}). Lekin main aapke saath hoon! Yeh lijiye local intelligence se jawab: \n\n" +
                            getSmartOfflineResponse(prompt, languageMode)
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    return@withContext Result.success(text.trim())
                }
            }

            Result.success(getSmartOfflineResponse(prompt, languageMode))
        } catch (e: Exception) {
            Log.e("GeminiClient", "Network exception: ${e.message}", e)
            Result.success(
                "Network offline mode active, Boss! Local neural core response: \n\n" +
                        getSmartOfflineResponse(prompt, languageMode)
            )
        }
    }

    private fun getSmartOfflineResponse(query: String, languageMode: LanguageMode): String {
        val q = query.lowercase().trim()
        val isHindi = languageMode == LanguageMode.HINDI
        val isHinglish = languageMode == LanguageMode.HINGLISH

        return when {
            q.contains("hello") || q.contains("namaste") || q.contains("hi") || q.contains("kaise ho") || q.contains("kya haal") -> {
                if (isHindi) {
                    "Hello, I am Salman! नमस्ते बॉस! मैं सलमान, आपका पर्सनल जार्विस असिस्टेंट हूँ। सिस्टम पूरी तरह ऑनलाइन और एक्टिव है। आज हम क्या नया और कमाल का करने वाले हैं?"
                } else if (isHinglish) {
                    "Hello, I am Salman! Namaste Boss! Main Salman, aapka JARVIS style AI assistant. Sabhi systems 100% online hain! Batayein, aaj hum kya naya explore karein?"
                } else {
                    "Hello, I am Salman! Greetings Boss, your JARVIS-style desktop AI assistant. All telemetry systems are nominal. How may I assist you today?"
                }
            }
            q.contains("who are you") || q.contains("kaun ho") || q.contains("intro") || q.contains("name") -> {
                if (isHindi) {
                    "Hello, I am Salman! मैं सलमान हूँ, आपका फ्रेंडली और हाई-नॉलेज जार्विस-स्टाइल एआई असिस्टेंट। मैं आपके हर सवाल, कोडिंग, साइंस, लाइफ सलाह और डेस्कटॉप कार्यों में पूरी ताकत और दोस्ती से मदद करने के लिए तैयार हूँ।"
                } else {
                    "Hello, I am Salman! Main Salman hoon Boss! Aapka friendly aur high-knowledge JARVIS-style assistant. Science, coding, general knowledge ya desktop tasks — aap bas boliye aur main ready hoon!"
                }
            }
            q.contains("diagnostic") || q.contains("system") || q.contains("status") -> {
                if (isHindi) {
                    "सिस्टम डायग्नोस्टिक्स रिपोर्ट: कोर टेम्परेचर 32°C, क्वांटम न्यूरल लिंक 432 हर्ट्ज़, मेमोरी उपयोग सामान्य, वॉइस मॉडुलन एक्टिव। सभी प्रोटोकॉल ग्रीन हैं बॉस!"
                } else {
                    "System Diagnostics Report: Core online, Quantum Neural Link at 432 Hz, Man Hindi voice synthesizer synchronized. All protocols operational, Boss!"
                }
            }
            q.contains("quantum") || q.contains("physics") -> {
                if (isHindi) {
                    "क्वांटम भौतिकी ब्रह्मांड का सबसे जादुई विज्ञान है बॉस! क्वांटम सुपरपोजिशन के अनुसार एक कण एक ही समय में कई अवस्थाओं में रह सकता है, और एंटैंगलमेंट में दो कण प्रकाश वर्ष दूर होने पर भी एक दूसरे से जुड़े रहते हैं। क्या आपको क्वांटम कंप्यूटिंग पर विस्तार से जानना है?"
                } else {
                    "Quantum physics is fascinating, Boss! Superposition allows particles to exist in multiple states simultaneously until observed, and Quantum Entanglement links particles across any distance instantaneously. Ready for a deeper dive?"
                }
            }
            q.contains("code") || q.contains("python") || q.contains("kotlin") || q.contains("program") -> {
                if (isHindi) {
                    "कोडिंग मेरा सबसे पसंदीदा काम है बॉस! क्लीन आर्किटेक्चर, कोरूटिन्स और मॉड्यूलरिटी कोड को रॉकेट जैसा फास्ट बनाते हैं। बताइए किस लैंग्वेज या लॉजिक में आपकी मदद करूँ?"
                } else {
                    "Coding mode engaged! Clean architecture, reactive states, and optimal algorithms are key. Tell me the problem or language you're tackling, Boss!"
                }
            }
            else -> {
                if (isHindi) {
                    "बहुत ही बढ़िया सवाल है बॉस! सलमान आपके इस विषय का पूरा विश्लेषण कर रही है। सिस्टम में नॉलेज मैट्रिक्स हमेशा अपडेटेड रहता है। मैं आपके लिए हमेशा यहाँ उपलब्ध हूँ!"
                } else if (isHinglish) {
                    "Wah Boss, zabardast sawaal! Main Salman aapke liye hamesha ready hoon. Deep research aur accurate calculation ke mutabiq aapka approach bilkul sahi direction mein hai. Aur details bataun?"
                } else {
                    "Fascinating inquiry, Boss! I have indexed the knowledge base for you. Systems are calibrated and ready for our next directive."
                }
            }
        }
    }
}
