package io.kailand.feature.world

internal data class WorldMapAnchor(val id: String, val x: Float, val y: Float)
internal data class WorldMapMarker(val anchor: WorldMapAnchor, val left: Float, val top: Float)

/** Label offsets never change the geographic anchor. The board draws a leader to that anchor. */
internal fun layoutWorldMapMarkers(
    anchors: List<WorldMapAnchor>, width: Float, height: Float, markerWidth: Float, markerHeight: Float,
    footerHeight: Float,
): List<WorldMapMarker> {
    val placed = mutableListOf<WorldMapMarker>()
    val maxX = (width - markerWidth).coerceAtLeast(0f)
    val maxY = (height - footerHeight - markerHeight).coerceAtLeast(0f)
    val ordered = anchors.sortedBy { it.y }
    fun candidates(anchor: WorldMapAnchor): List<WorldMapMarker> {
        val wantedX = anchor.x - markerWidth / 2
        val wantedY = anchor.y - 28f
        return buildList {
            for (dy in -2..2) for (dx in -2..2) {
                add(WorldMapMarker(anchor, (wantedX + dx * (markerWidth + 8)).coerceIn(0f, maxX),
                    (wantedY + dy * (markerHeight + 8)).coerceIn(0f, maxY)))
            }
            for (x in listOf(0f, maxX)) for (y in listOf(0f, maxY)) add(WorldMapMarker(anchor, x, y))
        }.distinctBy { it.left to it.top }.sortedBy {
            (it.left - wantedX) * (it.left - wantedX) + (it.top - wantedY) * (it.top - wantedY)
        }
    }
    fun overlaps(candidate: WorldMapMarker) = placed.any {
            candidate.left < it.left + markerWidth + 4 && candidate.left + markerWidth + 4 > it.left &&
                candidate.top < it.top + markerHeight + 4 && candidate.top + markerHeight + 4 > it.top
    }
    var budget = 10000
    fun place(index: Int): Boolean {
        if (index == ordered.size) return true
        if (--budget <= 0) return false
        for (candidate in candidates(ordered[index])) {
            if (overlaps(candidate)) continue
            placed += candidate
            if (place(index + 1)) return true
            placed.removeAt(placed.lastIndex)
        }
        return false
    }
    if (!place(0)) return ordered.map { candidates(it).first() }
    return placed
}
