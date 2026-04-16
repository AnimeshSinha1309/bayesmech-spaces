package spaces.bayesmech.com.data.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import spaces.bayesmech.com.data.ChatEvent
import spaces.bayesmech.com.data.ChatMessage
import spaces.bayesmech.com.data.ChatRepository
import spaces.bayesmech.com.data.CurrentUser
import spaces.bayesmech.com.data.CurrentUserRepository
import spaces.bayesmech.com.data.EventDraft
import spaces.bayesmech.com.data.EventAttendee
import spaces.bayesmech.com.data.InterestEntry
import spaces.bayesmech.com.data.JourneyEntry
import spaces.bayesmech.com.data.ProfileDictionary
import java.io.File
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

    override suspend fun getSignupEvents(userId: String): List<ChatEvent> {
        return withContext(Dispatchers.IO) {
            val userEventsRequest = Request.Builder()
                .url("${BackendConfig.baseUrl}/users/$userId/events")
                .get()
                .build()
            val eventRefsJson = executeJsonRequest(userEventsRequest)
            val hostedEventIds = eventRefsJson.optJSONArray("hosted_event_ids")
                ?.toStringList()
                ?.distinct()
                ?: emptyList()
            val joinedEventIds = eventRefsJson.optJSONArray("joined_event_ids")
                ?.toStringList()
                ?.distinct()
                ?: emptyList()
            val signupEventIds = (hostedEventIds + joinedEventIds).distinct()

            signupEventIds.mapNotNull { eventId ->
                val eventRequest = Request.Builder()
                    .url("${BackendConfig.baseUrl}/events/$eventId")
                    .get()
                    .build()
                val attendeesRequest = Request.Builder()
                    .url("${BackendConfig.baseUrl}/events/$eventId/attendees")
                    .get()
                    .build()

                val eventJson = executeJsonRequest(eventRequest)
                val attendeeMemberships = executeJsonArrayRequest(attendeesRequest)
                buildChatEvent(
                    eventJson = eventJson,
                    attendeeMemberships = attendeeMemberships,
                    isHostedByCurrentUser = hostedEventIds.contains(eventId),
                )
            }
        }
    }

    override suspend fun sendMessage(
        userId: String,
        authorName: String,
        body: String,
    ): List<ChatMessage> {
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
            val messagesJson = responseJson.optJSONArray("messages")
            val messages = if (messagesJson != null) {
                buildList {
                    for (index in 0 until messagesJson.length()) {
                        add(parseChatMessage(messagesJson.getJSONObject(index), userId, currentUser.displayName))
                    }
                }
            } else {
                listOf(parseChatMessage(responseJson, userId, currentUser.displayName))
            }
            cachedBootstrap = cachedBootstrap?.copy(messages = cachedBootstrap!!.messages + messages)
            messages
        }
    }

    override suspend fun createEvent(
        userId: String,
        draft: EventDraft,
    ): ChatMessage {
        return withContext(Dispatchers.IO) {
            val currentUser = cachedBootstrap?.currentUser ?: loadBootstrap(userId).currentUser
            val requestJson = JSONObject()
                .put("creator_user_id", userId)
                .put("title", draft.title)
                .put("description", draft.description)
                .put("category_tags", JSONArray(draft.categoryTags))
                .put("visibility_type", draft.visibilityType)
                .put(
                    "timing",
                    JSONObject()
                        .put("start_time", draft.startsAtIso)
                        .put("end_time", draft.endsAtIso),
                )
                .put(
                    "location",
                    JSONObject()
                        .put("location_name", draft.locationName)
                        .put("maps_url", draft.mapsUrl),
                )
                .put(
                    "capacity",
                    JSONObject().apply {
                        draft.capacityMax?.let { put("max_attendees", it) }
                    },
                )

            val createEventRequest = Request.Builder()
                .url("${BackendConfig.baseUrl}/events")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val eventJson = executeJsonRequest(createEventRequest)
            val eventCardMessage = buildEventCardMessage(userId, currentUser.displayName, eventJson)

            val chatMessageRequest = Request.Builder()
                .url("${BackendConfig.baseUrl}/chat/$userId/messages")
                .post(eventCardMessage.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val responseJson = executeJsonRequest(chatMessageRequest)
            val message = parseChatMessage(responseJson, userId, currentUser.displayName)
            cachedBootstrap = cachedBootstrap?.copy(messages = cachedBootstrap!!.messages + message)
            message
        }
    }

    override suspend fun rsvpToEvent(
        userId: String,
        eventId: String,
    ) {
        return withContext(Dispatchers.IO) {
            val requestJson = JSONObject()
                .put("user_id", userId)
                .put("role", "attendee")
                .put("rsvp_status", "joined")
                .put("discovery_source", "direct_chat")

            val request = Request.Builder()
                .url("${BackendConfig.baseUrl}/events/$eventId/join")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            executeJsonRequest(request)
        }
    }

    override suspend fun transcribeAudio(
        userId: String,
        filePath: String,
    ): String {
        return withContext(Dispatchers.IO) {
            val audioFile = File(filePath)
            if (!audioFile.exists()) {
                throw IOException("Recorded audio file is missing")
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name = "file",
                    filename = audioFile.name,
                    body = audioFile.asRequestBody("audio/mp4".toMediaType()),
                )
                .build()

            val request = Request.Builder()
                .url("${BackendConfig.baseUrl}/chat/$userId/transcribe")
                .post(requestBody)
                .build()

            executeJsonRequest(request).getString("text").trim()
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
            profileDictionary = parseProfileDictionary(persona),
            journey = mobileProfile?.optJSONArray("journey").toJourneyEntries(),
            interests = mobileProfile?.optJSONArray("interests").toInterestEntries(),
        )
    }

    private fun parseProfileDictionary(persona: JSONObject?): ProfileDictionary {
        val profileJson = persona?.optJSONObject("profile_dict") ?: persona
        return ProfileDictionary(
            homeBase = profileJson?.optString("home_base").orEmpty(),
            workContext = profileJson?.optString("work_context").orEmpty(),
            socialEnergy = profileJson?.optString("social_energy").orEmpty(),
            idealPlans = profileJson?.optString("ideal_plans").orEmpty(),
            socialGoals = profileJson?.optString("social_goals").orEmpty(),
            conversationSpark = profileJson?.optString("conversation_spark").orEmpty(),
            interestTags = profileJson?.optJSONArray("interest_tags")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        add(array.optString(index))
                    }
                }.filter { it.isNotBlank() }
            } ?: emptyList(),
            profileNotes = profileJson?.optString("profile_notes").orEmpty(),
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
            id = eventJson.optString("event_id"),
            title = eventJson.optString("title"),
            locationName = eventJson.optString("location_name"),
            mapsUrl = eventJson.optString("maps_url"),
            description = eventJson.optString("description"),
            attendees = attendees,
            additionalAttendeeCount = (attendeeCount - attendees.size).coerceAtLeast(0),
            startLabel = eventJson.optString("start_label"),
            categoryTags = eventJson.optJSONArray("category_tags")?.toStringList() ?: emptyList(),
        )
    }

    private fun buildChatEvent(
        eventJson: JSONObject,
        attendeeMemberships: JSONArray,
        isHostedByCurrentUser: Boolean = false,
    ): ChatEvent {
        val startTime = eventJson
            .optJSONObject("timing")
            ?.optString("start_time")
            .orEmpty()
        val attendeeCount = eventJson
            .optJSONObject("attendance")
            ?.optInt("attendee_count")
            ?: attendeeMemberships.length()
        return ChatEvent(
            id = eventJson.optString("_id"),
            title = eventJson.optString("title"),
            locationName = eventJson.optJSONObject("location")?.optString("location_name").orEmpty(),
            mapsUrl = eventJson.optJSONObject("location")?.optString("maps_url").orEmpty(),
            description = eventJson.optString("description"),
            attendees = attendeeMemberships.toMembershipAttendees(),
            additionalAttendeeCount = (attendeeCount - attendeeMemberships.length()).coerceAtLeast(0),
            startLabel = formatEventStart(startTime),
            categoryTags = eventJson.optJSONArray("category_tags")?.toStringList() ?: emptyList(),
            isHostedByCurrentUser = isHostedByCurrentUser,
        )
    }

    private fun buildEventCardMessage(
        userId: String,
        currentUserName: String,
        eventJson: JSONObject,
    ): JSONObject {
        val eventCardJson = JSONObject()
            .put("card_type", "event_card")
            .put(
                "event",
                JSONObject()
                    .put("event_id", eventJson.optString("_id"))
                    .put("title", eventJson.optString("title"))
                    .put("location_name", eventJson.optJSONObject("location")?.optString("location_name").orEmpty())
                    .put("maps_url", eventJson.optJSONObject("location")?.optString("maps_url").orEmpty())
                    .put("description", eventJson.optString("description"))
                    .put(
                        "attendees",
                        JSONArray().put(
                            JSONObject()
                                .put("display_name", currentUserName)
                                .put("avatar_url", JSONObject.NULL),
                        ),
                    )
                    .put("attendee_count", eventJson.optJSONObject("attendance")?.optInt("attendee_count") ?: 1)
                    .put("start_label", formatEventStart(eventJson.optJSONObject("timing")?.optString("start_time").orEmpty()))
                    .put("category_tags", eventJson.optJSONArray("category_tags") ?: JSONArray()),
            )

        return JSONObject()
            .put("sender_type", "assistant")
            .put("sender_user_id", userId)
            .put("message_type", "event_card")
            .put("content_text", "Event created. Here is your card.")
            .put("content_structured", eventCardJson)
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

    private fun executeJsonArrayRequest(request: Request): JSONArray {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Request failed with ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Empty response body")
            return JSONArray(body)
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

    private fun JSONArray?.toMembershipAttendees(): List<EventAttendee> {
        if (this == null) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until minOf(length(), 3)) {
                val item = optJSONObject(index) ?: continue
                add(
                    EventAttendee(
                        displayName = item.optString("display_name", item.optString("user_id")),
                        avatarUrl = item.optNullableString("avatar_url"),
                    )
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index)
                if (value.isNotBlank()) {
                    add(value)
                }
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

    private fun formatEventStart(isoTimestamp: String): String {
        if (isoTimestamp.isBlank()) {
            return ""
        }
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", Locale.US)
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
