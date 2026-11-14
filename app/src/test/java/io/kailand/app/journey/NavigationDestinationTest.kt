package io.kailand.app.journey

import io.kailand.app.decodeCatalog
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NavigationDestinationTest {
    private val catalog = decodeCatalog(File("../content/catalog.json").readText(Charsets.UTF_8))
    private fun draft(label: String, latitude: Double = 37.5, longitude: Double = 127.1) = NavigationDraft(
        "11111111-1111-4111-8111-111111111111", 0, label, latitude, longitude, 1000,
        NavigationDraftStatus.ACTIVE,
    )

    @Test fun detailedCityDestinationsMatchAwayFromCatalogReferencePoints() {
        mapOf(
            "강원특별자치도 원주시 중앙로 1 카페" to "wonju",
            "원주시청" to "wonju",
            "강원특별자치도 속초시 중앙시장로 12" to "sokcho",
            "대전광역시 중구 대종로 1 음식점" to "daejeon",
            "대전시 유성구 대학로 1" to "daejeon",
        ).forEach { (label, expected) ->
            val value = draft(label)
            val destination = value.destination(catalog)
            assertEquals(label, expected, destination.regionId)
            assertEquals(label, destination.label)
            assertEquals(value.latitude, destination.latitude, 0.0)
            assertEquals(value.longitude, destination.longitude, 0.0)
        }
    }

    @Test fun exactRegionNamesAlsoMatchWithoutReferenceCoordinates() {
        catalog.regions.forEach { region ->
            assertEquals(region.id, draft(" ${region.name} ").destination(catalog).regionId)
        }
    }

    @Test fun explicitCityNameTakesPriorityOverTheReferencePoint() {
        val sokcho = catalog.regions.single { it.id == "sokcho" }
        assertEquals("wonju", draft("원주시 중앙로", sokcho.latitude, sokcho.longitude).destination(catalog).regionId)
    }

    @Test fun anonymousDestinationsKeepExactCoordinateFallbackWithoutRadiusMatching() {
        catalog.regions.forEach { region ->
            assertEquals(region.id, draft("음식점", region.latitude, region.longitude).destination(catalog).regionId)
            assertNull(draft("음식점", region.latitude + .1, region.longitude + .1).destination(catalog).regionId)
        }
    }

    @Test fun unknownNamesAndAmbiguousCityNamesStayUnregistered() {
        listOf("서울특별시 강남구", "수원시 카페", "서울 원주커피", "속초식당", "대전로 1").forEach {
            assertNull(it, draft(it).destination(catalog).regionId)
        }
        val wonju = catalog.regions.single { it.id == "wonju" }
        assertNull(draft("원주시 속초시 공동행사", wonju.latitude, wonju.longitude).destination(catalog).regionId)
        assertNull(draft("대전광역시 원주시 안내소").destination(catalog).regionId)
    }

    @Test fun namedCityIsNotMatchedIfMissingFromTheCatalog() {
        val withoutWonju = catalog.copy(regions = catalog.regions.filterNot { it.id == "wonju" })
        assertNull(draft("원주시 중앙로 1").destination(withoutWonju).regionId)
    }
}
