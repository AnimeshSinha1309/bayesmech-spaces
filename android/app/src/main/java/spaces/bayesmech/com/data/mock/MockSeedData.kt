package spaces.bayesmech.com.data.mock

import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.CurrentUser

internal object MockSeedData {
    val currentUser = CurrentUser(
        id = "user-animesh",
        displayName = "Animesh",
    )

    val chatMessages = listOf(
        ChatMessage(
            id = "msg-1",
            authorName = "Spaces",
            body = "Good morning. What kind of event are you in the mood for today?",
            isFromCurrentUser = false,
            timestamp = "9:02 AM",
        ),
        ChatMessage(
            id = "msg-2",
            authorName = "Animesh",
            body = "Something social but low-pressure. Maybe music, a run, or a talk.",
            isFromCurrentUser = true,
            timestamp = "9:03 AM",
        ),
        ChatMessage(
            id = "msg-3",
            authorName = "Spaces",
            body = "I can help with that. Tonight looks strong for a rooftop acoustic set and a small founder dinner.",
            isFromCurrentUser = false,
            timestamp = "9:04 AM",
        ),
        ChatMessage(
            id = "msg-4",
            authorName = "Spaces",
            body = "If you'd rather keep it active, there's also a sunrise run gathering tomorrow.",
            isFromCurrentUser = false,
            timestamp = "9:05 AM",
        ),
    )
}
