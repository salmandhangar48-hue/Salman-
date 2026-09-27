package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.LanguageMode
import com.example.data.model.SalmanVoiceState
import com.example.ui.components.ArcReactorView
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.HolographicHudPanel
import com.example.ui.components.TelemetryDisplay
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBgDark
import com.example.ui.theme.JarvisBlueArc
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariantDark
import com.example.ui.theme.JarvisTeal
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val amplitude by viewModel.audioAmplitude.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val languageMode by viewModel.languageMode.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Permission launcher for speech recognition
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    fun handleMicClick() {
        if (voiceState == SalmanVoiceState.LISTENING) {
            viewModel.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisBgDark
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isDesktopOrWide = maxWidth >= 720.dp

            if (isDesktopOrWide) {
                // Desktop / Landscape Multi-Pane HUD
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Arc Reactor Core + Audio Spectrum + Telemetry + Controls
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DesktopHeader(
                            onSettingsClick = { showSettingsDialog = true },
                            onClearClick = { viewModel.clearChat() }
                        )

                        // Arc Reactor HUD Panel
                        HolographicHudPanel(
                            title = "Neural Voice Core",
                            statusBadge = if (voiceState == SalmanVoiceState.PROCESSING) "THINKING" else voiceState.name,
                            badgeColor = when (voiceState) {
                                SalmanVoiceState.LISTENING -> JarvisTeal
                                SalmanVoiceState.SPEAKING -> JarvisCyan
                                SalmanVoiceState.PROCESSING -> JarvisAmber
                                SalmanVoiceState.ERROR -> JarvisRedAlert
                                SalmanVoiceState.IDLE -> JarvisBlueArc
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ArcReactorView(
                                    state = voiceState,
                                    amplitude = amplitude,
                                    reactorSize = 180.dp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                AudioWaveformBar(
                                    state = voiceState,
                                    amplitude = amplitude,
                                    height = 36.dp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                VoiceControlButtons(
                                    voiceState = voiceState,
                                    isMuted = isMuted,
                                    onMicClick = { handleMicClick() },
                                    onStopSpeakClick = { viewModel.stopSpeaking() },
                                    onToggleMute = { viewModel.toggleMute() }
                                )
                            }
                        }

                        // Telemetry details
                        TelemetryDisplay(
                            telemetry = telemetry,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Right Column: Holographic Chat Feed & Interactive Knowledge Console
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickProtocolChipsRow(
                            onChipClick = { prompt -> viewModel.sendMessage(prompt) }
                        )

                        // Messages Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisSurfaceDark.copy(alpha = 0.7f))
                                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    ChatMessageItem(
                                        message = msg,
                                        onSpeakClicked = { text -> viewModel.speakText(text) }
                                    )
                                }
                            }
                        }

                        // Live audio transcript pill if talking
                        AnimatedVisibility(visible = voiceState == SalmanVoiceState.LISTENING && liveTranscript.isNotBlank()) {
                            Text(
                                text = "Hearing: \"$liveTranscript\"",
                                color = JarvisTeal,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Bottom Input Bar
                        CommandInputBar(
                            inputText = inputText,
                            onInputChanged = { inputText = it },
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            onMicClick = { handleMicClick() },
                            voiceState = voiceState
                        )
                    }
                }
            } else {
                // Mobile / Compact HUD Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DesktopHeader(
                        onSettingsClick = { showSettingsDialog = true },
                        onClearClick = { viewModel.clearChat() }
                    )

                    // Arc Reactor & Waveform (Compact)
                    HolographicHudPanel(
                        title = "SALMAN AI CORE",
                        statusBadge = if (voiceState == SalmanVoiceState.PROCESSING) "THINKING" else voiceState.name,
                        badgeColor = when (voiceState) {
                            SalmanVoiceState.LISTENING -> JarvisTeal
                            SalmanVoiceState.SPEAKING -> JarvisCyan
                            SalmanVoiceState.PROCESSING -> JarvisAmber
                            SalmanVoiceState.ERROR -> JarvisRedAlert
                            SalmanVoiceState.IDLE -> JarvisBlueArc
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ArcReactorView(
                                state = voiceState,
                                amplitude = amplitude,
                                reactorSize = 100.dp
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                AudioWaveformBar(
                                    state = voiceState,
                                    amplitude = amplitude,
                                    height = 32.dp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                VoiceControlButtons(
                                    voiceState = voiceState,
                                    isMuted = isMuted,
                                    onMicClick = { handleMicClick() },
                                    onStopSpeakClick = { viewModel.stopSpeaking() },
                                    onToggleMute = { viewModel.toggleMute() }
                                )
                            }
                        }
                    }

                    QuickProtocolChipsRow(
                        onChipClick = { prompt -> viewModel.sendMessage(prompt) }
                    )

                    // Messages Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurfaceDark.copy(alpha = 0.7f))
                            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatMessageItem(
                                    message = msg,
                                    onSpeakClicked = { text -> viewModel.speakText(text) }
                                )
                            }
                        }
                    }

                    // Input Bar
                    CommandInputBar(
                        inputText = inputText,
                        onInputChanged = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        onMicClick = { handleMicClick() },
                        voiceState = voiceState
                    )
                }
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    currentApiKey = customApiKey,
                    currentPitch = speechPitch,
                    currentRate = speechRate,
                    currentLanguageMode = languageMode,
                    onSaveApiKey = { key -> viewModel.updateCustomApiKey(key) },
                    onCalibrateVoice = { pitch, rate -> viewModel.calibrateVoice(pitch, rate) },
                    onLanguageModeChanged = { mode -> viewModel.setLanguageMode(mode) },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DesktopHeader(
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisSurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Holographic Logo
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(JarvisCyan, JarvisBlueArc)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "SALMAN AI",
                color = JarvisCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "JARVIS DESKTOP • MAN HINDI VOICE",
                color = JarvisTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClearClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("clear_chat_button")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear Chat",
                tint = JarvisTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = JarvisCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VoiceControlButtons(
    voiceState: SalmanVoiceState,
    isMuted: Boolean,
    onMicClick: () -> Unit,
    onStopSpeakClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mute / Unmute
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(JarvisSurfaceVariantDark)
                .border(1.dp, JarvisSurfaceBorder, CircleShape)
                .testTag("toggle_mute_button")
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) JarvisAmber else JarvisTeal,
                modifier = Modifier.size(18.dp)
            )
        }

        // Primary Mic / Push to Talk
        Button(
            onClick = onMicClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (voiceState == SalmanVoiceState.LISTENING) JarvisTeal else JarvisCyan
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(52.dp)
                .scale(if (voiceState == SalmanVoiceState.LISTENING) pulseScale else 1f)
                .testTag("voice_listen_button")
        ) {
            Icon(
                imageVector = if (voiceState == SalmanVoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (voiceState == SalmanVoiceState.LISTENING) "Stop Listening" else "Start Listening",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }

        // Stop Speaking button
        if (voiceState == SalmanVoiceState.SPEAKING) {
            IconButton(
                onClick = onStopSpeakClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(JarvisRedAlert.copy(alpha = 0.2f))
                    .border(1.dp, JarvisRedAlert, CircleShape)
                    .testTag("stop_speaking_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop speaking",
                    tint = JarvisRedAlert,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickProtocolChipsRow(
    onChipClick: (String) -> Unit
) {
    val quickProtocols = listOf(
        "🚀 Diagnostic Run" to "Run a complete JARVIS system diagnostic on core protocols.",
        "🎙️ Namaste Salman" to "नमस्ते सलमान! आज आप मेरे डेस्कटॉप पर क्या नया सिखाएंगी?",
        "🧠 Quantum Physics" to "Explain quantum superposition and entanglement in simple Hindi.",
        "💻 Code Logic" to "How do clean architecture and reactive coroutines optimize desktop applications?",
        "🌍 General Knowledge" to "दुनिया की सबसे आधुनिक और दिलचस्प तकनीकी खोज क्या है?",
        "📝 Daily Productivity" to "Give me a top 5 daily productivity checklist for desktop workflow."
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickProtocols.forEach { (label, prompt) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisSurfaceVariantDark.copy(alpha = 0.7f))
                    .border(0.8.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { onChipClick(prompt) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("quick_chip_${label.take(8)}")
            ) {
                Text(
                    text = label,
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CommandInputBar(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    voiceState: SalmanVoiceState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisSurfaceDark)
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("inline_mic_button")
        ) {
            Icon(
                imageVector = if (voiceState == SalmanVoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Speak Command",
                tint = if (voiceState == SalmanVoiceState.LISTENING) JarvisTeal else JarvisCyan
            )
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            placeholder = {
                Text(
                    text = if (voiceState == SalmanVoiceState.LISTENING) "Listening to your voice..." else "Command Salman or ask any knowledge query...",
                    color = JarvisTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("command_input_field")
        )

        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .testTag("send_command_button")
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (inputText.isNotBlank()) JarvisCyan else JarvisTextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}
