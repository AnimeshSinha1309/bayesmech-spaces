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

interface SharedContentRepository {
    fun getSharedContent(currentUser: CurrentUser): SnapshotStateList<SharedContentItem>
    fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser)
    fun toggleLike(contentId: String, currentUserId: String)
}
