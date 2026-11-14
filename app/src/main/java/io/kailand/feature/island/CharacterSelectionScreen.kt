package io.kailand.feature.island

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.core.ui.MemoridePleosFonts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemoridePleosTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** S01 entry. Card taps preview a choice; the existing host callback saves and routes on Start. */
@Composable
fun CharacterSelectionScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onSelect: suspend (String) -> MutationResult,
    modifier: Modifier = Modifier,
    onDemoReset: (suspend () -> MutationResult)? = null,
) {
    val choices = remember(snapshot, catalog) { characterSelectionChoices(snapshot, catalog) }
    var selectedId by rememberSaveable { mutableStateOf(choices.firstOrNull { it.selected }?.id) }
    val submission = remember { CharacterSelectionSubmission() }
    val currentGate = rememberUpdatedState(gate)
    val currentOnSelect = rememberUpdatedState(onSelect)
    val currentChoices = rememberUpdatedState(choices)
    val scope = rememberCoroutineScope()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var resetError by rememberSaveable { mutableStateOf<String?>(null) }
    val currentReset = rememberUpdatedState(onDemoReset)
    val canSelect = gate.canInteract && submission.pendingId == null && !resetting
    val failure = submission.failure?.takeIf { it.characterId == selectedId }

    LaunchedEffect(choices.map { it.id }) {
        if (choices.none { it.id == selectedId }) selectedId = choices.firstOrNull { it.selected }?.id
    }

    fun request(id: String) {
        if (resetting || currentChoices.value.none { it.id == id }) return
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            submission.select(id, currentGate.value) { currentOnSelect.value(it) }
        }
    }

    MemoridePleosTheme {
        CharacterSelectionPleosLayout(
            choices = choices, catalog = catalog, selectedId = selectedId,
            enabled = canSelect, busy = submission.pendingId != null,
            notice = when {
                !gate.canInteract -> gate.reason ?: "현재 캐릭터를 선택할 수 없어요."
                choices.isEmpty() -> "선택 가능한 캐릭터를 불러오지 못했어요."
                else -> resetError ?: failure?.message
            },
            retry = failure != null,
            onChoose = { id ->
                if (currentGate.value.canInteract && submission.pendingId == null && !resetting && currentChoices.value.any { it.id == id }) {
                    selectedId = id
                }
            },
            onStart = { selectedId?.let(::request) }, modifier = modifier,
            onDemoReset = onDemoReset?.let { { if (canSelect) confirmReset = true } },
        )
        if (confirmReset) AlertDialog(
            onDismissRequest = { if (!resetting) confirmReset = false },
            title = { Text("시연용 초기화", fontFamily = MemoridePleosFonts.CharacterName, fontSize = 28.sp) },
            text = { Text("선택한 캐릭터, 섬 성장, 보유 아이템, 발견 지역, 방문·여정 기록과 내비 목적지를 모두 초기화해요. 삭제한 게임 기록은 되돌릴 수 없어요.",
                fontFamily = MemoridePleosFonts.Body, fontSize = 20.sp) },
            dismissButton = { TextButton(onClick = { confirmReset = false }, enabled = !resetting,
                modifier = Modifier.heightIn(min = 56.dp)) { Text("취소", fontSize = 20.sp) } },
            confirmButton = { TextButton(enabled = canSelect && currentReset.value != null, modifier = Modifier.heightIn(min = 56.dp),
                onClick = {
                    val action = currentReset.value
                    if (action != null && currentGate.value.canInteract && !resetting && submission.pendingId == null) {
                        resetting = true
                        resetError = null
                        confirmReset = false
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            try {
                                when (val result = action()) {
                                    MutationResult.Success -> selectedId = null
                                    is MutationResult.Rejected -> resetError = result.reason
                                    is MutationResult.StorageFailure -> resetError = result.message
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                resetError = "초기화를 마치지 못했어요. 다시 시도해 주세요."
                            } finally { resetting = false }
                        }
                    }
                }) { Text("초기화", fontSize = 20.sp) } },
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
