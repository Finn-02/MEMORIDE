package io.kailand.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.ui.*
import io.kailand.feature.briefing.ArrivalSaveScreen
import io.kailand.feature.briefing.RegionBriefingScreen
import io.kailand.feature.island.CharacterSelectionScreen
import io.kailand.feature.island.IslandHomeStage
import io.kailand.feature.island.NewIslandScreen
import io.kailand.feature.island.islandPresentation
import io.kailand.feature.world.WorldMapScreen

/** Owner screens wired by A; the root applies the window insets once. */
data class ProductScreenSlots(
    val character: CharacterSelectionSlot = { snapshot, catalog, gate, select, modifier ->
        CharacterSelectionScreen(snapshot, catalog, gate, select, modifier)
    },
    val island: NewIslandSlot = { snapshot, catalog, gate, destination, select, start, inventory, world, modifier ->
        NewIslandScreen(snapshot, catalog, gate, destination, select, start, inventory, world, modifier)
    },
    val world: NewWorldSlot = { snapshot, catalog, history, gate, close, modifier ->
        WorldMapScreen(snapshot, catalog, history, gate, close, modifier)
    },
    val briefing: NewBriefingSlot = { receipt, catalog, avatar, gate, island, select, modifier ->
        RegionBriefingScreen(receipt, catalog, avatar, gate, island, select, modifier, applyInsets = false)
    },
    val save: ArrivalSaveSlot = { state, gate, retry, dismiss, modifier ->
        ArrivalSaveScreen(state, gate, retry, dismiss, modifier, applyInsets = false)
    },
)

/** Inject B's read-only scene through PM's scene slot, without a feature-to-feature import. */
internal fun productScreenSlots(
    snapshot: GameSnapshot, history: RegionVisitHistory = RegionVisitHistory.Loading,
    onDemoReset: (suspend () -> MutationResult)? = null,
) = ProductScreenSlots(
    character = { state, catalog, gate, select, modifier ->
        CharacterSelectionScreen(state, catalog, gate, select, modifier, onDemoReset = onDemoReset)
    },
    briefing = { receipt, catalog, avatar, gate, island, select, modifier ->
        RegionBriefingScreen(receipt, catalog, avatar, gate, island, select, modifier, applyInsets = false,
            scene = { _ ->
                val view = remember(snapshot, catalog) { islandPresentation(snapshot, catalog) }
                IslandHomeStage(snapshot, catalog, view, animate = gate.canInteract, modifier = Modifier.fillMaxSize())
            }, gameSnapshot = snapshot, visitHistory = history)
    },
)

@Composable
internal fun ProductColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
