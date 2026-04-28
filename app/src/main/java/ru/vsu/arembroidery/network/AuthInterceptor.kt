package ru.vsu.arembroidery.network

import okhttp3.Interceptor
import okhttp3.Response
import ru.vsu.arembroidery.utils.AuthManager

class AuthInterceptor(
    private val authManager: AuthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authManager.getAccessToken()

        val requestBuilder = chain.request().newBuilder()

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}