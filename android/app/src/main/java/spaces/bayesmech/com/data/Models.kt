package spaces.bayesmech.com.data

data class CurrentUser(
    val id: String,
    val displayName: String,
)

data class ChatMessage(
    val id: String,
    val authorName: String,
    val body: String,
    val isFromCurrentUser: Boolean,
    val timestamp: String,
)
