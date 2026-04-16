package spaces.bayesmech.com.data

import androidx.compose.runtime.snapshots.SnapshotStateList

interface ChatRepository {
    suspend fun getMessages(userId: String): List<ChatMessage>

    suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): ChatMessage
}

interface CurrentUserRepository {
    suspend fun getCurrentUser(userId: String): CurrentUser
}

interface SharedContentRepository {
    fun getSharedContent(): SnapshotStateList<SharedContentItem>
    fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser)
}
