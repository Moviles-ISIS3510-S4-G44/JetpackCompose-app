package com.university.marketplace.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface ProfileVisitsApi {
    @POST("users/{visitedUserId}/profile-visits")
    suspend fun registerProfileVisit(
        @Path("visitedUserId") visitedUserId: String
    ): Response<ProfileVisitResponseDto>
}

/** Backend returns JSON; Gson cannot deserialize a body into Unit reliably. */
data class ProfileVisitResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("visited_at") val visitedAt: String
)
