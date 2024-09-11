package io.dee.portal.api

import android.content.Context
import io.dee.portal.utils.AuthInterceptor
import io.dee.portal.utils.ExceptionHandlingInterceptor
import io.dee.portal.utils.NetworkAvailabilityInterceptor
import io.dee.portal.utils.appBaseUrl
import io.dee.portal.view.map_screen.data.dto.ReverseGeocodingResponse
import io.dee.portal.view.map_screen.data.dto.RouteResponse
import io.dee.portal.view.search_screen.data.dto.SearchDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

interface PortalService {

    @GET("v5/reverse")
    suspend fun reverseGeocoding(
        @Query("lat") lat: Double, @Query("lng") lng: Double
    ): ReverseGeocodingResponse

    @GET("v1/search")
    suspend fun search(
        @Query("term") term: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<SearchDto>

    @GET("v4/direction/no-traffic")
    suspend fun fetchRouting(
        @Query("origin") origin: String, @Query("destination") destination: String
    ): Response<RouteResponse>


    companion object {
        fun create(context: Context): PortalService {
            val logger =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }


            val client = OkHttpClient.Builder().addInterceptor(logger)
                .addInterceptor(NetworkAvailabilityInterceptor(context))
                .addInterceptor(ExceptionHandlingInterceptor())
                .addInterceptor(AuthInterceptor())
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    try {
                        val response = chain.proceed(requestBuilder.build())
                        response
                    } catch (e: Exception) {
                        val message = when (e) {
                            is SocketTimeoutException -> "SocketTimeoutException: ${e.message}"
                            is InterruptedIOException -> "InterruptedIOException: ${e.message}"
                            else -> "Unknown Exception: ${e.message}"
                        }
                        okhttp3.Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(500)
                            .message(message)
                            .body(ResponseBody.create("text/plain".toMediaTypeOrNull(), ""))
                            .build()
                    }

                }
                .build()
            val retrofit = Retrofit.Builder().baseUrl(appBaseUrl).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()

            return retrofit.create(PortalService::class.java)
        }
    }
}