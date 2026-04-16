package spaces.bayesmech.com.data.mock

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.CurrentUserRepository
import spaces.bayesmech.com.data.IncomingSharedContent
import spaces.bayesmech.com.data.SharedContentItem
import spaces.bayesmech.com.data.SharedContentRepository
import java.util.UUID

class MockChatRepository : ChatRepository {
    override fun getSeedMessages(): List<ChatMessage> = MockSeedData.chatMessages
}

class MockCurrentUserRepository : CurrentUserRepository {
    private val currentUserState = mutableStateOf(MockSeedData.currentUser)

    override fun getCurrentUser(): CurrentUser = currentUserState.value

    override fun updateProfileDictionary(profileDictionary: spaces.bayesmech.com.data.ProfileDictionary): CurrentUser {
        currentUserState.value = currentUserState.value.copy(profileDictionary = profileDictionary)
        return currentUserState.value
    }
}

class MockSharedContentRepository : SharedContentRepository {
    private val sharedContent = mutableStateListOf<SharedContentItem>().apply {
        addAll(MockSeedData.sharedContent)
    }

    override fun getSharedContent() = sharedContent

    override fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser) {
        sharedContent.add(
            index = 0,
            element = SharedContentItem(
                id = UUID.randomUUID().toString(),
                type = content.type,
                title = content.title,
                url = content.url,
                previewText = content.previewText,
                sharedByUserId = sharedBy.id,
                sharedByName = sharedBy.displayName,
                sharedByAvatarUrl = sharedBy.avatarUrl,
                sharedAt = "Just now",
                sourceAppLabel = content.sourceAppLabel,
            ),
        )
    }
}

object MockRepositories {
    val chatRepository: ChatRepository = MockChatRepository()
    val currentUserRepository: CurrentUserRepository = MockCurrentUserRepository()
    val sharedContentRepository: SharedContentRepository = MockSharedContentRepository()
}
