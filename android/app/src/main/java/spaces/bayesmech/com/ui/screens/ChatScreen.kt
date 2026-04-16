package spaces.bayesmech.com.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import spaces.bayesmech.com.audio.VoiceNoteRecorder
import spaces.bayesmech.com.data.ChatEvent
import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.EventDraft
import spaces.bayesmech.com.ui.components.EventCard
import spaces.bayesmech.com.ui.components.SpacesMenuButton
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRepository: ChatRepository,
    currentUser: CurrentUser,
    drawerState: DrawerState,
    onOpenEventChat: (ChatEvent) -> Unit,
    onProfileClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val rsvpedEventIds = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var composerText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isTranscribingAudio by remember { mutableStateOf(false) }
    var pendingStartRecording by remember { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val voiceNoteRecorder = remember { VoiceNoteRecorder(context.applicationContext) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted && pendingStartRecording) {
            runCatching { voiceNoteRecorder.start() }
                .onSuccess {
                    isRecordingAudio = true
                    loadError = null
                }
                .onFailure { error ->
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
            val loadedMessages = chatRepository.getMessages(currentUser.id)
            val signupEvents = chatRepository.getSignupEvents(currentUser.id)
            loadedMessages to signupEvents
        }.onSuccess { (loadedMessages, signupEvents) ->
            messages.clear()
            messages.addAll(loadedMessages)
            rsvpedEventIds.clear()
            rsvpedEventIds.addAll(signupEvents.map { it.id })
            isLoading = false
            loadError = null
        }.onFailure { error ->
            Log.e("ChatScreen", "Failed to load chat state", error)
            isLoading = false
            loadError = error.message ?: "Unable to load messages"
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { draft ->
                coroutineScope.launch {
                    runCatching {
                        chatRepository.createEvent(currentUser.id, draft)
                    }.onSuccess { createdMessage ->
                        messages += createdMessage
                        createdMessage.event?.id?.let { eventId ->
                            if (!rsvpedEventIds.contains(eventId)) {
                                rsvpedEventIds += eventId
                            }
                        }
                        composerText = ""
                        showCreateDialog = false
                        loadError = null
                    }.onFailure { error ->
                        Log.e("ChatScreen", "Failed to create event", error)
                        loadError = error.message ?: "Unable to create event"
                    }
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
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
                        onClick = { coroutineScope.launch { drawerState.open() } },
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Describe what you feel like doing, and Spaces will route you toward the right events. Type /create to open the event form.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
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
                        isAlreadyRsvped = message.event?.id?.let(rsvpedEventIds::contains) == true,
                        onOpenEventChat = onOpenEventChat,
                        onRsvp = { eventId ->
                            coroutineScope.launch {
                                runCatching {
                                    chatRepository.rsvpToEvent(currentUser.id, eventId)
                                }.onSuccess {
                                    if (!rsvpedEventIds.contains(eventId)) {
                                        rsvpedEventIds += eventId
                                    }
                                    loadError = null
                                }.onFailure { error ->
                                    Log.e("ChatScreen", "Failed to RSVP to event", error)
                                    loadError = error.message ?: "Unable to RSVP"
                                }
                            }
                        },
                    )
                }
            }
            Composer(
                value = composerText,
                onValueChange = { composerText = it },
                isRecordingAudio = isRecordingAudio,
                isTranscribingAudio = isTranscribingAudio,
                onRecordAudio = {
                    if (isTranscribingAudio) return@Composer
                    if (isRecordingAudio) {
                        runCatching { voiceNoteRecorder.stop() }
                            .onSuccess { audioNote ->
                                isRecordingAudio = false
                                isTranscribingAudio = true
                                coroutineScope.launch {
                                    var optimisticMessage: ChatMessage? = null
                                    runCatching {
                                        val transcript = chatRepository.transcribeAudio(
                                            userId = currentUser.id,
                                            filePath = audioNote.filePath,
                                        )
                                        if (transcript.isBlank()) error("Transcription came back empty")
                                        optimisticMessage = ChatMessage(
                                            id = "local-${System.nanoTime()}",
                                            authorName = currentUser.displayName,
                                            body = transcript,
                                            isFromCurrentUser = true,
                                            timestamp = "Now",
                                        )
                                        messages += optimisticMessage!!
                                        chatRepository.sendMessage(
                                            userId = currentUser.id,
                                            authorName = currentUser.displayName,
                                            body = transcript,
                                        )
                                    }.onSuccess { createdMessages ->
                                        messages += createdMessages
                                        loadError = null
                                    }.onFailure { error ->
                                        Log.e("ChatScreen", "Failed to transcribe voice note", error)
                                        optimisticMessage?.let(messages::remove)
                                        loadError = error.message ?: "Unable to transcribe recording"
                                    }
                                    runCatching { java.io.File(audioNote.filePath).delete() }
                                    isTranscribingAudio = false
                                }
                            }
                            .onFailure { error ->
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
                            runCatching { voiceNoteRecorder.start() }
                                .onSuccess {
                                    isRecordingAudio = true
                                    loadError = null
                                }
                                .onFailure { error ->
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
                    if (isTranscribingAudio) return@Composer
                    val messageText = composerText.trim()
                    if (messageText.isEmpty()) return@Composer
                    if (messageText == "/create") {
                        showCreateDialog = true
                        composerText = ""
                        return@Composer
                    }
                    coroutineScope.launch {
                        val optimisticMessage = ChatMessage(
                            id = "local-${System.nanoTime()}",
                            authorName = currentUser.displayName,
                            body = messageText,
                            isFromCurrentUser = true,
                            timestamp = "Now",
                        )
                        messages += optimisticMessage
                        composerText = ""
                        runCatching {
                            chatRepository.sendMessage(
                                userId = currentUser.id,
                                authorName = currentUser.displayName,
                                body = messageText,
                            )
                        }.onSuccess { createdMessages ->
                            messages += createdMessages
                            loadError = null
                        }.onFailure { error ->
                            Log.e("ChatScreen", "Failed to send message", error)
                            messages.remove(optimisticMessage)
                            loadError = error.message ?: "Unable to send message"
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onOpenEventChat: (ChatEvent) -> Unit,
    onRsvp: (String) -> Unit,
    isAlreadyRsvped: Boolean,
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
                        event = event,
                        onOpenMaps = {
                            if (event.mapsUrl.isNotBlank()) uriHandler.openUri(event.mapsUrl)
                        },
                        onOpenEventChat = { onOpenEventChat(event) },
                        actionLabel = if (isAlreadyRsvped) "RSVP'd" else "RSVP Yes",
                        actionEnabled = !isAlreadyRsvped,
                        onActionClick = { onRsvp(event.id) },
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
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    isRecordingAudio: Boolean,
    isTranscribingAudio: Boolean,
    onRecordAudio: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                            "Recording voice note... tap the mic again to stop"
                        } else if (isTranscribingAudio) {
                            "Transcribing your voice note..."
                        } else {
                            "Ask about tonight, start a plan, or type /create"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventDialog(
    onDismiss: () -> Unit,
    onCreate: (EventDraft) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var locationName by rememberSaveable { mutableStateOf("") }
    var mapsUrl by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    var selectedTime by rememberSaveable { mutableStateOf(LocalTime.of(19, 0)) }
    var durationMinutes by rememberSaveable { mutableStateOf(60) }
    var categoryTagsInput by rememberSaveable { mutableStateOf("") }
    var visibilityType by rememberSaveable { mutableStateOf("targeted") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var durationMenuExpanded by remember { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.US) }
    val durationOptions = remember {
        listOf(
            30 to "30 min",
            60 to "1 hr",
            120 to "2 hr",
            180 to "3 hr",
            300 to "5 hr",
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            formError = "Location permission is required to use current location."
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            isLoadingLocation = true
            runCatching { resolveCurrentLocation(context) }
                .onSuccess { resolved ->
                    locationName = resolved.first
                    mapsUrl = resolved.second
                    formError = null
                }
                .onFailure { error ->
                    formError = error.message ?: "Unable to resolve current location."
                }
            isLoadingLocation = false
        }
    }

    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth,
        ).show()
    }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            false,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedDescription = description.trim()
                    val trimmedLocation = locationName.trim()
                    if (trimmedTitle.isBlank() || trimmedDescription.isBlank() || trimmedLocation.isBlank()) {
                        formError = "Fill in title, description, and location."
                        return@TextButton
                    }
                    val startDateTime = LocalDateTime.of(selectedDate, selectedTime)
                    val endDateTime = startDateTime.plusMinutes(durationMinutes.toLong())
                    onCreate(
                        EventDraft(
                            title = trimmedTitle,
                            description = trimmedDescription,
                            locationName = trimmedLocation,
                            mapsUrl = mapsUrl.ifBlank { buildMapsUrl(trimmedLocation) },
                            startsAtIso = startDateTime.toUtcIsoString(),
                            endsAtIso = endDateTime.toUtcIsoString(),
                            categoryTags = categoryTagsInput.split(",").map(String::trim).filter(String::isNotBlank),
                            visibilityType = visibilityType,
                            capacityMax = null,
                        )
                    )
                },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        title = { Text("Create event") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "Use structured fields so location and timing stay valid and easy to scan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("What is this about?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
                item {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = {
                            locationName = it
                            mapsUrl = buildMapsUrl(it)
                        },
                        label = { Text("Place") },
                        placeholder = { Text("Cafe, park, office, venue...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                            )
                        },
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val permissionState = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                )
                                if (permissionState == PackageManager.PERMISSION_GRANTED) {
                                    coroutineScope.launch {
                                        isLoadingLocation = true
                                        runCatching { resolveCurrentLocation(context) }
                                            .onSuccess { resolved ->
                                                locationName = resolved.first
                                                mapsUrl = resolved.second
                                                formError = null
                                            }
                                            .onFailure { error ->
                                                formError = error.message ?: "Unable to resolve current location."
                                            }
                                        isLoadingLocation = false
                                    }
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isLoadingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = "Current location",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                val query = if (locationName.isBlank()) "event venues near me" else locationName
                                openMapsSearch(context, query)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Open Maps",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                item {
                    SelectorField(
                        label = "Event date",
                        value = selectedDate.format(dateFormatter),
                        onClick = ::openDatePicker,
                    )
                }
                item {
                    SelectorField(
                        label = "Start time",
                        value = selectedTime.format(timeFormatter),
                        onClick = ::openTimePicker,
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = durationMenuExpanded,
                        onExpandedChange = { durationMenuExpanded = !durationMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = durationOptions.first { it.first == durationMinutes }.second,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estimated duration") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationMenuExpanded)
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = durationMenuExpanded,
                            onDismissRequest = { durationMenuExpanded = false },
                        ) {
                            durationOptions.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        durationMinutes = minutes
                                        durationMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = categoryTagsInput,
                        onValueChange = { categoryTagsInput = it },
                        label = { Text("Tags") },
                        placeholder = { Text("running, coffee, music") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = visibilityType,
                        onValueChange = { visibilityType = it },
                        label = { Text("Visibility") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        text = "Ends around ${LocalDateTime.of(selectedDate, selectedTime).plusMinutes(durationMinutes.toLong()).format(DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.US))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                formError?.let { errorText ->
                    item {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SelectorField(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private suspend fun resolveCurrentLocation(context: Context): Pair<String, String> = withContext(Dispatchers.IO) {
    val locationManager = context.getSystemService(LocationManager::class.java)
        ?: error("Location manager unavailable.")
    val providers = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    ).distinct()

    val location = providers
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
        ?: error("No recent location available.")

    val geocoder = Geocoder(context, Locale.getDefault())
    val placeName = runCatching {
        geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.let { address ->
                listOfNotNull(
                    address.featureName,
                    address.subLocality,
                    address.locality,
                ).distinct().joinToString(", ")
            }
    }.getOrNull().orEmpty().ifBlank {
        "${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}"
    }

    placeName to "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
}

private fun openMapsSearch(context: Context, query: String) {
    val encodedQuery = Uri.encode(query)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun buildMapsUrl(locationName: String): String {
    if (locationName.isBlank()) return ""
    return "geo:0,0?q=${URLEncoder.encode(locationName, StandardCharsets.UTF_8)}"
}

private fun LocalDateTime.toUtcIsoString(): String {
    return atZone(ZoneId.systemDefault()).toInstant().toString()
}
