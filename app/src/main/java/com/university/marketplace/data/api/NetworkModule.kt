package com.university.marketplace.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.university.marketplace.BuildConfig
import com.university.marketplace.data.auth.AuthApiService
import com.university.marketplace.data.auth.AuthSessionStorage
import com.university.marketplace.data.chat.ChatWebSocketClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    private const val FALLBACK_BASE_URL = "http://127.0.0.1:8000/"

    @Volatile
    private var initialized: Boolean = false

    private lateinit var appContext: Context
    private lateinit var authSessionStorageInternal: AuthSessionStorage
    private lateinit var authApiInternal: AuthApiService
    private lateinit var listingsApiInternal: ListingsApi
    private lateinit var interactionsApiInternal: InteractionsApi
    private lateinit var favoritesApiInternal: FavoritesApi
    private lateinit var categoriesApiInternal: CategoriesApi
    private lateinit var purchasesApiInternal: PurchasesApi
    private lateinit var chatApiInternal: ChatApi
    private lateinit var groqApiInternal: GroqApi
    private lateinit var authenticatedClientInternal: OkHttpClient
    private lateinit var wsBaseUrlInternal: String

    val authSessionStorage: AuthSessionStorage
        get() = synchronized(this) { authSessionStorageInternal }

    val authApi: AuthApiService
        get() = synchronized(this) { authApiInternal }

    val listingsApi: ListingsApi
        get() = synchronized(this) { listingsApiInternal }

    val interactionsApi: InteractionsApi
        get() = synchronized(this) { interactionsApiInternal }

    val favoritesApi: FavoritesApi
        get() = synchronized(this) { favoritesApiInternal }

    val categoriesApi: CategoriesApi
        get() = synchronized(this) { categoriesApiInternal }

    val purchasesApi: PurchasesApi
        get() = synchronized(this) { purchasesApiInternal }

    val chatApi: ChatApi
        get() = synchronized(this) { chatApiInternal }

    val groqApi: GroqApi
        get() = synchronized(this) { groqApiInternal }

    fun createChatWebSocketClient(): ChatWebSocketClient =
        synchronized(this) {
            ChatWebSocketClient(
                okHttpClient = authenticatedClientInternal,
                wsBaseUrl = wsBaseUrlInternal
            )
        }

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
                    HttpLoggingInterceptor.Level.NONE
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
            authenticatedClientInternal = authenticatedClient
            wsBaseUrlInternal = baseUrl.replace("https://", "wss://").replace("http://", "ws://")

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

            val groqRetrofit = Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/")
                .client(baseClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            authApiInternal = authenticatedRetrofit.create(AuthApiService::class.java)
            listingsApiInternal = authenticatedMoshiRetrofit.create(ListingsApi::class.java)
            interactionsApiInternal = authenticatedRetrofit.create(InteractionsApi::class.java)
            favoritesApiInternal = authenticatedRetrofit.create(FavoritesApi::class.java)
            categoriesApiInternal = authenticatedMoshiRetrofit.create(CategoriesApi::class.java)
            purchasesApiInternal = authenticatedMoshiRetrofit.create(PurchasesApi::class.java)
            chatApiInternal = authenticatedMoshiRetrofit.create(ChatApi::class.java)
            groqApiInternal = groqRetrofit.create(GroqApi::class.java)

            initialized = true
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        val normalized = if (trimmed.endsWith('/')) trimmed else "$trimmed/"
        return if (normalized.toHttpUrlOrNull() != null) {
            normalized
        } else {
            FALLBACK_BASE_URL
        }
    }
}
