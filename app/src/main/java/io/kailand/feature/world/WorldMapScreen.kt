package io.kailand.feature.world

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.*
import io.kailand.core.ui.MemoridePleosTheme

/** NewWorldSlot consumer. A owns routing/insets; this screen never requests travel or writes saves. */
@Composable
fun WorldMapScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    history: RegionVisitHistory,
    gate: InteractionGate,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WorldMapContent(snapshot, catalog, history, gate, foreground, onClose, modifier)
}

/** Reject ambiguous/unknown discovery IDs; ownership alone never reveals a region. */
internal fun discoveredWorldRegions(snapshot: GameSnapshot, catalog: Catalog): List<WorldRegion> {
    val counts = catalog.regions.groupingBy { it.id }.eachCount()
    return worldPresentation(snapshot, catalog).regions.filter {
        it.discovered && it.id.isNotBlank() && it.name.isNotBlank() && counts[it.id] == 1
    }
}

/** A's measured 841x1210 split window is portrait, despite crossing the width breakpoint. */
internal fun worldMapUsesColumns(width: Float, height: Float, fontScale: Float): Boolean =
    width >= 840f && width > height && fontScale <= 1.3f

@Composable
internal fun WorldMapContent(
    snapshot: GameSnapshot, catalog: Catalog, history: RegionVisitHistory,
    gate: InteractionGate, foreground: Boolean, onClose: () -> Unit,
    modifier: Modifier = Modifier, initialSelectedId: String? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialSelectedId) }
    val regions = discoveredWorldRegions(snapshot, catalog)
    val selected = regions.singleOrNull { it.id == selectedId }
    LaunchedEffect(regions.map { it.id }) {
        if (selected == null) selectedId = null
    }
    val allowed = gate.canInteract && foreground
    val latestAllowed by rememberUpdatedState(allowed)
    val latestRegions by rememberUpdatedState(regions)
    val latestClose by rememberUpdatedState(onClose)
    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = allowed) {
            if (latestAllowed) {
                if (selected != null) selectedId = null else latestClose()
            }
        }
    }
    val allRegions = remember(snapshot, catalog) {
        val counts = catalog.regions.groupingBy { it.id }.eachCount()
        worldPresentation(snapshot, catalog).regions.filter {
            it.id.isNotBlank() && it.name.isNotBlank() && counts[it.id] == 1
        }
    }
    val total = catalog.regions.map { it.id }.filter { it.isNotBlank() }.distinct().size
    MemoridePleosTheme {
        if (!allowed) {
            Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF4FB), Color(0xFFFFF6EC))))) {
                Column(Modifier.padding(24.dp)) {
                    Text("월드맵 이용 중단", style = MaterialTheme.typography.headlineSmall)
                    Text(gate.reason ?: "이용 가능한 활성 화면에서 다시 확인해 주세요.")
                }
            }
        } else {
            WorldPleosLayout(allRegions, regions, total, catalog, history, selected,
                onSelect = { id ->
                    if (latestAllowed && latestRegions.any { it.id == id }) selectedId = id
                },
                onList = { if (latestAllowed) selectedId = null },
                onClose = { if (latestAllowed) latestClose() },
                modifier = modifier)
        }
    }
}
