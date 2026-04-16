package spaces.bayesmech.com.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import spaces.bayesmech.com.data.mock.MockChatRepository
import spaces.bayesmech.com.data.mock.MockCurrentUserRepository
import spaces.bayesmech.com.ui.navigation.AppDestination
import spaces.bayesmech.com.ui.screens.AiChatScreen
import spaces.bayesmech.com.ui.screens.ChatScreen
import spaces.bayesmech.com.ui.screens.PlaceholderScreen
import spaces.bayesmech.com.ui.screens.ProfileScreen

@Composable
fun SpacesApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val chatRepository = remember { MockChatRepository() }
    val currentUserRepository = remember { MockCurrentUserRepository() }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(AppDestination.Chat.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentUserName = currentUserRepository.getCurrentUser().displayName,
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
                        chatRepository = chatRepository,
                        currentUser = currentUserRepository.getCurrentUser(),
                        drawerState = drawerState,
                        onProfileClick = { navigateTo(AppDestination.Profile) },
                    )
                }
                composable(AppDestination.Community.route) {
                    PlaceholderScreen(
                        title = "Community",
                        description = "See the people you've crossed paths with through events and shared context.",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.Signups.route) {
                    PlaceholderScreen(
                        title = "Signups",
                        description = "Review the events you've RSVP'd to and keep upcoming plans in one place.",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.Content.route) {
                    PlaceholderScreen(
                        title = "Content",
                        description = "This page is intentionally empty for now and will become the home for future content flows.",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        currentUser = currentUserRepository.getCurrentUser(),
                        onTalkToAi = { navigateTo(AppDestination.ProfileAi) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.ProfileAi.route) {
                    AiChatScreen(
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
