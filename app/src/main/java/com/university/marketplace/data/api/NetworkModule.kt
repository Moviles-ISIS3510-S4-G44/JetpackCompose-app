package com.university.marketplace.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.university.marketplace.BuildConfig
import com.university.marketplace.data.auth.AuthApiService
import com.university.marketplace.data.auth.AuthSessionStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    @Volatile
    private var initialized: Boolean = false

    private lateinit var appContext: Context
    private lateinit var authSessionStorageInternal: AuthSessionStorage
    private lateinit var authApiInternal: AuthApiService
    private lateinit var listingsApiInternal: ListingsApi

    val authSessionStorage: AuthSessionStorage
        get() = synchronized(this) { authSessionStorageInternal }

    val authApi: AuthApiService
        get() = synchronized(this) { authApiInternal }

    val listingsApi: ListingsApi
        get() = synchronized(this) { listingsApiInternal }

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            authSessionStorageInternal = AuthSessionStorage(appContext)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.BASIC
                }
            }

            val baseClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val gson: Gson = GsonBuilder().create()
            val baseUrl = normalizeBaseUrl(BuildConfig.API_BASE_URL)

            val unauthenticatedRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(baseClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            val bootstrapAuthApi = unauthenticatedRetrofit.create(AuthApiService::class.java)

            val authenticatedClient = baseClient.newBuilder()
                .addInterceptor(
                    AuthInterceptor(tokenProvider = { authSessionStorageInternal.getAccessToken() })
                )
                .authenticator(
                    TokenAuthenticator(
                        sessionStorage = authSessionStorageInternal,
                        refreshApi = bootstrapAuthApi
                    )
                )
                .build()

            val authenticatedRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(authenticatedClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val authenticatedMoshiRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(authenticatedClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            authApiInternal = authenticatedRetrofit.create(AuthApiService::class.java)
            listingsApiInternal = authenticatedMoshiRetrofit.create(ListingsApi::class.java)

            initialized = true
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    }
}
