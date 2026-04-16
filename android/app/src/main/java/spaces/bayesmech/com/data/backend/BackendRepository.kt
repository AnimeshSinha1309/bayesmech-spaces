package spaces.bayesmech.com.data.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import spaces.bayesmech.com.data.ChatEvent
import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.CurrentUserRepository
import spaces.bayesmech.com.data.EventAttendee
import spaces.bayesmech.com.data.InterestEntry
import spaces.bayesmech.com.data.JourneyEntry
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class BackendRepository(
    private val client: OkHttpClient = OkHttpClient(),
) : ChatRepository, CurrentUserRepository {

    private var cachedBootstrap: BootstrapPayload? = null

    override suspend fun getCurrentUser(userId: String): CurrentUser {
        return withContext(Dispatchers.IO) {
            loadBootstrap(userId).currentUser
        }
    }

    override suspend fun getMessages(userId: String): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            loadBootstrap(userId).messages
        }
    }

    override suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): ChatMessage {
        return withContext(Dispatchers.IO) {
            val requestJson = JSONObject()
                .put("sender_type", "user")
                .put("sender_user_id", userId)
                .put("message_type", "text")
                .put("content_text", body)

            val request = Request.Builder()
                .url("${BackendConfig.baseUrl}/chat/$userId/messages")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val responseJson = executeJsonRequest(request)
            val currentUser = cachedBootstrap?.currentUser ?: loadBootstrap(userId).currentUser
            val message = parseChatMessage(responseJson, userId, currentUser.displayName)
            cachedBootstrap = cachedBootstrap?.copy(messages = cachedBootstrap!!.messages + message)
            message
        }
    }

    private fun loadBootstrap(userId: String): BootstrapPayload {
        cachedBootstrap?.let { return it }

        val request = Request.Builder()
            .url("${BackendConfig.baseUrl}/mobile/bootstrap/$userId")
            .get()
            .build()
        val responseJson = executeJsonRequest(request)
        val currentUser = parseCurrentUser(responseJson.getJSONObject("current_user"))
        val messageItems = responseJson
            .getJSONObject("main_chat")
            .getJSONArray("items")
        val messages = buildList {
            for (index in 0 until messageItems.length()) {
                add(parseChatMessage(messageItems.getJSONObject(index), currentUser.id, currentUser.displayName))
            }
        }
        return BootstrapPayload(
            currentUser = currentUser,
            messages = messages,
        ).also { cachedBootstrap = it }
    }

    private fun parseCurrentUser(json: JSONObject): CurrentUser {
        val persona = json.optJSONObject("persona")
        val mobileProfile = persona?.optJSONObject("mobile_profile")
        return CurrentUser(
            id = json.getString("_id"),
            displayName = json.optString("display_name", "Unknown"),
            headline = mobileProfile?.optString("headline").orEmpty(),
            locationLabel = mobileProfile?.optString("location_label").orEmpty(),
            avatarUrl = json.optNullableString("avatar_url"),
            avatarFallbackPrompt = mobileProfile?.optString("avatar_fallback_prompt").orEmpty(),
            interestsSummary = mobileProfile?.optString("interests_summary").orEmpty(),
            journey = mobileProfile?.optJSONArray("journey").toJourneyEntries(),
            interests = mobileProfile?.optJSONArray("interests").toInterestEntries(),
        )
    }

    private fun parseChatMessage(
        json: JSONObject,
        currentUserId: String,
        currentUserName: String,
    ): ChatMessage {
        val senderType = json.optString("sender_type")
        val senderUserId = json.optNullableString("sender_user_id")
        val authorName = when {
            senderUserId == currentUserId -> currentUserName
            senderType == "assistant" || senderType == "system" -> "Spaces"
            else -> json.optString("author_name", "Spaces")
        }
        return ChatMessage(
            id = json.optString("_id", "message-${System.nanoTime()}"),
            authorName = authorName,
            body = json.optString("content_text"),
            isFromCurrentUser = senderUserId == currentUserId,
            timestamp = formatTimestamp(json.optString("created_at")),
            event = parseChatEvent(json.optJSONObject("content_structured")),
        )
    }

    private fun parseChatEvent(contentStructured: JSONObject?): ChatEvent? {
        if (contentStructured == null) {
            return null
        }
        if (contentStructured.optString("card_type") != "event_card") {
            return null
        }

        val eventJson = when {
            contentStructured.has("event") -> contentStructured.optJSONObject("event")
            contentStructured.has("events") -> contentStructured.optJSONArray("events")?.optJSONObject(0)
            else -> null
        } ?: return null

        val attendees = eventJson
            .optJSONArray("attendees")
            .toAttendees()
        val attendeeCount = eventJson.optInt("attendee_count", attendees.size)
        return ChatEvent(
            title = eventJson.optString("title"),
            locationName = eventJson.optString("location_name"),
            mapsUrl = eventJson.optString("maps_url"),
            description = eventJson.optString("description"),
            attendees = attendees,
            additionalAttendeeCount = (attendeeCount - attendees.size).coerceAtLeast(0),
        )
    }

    private fun executeJsonRequest(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Request failed with ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Empty response body")
            return JSONObject(body)
        }
    }

    private fun JSONArray?.toJourneyEntries(): List<JourneyEntry> {
        if (this == null) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    JourneyEntry(
                        title = item.optString("title"),
                        body = item.optString("body"),
                    )
                )
            }
        }
    }

    private fun JSONArray?.toInterestEntries(): List<InterestEntry> {
        if (this == null) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    InterestEntry(
                        tag = item.optString("tag"),
                        description = item.optString("description"),
                    )
                )
            }
        }
    }

    private fun JSONArray?.toAttendees(): List<EventAttendee> {
        if (this == null) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    EventAttendee(
                        displayName = item.optString("display_name"),
                        avatarUrl = item.optNullableString("avatar_url"),
                    )
                )
            }
        }
    }

    private fun formatTimestamp(isoTimestamp: String): String {
        if (isoTimestamp.isBlank()) {
            return ""
        }
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        return Instant.parse(isoTimestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key)
    }

    private data class BootstrapPayload(
        val currentUser: CurrentUser,
        val messages: List<ChatMessage>,
    )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
