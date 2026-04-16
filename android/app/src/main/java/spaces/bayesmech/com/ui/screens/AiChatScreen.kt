package spaces.bayesmech.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val transcript = remember { mutableStateListOf<AiConversationMessage>() }
    var profileDictionary by remember(currentUser.id) { mutableStateOf(currentUser.profileDictionary) }
    var currentQuestion by rememberSaveable { mutableStateOf("") }
    var draftReply by rememberSaveable { mutableStateOf("") }
    var isConnecting by rememberSaveable { mutableStateOf(true) }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var isEnding by rememberSaveable { mutableStateOf(false) }
    var isConversationActive by rememberSaveable { mutableStateOf(true) }
    var statusText by rememberSaveable { mutableStateOf("Connecting to profile AI") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

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
                "Profile looks complete. You can still refine it."
            } else {
                "AI is filling in the remaining profile fields."
            }
            isConnecting = false
        }.onFailure { throwable ->
            currentQuestion = "I couldn't reach the profile AI right now."
            errorText = throwable.message
            statusText = "Connection failed"
            isConnecting = false
            isConversationActive = false
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
                .navigationBarsPadding()
                .imePadding(),
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
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (isConnecting) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Starting the profile call and loading your current dictionary.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = draftReply,
                            onValueChange = { draftReply = it },
                            enabled = isConversationActive && !isConnecting && !isSending && !isEnding,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Answer the AI or add something new")
                            },
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Surface(
                                onClick = {
                                    val message = draftReply.trim()
                                    if (message.isNotEmpty()) {
                                        coroutineScope.launch {
                                            isSending = true
                                            errorText = null
                                            transcript += AiConversationMessage(role = "user", text = message)
                                            runCatching {
                                                profileAiApi.sendReply(
                                                    user = currentUser,
                                                    transcript = transcript,
                                                    profileDictionary = profileDictionary,
                                                    userMessage = message,
                                                )
                                            }.onSuccess { result ->
                                                draftReply = ""
                                                currentQuestion = result.assistantText
                                                profileDictionary = result.updatedProfileDict
                                                transcript += AiConversationMessage(role = "assistant", text = result.assistantText)
                                                statusText = if (result.isComplete) {
                                                    "Profile dict is complete. Keep talking if you want to refine it."
                                                } else {
                                                    "AI is still filling profile gaps."
                                                }
                                            }.onFailure { throwable ->
                                                errorText = throwable.message
                                            }
                                            isSending = false
                                        }
                                    }
                                },
                                enabled = isConversationActive && !isConnecting && !isSending && !isEnding,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.NorthEast,
                                        contentDescription = "Send reply",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                onClick = {
                    if (isEnding) {
                        return@Surface
                    }
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
                                "Call ended. Final profile dict saved from the AI endpoint."
                            } else {
                                "Call ended. The AI returned the latest draft profile dict."
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
