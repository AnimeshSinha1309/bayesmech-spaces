package spaces.bayesmech.com.data.mock

import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.CurrentUserRepository

class MockChatRepository : ChatRepository {
    override fun getSeedMessages(): List<ChatMessage> = MockSeedData.chatMessages
}

class MockCurrentUserRepository : CurrentUserRepository {
    override fun getCurrentUser(): CurrentUser = MockSeedData.currentUser
}
