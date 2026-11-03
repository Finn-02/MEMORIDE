package io.kailand.feature.world

import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType

internal data class WorldItem(
    val id: String, val name: String?, val type: ItemType?, val owned: Boolean,
    val imagePath: String?, val issues: List<String>,
)
internal data class WorldRegion(
    val id: String, val name: String, val theme: String, val discovered: Boolean,
    val items: List<WorldItem>, val issues: List<String>, val canPrepare: Boolean,
)
internal data class WorldPresentation(
    val regions: List<WorldRegion>, val ownedCount: Int, val totalCount: Int,
    val issues: List<String>,
)

internal fun safeWorldImagePath(path: String): Boolean =
    path.startsWith("assets/starter/") && path.endsWith(".png") &&
        path.removePrefix("assets/starter/").let { it.isNotBlank() && '/' !in it && '\\' !in it && it != ".." }

/** Read-only projection. Unknown saved IDs never become catalog entries or rewards. */
internal fun worldPresentation(snapshot: GameSnapshot, catalog: Catalog): WorldPresentation {
    val items = catalog.items.groupBy { it.id }
    val regionCounts = catalog.regions.groupingBy { it.id }.eachCount()
    val issues = buildList {
        if (regionCounts.any { it.value > 1 }) add("지역 ID가 중복되어 있어요.")
        if (items.any { it.value.size > 1 }) add("아이템 ID가 중복되어 있어요.")
        if (items.containsKey("")) add("비어 있는 아이템 ID가 있어요.")
    }
    val regions = catalog.regions.map { region ->
        val regionIssues = buildList {
            if (region.id.isBlank() || region.name.isBlank()) add("지역 ID 또는 이름이 비어 있어요.")
            if (regionCounts[region.id] != 1) add("같은 ID의 지역을 구분할 수 없어요.")
            if (!region.latitude.isFinite() || region.latitude !in -90.0..90.0 ||
                !region.longitude.isFinite() || region.longitude !in -180.0..180.0) add("목적지 좌표를 확인할 수 없어요.")
            if (region.itemIds.distinct().size != region.itemIds.size) add("기념품 참조가 중복되어 있어요.")
        }
        val regionalItems = region.itemIds.distinct().map { id ->
            val item = items[id]?.singleOrNull()
            val asset = item?.let { catalog.assets[it.assetKey] }
            val itemIssues = buildList {
                if (item == null || id.isBlank()) add("등록된 기념품을 확인할 수 없어요: $id")
                if (item != null && item.regionId != region.id) add("기념품의 소속 지역이 일치하지 않아요.")
                if (item != null && item.name.isBlank()) add("기념품 이름이 비어 있어요.")
                if (item != null && (asset == null || !safeWorldImagePath(asset.path))) add("기념품 이미지 경로가 누락되었거나 올바르지 않아요.")
            }
            WorldItem(id, item?.name, item?.type, id in snapshot.ownedItemIds,
                asset?.path?.takeIf(::safeWorldImagePath), itemIssues)
        }
        WorldRegion(region.id, region.name, region.theme, region.id in snapshot.discoveredRegionIds,
            regionalItems, regionIssues,
            canPrepare = regionIssues.isEmpty() && issues.isEmpty() && regionalItems.all { it.issues.isEmpty() })
    }
    val registered = items.keys.filter { it.isNotBlank() }.toSet()
    return WorldPresentation(regions, registered.count { it in snapshot.ownedItemIds }, registered.size, issues)
}

internal data class WorldInputs(
    val snapshot: GameSnapshot, val catalog: Catalog, val gate: InteractionGate, val foreground: Boolean,
)

/** Read fresh inputs at click time so a previously enabled button cannot dispatch stale data. */
internal class WorldActions(
    private val inputs: () -> WorldInputs,
    private val onSelectRegion: (String) -> Unit,
    private val onOpenItems: () -> Unit,
) {
    fun prepare(regionId: String): Boolean {
        val current = inputs()
        if (!current.gate.canInteract || !current.foreground) return false
        val region = worldPresentation(current.snapshot, current.catalog).regions.singleOrNull { it.id == regionId }
        if (region?.canPrepare != true) return false
        onSelectRegion(regionId)
        return true
    }
    fun openItems(): Boolean {
        val current = inputs()
        if (!current.gate.canInteract || !current.foreground) return false
        onOpenItems()
        return true
    }
}
