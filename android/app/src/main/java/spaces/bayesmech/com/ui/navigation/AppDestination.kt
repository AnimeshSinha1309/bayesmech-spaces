package spaces.bayesmech.com.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Chat(
        route = "chat",
        label = "Chat",
        icon = Icons.Rounded.Person,
    ),
    Community(
        route = "community",
        label = "Community",
        icon = Icons.Rounded.Groups,
    ),
    Signups(
        route = "signups",
        label = "Signups",
        icon = Icons.Rounded.EventAvailable,
    ),
    Content(
        route = "content",
        label = "Content",
        icon = Icons.Rounded.LibraryBooks,
    ),
    ProfileAi(
        route = "profile-ai",
        label = "Talk to AI",
        icon = Icons.Rounded.Person,
    ),
    Profile(
        route = "profile",
        label = "Profile",
        icon = Icons.Rounded.Person,
    ),
}
