package spaces.bayesmech.com.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SpacesMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.semantics {
                contentDescription = "Open navigation menu"
            },
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 13.dp),
            ) {
                val strokeWidth = 2.dp.toPx()
                val startX = size.width * 0.10f
                val topY = size.height * 0.34f
                val bottomY = size.height * 0.66f

                drawLine(
                    color = iconColor,
                    start = Offset(x = startX, y = topY),
                    end = Offset(x = size.width * 0.90f, y = topY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = Offset(x = startX, y = bottomY),
                    end = Offset(x = size.width * 0.66f, y = bottomY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
