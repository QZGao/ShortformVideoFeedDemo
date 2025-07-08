package com.example.shortformvideofeed.data.mapper

import com.example.shortformvideofeed.data.local.VideoEntity
import com.example.shortformvideofeed.data.remote.FeedDto
import com.example.shortformvideofeed.domain.model.VideoItem

fun FeedDto.toDomain(indexOverride: Int = -1): VideoItem {
    return VideoItem(
        id = id,
        title = title,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        authorName = authorName,
        description = description,
        orderIndex = if (orderIndex < 0) indexOverride else orderIndex
    )
}

fun VideoItem.toEntity(): VideoEntity {
    return VideoEntity(
        id = id,
        title = title,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        authorName = authorName,
        description = description,
        orderIndex = orderIndex,
        cachedAt = cachedAt ?: System.currentTimeMillis()
    )
}

fun VideoEntity.toDomain(): VideoItem {
    return VideoItem(
        id = id,
        title = title,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        authorName = authorName,
        description = description,
        orderIndex = orderIndex,
        cachedAt = cachedAt
    )
}
