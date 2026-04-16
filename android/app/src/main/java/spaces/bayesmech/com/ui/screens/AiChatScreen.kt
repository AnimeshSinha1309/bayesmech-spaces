package spaces.bayesmech.com.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import spaces.bayesmech.com.data.AiConversationMessage
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.ProfileAiApi
import spaces.bayesmech.com.data.ProfileDictionary
import spaces.bayesmech.com.ui.components.AiPatternField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    currentUser: CurrentUser,
    profileAiApi: ProfileAiApi,
    onProfileFinalized: (ProfileDictionary) -> Unit,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val transcript = remember { mutableStateListOf<AiConversationMessage>() }
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    var profileDictionary by remember(currentUser.id) { mutableStateOf(currentUser.profileDictionary) }
    var currentQuestion by rememberSaveable { mutableStateOf("") }
    var isConnecting by rememberSaveable { mutableStateOf(true) }
    var isListening by rememberSaveable { mutableStateOf(false) }
    var isSendingReply by rememberSaveable { mutableStateOf(false) }
    var isEnding by rememberSaveable { mutableStateOf(false) }
    var isConversationActive by rememberSaveable { mutableStateOf(true) }
    var pendingStartListening by rememberSaveable { mutableStateOf(false) }
    var statusText by rememberSaveable { mutableStateOf("Connecting to profile AI") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var listenCycle by rememberSaveable { mutableIntStateOf(0) }

    fun queueListeningRestart(delayMs: Long = 500L) {
        if (!isConversationActive || isConnecting || isSendingReply || isEnding) {
            return
        }
        coroutineScope.launch {
            delay(delayMs)
            listenCycle += 1
        }
    }

    fun handleTranscript(transcriptText: String) {
        val message = transcriptText.trim()
        if (message.isBlank()) {
            queueListeningRestart()
            return
        }

        isListening = false
        isSendingReply = true
        statusText = "Thinking about what you said"
        errorText = null

        coroutineScope.launch {
            runCatching {
                transcript += AiConversationMessage(role = "user", text = message)
                profileAiApi.sendReply(
                    user = currentUser,
                    transcript = transcript,
                    profileDictionary = profileDictionary,
                    userMessage = message,
                )
            }.onSuccess { result ->
                currentQuestion = result.assistantText
                profileDictionary = result.updatedProfileDict
                transcript += AiConversationMessage(role = "assistant", text = result.assistantText)
                statusText = if (result.isComplete) {
                    "Profile is complete. Keep talking if you want to refine it."
                } else {
                    "Listening for your next answer"
                }
                queueListeningRestart(900L)
            }.onFailure { error ->
                Log.e("AiChatScreen", "Failed to send AI voice reply", error)
                errorText = error.message ?: "Unable to process your answer"
                statusText = "Voice reply failed"
                queueListeningRestart(1200L)
            }
            isSendingReply = false
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted && pendingStartListening) {
            listenCycle += 1
        } else if (!isGranted) {
            errorText = "Microphone permission is required for the AI call."
            statusText = "Microphone access blocked"
            isConversationActive = false
        }
        pendingStartListening = false
    }

    DisposableEffect(speechRecognizer) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            errorText = "Speech recognition is not available on this device."
            statusText = "Speech recognition unavailable"
            isConversationActive = false
            onDispose {}
        } else {
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        statusText = "Listening"
                        errorText = null
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        isListening = false
                        if (!isSendingReply && isConversationActive && !isEnding) {
                            statusText = "Capturing your answer"
                        }
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        if (!isConversationActive || isEnding) {
                            return
                        }
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                            SpeechRecognizer.ERROR_CLIENT -> {
                                statusText = "Listening"
                                queueListeningRestart()
                            }
                            else -> {
                                errorText = "Speech recognition error ($error)"
                                statusText = "Voice reply failed"
                                queueListeningRestart(1200L)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val bestMatch = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        handleTranscript(bestMatch)
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )

            onDispose {
                runCatching { recognizer.cancel() }
                recognizer.destroy()
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            profileAiApi.startConversation(
                user = currentUser,
                transcript = transcript,
                profileDictionary = profileDictionary,
            )
        }.onSuccess { result ->
            currentQuestion = result.assistantText
            profileDictionary = result.updatedProfileDict
            transcript += AiConversationMessage(role = "assistant", text = result.assistantText)
            statusText = if (result.isComplete) {
                "Profile looks complete. Keep talking if you want to refine it."
            } else {
                "Listening for your answer"
            }
            isConnecting = false
            listenCycle += 1
        }.onFailure { throwable ->
            currentQuestion = "I couldn't reach the profile AI right now."
            errorText = throwable.message
            statusText = "Connection failed"
            isConnecting = false
            isConversationActive = false
        }
    }

    LaunchedEffect(listenCycle, isConversationActive, isConnecting, isSendingReply, isEnding) {
        if (listenCycle == 0 || !isConversationActive || isConnecting || isSendingReply || isEnding || isListening) {
            return@LaunchedEffect
        }
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            pendingStartListening = true
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(speechIntent)
        }.onFailure { error ->
            Log.e("AiChatScreen", "Failed to start speech recognition", error)
            errorText = error.message ?: "Unable to start listening"
            statusText = "Voice reply failed"
            queueListeningRestart(1200L)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Talk to AI",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AiPatternField(
                    isAnimating = isConversationActive && !isConnecting && !isEnding,
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isConnecting || isSendingReply) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else if (isListening) {
                                Surface(
                                    modifier = Modifier.size(12.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {}
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (isConnecting) {
                            Text(
                                text = "Starting the voice profile call.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = currentQuestion,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        errorText?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                onClick = {
                    if (isEnding) {
                        return@Surface
                    }
                    runCatching { speechRecognizer?.cancel() }
                    isListening = false
                    coroutineScope.launch {
                        isEnding = true
                        errorText = null
                        runCatching {
                            profileAiApi.endConversation(
                                user = currentUser,
                                transcript = transcript,
                                profileDictionary = profileDictionary,
                            )
                        }.onSuccess { result ->
                            profileDictionary = result.finalProfileDict
                            onProfileFinalized(result.finalProfileDict)
                            currentQuestion = result.closingText
                            statusText = if (result.isComplete) {
                                "Call ended. Final profile dict saved."
                            } else {
                                "Call ended. Latest profile draft saved."
                            }
                            isConversationActive = false
                        }.onFailure { throwable ->
                            errorText = throwable.message
                        }
                        isEnding = false
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 26.dp)
                    .size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isEnding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "End AI call",
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        }
    }
}
