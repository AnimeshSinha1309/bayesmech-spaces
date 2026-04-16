package spaces.bayesmech.com

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import spaces.bayesmech.com.data.AppRepositories
import spaces.bayesmech.com.data.backend.BackendConfig
import spaces.bayesmech.com.share.ShareIntentParser
import spaces.bayesmech.com.ui.SpacesApp
import spaces.bayesmech.com.ui.theme.SpacesTheme

class MainActivity : ComponentActivity() {
    private var openContentSignal by mutableIntStateOf(0)
    private var sharedFromLabel by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            SpacesTheme {
                SpacesApp(
                    openContentSignal = openContentSignal,
                    sharedFromLabel = sharedFromLabel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val safeIntent = intent ?: return
        val sourceAppLabel = safeIntent.`package`?.let { packageName ->
            runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            }.getOrNull()
        }

        val sharedContent = ShareIntentParser.parse(
            intent = safeIntent,
            sourceAppLabel = sourceAppLabel,
        ) ?: return

        val currentUser = runCatching {
            runBlocking {
                AppRepositories.backendRepository.getCurrentUser(BackendConfig.currentUserId)
            }
        }.getOrNull() ?: return

        AppRepositories.sharedContentRepository.addSharedContent(
            content = sharedContent,
            sharedBy = currentUser,
        )
        sharedFromLabel = sourceAppLabel
        openContentSignal += 1
    }
}
