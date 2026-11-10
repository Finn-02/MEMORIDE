package io.kailand.feature.island

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.app.CatalogAssets
import io.kailand.app.decodeCatalog
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Preview(name = "S03 Food wide locked", widthDp = 1100, heightDp = 640)
@Composable
fun InventoryFoodRuntimePreview() {
    InventoryPreview(ItemType.FOOD, GameSave())
}

@Preview(name = "S03 Outfit split", widthDp = 640, heightDp = 720)
@Preview(name = "S03 Outfit 411dp font 2", widthDp = 411, heightDp = 740, fontScale = 2f)
@Composable
fun InventoryOutfitRuntimePreview() {
    InventoryPreview(ItemType.OUTFIT, GameSave(
        equippedOutfitId = "wonju_crown",
        ownedItemIds = setOf("wonju_crown", "daejeon_hat"),
    ))
}

@Preview(name = "S03 Decoration portrait", widthDp = 560, heightDp = 960)
@Preview(name = "S03 Decoration 411dp", widthDp = 411, heightDp = 740)
@Composable
fun InventoryDecorationRuntimePreview() {
    InventoryPreview(ItemType.DECOR, GameSave(
        dailyGrowth = 3,
        ownedItemIds = setOf("wonju_tree", "daejeon_sign"),
        decorationSlots = mapOf("slot_left" to "wonju_tree"),
    ))
}

@Composable
private fun InventoryPreview(type: ItemType, save: GameSave) {
    val assets = LocalContext.current.assets
    val catalog by produceState<Result<Catalog>?>(null, assets) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeCatalog(CatalogAssets(assets).readCatalogJson()) }
        }
    }
    val loaded = catalog?.getOrNull()
    if (loaded == null) {
        Text(if (catalog == null) "Catalog를 불러오는 중…" else "Catalog를 불러오지 못했어요.")
        return
    }
    InventoryScreenContent(
        save.toSnapshot(), loaded, InteractionGate(GateState.PARKED_ALLOWED),
        onAction = { MutationResult.Rejected("Preview") },
        onClose = {}, initialTab = type,
    )
}
