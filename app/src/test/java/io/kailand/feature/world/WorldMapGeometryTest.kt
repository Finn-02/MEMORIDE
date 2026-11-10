package io.kailand.feature.world

import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class WorldMapGeometryTest {
    @Test fun threeDiscoveredLabelsDoNotOverlapInNarrowNormalAndLargeTextBoards() {
        val coordinates = listOf("daejeon" to (36.35 to 127.38), "wonju" to (37.34 to 127.92),
            "sokcho" to (38.21 to 128.59))
        for ((width, height, markerWidth, markerHeight, footer) in listOf(
            listOf(363f, 360f, 88f, 96f, 40f), listOf(363f, 360f, 120f, 132f, 88f))) {
            val anchors = coordinates.map { (id, point) ->
                val projected = worldMapPoint(point.first, point.second)!!
                WorldMapAnchor(id, (width - height) / 2 + projected.first * height, projected.second * height)
            }
            val markers = layoutWorldMapMarkers(anchors, width, height, markerWidth, markerHeight, footer)
            assertEquals(anchors.toSet(), markers.map { it.anchor }.toSet())
            markers.forEach { assertTrue(it.left >= 0 && it.top >= 0 && it.top + markerHeight <= height - footer) }
            for (i in markers.indices) for (j in 0 until i) {
                val a = markers[i]; val b = markers[j]
                assertFalse(a.left < b.left + markerWidth && a.left + markerWidth > b.left &&
                    a.top < b.top + markerHeight && a.top + markerHeight > b.top)
            }
        }
    }
    @Test fun mapPathsStayBelowPackagingStringLimitAndContainOnlySupportedCommands() {
        val root = listOf(File("."), File("..")).first { File(it, "content/catalog.json").exists() }
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(root, "app/src/main/res/drawable/memoride_korea_map.xml"))
        val paths = document.getElementsByTagName("path")
        assertTrue(paths.length > 0)
        for (i in 0 until paths.length) {
            val data = paths.item(i).attributes.getNamedItem("android:pathData").nodeValue
            assertTrue(data.toByteArray(Charsets.UTF_8).size < 30000)
            assertTrue(data.filter { it.isLetter() }.all { it in "MLZ" })
        }
    }
    @Test fun measuredPortraitSplitIsStackedAndLargeTextNeverForcesTwoColumns() {
        assertTrue(worldMapUsesColumns(1682f, 1210f, 1f))
        assertFalse(worldMapUsesColumns(841f, 1210f, 1f))
        assertFalse(worldMapUsesColumns(720f, 1280f, 1f))
        assertFalse(worldMapUsesColumns(640f, 720f, 1f))
        assertFalse(worldMapUsesColumns(1682f, 1210f, 2f))
    }
    @Test fun catalogRegionCoordinatesFitStaticMapWithoutInventedPositions() {
        val root = listOf(File("."), File("..")).first { File(it, "content/catalog.json").exists() }
        val regions = Json.parseToJsonElement(File(root, "content/catalog.json").readText())
            .jsonObject.getValue("regions").jsonArray
        for (region in regions) {
            val r = region.jsonObject
            val point = worldMapPoint(r.getValue("latitude").jsonPrimitive.double,
                r.getValue("longitude").jsonPrimitive.double)
            assertNotNull(point)
            assertTrue(point!!.first in 0f..1f && point.second in 0f..1f)
        }
    }

    @Test fun northIsUpAndEastIsRightAndUnsupportedCoordinatesAreNotClamped() {
        assertTrue(worldMapPoint(38.0, 128.0)!!.second < worldMapPoint(36.0, 128.0)!!.second)
        assertTrue(worldMapPoint(36.0, 129.0)!!.first > worldMapPoint(36.0, 127.0)!!.first)
        assertNull(worldMapPoint(Double.NaN, 127.0))
        assertNull(worldMapPoint(36.0, Double.POSITIVE_INFINITY))
        assertNull(worldMapPoint(0.0, 0.0))
    }
}
