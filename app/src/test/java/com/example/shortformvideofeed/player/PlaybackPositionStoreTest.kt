package com.example.shortformvideofeed.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackPositionStoreTest {

    @Test
    fun inMemoryStore_savesAndLoadsPositionByItemId() {
        val store = InMemoryPlaybackPositionStore()

        assertNull(store.load("video-a"))

        store.save("video-a", 12_500L)
        assertEquals(12_500L, store.load("video-a"))
    }

    @Test
    fun inMemoryStore_overwritesPreviousPosition() {
        val store = InMemoryPlaybackPositionStore()

        store.save("video-a", 1_000L)
        store.save("video-a", 4_200L)

        assertEquals(4_200L, store.load("video-a"))
    }
}
