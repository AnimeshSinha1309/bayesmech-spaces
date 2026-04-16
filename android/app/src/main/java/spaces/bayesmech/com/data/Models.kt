package spaces.bayesmech.com.data

data class CurrentUser(
    val id: String,
    val displayName: String,
    val headline: String,
    val locationLabel: String,
    val avatarUrl: String? = null,
    val avatarFallbackPrompt: String,
    val interestsSummary: String,
    val journey: List<JourneyEntry>,
    val interests: List<InterestEntry>,
)

data class JourneyEntry(
    val title: String,
    val body: String,
)

data class InterestEntry(
    val tag: String,
    val description: String,
)

data class ChatMessage(
    val id: String,
    val authorName: String,
    val body: String,
    val isFromCurrentUser: Boolean,
    val timestamp: String,
)
