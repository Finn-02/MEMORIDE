package io.kailand.feature.world

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.core.contract.*
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.time.Instant

@Preview(name = "C03 DEMO 초기", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun WorldFreshPreview() = WorldExample("fresh")

@Preview(name = "C03 DEMO 발견 지역", widthDp = 800, heightDp = 480, showBackground = true)
@Preview(name = "C03 DEMO 큰 글자", widthDp = 320, heightDp = 800, fontScale = 1.3f, showBackground = true)
@Composable
fun WorldRegionalPreview() = WorldExample("regional")

@Preview(name = "C03 UNKNOWN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun WorldBlockedPreview() = WorldExample("regional", GateState.UNKNOWN)

@Preview(name = "UX-C01 빈 지도 · 넓은 창", widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
fun NewWorldEmptyPreview() = WorldExample("fresh", newUi = true)

@Preview(name = "UX-C01 발견 · 분할 창", widthDp = 640, heightDp = 720, showBackground = true)
@Preview(name = "UX-C01 발견 · 세로 창", widthDp = 720, heightDp = 1280, showBackground = true)
@Preview(name = "UX-C01 보조 큰 글자", widthDp = 411, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
fun NewWorldDiscoveredPreview() = WorldExample("wonju", newUi = true, historySample = "available")

@Preview(name = "UX-C02 기록 오류", widthDp = 640, heightDp = 1000, showBackground = true)
@Composable
fun NewWorldHistoryErrorPreview() = WorldExample("regional", newUi = true, historySample = "unavailable")

@Preview(name = "UX-C01 UNKNOWN 차단", widthDp = 640, heightDp = 720, showBackground = true)
@Composable
fun NewWorldBlockedPreview() = WorldExample("regional", GateState.UNKNOWN, newUi = true)

private data class WorldPreviewData(val catalog: Catalog, val snapshot: GameSnapshot)
private data class WorldPreviewLoad(val data: WorldPreviewData? = null, val error: Boolean = false)

@Composable
internal fun WorldExample(sample: String, gate: GateState = GateState.PARKED_ALLOWED, newUi: Boolean = false,
    historySample: String = "loading", showDetail: Boolean = true, onClose: () -> Unit = {}) {
    val assets = LocalContext.current.assets
    val loaded by produceState(WorldPreviewLoad(), assets, sample) {
        value = withContext(Dispatchers.IO) {
            try {
                val catalog = assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val fixtures = assets.open("fixtures/ui_samples.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                WorldPreviewLoad(parseWorldPreview(catalog, fixtures, sample))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { WorldPreviewLoad(error = true) }
        }
    }
    MemorideTheme {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text("Preview DEMO · 저장하지 않는 샘플")
            if (newUi && historySample == "available") Text("방문 횟수·날짜는 예시예요.")
            val data = loaded.data
            when {
                loaded.error -> Text("Preview용 Catalog/샘플을 불러오지 못했어요.")
                data == null -> Text("Preview 샘플 불러오는 중")
                newUi -> WorldMapContent(data.snapshot, data.catalog, when (historySample) {
                    "available" -> RegionVisitHistory.Available(data.snapshot.discoveredRegionIds.map { id ->
                        RegionVisitSummary(id, 2, Instant.parse("2026-11-08T00:00:00Z").toEpochMilli(),
                            Instant.parse("2026-11-09T00:00:00Z").toEpochMilli())
                    })
                    "empty" -> RegionVisitHistory.Available(emptyList())
                    "unavailable" -> RegionVisitHistory.Unavailable("Preview 기록 오류 예시")
                    else -> RegionVisitHistory.Loading
                },
                    InteractionGate(gate), true, onClose, modifier = Modifier.weight(1f),
                    initialSelectedId = if (showDetail) data.snapshot.discoveredRegionIds.firstOrNull() else null)
                else -> WorldContent(data.snapshot, data.catalog, InteractionGate(gate), true, {}, {},
                    modifier = Modifier.weight(1f), initialSelectedId = data.snapshot.discoveredRegionIds.firstOrNull())
            }
        }
    }
}

// Debug-only parsing of the actual packaged sources. No duplicated Kotlin content list or live load.
private fun parseWorldPreview(catalogText: String, fixtureText: String, sample: String): WorldPreviewData {
    fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
    fun JsonElement.strings() = jsonArray.map { it.jsonPrimitive.content }
    val json = Json.parseToJsonElement(catalogText).jsonObject
    val rules = json.getValue("rules").jsonObject
    val catalog = Catalog(json.text("contentVersion"), json.text("description"),
        json.getValue("characters").jsonArray.map { v -> v.jsonObject.let { CharacterDefinition(it.text("id"), it.text("name"), it.text("assetKey")) } },
        json.getValue("expressions").jsonObject.mapValues { it.value.jsonPrimitive.content },
        json.getValue("assets").jsonObject.mapValues { (_, v) -> v.jsonObject.let { AssetDefinition(it.text("path"), it.text("kind")) } },
        json.getValue("regions").jsonArray.map { v -> v.jsonObject.let {
            RegionDefinition(it.text("id"), it.text("name"), it.text("theme"), it.getValue("latitude").jsonPrimitive.double,
                it.getValue("longitude").jsonPrimitive.double, it.getValue("itemIds").strings())
        } },
        json.getValue("items").jsonArray.map { v -> v.jsonObject.let {
            ItemDefinition(it.text("id"), it.text("regionId"), ItemType.valueOf(it.text("type")), it.text("name"), it.text("assetKey"))
        } },
        GameRules(rules.getValue("dailyGrowthCap").jsonPrimitive.int, rules.getValue("dailyGrantLimit").jsonPrimitive.int,
            rules.text("timeZone"), rules.getValue("decorationSlotIds").strings()))
    val game = Json.parseToJsonElement(fixtureText).jsonObject.getValue("samples").jsonArray
        .single { it.jsonObject.text("id") == if (sample == "wonju") "regional" else sample }.jsonObject.getValue("game").jsonObject
    val avatar = game.getValue("avatar").jsonObject
    val snapshot = GameSnapshot(AvatarPreview(avatar.text("characterId"), avatar["outfitItemId"]?.jsonPrimitive?.contentOrNull),
        game.getValue("dailyGrowth").jsonPrimitive.int, game.getValue("ownedItemIds").strings().toSet(),
        game.getValue("discoveredRegionIds").strings().toSet(),
        game.getValue("decorationSlots").jsonObject.mapValues { it.value.jsonPrimitive.content }, null)
    val exampleSnapshot = if (sample == "wonju") {
        val wonju = catalog.regions.single { it.id == "wonju" }
        snapshot.copy(discoveredRegionIds = setOf(wonju.id), ownedItemIds = wonju.itemIds.toSet())
    } else snapshot
    return WorldPreviewData(catalog, exampleSnapshot)
}
