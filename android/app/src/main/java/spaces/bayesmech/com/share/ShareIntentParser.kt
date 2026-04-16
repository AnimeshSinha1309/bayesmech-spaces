package spaces.bayesmech.com.share

import android.content.Intent
import android.os.Build
import android.net.Uri
import spaces.bayesmech.com.data.IncomingSharedContent
import spaces.bayesmech.com.data.SharedContentType

object ShareIntentParser {
    fun parse(intent: Intent, sourceAppLabel: String?): IncomingSharedContent? {
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND) {
            return null
        }

        val mimeType = intent.type.orEmpty()
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        return when {
            mimeType.startsWith("video/") || streamUri != null -> {
                val title = sharedSubject.ifBlank { "Shared video" }
                IncomingSharedContent(
                    type = SharedContentType.Video,
                    title = title,
                    url = streamUri?.toString(),
                    previewText = sharedText.ifBlank {
                        "A video was shared into Spaces from ${sourceAppLabel ?: "another app"}."
                    },
                    sourceAppLabel = sourceAppLabel,
                )
            }

            sharedText.isNotBlank() || sharedSubject.isNotBlank() -> {
                val url = extractFirstUrl(sharedText)
                val title = sharedSubject.ifBlank {
                    sharedText
                        .lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() && it != url }
                        ?.take(80)
                        ?: "Shared article"
                }
                IncomingSharedContent(
                    type = SharedContentType.Article,
                    title = title,
                    url = url,
                    previewText = sharedText.ifBlank {
                        "A link was shared into Spaces from ${sourceAppLabel ?: "another app"}."
                    },
                    sourceAppLabel = sourceAppLabel,
                )
            }

            else -> null
        }
    }

    private fun extractFirstUrl(text: String): String? =
        text.split("\\s+".toRegex())
            .firstOrNull { token ->
                token.startsWith("http://") || token.startsWith("https://")
            }
}
