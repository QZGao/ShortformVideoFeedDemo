package com.example.shortformvideofeed.player

import android.content.Context

interface PlaybackPositionStore {
    fun save(itemId: String, positionMs: Long)
    fun load(itemId: String): Long?
}

class InMemoryPlaybackPositionStore : PlaybackPositionStore {
    private val values = LinkedHashMap<String, Long>()

    override fun save(itemId: String, positionMs: Long) {
        values[itemId] = positionMs
    }

    override fun load(itemId: String): Long? {
        return values[itemId]
    }
}

class SharedPreferencesPlaybackPositionStore(context: Context) : PlaybackPositionStore {
    private val prefs = context.applicationContext.getSharedPreferences(
        "shortform_feed_playback_positions",
        Context.MODE_PRIVATE
    )

    override fun save(itemId: String, positionMs: Long) {
        prefs.edit().putLong("item:$itemId", positionMs).apply()
    }

    override fun load(itemId: String): Long? {
        val key = "item:$itemId"
        return if (prefs.contains(key)) {
            prefs.getLong(key, 0L)
        } else {
            null
        }
    }
}
