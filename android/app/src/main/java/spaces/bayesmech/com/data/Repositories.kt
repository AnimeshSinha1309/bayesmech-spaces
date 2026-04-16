package spaces.bayesmech.com.data

import androidx.compose.runtime.snapshots.SnapshotStateList

interface ChatRepository {
    suspend fun getMessages(userId: String): List<ChatMessage>

    suspend fun getSignupEvents(userId: String): List<ChatEvent>

    suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): ChatMessage

    suspend fun createEvent(
        userId: String,
        draft: EventDraft,
    ): ChatMessage

    suspend fun rsvpToEvent(
        userId: String,
        eventId: String,
    )

    suspend fun transcribeAudio(
        userId: String,
        filePath: String,
    ): String
}

interface CurrentUserRepository {
    suspend fun getCurrentUser(userId: String): CurrentUser
}

interface SharedContentRepository {
    fun getSharedContent(): SnapshotStateList<SharedContentItem>
    fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser)
}

data class EventDraft(
    val title: String,
    val description: String,
    val locationName: String,
    val mapsUrl: String,
    val startsAtIso: String,
    val endsAtIso: String,
    val categoryTags: List<String>,
    val visibilityType: String,
    val capacityMax: Int?,
)
