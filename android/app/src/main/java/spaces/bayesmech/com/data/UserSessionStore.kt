package spaces.bayesmech.com.data

import android.content.Context

class UserSessionStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val PREFS_NAME = "spaces_session"
        const val KEY_USER_ID = "current_user_id"
    }
}
