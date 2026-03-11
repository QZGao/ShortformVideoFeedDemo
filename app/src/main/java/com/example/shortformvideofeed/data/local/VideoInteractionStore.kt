package com.example.shortformvideofeed.data.local

import android.content.Context

interface VideoInteractionStore {
    fun isLiked(itemId: String): Boolean
    fun setLiked(itemId: String, liked: Boolean)
    fun getLikedItemIds(): Set<String>
}

class InMemoryVideoInteractionStore : VideoInteractionStore {

    private val likedItems = LinkedHashSet<String>()

    override fun isLiked(itemId: String): Boolean = likedItems.contains(itemId)

    override fun setLiked(itemId: String, liked: Boolean) {
        if (liked) {
            likedItems.add(itemId)
        } else {
            likedItems.remove(itemId)
        }
    }

    override fun getLikedItemIds(): Set<String> = likedItems.toSet()
}

class SharedPreferencesVideoInteractionStore(context: Context) : VideoInteractionStore {

    private val prefs = context.applicationContext.getSharedPreferences(
        "shortform_video_interactions",
        Context.MODE_PRIVATE
    )

    override fun isLiked(itemId: String): Boolean {
        return prefs.getBoolean(itemKey(itemId), false)
    }

    override fun setLiked(itemId: String, liked: Boolean) {
        prefs.edit().run {
            if (liked) putBoolean(itemKey(itemId), true) else remove(itemKey(itemId))
            apply()
        }
    }

    override fun getLikedItemIds(): Set<String> {
        return prefs.all.keys
            .filter { it.startsWith("liked:") }
            .map { it.substringAfter("liked:") }
            .toSet()
    }

    private fun itemKey(itemId: String): String = "liked:$itemId"
}
