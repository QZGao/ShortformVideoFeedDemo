package com.example.shortformvideofeed.ui.feed

import kotlin.test.assertEquals
import org.junit.Test

class FeedVisibilityUtilsTest {

    @Test
    fun pickActiveItemIndex_returnsItemAboveThreshold() {
        val visibleItems = listOf(
            VisibleItemVisibility(index = 0, offset = -600, size = 1000),
            VisibleItemVisibility(index = 1, offset = 400, size = 1000)
        )
        val viewportStart = 0
        val viewportEnd = 1000

        val activeIndex = pickActiveItemIndex(
            visibleItems = visibleItems,
            viewportStart = viewportStart,
            viewportEnd = viewportEnd,
            visibilityThreshold = 0.6f
        )

        assertEquals(1, activeIndex)
    }

    @Test
    fun pickActiveItemIndex_returnsNoActiveItemWhenThresholdNotMet() {
        val visibleItems = listOf(
            VisibleItemVisibility(index = 0, offset = -700, size = 1000),
            VisibleItemVisibility(index = 1, offset = 700, size = 1000)
        )
        val activeIndex = pickActiveItemIndex(
            visibleItems = visibleItems,
            viewportStart = 0,
            viewportEnd = 1000,
            visibilityThreshold = 0.6f
        )

        assertEquals(-1, activeIndex)
    }

    @Test
    fun pickSnapItemIndex_selectsMostVisibleItemEvenWhenThresholdNotMet() {
        val visibleItems = listOf(
            VisibleItemVisibility(index = 0, offset = -700, size = 1000),
            VisibleItemVisibility(index = 1, offset = 700, size = 1000)
        )
        val snapIndex = pickSnapItemIndex(
            visibleItems = visibleItems,
            viewportStart = 0,
            viewportEnd = 1000
        )

        assertEquals(0, snapIndex)
    }
}
