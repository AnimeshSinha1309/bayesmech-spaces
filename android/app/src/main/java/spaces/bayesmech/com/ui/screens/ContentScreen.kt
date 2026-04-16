package spaces.bayesmech.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.SharedContentItem
import spaces.bayesmech.com.data.SharedContentType
import spaces.bayesmech.com.ui.components.AvatarImage
import spaces.bayesmech.com.ui.components.SpacesMenuButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    sharedContent: SnapshotStateList<SharedContentItem>,
    currentUser: CurrentUser,
    drawerState: DrawerState,
    latestSourceAppLabel: String?,
    onToggleLike: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

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
                        text = "Shared Content",
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
                ContentFeedHeader(
                    currentUser = currentUser,
                    latestSourceAppLabel = latestSourceAppLabel,
                )
            }
            items(sharedContent, key = { it.id }) { item ->
                SharedContentCard(
                    item = item,
                    currentUser = currentUser,
                    onToggleLike = onToggleLike,
                )
            }
        }
    }
}

@Composable
private fun ContentFeedHeader(
    currentUser: CurrentUser,
    latestSourceAppLabel: String?,
) {
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
                text = "Share articles and videos into Spaces from any Android app.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = buildString {
                    append("Anything you share here is shown to other users in the shared feed. ")
                    append("Your own shares are excluded from your feed, repeated links are collapsed, and cards are ranked by your interests.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = latestSourceAppLabel?.let {
                    "Latest import came from $it as ${currentUser.displayName}, but it will not appear in your own feed."
                } ?: "Use the system share sheet to send something here as ${currentUser.displayName}. Your own shares stay hidden from you.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SharedContentCard(
    item: SharedContentItem,
    currentUser: CurrentUser,
    onToggleLike: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val icon = when (item.type) {
        SharedContentType.Article -> Icons.AutoMirrored.Rounded.Article
        SharedContentType.Video -> Icons.Rounded.Movie
    }
    val typeLabel = when (item.type) {
        SharedContentType.Article -> "Article"
        SharedContentType.Video -> "Video"
    }
    val isLiked = currentUser.id in item.likedByUserIds
    val likeCount = item.likedByUserIds.size
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = {},
                onDoubleClick = { onToggleLike(item.id) },
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    AvatarImage(
                        avatarUrl = item.sharedByAvatarUrl,
                        fallbackLabel = item.sharedByName,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Text(
                text = item.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            item.interestMatchLabel?.let { matchLabel ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = matchLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item.url?.let { url ->
                Row(
                    modifier = Modifier.clickable { uriHandler.openUri(url) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = buildString {
                    append("Shared by ")
                    append(item.sharedByName)
                    append(" • ")
                    append(item.sharedAt)
                    item.sourceAppLabel?.let {
                        append(" • from ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .clickable { onToggleLike(item.id) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (likeCount == 0) "Like" else likeCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Text(
                    text = "Double tap to heart this post.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(180.dp),
                )
            }

            Text(
                text = "Visible in the shared community feed.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
