package com.clubnexus.android.data.auth

import android.content.Context
import com.clubnexus.android.data.model.ClubMember
import com.clubnexus.android.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(val user: User)

/** Keystore encrypted token plus the non-secret profile used to restore the app shell. */
class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStorage = SecureTokenStorage(appContext)
    private val profilePreferences = appContext.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    private val mutableSession = MutableStateFlow(loadCachedSession())
    val session: StateFlow<Session?> = mutableSession.asStateFlow()

    fun token(): String? = tokenStorage.readToken()

    @Synchronized
    fun save(token: String, user: User) {
        tokenStorage.saveToken(token)
        profilePreferences.edit()
            .putString(USER_ID, user.id)
            .putString(USER_NAME, user.name)
            .putString(USER_EMAIL, user.email)
            .putString(USER_ROLE, user.role)
            .putString(MEMBER_ID, user.member?.id)
            .apply()
        mutableSession.value = Session(user)
    }

    @Synchronized
    fun updateUser(user: User) {
        val activeToken = token() ?: return clear()
        save(activeToken, user)
    }

    @Synchronized
    fun clear() {
        tokenStorage.clearToken()
        profilePreferences.edit().clear().apply()
        mutableSession.value = null
    }

    private fun loadCachedSession(): Session? {
        if (tokenStorage.readToken().isNullOrBlank()) return null
        val id = profilePreferences.getString(USER_ID, null) ?: return null
        val name = profilePreferences.getString(USER_NAME, null) ?: return null
        val email = profilePreferences.getString(USER_EMAIL, null) ?: return null
        val role = profilePreferences.getString(USER_ROLE, "MEMBER") ?: "MEMBER"
        val memberId = profilePreferences.getString(MEMBER_ID, null)
        return Session(User(id, name, email, role, memberId?.let(::ClubMember)))
    }

    private companion object {
        const val PROFILE_PREFS = "clubnexus_session_profile"
        const val USER_ID = "id"
        const val USER_NAME = "name"
        const val USER_EMAIL = "email"
        const val USER_ROLE = "role"
        const val MEMBER_ID = "member_id"
    }
}
