package com.poultrycrm.android.data.remote

import com.poultrycrm.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackendClient(val session: ApiSession = ApiSession()) {
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            session.header()?.let { header -> header("Authorization", header) }
        }.build()
        val response = chain.proceed(request)
        if (response.code == 401) session.clear()
        response
    }

    private val http = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val auth: AuthApi = retrofit.create(AuthApi::class.java)
    val business: BusinessApi = retrofit.create(BusinessApi::class.java)
    val finance: FinanceApi = retrofit.create(FinanceApi::class.java)
    val trips: TripsApi = retrofit.create(TripsApi::class.java)

    init {
        require(BuildConfig.API_BASE_URL.endsWith("/")) {
            "POULTRY_API_BASE_URL must end with a slash"
        }
    }
}
