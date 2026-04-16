package spaces.bayesmech.com.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
fun AvatarImage(
    avatarUrl: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatarResId = avatarUrl
        ?.takeIf { it.isNotBlank() }
        ?.let { resourceName ->
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
        ?.takeIf { it != 0 }

    if (avatarResId != null) {
        Image(
            painter = painterResource(id = avatarResId),
            contentDescription = fallbackLabel,
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackLabel.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
