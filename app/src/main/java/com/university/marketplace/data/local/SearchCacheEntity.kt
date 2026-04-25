package com.university.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey val query: String,
    val resultIdsJson: String,
    val intentJson: String?,
    val timestamp: Long = System.currentTimeMillis()
)
