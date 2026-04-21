package com.university.marketplace.data.auth

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.university.marketplace.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signup")
    suspend fun signup(@Body payload: SignupRequestDto): Response<SignupResponseDto>

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<TokenResponseDto>

    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<CurrentUserResponseDto>
}

data class SignupRequestDto(
    val name: String,
    val email: String,
    val password: String
)

data class SignupResponseDto(
    @SerializedName("user_id")
    val userId: String
)

data class TokenResponseDto(
    @SerializedName("session_id")
    val sessionId: String?,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("refresh_expires_in")
    val refreshExpiresIn: Long?
)

data class CurrentUserResponseDto(
    val id: String,
    val name: String,
    val email: String,
    val rating: Int
)

data class ApiErrorResponseDto(
    val detail: String?
)

object AuthApiFactory {
    fun create(): AuthApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().create()
                )
            )
            .build()
            .create(AuthApiService::class.java)
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    }
}
