package io.kailand.feature.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.IslandDestinationSlot
import io.kailand.core.ui.MemorideBrushes
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideSecondaryButton
import io.kailand.core.ui.MemorideTheme

/** S02 entry for NewIslandSlot. The host owns navigation and the destination content. */
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

    val selectCharacter: @Composable () -> Unit = {
        if (showActions) {
            MemorideSecondaryButton(
                text = "캐릭터 선택",
                onClick = { runAction(onSelectCharacter) },
                enabled = gate.canInteract,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    val actionArea: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            destination?.content?.invoke()
            if (showActions) {
                IslandJourneyActions(
                    enabled = gate.canInteract,
                    onStartJourney = { runAction(onStartJourney) },
                    onOpenInventory = { runAction(onOpenInventory) },
                    onOpenWorld = { runAction(onOpenWorld) },
                )
            }
        }
    }

    MemorideTheme {
        MemorideScreen(modifier) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val fontScale = LocalDensity.current.fontScale
                val wide = useWideIslandLayout(maxWidth, maxHeight, fontScale)
                val padding = if (wide) 32.dp else MemorideDimens.ScreenPadding
                val canvasLimit = if (wide) {
                    (maxHeight - padding * 2).coerceAtLeast(220.dp)
                } else {
                    (maxHeight * 0.55f).coerceAtLeast(220.dp)
                }
                val body = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)

                if (wide) {
                    Row(body, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        IslandStage(snapshot, catalog, view, canvasLimit, Modifier.weight(1.8f).heightIn(min = canvasLimit))
                        Column(
                            Modifier.weight(1f).heightIn(min = canvasLimit),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                Modifier.padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IslandInformation(view, gate, catalogIssue)
                                selectCharacter()
                            }
                            actionArea()
                        }
                    }
                } else {
                    val inlineGrowth = maxWidth >= 560.dp && maxWidth > maxHeight * 0.75f && fontScale <= 1.3f
                    Column(body, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IslandInformation(view, gate, catalogIssue, inlineGrowth)
                        selectCharacter()
                        IslandStage(snapshot, catalog, view, canvasLimit, Modifier.fillMaxWidth())
                        actionArea()
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandInformation(
    view: IslandPresentation,
    gate: InteractionGate,
    catalogIssue: Boolean,
    inlineGrowth: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (inlineGrowth) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IslandTitle(view, Modifier.weight(1f))
                IslandGrowthMeter(view.growth, Modifier.weight(1f))
            }
        } else {
            IslandTitle(view)
            IslandGrowthMeter(view.growth, Modifier.fillMaxWidth())
        }
        if (catalogIssue) {
            Text(
                "캐릭터 또는 섬 자산 정보를 확인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!gate.canInteract) {
            Text(
                gate.reason ?: "현재 나의 섬의 기본 행동을 사용할 수 없어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MemorideColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun IslandTitle(view: IslandPresentation, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val name = view.characters.firstOrNull { it.selected }?.name
        Text(
            name?.let { "$it 캐릭터" } ?: view.characterName,
            modifier = Modifier
                .background(MemorideColors.ChipBackground, RoundedCornerShape(percent = 50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MemorideColors.ChipText,
        )
        Text(
            "나의 섬",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun IslandGrowthMeter(growth: IslandGrowth?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (growth == null) {
            Text("섬 성장 정보를 확인해 주세요.", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        val stage = growth.ordinal
        val lastStage = IslandGrowth.REST_AREA.ordinal
        Text("섬 성장 $stage / ${lastStage}단계", style = MaterialTheme.typography.titleMedium)
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(percent = 50))
                .background(MemorideColors.ProgressEmpty)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(stage.toFloat(), 0f..lastStage.toFloat()) },
        ) {
            if (stage > 0) {
                Box(
                    Modifier.fillMaxWidth(stage.toFloat() / lastStage).height(10.dp)
                        .background(MemorideBrushes.Progress, RoundedCornerShape(percent = 50)),
                )
            }
        }
    }
}

@Composable
private fun IslandStage(
    snapshot: GameSnapshot,
    catalog: Catalog,
    view: IslandPresentation,
    canvasLimit: Dp,
    modifier: Modifier,
) {
    Box(
        modifier.clip(RoundedCornerShape(MemorideDimens.PanelRadius)).background(MemorideBrushes.Sea),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = canvasLimit).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            IslandScene(
                growth = view.growth,
                avatar = snapshot.avatar,
                catalog = catalog,
                characterName = view.characterName,
                eating = false,
                slots = view.slots,
            )
        }
    }
}

@Composable
private fun IslandJourneyActions(
    enabled: Boolean,
    onStartJourney: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenWorld: () -> Unit,
) {
    val inventoryIcon: @Composable () -> Unit = {
        IslandActionIcon(R.drawable.memoride_inventory, 28.dp, enabled)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MemoridePrimaryButton(
            text = "여정 출발",
            supportingText = "내비가 자동으로 켜져요",
            onClick = onStartJourney,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            leading = { IslandActionIcon(R.drawable.memoride_departure, 32.dp, enabled) },
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 320.dp && LocalDensity.current.fontScale <= 1.3f) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemorideSecondaryButton("인벤토리", onOpenInventory, Modifier.weight(1f), enabled, leading = inventoryIcon)
                    MemorideSecondaryButton("월드맵·도감", onOpenWorld, Modifier.weight(1f), enabled)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemorideSecondaryButton("인벤토리", onOpenInventory, Modifier.fillMaxWidth(), enabled, leading = inventoryIcon)
                    MemorideSecondaryButton("월드맵·도감", onOpenWorld, Modifier.fillMaxWidth(), enabled)
                }
            }
        }
    }
}

@Composable
private fun IslandActionIcon(resource: Int, size: Dp, enabled: Boolean) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(size).alpha(if (enabled) 1f else 0.5f),
    )
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
