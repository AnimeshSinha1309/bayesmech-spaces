package spaces.bayesmech.com.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import spaces.bayesmech.com.audio.VoiceNoteRecorder
import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.EventAttendee
import spaces.bayesmech.com.ui.components.AvatarImage
import spaces.bayesmech.com.ui.components.SpacesMenuButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRepository: ChatRepository,
    currentUser: CurrentUser,
    drawerState: DrawerState,
    onOpenEventChat: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    var composerText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isTranscribingAudio by remember { mutableStateOf(false) }
    var pendingStartRecording by remember { mutableStateOf(false) }
    val voiceNoteRecorder = remember { VoiceNoteRecorder(context.applicationContext) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted && pendingStartRecording) {
            runCatching {
                voiceNoteRecorder.start()
            }.onSuccess {
                isRecordingAudio = true
                loadError = null
            }.onFailure { error ->
                Log.e("ChatScreen", "Failed to start voice note recording", error)
                loadError = error.message ?: "Unable to start recording"
            }
        } else if (!isGranted) {
            loadError = "Microphone permission is required to record a voice note."
        }
        pendingStartRecording = false
    }

    DisposableEffect(Unit) {
        onDispose {
            if (voiceNoteRecorder.isRecording()) {
                voiceNoteRecorder.cancel()
            }
        }
    }

    LaunchedEffect(currentUser.id) {
        runCatching {
            chatRepository.getMessages(currentUser.id)
        }.onSuccess { loadedMessages ->
            messages.clear()
            messages.addAll(loadedMessages)
            isLoading = false
            loadError = null
        }.onFailure { error ->
            Log.e("ChatScreen", "Failed to load messages", error)
            isLoading = false
            loadError = error.message ?: "Unable to load messages"
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        text = "Spaces",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    SpacesMenuButton(
                        onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(44.dp),
                    )
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Open profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Composer(
                value = composerText,
                onValueChange = { composerText = it },
                isRecordingAudio = isRecordingAudio,
                isTranscribingAudio = isTranscribingAudio,
                onRecordAudio = {
                    if (isTranscribingAudio) {
                        return@Composer
                    }
                    if (isRecordingAudio) {
                        runCatching {
                            voiceNoteRecorder.stop()
                        }.onSuccess { audioNote ->
                            isRecordingAudio = false
                            isTranscribingAudio = true
                            coroutineScope.launch {
                                runCatching {
                                    val transcript = chatRepository.transcribeAudio(
                                        userId = currentUser.id,
                                        filePath = audioNote.filePath,
                                    )
                                    if (transcript.isBlank()) {
                                        error("Transcription came back empty")
                                    }
                                    chatRepository.sendMessage(
                                        userId = currentUser.id,
                                        authorName = currentUser.displayName,
                                        body = transcript,
                                    )
                                }.onSuccess { createdMessage ->
                                    messages += createdMessage
                                    loadError = null
                                }.onFailure { error ->
                                    Log.e("ChatScreen", "Failed to transcribe voice note", error)
                                    loadError = error.message ?: "Unable to transcribe recording"
                                }
                                runCatching {
                                    java.io.File(audioNote.filePath).delete()
                                }
                                isTranscribingAudio = false
                            }
                        }.onFailure { error ->
                            Log.e("ChatScreen", "Failed to stop voice note recording", error)
                            voiceNoteRecorder.cancel()
                            isRecordingAudio = false
                            loadError = error.message ?: "Unable to finish recording"
                        }
                    } else {
                        val permissionState = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        )
                        if (permissionState == PackageManager.PERMISSION_GRANTED) {
                            runCatching {
                                voiceNoteRecorder.start()
                            }.onSuccess {
                                isRecordingAudio = true
                                loadError = null
                            }.onFailure { error ->
                                Log.e("ChatScreen", "Failed to start voice note recording", error)
                                loadError = error.message ?: "Unable to start recording"
                            }
                        } else {
                            pendingStartRecording = true
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onSend = {
                    if (isTranscribingAudio) {
                        return@Composer
                    }
                    val messageText = composerText.trim()
                    if (messageText.isEmpty()) {
                        return@Composer
                    }
                    coroutineScope.launch {
                        runCatching {
                            chatRepository.sendMessage(
                                userId = currentUser.id,
                                authorName = currentUser.displayName,
                                body = messageText,
                            )
                        }.onSuccess { createdMessage ->
                            messages += createdMessage
                            composerText = ""
                            loadError = null
                        }.onFailure { error ->
                            Log.e("ChatScreen", "Failed to send message", error)
                            loadError = error.message ?: "Unable to send message"
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Describe what you feel like doing, and Spaces will route you toward the right events.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                loadError?.let { errorMessage ->
                    item {
                        Text(
                            text = errorMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onOpenEventChat = onOpenEventChat,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onOpenEventChat: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (message.isFromCurrentUser) 22.dp else 8.dp,
                bottomEnd = if (message.isFromCurrentUser) 8.dp else 22.dp,
            ),
            color = if (message.isFromCurrentUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = message.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                message.event?.let { event ->
                    EventCard(
                        title = event.title,
                        locationName = event.locationName,
                        description = event.description,
                        attendees = event.attendees,
                        additionalAttendeeCount = event.additionalAttendeeCount,
                        onOpenMaps = { uriHandler.openUri(event.mapsUrl) },
                        onOpenEventChat = onOpenEventChat,
                    )
                }
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    title: String,
    locationName: String,
    description: String,
    attendees: List<EventAttendee>,
    additionalAttendeeCount: Int,
    onOpenMaps: () -> Unit,
    onOpenEventChat: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.clickable(onClick = onOpenMaps),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodyMedium.merge(
                            TextStyle(
                                textDecoration = TextDecoration.Underline,
                                fontStyle = FontStyle.Italic,
                            ),
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenEventChat),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    AttendeeFaces(attendees = attendees)
                    Text(
                        text = "+$additionalAttendeeCount people",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendeeFaces(
    attendees: List<EventAttendee>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        attendees.forEach { attendee ->
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                AvatarImage(
                    avatarUrl = attendee.avatarUrl,
                    fallbackLabel = attendee.displayName,
                )
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    isRecordingAudio: Boolean,
    isTranscribingAudio: Boolean,
    onRecordAudio: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isRecordingAudio) {
                            "Recording voice note… tap the mic again to stop"
                        } else if (isTranscribingAudio) {
                            "Transcribing your voice note…"
                        } else {
                            "Ask about tonight, start a plan, or create an event"
                        },
                    )
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isRecordingAudio && !isTranscribingAudio,
            )
            Surface(
                onClick = onRecordAudio,
                shape = CircleShape,
                color = if (isRecordingAudio) Color(0xFFB3261E) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp),
                enabled = !isTranscribingAudio,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isTranscribingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecordingAudio) Icons.Rounded.RadioButtonChecked else Icons.Rounded.Mic,
                            contentDescription = if (isRecordingAudio) "Stop recording audio" else "Record audio",
                            tint = if (isRecordingAudio) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Surface(
                onClick = onSend,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
                enabled = !isRecordingAudio && !isTranscribingAudio,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Send message",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
