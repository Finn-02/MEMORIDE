package io.kailand.feature.island

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.IslandDestinationSlot
import io.kailand.core.ui.MemorideDimens

/** S02 presentation only. The host continues to own navigation, gating and game mutations. */
@Composable
fun NewIslandScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    destination: IslandDestinationSlot?,
    onSelectCharacter: () -> Unit,
    onStartJourney: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenWorld: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = remember(snapshot, catalog) { islandPresentation(snapshot, catalog) }
    val catalogIssue = remember(snapshot, catalog, view) { hasIslandCatalogIssue(snapshot, catalog, view) }
    val currentGate = rememberUpdatedState(gate)
    val currentDestination = rememberUpdatedState(destination)
    val showActions = showsIslandActions(destination)

    fun runAction(action: () -> Unit) {
        dispatchIslandAction(currentGate.value, currentDestination.value, action)
    }

    IslandHomeTheme {
        Box(modifier.fillMaxSize().background(IslandHomeColors.Background).safeDrawingPadding()) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp)) {
                val wide = useWideIslandLayout(maxWidth + 48.dp, maxHeight + 48.dp, LocalDensity.current.fontScale)
                if (wide) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        val waitingLabel = destination?.destinationLabel
                        if (waitingLabel != null) IslandJourneyCarStage(waitingLabel, gate.canInteract,
                            Modifier.weight(3f).fillMaxHeight(), characterName = view.characterName)
                        else IslandHomeStage(snapshot, catalog, view,
                            animate = gate.canInteract && showActions,
                            modifier = Modifier.weight(3f).fillMaxHeight())
                        IslandHomePanel(snapshot, catalog, view, gate, catalogIssue, destination, showActions,
                            onSelectCharacter = { runAction(onSelectCharacter) },
                            onStartJourney = { runAction(onStartJourney) },
                            onOpenInventory = { runAction(onOpenInventory) },
                            onOpenWorld = { runAction(onOpenWorld) },
                            modifier = Modifier.weight(2f).fillMaxHeight())
                    }
                } else {
                    IslandHomeCompact(snapshot, catalog, view, gate, catalogIssue, destination, showActions,
                        onSelectCharacter = { runAction(onSelectCharacter) },
                        onStartJourney = { runAction(onStartJourney) },
                        onOpenInventory = { runAction(onOpenInventory) },
                        onOpenWorld = { runAction(onOpenWorld) })
                }
            }
        }
    }
}

internal fun useWideIslandLayout(width: Dp, height: Dp, fontScale: Float): Boolean =
    width >= MemorideDimens.WideBreakpoint && width > height && fontScale <= 1.3f

internal fun showsIslandActions(destination: IslandDestinationSlot?): Boolean = destination?.replacesActions != true

internal fun dispatchIslandAction(gate: InteractionGate, destination: IslandDestinationSlot?, action: () -> Unit) {
    if (gate.canInteract && showsIslandActions(destination)) action()
}

internal fun hasIslandCatalogIssue(snapshot: GameSnapshot, catalog: Catalog, view: IslandPresentation): Boolean {
    val avatar = AvatarAssetResolver.resolve(snapshot.avatar, catalog, "idle")
    return view.hasCatalogIssues || catalog.items.isEmpty() || view.characters.none { it.selected } ||
        avatar.body == null || avatar.faceCandidates.isEmpty() ||
        (view.outfitId != null && (avatar.outfit == null || view.items.none { it.equipped })) ||
        decorationPaths(view.slots, catalog).size != snapshot.decorationSlots.size
}
