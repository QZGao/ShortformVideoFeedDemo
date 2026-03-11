package com.example.shortformvideofeed.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoInteractionStoreTest {

    @Test
    fun inMemoryInteractionStore_togglesLikes() {
        val store = InMemoryVideoInteractionStore()

        assertFalse(store.isLiked("item-1"))
        assertEquals(emptySet(), store.getLikedItemIds())

        store.setLiked("item-1", true)
        assertTrue(store.isLiked("item-1"))
        assertEquals(setOf("item-1"), store.getLikedItemIds())

        store.setLiked("item-1", false)
        assertFalse(store.isLiked("item-1"))
        assertEquals(emptySet(), store.getLikedItemIds())
    }
}
