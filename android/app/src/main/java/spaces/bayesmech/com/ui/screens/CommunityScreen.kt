package spaces.bayesmech.com.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import spaces.bayesmech.com.data.CommunityCandidate
import spaces.bayesmech.com.data.CommunityRepository
import spaces.bayesmech.com.data.ConversationRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.ConversationThread
import spaces.bayesmech.com.ui.components.AvatarImage
import spaces.bayesmech.com.ui.components.SpacesMenuButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    communityRepository: CommunityRepository,
    conversationRepository: ConversationRepository,
    currentUser: CurrentUser,
    drawerState: DrawerState,
    onOpenConversation: (ConversationThread) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val candidates = remember { mutableStateListOf<CommunityCandidate>() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var openingUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser.id) {
        runCatching {
            communityRepository.getCommunity(currentUser.id)
        }.onSuccess { loadedCandidates ->
            candidates.clear()
            candidates.addAll(loadedCandidates)
            isLoading = false
            loadError = null
        }.onFailure { error ->
            Log.e("CommunityScreen", "Failed to load community", error)
            isLoading = false
            loadError = error.message ?: "Unable to load community"
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
                        text = "Community",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    SpacesMenuButton(
                        onClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "People are ranked from your profile using embeddings for recall and a pairwise OpenAI reranker for the final cut.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Tap anyone to open a direct message.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            loadError?.let { error ->
                item {
                    Text(
                        text = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(candidates, key = { it.userId }) { candidate ->
                CommunityCandidateCard(
                    candidate = candidate,
                    isOpening = openingUserId == candidate.userId,
                    onClick = {
                        openingUserId = candidate.userId
                        coroutineScope.launch {
                            runCatching {
                                conversationRepository.openDirectMessage(
                                    userId = currentUser.id,
                                    otherUserId = candidate.userId,
                                )
                            }.onSuccess { thread ->
                                loadError = null
                                onOpenConversation(thread)
                            }.onFailure { error ->
                                Log.e("CommunityScreen", "Failed to open DM", error)
                                loadError = error.message ?: "Unable to open conversation"
                            }
                            openingUserId = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CommunityCandidateCard(
    candidate: CommunityCandidate,
    isOpening: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    AvatarImage(
                        avatarUrl = candidate.avatarUrl,
                        fallbackLabel = candidate.displayName,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val secondary = buildString {
                        candidate.username?.let { append("@$it") }
                        if (candidate.score > 0) {
                            if (isNotBlank()) append(" • ")
                            append("${candidate.score} match")
                        }
                    }
                    if (secondary.isNotBlank()) {
                        Text(
                            text = secondary,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isOpening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (candidate.headline.isNotBlank()) {
                Text(
                    text = candidate.headline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = candidate.reasoning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (candidate.whatMatches.isNotEmpty()) {
                Text(
                    text = candidate.whatMatches.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (candidate.sharedEventCount > 0) {
                Text(
                    text = "You already overlap on ${candidate.sharedEventCount} joined events.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
