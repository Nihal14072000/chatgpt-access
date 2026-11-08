package com.poultrycrm.android.data.remote

import retrofit2.Response

suspend fun <T> Response<T>.requireBody(): T {
    if (!isSuccessful) {
        throw IllegalStateException("Backend request failed (HTTP ${code()}).")
    }
    return body() ?: throw IllegalStateException("Backend returned an empty response.")
}
