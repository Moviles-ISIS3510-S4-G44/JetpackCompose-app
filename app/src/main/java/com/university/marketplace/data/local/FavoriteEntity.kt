package com.university.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val listingId: String,
    val addedAt: Long = System.currentTimeMillis()
)
