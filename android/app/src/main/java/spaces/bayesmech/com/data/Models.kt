package spaces.bayesmech.com.data

data class CurrentUser(
    val id: String,
    val displayName: String,
    val headline: String,
    val locationLabel: String,
    val avatarUrl: String? = null,
    val avatarFallbackPrompt: String,
    val interestsSummary: String,
    val profileDictionary: ProfileDictionary,
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
    val audioNote: AudioNoteAttachment? = null,
)

data class AudioNoteAttachment(
    val filePath: String,
    val durationSeconds: Int,
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

data class ProfileDictionary(
    val homeBase: String = "",
    val workContext: String = "",
    val socialEnergy: String = "",
    val idealPlans: String = "",
    val socialGoals: String = "",
    val conversationSpark: String = "",
    val interestTags: List<String> = emptyList(),
    val profileNotes: String = "",
)

data class AiConversationMessage(
    val role: String,
    val text: String,
)

data class AiTurnResult(
    val assistantText: String,
    val updatedProfileDict: ProfileDictionary,
    val isComplete: Boolean,
    val model: String,
)

data class AiEndResult(
    val finalProfileDict: ProfileDictionary,
    val closingText: String,
    val isComplete: Boolean,
    val model: String,
)
