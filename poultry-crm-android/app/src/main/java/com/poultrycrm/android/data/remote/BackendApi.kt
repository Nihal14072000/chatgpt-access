package com.poultrycrm.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class BackendUser(
    val name: String,
    val role: String
)

interface AuthApi {
    @GET("auth/me")
    suspend fun currentUser(@Header("Authorization") authorization: String): BackendUser
}

/** Generic CRUD contract matching the web app's ApiDataAdapter (/v1/{resource}). */
interface BusinessApi {
    @GET("v1/{resource}")
    suspend fun get(@Path("resource") resource: String): Response<Any>

    @POST("v1/{resource}")
    suspend fun create(
        @Path("resource") resource: String,
        @Body record: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @PUT("v1/{resource}/{id}")
    suspend fun update(
        @Path("resource") resource: String,
        @Path("id") id: String,
        @Body record: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @DELETE("v1/{resource}/{id}")
    suspend fun delete(@Path("resource") resource: String, @Path("id") id: String): Response<Unit>
}

/** Dedicated endpoints already used by the web app's FinanceApiService. */
interface FinanceApi {
    @GET("payments/retrieve")
    suspend fun payments(): Response<Any>

    @GET("payments/{id}/retrieve")
    suspend fun payment(@Path("id") id: String): Response<Map<String, Any?>>

    @POST("payments")
    suspend fun createPayment(@Body payment: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any?>>

    @PUT("payments/{id}")
    suspend fun updatePayment(
        @Path("id") id: String,
        @Body payment: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @GET("finance/settlements")
    suspend fun settlements(): Response<Any>

    @GET("finance/receivables")
    suspend fun receivables(@Query("tripId") tripId: String? = null, @Query("buyingPartyId") buyingPartyId: String? = null): Response<Any>

    @GET("finance/payables")
    suspend fun payables(@Query("tripId") tripId: String? = null, @Query("sellingPartyId") sellingPartyId: String? = null): Response<Any>

    @POST("finance/settlements")
    suspend fun createSettlement(@Body settlement: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any?>>

    @POST("finance/allocations")
    suspend fun allocate(@Body allocation: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any?>>
}

/** Core trip endpoints used by the web app's TripApiService. */
interface TripsApi {
    @GET("trips")
    suspend fun trips(): Response<Any>

    @GET("vehicles")
    suspend fun vehicles(): Response<Any>

    @GET("drivers")
    suspend fun drivers(): Response<Any>

    @GET("supervisors")
    suspend fun supervisors(): Response<Any>

    @GET("buying-parties")
    suspend fun buyingParties(): Response<Any>

    @GET("selling-parties")
    suspend fun sellingParties(): Response<Any>

    @GET("order-types")
    suspend fun orderTypes(): Response<Any>

    @POST("trips/insert")
    suspend fun createTrip(@Body trip: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any?>>
}
