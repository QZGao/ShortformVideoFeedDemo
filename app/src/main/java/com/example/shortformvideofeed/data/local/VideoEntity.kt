package com.example.shortformvideofeed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val authorName: String,
    val description: String,
    val orderIndex: Int,
    val cachedAt: Long
)
