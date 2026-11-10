package io.kailand.feature.island

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import io.kailand.core.ui.MemorideBrushes
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemoridePanel
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** S01 entry for CharacterSelectionSlot. The shell owns safeDrawing; A owns routing and saving. */
@Composable
fun CharacterSelectionScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onSelect: suspend (String) -> MutationResult,
    modifier: Modifier = Modifier,
) {
    val choices = remember(snapshot, catalog) { characterSelectionChoices(snapshot, catalog) }
    val submission = remember { CharacterSelectionSubmission() }
    val currentGate = rememberUpdatedState(gate)
    val currentOnSelect = rememberUpdatedState(onSelect)
    val currentChoices = rememberUpdatedState(choices)
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val failure = submission.failure

    fun request(id: String) {
        if (currentChoices.value.none { it.id == id }) return
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            submission.select(id, currentGate.value) { currentOnSelect.value(it) }
        }
    }

    // The error and retry are at the top, including when the last card needed scrolling.
    LaunchedEffect(failure) { if (failure != null) scroll.scrollTo(0) }

    MemorideTheme {
        MemorideScreen(modifier) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = useWideCharacterCards(maxWidth, maxHeight, LocalDensity.current.fontScale)
                val thumbnail = (maxWidth * 0.22f).coerceIn(56.dp, 96.dp)
                val canSelect = gate.canInteract && submission.pendingId == null
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll)
                        .padding(if (wide) MemorideDimens.ScreenPadding * 2 else MemorideDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(if (wide) 28.dp else 16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "함께 갈 캐릭터를 골라 주세요",
                            Modifier.semantics { heading() },
                            style = if (wide) MaterialTheme.typography.headlineLarge
                                else MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "선택을 저장하면 나의 섬으로 이동해요",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MemorideColors.TextSecondary,
                        )
                    }
                    if (!gate.canInteract || failure != null || choices.isEmpty()) {
                        MemoridePanel(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                            if (!gate.canInteract) Text(gate.reason ?: "현재 캐릭터를 선택할 수 없어요.")
                            if (choices.isEmpty()) Text("선택 가능한 캐릭터를 불러오지 못했어요.")
                            if (failure != null) {
                                Text(failure.message)
                                MemoridePrimaryButton(
                                    text = "다시 시도",
                                    onClick = { request(failure.characterId) },
                                    enabled = canSelect && choices.any { it.id == failure.characterId },
                                )
                            }
                        }
                    }
                    if (wide) {
                        Row(
                            Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            for (choice in choices) {
                                CharacterSelectionCard(
                                    choice, catalog, wide = true, thumbnail = thumbnail,
                                    busy = submission.pendingId == choice.id, enabled = canSelect,
                                    onClick = { request(choice.id) }, modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth().selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (choice in choices) {
                                CharacterSelectionCard(
                                    choice, catalog, wide = false, thumbnail = thumbnail,
                                    busy = submission.pendingId == choice.id, enabled = canSelect,
                                    onClick = { request(choice.id) }, modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterSelectionCard(
    choice: IslandCharacter,
    catalog: Catalog,
    wide: Boolean,
    thumbnail: Dp,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(MemorideDimens.PanelRadius)
    Surface(
        modifier = modifier.heightIn(min = MemorideDimens.MinimumTouchTarget).clip(shape)
            .selectable(selected = choice.selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = MemorideColors.Surface,
        border = BorderStroke(
            if (choice.selected || busy) 2.dp else 1.dp,
            when {
                busy -> MemorideColors.OceanAccessible
                choice.selected -> MemorideColors.Navy
                else -> MemorideColors.BorderCard
            },
        ),
        shadowElevation = 4.dp,
    ) {
        if (wide) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterSelectionArt(choice.id, catalog, Modifier.fillMaxWidth().aspectRatio(1.43f))
                CharacterSelectionLabel(choice, busy)
            }
        } else {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterSelectionArt(choice.id, catalog, Modifier.size(thumbnail))
                CharacterSelectionLabel(choice, busy, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CharacterSelectionArt(id: String, catalog: Catalog, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MemorideBrushes.CharacterStage)) {
        AvatarView(AvatarPreview(id, null), catalog, "idle", Modifier.matchParentSize())
    }
}

@Composable
private fun CharacterSelectionLabel(choice: IslandCharacter, busy: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(choice.name, style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                busy && choice.selected -> "현재 선택 · 저장 중…"
                busy -> "저장 중…"
                choice.selected -> "현재 선택"
                else -> "캐릭터 선택"
            },
            modifier = Modifier.semantics { if (busy) liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = if (choice.selected) MemorideColors.Navy else MemorideColors.TextSecondary,
        )
    }
}

internal fun characterSelectionChoices(snapshot: GameSnapshot, catalog: Catalog): List<IslandCharacter> =
    islandPresentation(snapshot, catalog).characters.filter { choice ->
        val avatar = AvatarPreview(choice.id, null)
        AvatarAssetResolver.approvedFrames(avatar, "idle", AvatarAssetResolver.resolve(avatar, catalog, "idle"))
            .isNotEmpty()
    }

internal fun useWideCharacterCards(width: Dp, height: Dp, fontScale: Float): Boolean =
    width >= MemorideDimens.WideBreakpoint && width > height && fontScale <= 1.3f

/** Main-thread UI state. The saved selection is always read from the host's snapshot. */
internal class CharacterSelectionSubmission {
    data class Failure(val characterId: String, val message: String)

    var pendingId: String? by mutableStateOf(null)
        private set
    var failure: Failure? by mutableStateOf(null)
        private set

    suspend fun select(id: String, gate: InteractionGate, onSelect: suspend (String) -> MutationResult) {
        if (!gate.canInteract || pendingId != null) return
        pendingId = id
        failure = null
        try {
            failure = when (onSelect(id)) {
                MutationResult.Success -> null
                is MutationResult.Rejected -> Failure(id, "지금은 캐릭터를 선택할 수 없어요. 잠시 후 다시 시도해 주세요.")
                is MutationResult.StorageFailure -> Failure(id, "캐릭터를 저장하지 못했어요. 다시 시도해 주세요.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failure = Failure(id, "캐릭터 선택을 완료하지 못했어요. 다시 시도해 주세요.")
        } finally {
            pendingId = null
        }
    }
}
