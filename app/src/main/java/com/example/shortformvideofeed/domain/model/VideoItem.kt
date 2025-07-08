package com.example.shortformvideofeed.domain.model

data class VideoItem(
    val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val authorName: String,
    val description: String,
    val orderIndex: Int,
    val cachedAt: Long? = null
)
