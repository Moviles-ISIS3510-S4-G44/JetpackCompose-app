package com.university.marketplace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY updatedAt DESC")
    fun getActiveListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings ORDER BY updatedAt DESC")
    suspend fun getActiveListingsList(): List<ListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<ListingEntity>)

    @Query("DELETE FROM listings WHERE updatedAt < :timestamp")
    suspend fun deleteStaleListings(timestamp: Long)

    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getListingById(id: String): ListingEntity?

    // Search Cache
    @Query("SELECT * FROM search_cache WHERE `query` = :query")
    suspend fun getSearchCache(query: String): SearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchCache(cache: SearchCacheEntity)

    @Query("DELETE FROM search_cache WHERE timestamp < :timestamp")
    suspend fun deleteStaleSearchCache(timestamp: Long)
}
