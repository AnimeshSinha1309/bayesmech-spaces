package spaces.bayesmech.com.data

import androidx.compose.runtime.snapshots.SnapshotStateList

interface ChatRepository {
    suspend fun getMessages(userId: String): List<ChatMessage>

    suspend fun getSignupEvents(userId: String): List<ChatEvent>

    suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): List<ChatMessage>

    suspend fun createEvent(
        userId: String,
        draft: EventDraft,
    ): ChatMessage

    suspend fun setEventRsvp(
        userId: String,
        eventId: String,
        isRsvped: Boolean,
    )

    suspend fun transcribeAudio(
        userId: String,
        filePath: String,
    ): String
}

interface CurrentUserRepository {
    suspend fun getCurrentUser(userId: String): CurrentUser
    suspend fun signInWithUsername(username: String): CurrentUser
}

interface CommunityRepository {
    suspend fun getCommunity(userId: String): List<CommunityCandidate>
}

interface ConversationRepository {
    suspend fun openDirectMessage(
        userId: String,
        otherUserId: String,
    ): ConversationThread

    suspend fun getThread(
        threadId: String,
        viewerUserId: String,
    ): ConversationThread

    suspend fun getEventChat(
        eventId: String,
        viewerUserId: String,
    ): ConversationThread

    suspend fun sendThreadMessage(
        threadId: String,
        senderUserId: String,
        body: String,
    ): ChatMessage
}

interface SharedContentRepository {
    fun getSharedContent(currentUser: CurrentUser): SnapshotStateList<SharedContentItem>
    fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser)
    fun toggleLike(contentId: String, currentUserId: String)
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
