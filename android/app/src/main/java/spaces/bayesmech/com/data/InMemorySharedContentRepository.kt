package spaces.bayesmech.com.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.net.URI
import java.util.Locale
import java.util.UUID

class InMemorySharedContentRepository : SharedContentRepository {
    private val allSharedContent = mutableStateListOf<SharedContentItem>().apply {
        addAll(seedSharedContent())
    }
    private val visibleSharedContent = mutableStateListOf<SharedContentItem>()
    private var activeViewer: CurrentUser? = null

    override fun getSharedContent(currentUser: CurrentUser): SnapshotStateList<SharedContentItem> {
        activeViewer = currentUser
        refreshVisibleContent()
        return visibleSharedContent
    }

    override fun addSharedContent(content: IncomingSharedContent, sharedBy: CurrentUser) {
        allSharedContent.add(
            index = 0,
            element = SharedContentItem(
                id = UUID.randomUUID().toString(),
                type = content.type,
                title = content.title,
                url = content.url,
                previewText = content.previewText,
                topicTags = inferTopicTags(content),
                sharedByUserId = sharedBy.id,
                sharedByName = sharedBy.displayName,
                sharedByAvatarUrl = sharedBy.avatarUrl,
                sharedAt = "Just now",
                sourceAppLabel = content.sourceAppLabel,
                isVisibleToSharer = false,
            ),
        )
        refreshVisibleContent()
    }

    override fun toggleLike(contentId: String, currentUserId: String) {
        val currentIndex = allSharedContent.indexOfFirst { it.id == contentId }
        if (currentIndex == -1) {
            return
        }
        val item = allSharedContent[currentIndex]
        val updatedLikes = item.likedByUserIds.toMutableSet().apply {
            if (!add(currentUserId)) {
                remove(currentUserId)
            }
        }
        allSharedContent[currentIndex] = item.copy(likedByUserIds = updatedLikes)
        refreshVisibleContent()
    }

    private fun refreshVisibleContent() {
        val viewer = activeViewer ?: return
        val viewerTokens = viewer.interestTokens()
        val dedupedItems = linkedMapOf<String, RankedItem>()

        allSharedContent.forEachIndexed { position, item ->
            if (item.sharedByUserId == viewer.id) {
                return@forEachIndexed
            }
            val dedupeKey = item.url?.normalizeUrlForDedupe()
                ?: "${item.type.name}:${item.title.normalizeTextForDedupe()}"
            dedupedItems.putIfAbsent(
                dedupeKey,
                RankedItem(
                    position = position,
                    item = item,
                    match = item.bestInterestMatch(viewerTokens),
                ),
            )
        }

        val rankedItems = dedupedItems.values
            .sortedWith(
                compareByDescending<RankedItem> { it.match.score }
                    .thenBy { it.position },
            )
            .map { ranked ->
                ranked.item.copy(
                    interestMatchLabel = ranked.match.label,
                )
            }

        visibleSharedContent.clear()
        visibleSharedContent.addAll(rankedItems)
    }
}

private data class RankedItem(
    val position: Int,
    val item: SharedContentItem,
    val match: InterestMatch,
)

private data class InterestMatch(
    val score: Int,
    val label: String?,
)

