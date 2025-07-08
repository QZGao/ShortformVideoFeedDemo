package com.example.shortformvideofeed.data.remote

data class FeedDto(
    val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val authorName: String = "Unknown",
    val description: String = "",
    val orderIndex: Int = -1
)
