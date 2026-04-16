package spaces.bayesmech.com.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import spaces.bayesmech.com.data.ProfileAiApi
import spaces.bayesmech.com.data.AppRepositories
import spaces.bayesmech.com.data.ChatEvent
import spaces.bayesmech.com.data.ConversationThread
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.backend.BackendConfig
import spaces.bayesmech.com.ui.navigation.AppDestination
import spaces.bayesmech.com.ui.screens.AiChatScreen
import spaces.bayesmech.com.ui.screens.ChatScreen
import spaces.bayesmech.com.ui.screens.CommunityScreen
import spaces.bayesmech.com.ui.screens.ConversationScreen
import spaces.bayesmech.com.ui.screens.ContentScreen
import spaces.bayesmech.com.ui.screens.PlaceholderScreen
import spaces.bayesmech.com.ui.screens.ProfileScreen
import spaces.bayesmech.com.ui.screens.SignupsScreen

@Composable
fun SpacesApp(
    openContentSignal: Int = 0,
    sharedFromLabel: String? = null,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val repository = remember { AppRepositories.backendRepository }
    val sharedContentRepository = remember { AppRepositories.sharedContentRepository }
    val profileAiApi = remember { ProfileAiApi() }
    var currentUser by remember { mutableStateOf<CurrentUser?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var activeConversation by remember { mutableStateOf<ConversationThread?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            repository.getCurrentUser(BackendConfig.currentUserId)
        }.onSuccess { user ->
            currentUser = user
            loadError = null
        }.onFailure { error ->
            Log.e("SpacesApp", "Failed to load current user from backend", error)
            loadError = error.message ?: "Unable to connect to backend"
        }
    }

    val resolvedCurrentUser = currentUser
    if (resolvedCurrentUser == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (loadError == null) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading Spaces…",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    Text(
                        text = loadError ?: "Unable to load data",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Make sure the backend is running and reachable at ${BackendConfig.baseUrl}.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(AppDestination.Chat.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(openContentSignal) {
        if (openContentSignal > 0) {
            navigateTo(AppDestination.Content)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentUserName = resolvedCurrentUser.displayName,
                onDestinationSelected = { destination ->
                    navigateTo(destination)
                    drawerScope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Chat.route,
            ) {
                composable(AppDestination.Chat.route) {
                    ChatScreen(
                        chatRepository = repository,
                        currentUser = resolvedCurrentUser,
                        drawerState = drawerState,
                        onOpenEventChat = { event: ChatEvent ->
                            drawerScope.launch {
                                runCatching {
                                    repository.getEventChat(event.id, resolvedCurrentUser.id)
                                }.onSuccess { thread ->
                                    activeConversation = thread
                                    navigateTo(AppDestination.Conversation)
                                }.onFailure { error ->
                                    Log.e("SpacesApp", "Failed to open event chat", error)
                                }
                            }
                        },
                        onProfileClick = { navigateTo(AppDestination.Profile) },
                    )
                }
                composable(AppDestination.Community.route) {
                    CommunityScreen(
                        communityRepository = repository,
                        conversationRepository = repository,
                        currentUser = resolvedCurrentUser,
                        drawerState = drawerState,
                        onOpenConversation = { thread ->
                            activeConversation = thread
                            navigateTo(AppDestination.Conversation)
                        },
                    )
                }
                composable(AppDestination.Signups.route) {
                    SignupsScreen(
                        chatRepository = repository,
                        currentUser = resolvedCurrentUser,
                        onOpenEventChat = { event ->
                            drawerScope.launch {
                                runCatching {
                                    repository.getEventChat(event.id, resolvedCurrentUser.id)
                                }.onSuccess { thread ->
                                    activeConversation = thread
                                    navigateTo(AppDestination.Conversation)
                                }.onFailure { error ->
                                    Log.e("SpacesApp", "Failed to open signup event chat", error)
                                }
                            }
                        },
                    )
                }
                composable(AppDestination.Content.route) {
                    ContentScreen(
                        sharedContent = sharedContentRepository.getSharedContent(resolvedCurrentUser),
                        currentUser = resolvedCurrentUser,
                        drawerState = drawerState,
                        latestSourceAppLabel = sharedFromLabel,
                        onToggleLike = { contentId ->
                            sharedContentRepository.toggleLike(contentId, resolvedCurrentUser.id)
                        },
                    )
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        currentUser = resolvedCurrentUser,
                        onTalkToAi = { navigateTo(AppDestination.ProfileAi) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.ProfileAi.route) {
                    AiChatScreen(
                        currentUser = resolvedCurrentUser,
                        profileAiApi = profileAiApi,
                        onProfileFinalized = { updatedProfile ->
                            currentUser = resolvedCurrentUser.copy(profileDictionary = updatedProfile)
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.Conversation.route) {
                    activeConversation?.let { thread ->
                        ConversationScreen(
                            conversationRepository = repository,
                            currentUser = resolvedCurrentUser,
                            thread = thread,
                            onBack = { navController.popBackStack() },
                        )
                    } ?: PlaceholderScreen(
                        title = "Conversation",
                        description = "Open a community match or event chat first.",
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(
    navController: NavHostController,
    currentUserName: String,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val drawerItems = listOf(
        AppDestination.Community,
        AppDestination.Signups,
        AppDestination.Content,
    )

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Spaces",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Navigate the social graph around your events.",
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            drawerItems.forEach { destination ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    selected = currentRoute == destination.route,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Surface(
                onClick = { onDestinationSelected(AppDestination.Profile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = currentUserName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