private fun seedSharedContent(): List<SharedContentItem> = listOf(
    SharedContentItem(
        id = "share-runner-sana-01",
        type = SharedContentType.Article,
        title = "Why does running give you a high? Here's the science",
        url = "https://ideas.ted.com/why-does-running-give-you-a-high-heres-the-science/",
        previewText = "A strong science-led explainer on why running can change mood, attention, and energy so reliably.",
        topicTags = listOf("running", "fitness", "science", "endurance"),
        sharedByUserId = "usr-attendee-03",
        sharedByName = "Sana",
        sharedByAvatarUrl = "avatar_sana",
        sharedAt = "Today, 8:10 AM",
        sourceAppLabel = "TED Ideas",
    ),
    SharedContentItem(
        id = "share-runner-meera-duplicate",
        type = SharedContentType.Article,
        title = "Why does running give you a high? Here's the science",
        url = "https://ideas.ted.com/why-does-running-give-you-a-high-heres-the-science/?utm_source=share",
        previewText = "Shared again for the way it connects endurance, brain chemistry, and the emotional side of movement.",
        topicTags = listOf("running", "fitness", "science", "endurance"),
        sharedByUserId = "usr-attendee-05",
        sharedByName = "Meera",
        sharedAt = "Today, 9:05 AM",
        sourceAppLabel = "TED Ideas",
    ),
    SharedContentItem(
        id = "share-chess-rhea-01",
        type = SharedContentType.Article,
        title = "Chess Strategy | Study Plan For Beginners",
        url = "https://www.chess.com/article/view/chess-strategy",
        previewText = "A structured beginner-friendly piece on coordination, planning, and how to study chess with more intent.",
        topicTags = listOf("chess", "strategy", "games", "learning"),
        sharedByUserId = "usr-attendee-02",
        sharedByName = "Rhea",
        sharedByAvatarUrl = "avatar_rhea",
        sharedAt = "Today, 10:15 AM",
        sourceAppLabel = "Chess.com",
    ),
    SharedContentItem(
        id = "share-writing-leah-01",
        type = SharedContentType.Article,
        title = "13 writing tips, from beloved teacher Anne Lamott",
        url = "https://ideas.ted.com/14-writing-tips-from-beloved-teacher-anne-lamott/",
        previewText = "A useful, readable set of writing principles on first drafts, revision, honesty, and sticking with the work.",
        topicTags = listOf("writing", "creativity", "craft", "ideas"),
        sharedByUserId = "usr-attendee-15",
        sharedByName = "Leah",
        sharedAt = "Today, 11:20 AM",
        sourceAppLabel = "TED Ideas",
    ),
    SharedContentItem(
        id = "share-video-zoya-01",
        type = SharedContentType.Video,
        title = "Your Elusive Creative Genius",
        url = "https://www.ted.com/talks/elizabeth_gilbert_your_elusive_creative_genius",
        previewText = "Elizabeth Gilbert on creative pressure, steadier expectations, and how to keep making work without turning art into self-punishment.",
        topicTags = listOf("writing", "creativity", "art", "work"),
        sharedByUserId = "usr-attendee-16",
        sharedByName = "Zoya",
        sharedAt = "Today, 12:05 PM",
        sourceAppLabel = "TED",
    ),
    SharedContentItem(
        id = "share-video-maya-duplicate",
        type = SharedContentType.Video,
        title = "Your Elusive Creative Genius",
        url = "https://www.ted.com/talks/elizabeth_gilbert_your_elusive_creative_genius?language=en",
        previewText = "Worth revisiting for anyone balancing output, taste, and the emotional weirdness of creative work.",
        topicTags = listOf("writing", "creativity", "art", "work"),
        sharedByUserId = "usr-attendee-09",
        sharedByName = "Maya",
        sharedAt = "Today, 12:18 PM",
        sourceAppLabel = "TED",
    ),
    SharedContentItem(
        id = "share-video-kabir-01",
        type = SharedContentType.Video,
        title = "The danger of a single story",
        url = "https://www.ted.com/talks/chimamanda_ngozi_adichie_the_danger_of_a_single_story",
        previewText = "Chimamanda Ngozi Adichie on narrative breadth, cultural attention, and why better stories lead to better conversations.",
        topicTags = listOf("writing", "culture", "conversation", "ideas"),
        sharedByUserId = "usr-attendee-06",
        sharedByName = "Kabir",
        sharedAt = "Today, 1:10 PM",
        sourceAppLabel = "TED",
    ),
    SharedContentItem(
        id = "share-video-neil-01",
        type = SharedContentType.Video,
        title = "Want to be more creative? Go for a walk",
        url = "https://www.ted.com/talks/marily_oppezzo_want_to_be_more_creative_go_for_a_walk",
        previewText = "A concise TED talk that connects movement and idea generation in a way that feels immediately usable.",
        topicTags = listOf("walking", "creativity", "ideas", "citywalks"),
        sharedByUserId = "usr-attendee-12",
        sharedByName = "Neil",
        sharedAt = "Today, 2:00 PM",
        sourceAppLabel = "TED",
    ),
)

