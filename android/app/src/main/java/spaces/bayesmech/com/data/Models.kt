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
    val event: ChatEvent? = null,
)

data class ChatEvent(
    val title: String,
    val locationName: String,
    val mapsUrl: String,
    val description: String,
    val attendees: List<EventAttendee>,
    val additionalAttendeeCount: Int = 0,
)

data class EventAttendee(
    val displayName: String,
    val avatarUrl: String? = null,
)

enum class SharedContentType {
    Article,
    Video,
}

data class SharedContentItem(
    val id: String,
    val type: SharedContentType,
    val title: String,
    val url: String? = null,
    val previewText: String,
    val sharedByUserId: String,
    val sharedByName: String,
    val sharedByAvatarUrl: String? = null,
    val sharedAt: String,
    val sourceAppLabel: String? = null,
    val isVisibleToSharer: Boolean = true,
)

data class IncomingSharedContent(
    val type: SharedContentType,
    val title: String,
    val url: String? = null,
    val previewText: String,
    val sourceAppLabel: String? = null,
)
