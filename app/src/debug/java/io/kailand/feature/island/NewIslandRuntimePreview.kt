package io.kailand.feature.island

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.app.CatalogAssets
import io.kailand.app.decodeCatalog
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GateState
import io.kailand.core.contract.InteractionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Preview(name = "S02 Wide", widthDp = 1100, heightDp = 640)
@Preview(name = "S02 Split", widthDp = 640, heightDp = 720)
@Preview(name = "S02 Portrait", widthDp = 560, heightDp = 960)
@Preview(name = "S02 411dp", widthDp = 411, heightDp = 740)
@Preview(name = "S02 411dp font 2", widthDp = 411, heightDp = 740, fontScale = 2f)
@Composable
fun NewIslandRuntimePreview() {
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
    val decorations = mapOf(
        "slot_left" to "daejeon_sign",
        "slot_right" to "wonju_tree",
        "slot_back" to "sokcho_shell",
    )
    val snapshot = GameSave().toSnapshot().copy(
        avatar = AvatarPreview("momo", "daejeon_hat"),
        dailyGrowth = 3,
        ownedItemIds = decorations.values.toSet() + "daejeon_hat",
        decorationSlots = decorations,
    )
    NewIslandScreen(
        snapshot = snapshot,
        catalog = loaded,
        gate = InteractionGate(GateState.PARKED_ALLOWED),
        destination = null,
        onSelectCharacter = {},
        onStartJourney = {},
        onOpenInventory = {},
        onOpenWorld = {},
    )
}
