package spaces.bayesmech.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import spaces.bayesmech.com.ui.SpacesApp
import spaces.bayesmech.com.ui.theme.SpacesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpacesTheme {
                SpacesApp()
            }
        }
    }
}
