package spaces.bayesmech.com.data

import androidx.compose.runtime.snapshots.SnapshotStateList

interface ChatRepository {
    fun getSeedMessages(): List<ChatMessage>
}

interface CurrentUserRepository {
    fun getCurrentUser(): CurrentUser
    fun updateProfileDictionary(profileDictionary: ProfileDictionary): CurrentUser
}

interface SharedContentRepository {
    fun getSharedContent(): SnapshotStateList<SharedContentItem>
    fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser)
}
