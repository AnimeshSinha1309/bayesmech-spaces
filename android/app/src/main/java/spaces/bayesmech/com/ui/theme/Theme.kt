package spaces.bayesmech.com.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SpacesColorScheme: ColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = BrandBlack,
    primaryContainer = PaperDeep,
    onPrimaryContainer = BrandBlack,
    secondary = PaperWarm,
    onSecondary = BrandBlack,
    secondaryContainer = PaperDeep,
    onSecondaryContainer = BrandBlack,
    tertiary = MutedBrown,
    onTertiary = PaperBase,
    background = PaperBase,
    onBackground = BrandBlack,
    surface = PaperWarm,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = MutedBrown,
    outline = Line,
    outlineVariant = Line.copy(alpha = 0.65f),
)

@Composable
fun SpacesTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SpacesColorScheme,
        typography = Typography(),
        content = content,
    )
}
