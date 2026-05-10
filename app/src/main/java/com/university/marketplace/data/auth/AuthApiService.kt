package com.university.marketplace.data.auth

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
    suspend fun getCurrentUser(): Response<CurrentUserResponseDto>

    @GET("users/{user_id}")
    suspend fun getUserProfile(@Path("user_id") userId: String): Response<CurrentUserResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body payload: LogoutRequestDto): Response<Unit>

    @POST("auth/refresh")
    suspend fun refresh(@Body payload: RefreshTokenRequestDto): Response<TokenResponseDto>
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

data class LogoutRequestDto(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenRequestDto(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class ApiErrorResponseDto(
    val detail: String?
)
