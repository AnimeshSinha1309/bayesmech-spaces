package spaces.bayesmech.com.data

import androidx.compose.runtime.snapshots.SnapshotStateList

interface ChatRepository {
    suspend fun getMessages(userId: String): List<ChatMessage>

    suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): ChatMessage

    suspend fun transcribeAudio(
        userId: String,
        filePath: String,
    ): String
}

interface CurrentUserRepository {
    suspend fun getCurrentUser(userId: String): CurrentUser
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