private fun inferTopicTags(content: IncomingSharedContent): List<String> {
    val normalized = listOfNotNull(
        content.title,
        content.previewText,
        content.sourceAppLabel,
    ).joinToString(" ").lowercase(Locale.US)

    return buildList {
        if ("run" in normalized || "running" in normalized) add("running")
        if ("walk" in normalized || "walking" in normalized) add("citywalks")
        if ("chess" in normalized) add("chess")
        if ("write" in normalized || "writing" in normalized) add("writing")
        if ("music" in normalized) add("music")
        if ("design" in normalized) add("design")
        if ("startup" in normalized || "product" in normalized) add("startups")
        if ("creative" in normalized || "creativity" in normalized) add("creativity")
    }.ifEmpty { listOf("ideas") }
}

private fun CurrentUser.interestTokens(): Set<String> = buildSet {
    interests.forEach { interest ->
        addAll(interest.tag.extractTokens())
        addAll(interest.description.extractTokens())
    }
    addAll(interestsSummary.extractTokens())
    addAll(profileDictionary.interestTags.map { it.lowercase(Locale.US) })
    addAll(headline.extractTokens())
}.filterMeaningfulTokens()

private fun SharedContentItem.bestInterestMatch(viewerTokens: Set<String>): InterestMatch {
    if (viewerTokens.isEmpty()) {
        return InterestMatch(score = 0, label = null)
    }

    val normalizedTopicTags = topicTags.map { it.lowercase(Locale.US) }
    val matchingTag = normalizedTopicTags.firstOrNull { it in viewerTokens }
    if (matchingTag != null) {
        return InterestMatch(
            score = 2,
            label = "Ranked for your interest in ${matchingTag.toDisplayInterest()}",
        )
    }

    val titleTokens = title.extractTokens()
    val previewTokens = previewText.extractTokens()
    val fallbackMatch = viewerTokens.firstOrNull { token ->
        token in titleTokens || token in previewTokens
    }
    return if (fallbackMatch != null) {
        InterestMatch(
            score = 1,
            label = "Related to ${fallbackMatch.toDisplayInterest()}",
        )
    } else {
        InterestMatch(score = 0, label = null)
    }
}

private fun Iterable<String>.filterMeaningfulTokens(): Set<String> = filterTo(linkedSetOf()) { token ->
    token.length > 2 && token !in STOP_WORDS
}

private fun String.extractTokens(): Set<String> = lowercase(Locale.US)
    .replace("#", " ")
    .split(Regex("[^a-z0-9]+"))
    .filterMeaningfulTokens()

private fun String.normalizeTextForDedupe(): String = trim().lowercase(Locale.US)

private fun String.normalizeUrlForDedupe(): String {
    val raw = trim()
    return runCatching {
        val uri = URI(raw)
        val filteredQuery = uri.rawQuery
            ?.split("&")
            ?.filterNot { parameter ->
                val key = parameter.substringBefore("=").lowercase(Locale.US)
                key.startsWith("utm_") || key == "language"
            }
            ?.joinToString("&")
            ?.takeIf { it.isNotBlank() }
        URI(
            uri.scheme?.lowercase(Locale.US),
            uri.authority?.lowercase(Locale.US),
            uri.path?.trimEnd('/'),
            filteredQuery,
            null,
        ).toString()
    }.getOrElse {
        raw.lowercase(Locale.US)
    }
}

private fun String.toDisplayInterest(): String = "#$this"

private val STOP_WORDS = setOf(
    "about",
    "actually",
    "around",
    "because",
    "being",
    "better",
    "between",
    "could",
    "enough",
    "from",
    "here",
    "into",
    "just",
    "like",
    "more",
    "really",
    "that",
    "their",
    "them",
    "there",
    "they",
    "this",
    "those",
    "want",
    "with",
    "without",
    "would",
)
