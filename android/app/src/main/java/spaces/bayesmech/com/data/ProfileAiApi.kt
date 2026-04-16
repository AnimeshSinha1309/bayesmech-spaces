package spaces.bayesmech.com.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ProfileAiApi(
    private val baseUrl: String = "http://127.0.0.1:8000",
) {
    suspend fun startConversation(
        user: CurrentUser,
        transcript: List<AiConversationMessage>,
        profileDictionary: ProfileDictionary,
    ): AiTurnResult = postTurn(
        path = "/users/${user.id}/profile-ai/start",
        body = buildRequestBody(
            user = user,
            transcript = transcript,
            profileDictionary = profileDictionary,
            userMessage = null,
        ),
    )

    suspend fun sendReply(
        user: CurrentUser,
        transcript: List<AiConversationMessage>,
        profileDictionary: ProfileDictionary,
        userMessage: String,
    ): AiTurnResult = postTurn(
        path = "/users/${user.id}/profile-ai/reply",
        body = buildRequestBody(
            user = user,
            transcript = transcript,
            profileDictionary = profileDictionary,
            userMessage = userMessage,
        ),
    )

    suspend fun endConversation(
        user: CurrentUser,
        transcript: List<AiConversationMessage>,
        profileDictionary: ProfileDictionary,
    ): AiEndResult = withContext(Dispatchers.IO) {
        val response = postJson(
            path = "/users/${user.id}/profile-ai/end",
            body = buildRequestBody(
                user = user,
                transcript = transcript,
                profileDictionary = profileDictionary,
                userMessage = null,
            ),
        )
        AiEndResult(
            finalProfileDict = response.getJSONObject("final_profile_dict").toProfileDictionary(),
            closingText = response.getString("closing_text"),
            isComplete = response.getBoolean("is_complete"),
            model = response.getString("model"),
        )
    }

    private suspend fun postTurn(
        path: String,
        body: JSONObject,
    ): AiTurnResult = withContext(Dispatchers.IO) {
        val response = postJson(path = path, body = body)
        AiTurnResult(
            assistantText = response.getString("assistant_text"),
            updatedProfileDict = response.getJSONObject("updated_profile_dict").toProfileDictionary(),
            isComplete = response.getBoolean("is_complete"),
            model = response.getString("model"),
        )
    }

    private fun buildRequestBody(
        user: CurrentUser,
        transcript: List<AiConversationMessage>,
        profileDictionary: ProfileDictionary,
        userMessage: String?,
    ): JSONObject = JSONObject().apply {
        put("display_name", user.displayName)
        put("current_profile_dict", profileDictionary.toJson())
        put(
            "transcript",
            JSONArray().apply {
                transcript.forEach { message ->
                    put(
                        JSONObject().apply {
                            put("role", message.role)
                            put("text", message.text)
                        },
                    )
                }
            },
        )
        if (userMessage != null) {
            put("user_message", userMessage)
        }
    }

    private fun postJson(
        path: String,
        body: JSONObject,
    ): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        return connection.use { safeConnection ->
            OutputStreamWriter(safeConnection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val stream = if (safeConnection.responseCode in 200..299) {
                safeConnection.inputStream
            } else {
                safeConnection.errorStream
            }

            val responseText = BufferedReader(stream.reader()).use { it.readText() }
            if (safeConnection.responseCode !in 200..299) {
                throw IllegalStateException(
                    JSONObject(responseText).optString("detail")
                        .ifBlank { "Profile AI request failed with ${safeConnection.responseCode}" },
                )
            }
            JSONObject(responseText)
        }
    }
}

private fun ProfileDictionary.toJson(): JSONObject = JSONObject().apply {
    put("home_base", homeBase)
    put("work_context", workContext)
    put("social_energy", socialEnergy)
    put("ideal_plans", idealPlans)
    put("social_goals", socialGoals)
    put("conversation_spark", conversationSpark)
    put(
        "interest_tags",
        JSONArray().apply {
            interestTags.forEach { put(it) }
        },
    )
    put("profile_notes", profileNotes)
}

private fun JSONObject.toProfileDictionary(): ProfileDictionary =
    ProfileDictionary(
        homeBase = optString("home_base"),
        workContext = optString("work_context"),
        socialEnergy = optString("social_energy"),
        idealPlans = optString("ideal_plans"),
        socialGoals = optString("social_goals"),
        conversationSpark = optString("conversation_spark"),
        interestTags = optJSONArray("interest_tags")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.optString(index))
                }
            }.filter { it.isNotBlank() }
        } ?: emptyList(),
        profileNotes = optString("profile_notes"),
    )

private inline fun <T : HttpURLConnection?, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        this?.disconnect()
    }
