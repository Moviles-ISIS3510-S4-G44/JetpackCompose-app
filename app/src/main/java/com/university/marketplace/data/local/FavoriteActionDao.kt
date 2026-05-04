package com.university.marketplace.data.local

import androidx.room.*

@Dao
interface FavoriteActionDao {
    @Insert
    suspend fun insertAction(action: FavoriteActionEntity)

    @Query("SELECT * FROM favorite_actions ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<FavoriteActionEntity>

    @Delete
    suspend fun deleteAction(action: FavoriteActionEntity)

    @Query("DELETE FROM favorite_actions")
    suspend fun clearAll()
}
