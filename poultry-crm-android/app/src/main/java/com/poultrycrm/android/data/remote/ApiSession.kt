package com.poultrycrm.android.data.remote

import java.util.concurrent.atomic.AtomicReference

/** Holds the Basic authorization header for the current app process only. */
class ApiSession {
    private val authorization = AtomicReference<String?>(null)

    fun header(): String? = authorization.get()
    fun start(header: String) { authorization.set(header) }
    fun clear() { authorization.set(null) }
    fun isAuthenticated(): Boolean = authorization.get() != null
}
