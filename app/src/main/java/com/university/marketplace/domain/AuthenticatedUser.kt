package com.university.marketplace.domain

data class AuthenticatedUser(
    val id: String,
    val name: String,
    val email: String,
    val rating: Int
)
