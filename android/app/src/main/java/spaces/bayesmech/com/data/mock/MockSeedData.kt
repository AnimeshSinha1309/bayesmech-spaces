package spaces.bayesmech.com.data.mock

import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatEvent
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.EventAttendee
import spaces.bayesmech.com.data.InterestEntry
import spaces.bayesmech.com.data.JourneyEntry
import spaces.bayesmech.com.data.ProfileDictionary
import spaces.bayesmech.com.data.SharedContentItem
import spaces.bayesmech.com.data.SharedContentType

internal object MockSeedData {
    val currentUser = CurrentUser(
        id = "user-animesh",
        displayName = "Animesh",
        headline = "Curious builder, runner, and low-pressure social planner",
        locationLabel = "Bengaluru",
        avatarUrl = "avatar_animesh",
        avatarFallbackPrompt = "A warm Studio Ghibli-inspired portrait of Animesh with a thoughtful expression, soft paper-toned palette, subtle city background, and friendly social energy.",
        interestsSummary = "I’m drawn to things that feel alive, thoughtful, and a little textured. The best events for me usually combine movement, strong taste, or conversations with people who actually care about what they’re doing.",
        profileDictionary = ProfileDictionary(
            homeBase = "Bengaluru",
            workContext = "I work on technology and product problems and care about behavior, interfaces, and why people show up.",
            socialEnergy = "Low-pressure, thoughtful, and quietly social.",
            idealPlans = "Small music gatherings, group runs, city walks, and conversations with people building interesting things.",
            socialGoals = "",
            conversationSpark = "",
            interestTags = listOf("running", "startups", "music", "design", "citywalks"),
            profileNotes = "Prefers spaces with taste, curiosity, and room for real conversation.",
        ),
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
            body = "I can help with that. Here is one event that matches your vibe tonight.",
            isFromCurrentUser = false,
            timestamp = "9:04 AM",
            event = ChatEvent(
                title = "Rooftop Acoustic Session",
                locationName = "Dialogues Cafe, Koramangala",
                mapsUrl = "https://maps.google.com/?q=Dialogues+Cafe+Koramangala+Bengaluru",
                description = "An intimate acoustic evening on a rooftop with stripped-back sets, a small crowd, and enough room to actually talk between songs.",
                attendees = listOf(
                    EventAttendee("Nikhil", avatarUrl = "avatar_nikhil"),
                    EventAttendee("Rhea", avatarUrl = "avatar_rhea"),
                    EventAttendee("Sana", avatarUrl = "avatar_sana"),
                ),
                additionalAttendeeCount = 13,
            ),
        ),
        ChatMessage(
            id = "msg-4",
            authorName = "Spaces",
            body = "If you'd rather keep it active, there's also a sunrise run gathering tomorrow.",
            isFromCurrentUser = false,
            timestamp = "9:05 AM",
        ),
    )

    val sharedContent = listOf(
        SharedContentItem(
            id = "shared-1",
            type = SharedContentType.Article,
            title = "Why small group rituals build better communities",
            url = "https://example.com/community-rituals",
            previewText = "A long-form piece on why repeated, low-pressure gatherings create stronger ties than one-off networking events.",
            sharedByUserId = "user-rhea",
            sharedByName = "Rhea",
            sharedByAvatarUrl = "avatar_rhea",
            sharedAt = "Today, 8:12 AM",
            sourceAppLabel = "Chrome",
        ),
        SharedContentItem(
            id = "shared-2",
            type = SharedContentType.Video,
            title = "Sunrise run route recap",
            url = "content://videos/mock/sunrise-run-route",
            previewText = "Short video recap from the Cubbon Park loop the group wants to repeat this weekend.",
            sharedByUserId = "user-nikhil",
            sharedByName = "Nikhil",
            sharedByAvatarUrl = "avatar_nikhil",
            sharedAt = "Today, 7:40 AM",
            sourceAppLabel = "Photos",
        ),
        SharedContentItem(
            id = "shared-3",
            type = SharedContentType.Article,
            title = "Designing for better social serendipity",
            url = "https://example.com/social-serendipity",
            previewText = "Shared by you. For now your own posts stay visible to you while we sort out the final visibility model.",
            sharedByUserId = currentUser.id,
            sharedByName = currentUser.displayName,
            sharedByAvatarUrl = currentUser.avatarUrl,
            sharedAt = "Yesterday",
            sourceAppLabel = "Firefox",
        ),
    )
}
