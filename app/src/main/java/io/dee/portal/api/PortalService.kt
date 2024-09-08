package io.dee.portal.api

import io.dee.portal.BuildConfig
import io.dee.portal.view.map_screen.data.dto.ReverseGeocodingResponse
import io.dee.portal.view.map_screen.data.dto.RouteResponse
import io.dee.portal.view.search_screen.data.dto.SearchDto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PortalService {

    @GET("v5/reverse")
    suspend fun reverseGeocoding(
        @Query("lat") lat: Double, @Query("lng") lng: Double
    ): ReverseGeocodingResponse

    @GET("v1/search")
    suspend fun search(
        @Query("term") term: String, @Query("lat") lat: Double, @Query("lng") lng: Double
    ): Response<SearchDto>

    @GET("v4/direction/no-traffic")
    suspend fun fetchRouting(
        @Query("origin") origin: String, @Query("destination") destination: String
    ): Response<RouteResponse>


    companion object {
        fun create(): PortalService {
            val logger =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder().addInterceptor(logger).addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Api-Key", BuildConfig.PORTAL_API_KEY).build()
                try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    okhttp3.Response.Builder().request(request)
                        .protocol(okhttp3.Protocol.HTTP_1_1).code(500).message(e.message ?: "")
                        .build()
                }


            }.build()
            val retrofit = Retrofit.Builder().baseUrl("https://api.neshan.org/").client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()

            return retrofit.create(PortalService::class.java)
        }
    }
}