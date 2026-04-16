package spaces.bayesmech.com.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.UUID

class InMemorySharedContentRepository : SharedContentRepository {
    private val sharedContent = mutableStateListOf<SharedContentItem>()

    override fun getSharedContent(): SnapshotStateList<SharedContentItem> = sharedContent

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
