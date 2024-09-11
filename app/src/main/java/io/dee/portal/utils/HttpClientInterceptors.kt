package io.dee.portal.utils

import android.content.Context
import android.net.ConnectivityManager
import io.dee.portal.BuildConfig
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException


class ExceptionHandlingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: java.net.SocketException) {
            okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Socket Exception")
                .body((e.message ?: "").toResponseBody(null))
                .build()
        } catch (e: java.net.SocketTimeoutException) {
            okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("SocketTimeoutException")
                .body((e.message ?: "").toResponseBody(null))
                .build()
        } catch (e: java.io.InterruptedIOException) {
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(500)
                .message("InterruptedIOException").body("".toResponseBody(null)).build()
        } catch (e: IOException) {
            okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("IOException")
                .body((e.message ?: "").toResponseBody(null))
                .build()
        } catch (e: Exception) {
            okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Exception")
                .body((e.message ?: "").toResponseBody(null))
                .build()
        }
    }
}

class AuthInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .addHeader("Api-Key", BuildConfig.PORTAL_API_KEY).build()
        return chain.proceed(requestBuilder)
    }
}

class NetworkAvailabilityInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isNetworkAvailable(context = context)) {
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Network Service Unavailable")
                .body("".toResponseBody(null))
                .build()
        }

        // Proceed with the request
        return chain.proceed(chain.request())
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}

