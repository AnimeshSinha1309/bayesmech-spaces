package spaces.bayesmech.com.data.mock

import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.InterestEntry
import spaces.bayesmech.com.data.JourneyEntry

internal object MockSeedData {
    val currentUser = CurrentUser(
        id = "user-animesh",
        displayName = "Animesh",
        headline = "Curious builder, runner, and low-pressure social planner",
        locationLabel = "Bengaluru",
        avatarUrl = null,
        avatarFallbackPrompt = "A warm Studio Ghibli-inspired portrait of Animesh with a thoughtful expression, soft paper-toned palette, subtle city background, and friendly social energy.",
        interestsSummary = "I’m drawn to things that feel alive, thoughtful, and a little textured. The best events for me usually combine movement, strong taste, or conversations with people who actually care about what they’re doing.",
        journey = listOf(
            JourneyEntry(
                title = "Where I’m from",
                body = "I’m from India, and a lot of how I read people and places comes from growing up around density, ambition, and constant motion. I learned early how to tell the difference between energy that feels alive and noise that just drains you.",
            ),
            JourneyEntry(
                title = "Where I went to school",
                body = "School made me interested in how ideas connect across disciplines. I was always more interested in environments that made me curious and conversations that changed how I saw things than in following one narrow track.",
            ),
            JourneyEntry(
                title = "Where I work",
                body = "I work on technology and product problems, which means I spend a lot of time thinking about behavior, interfaces, and what makes people show up for something. What I find cool is thoughtful ambition, good taste, and people building things with real intent. In my free time I run, walk through the city, find small music gatherings, and look for conversations that leave me sharper than before.",
            ),
        ),
        interests = listOf(
            InterestEntry(
                tag = "#running",
                description = "Running 10Ks keeps me grounded. I ran a 10K in Cubbon Park, I like weekend loops around Ulsoor Lake, and I’m usually looking for small group runs that feel social without turning into a spectacle.",
            ),
            InterestEntry(
                tag = "#startups",
                description = "I like conversations about product taste, distribution, and odd early-stage decisions. Small founder dinners and operator meetups are much more interesting to me than loud networking events.",
            ),
            InterestEntry(
                tag = "#music",
                description = "I keep coming back to intimate live sets, especially acoustic sessions and smaller rooms where you can actually listen. I like events where the music shapes the mood instead of overpowering it.",
            ),
            InterestEntry(
                tag = "#design",
                description = "I’m drawn to people who care about details, language, and how something feels in use. Design critiques, product conversations, and spaces with strong visual taste almost always pull me in.",
            ),
            InterestEntry(
                tag = "#citywalks",
                description = "I like exploring neighborhoods on foot, finding coffee spots, and letting conversations unfold while walking. Unstructured city time often leads to the best kinds of plans.",
            ),
        ),
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
