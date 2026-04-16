package spaces.bayesmech.com.data

interface ChatRepository {
    fun getSeedMessages(): List<ChatMessage>
}

interface CurrentUserRepository {
    fun getCurrentUser(): CurrentUser
}
