package spaces.bayesmech.com.data

import spaces.bayesmech.com.data.backend.BackendRepository

object AppRepositories {
    val backendRepository: BackendRepository = BackendRepository()
    val sharedContentRepository: SharedContentRepository = InMemorySharedContentRepository()
}
