package com.example.shortformvideofeed.ui.feed

private const val DEFAULT_VISIBILITY_THRESHOLD = 0.6f

data class VisibleItemVisibility(
    val index: Int,
    val offset: Int,
    val size: Int
)

fun pickActiveItemIndex(
    visibleItems: List<VisibleItemVisibility>,
    viewportStart: Int,
    viewportEnd: Int,
    visibilityThreshold: Float = DEFAULT_VISIBILITY_THRESHOLD
): Int {
    val best = visibleItems
        .mapNotNull { item ->
            visibleRatio(
                item = item,
                viewportStart = viewportStart,
                viewportEnd = viewportEnd
            ).takeIf { it.second > 0f }?.let { item.index to it.second }
        }
        .maxByOrNull { it.second }
    return if (best != null && best.second >= visibilityThreshold) best.first else -1
}

fun pickSnapItemIndex(
    visibleItems: List<VisibleItemVisibility>,
    viewportStart: Int,
    viewportEnd: Int
): Int {
    return visibleItems
        .mapNotNull { item ->
            visibleRatio(
                item = item,
                viewportStart = viewportStart,
                viewportEnd = viewportEnd
            ).takeIf { it.second > 0f }?.let { item.index to it.second }
        }
        .maxByOrNull { it.second }
        ?.first ?: -1
}

private fun visibleRatio(
    item: VisibleItemVisibility,
    viewportStart: Int,
    viewportEnd: Int
): Pair<Int, Float> {
    if (item.size <= 0) return item.index to 0f

    val visibleStart = maxOf(item.offset, viewportStart)
    val visibleEnd = minOf(item.offset + item.size, viewportEnd)
    val visibleHeight = (visibleEnd - visibleStart).coerceAtLeast(0)
    return item.index to (visibleHeight.toFloat() / item.size.toFloat())
}
