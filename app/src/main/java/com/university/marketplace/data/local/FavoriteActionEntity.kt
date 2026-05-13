package com.university.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_actions")
data class FavoriteActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: String,
    val action: String, // "ADD" or "REMOVE"
    val timestamp: Long = System.currentTimeMillis()
)
