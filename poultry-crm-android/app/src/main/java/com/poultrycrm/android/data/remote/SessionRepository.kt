package com.poultrycrm.android.data.remote

import android.util.Base64

class SessionRepository(private val backend: BackendClient) {
    suspend fun signIn(username: String, password: String): BackendUser {
        require(username.isNotBlank()) { "Enter your username." }
        require(password.isNotEmpty()) { "Enter your password." }

        val raw = "$username:$password"
        val authorization = "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val user = backend.auth.currentUser(authorization)
        backend.session.start(authorization)
        return user
    }

    suspend fun restoreSession(): BackendUser? {
        val authorization = backend.session.header() ?: return null
        return try {
            backend.auth.currentUser(authorization)
        } catch (failure: Exception) {
            backend.session.clear()
            null
        }
    }

    fun signOut() {
        backend.session.clear()
    }
}
