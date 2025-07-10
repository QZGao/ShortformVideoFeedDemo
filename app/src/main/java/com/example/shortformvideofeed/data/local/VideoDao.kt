package com.example.shortformvideofeed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource

@Dao
interface VideoDao {
    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int

    @Query("SELECT * FROM videos ORDER BY orderIndex ASC")
    suspend fun getAllOrdered(): List<VideoEntity>

    @Query("SELECT * FROM videos ORDER BY orderIndex ASC")
    fun pagingSource(): PagingSource<Int, VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoEntity>)

    @Query("DELETE FROM videos")
    suspend fun clear()
}
