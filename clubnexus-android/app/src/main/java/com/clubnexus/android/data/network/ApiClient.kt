package com.clubnexus.android.data.network

import com.clubnexus.android.BuildConfig
import com.clubnexus.android.data.auth.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    fun create(sessionStore: SessionStore): ClubNexusApi {
        val configuredUrl = BuildConfig.API_BASE_URL.trim()
        val baseUrl = configuredUrl.ifBlank { "https://api.invalid/api/" }
        val developmentHttp = BuildConfig.FLAVOR == "development" && BuildConfig.DEBUG && baseUrl.startsWith("http://")
        require(baseUrl.startsWith("https://") || developmentHttp) {
            "Configure a valid API URL. Production builds require CLUBNEXUS_PROD_API_URL with HTTPS."
        }
        require(baseUrl.endsWith('/')) { "API base URL must end with / (for example, https://api.example.com/api/)" }

        val authentication = Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                sessionStore.token()?.let { token -> header("Authorization", "Bearer $token") }
            }.build()
            chain.proceed(request).also { response ->
                if (response.code == 401 && sessionStore.token() != null) sessionStore.clear()
            }
        }
        val client = OkHttpClient.Builder().addInterceptor(authentication).build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClubNexusApi::class.java)
    }
}
