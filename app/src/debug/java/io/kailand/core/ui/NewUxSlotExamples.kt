package io.kailand.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.kailand.core.contract.*
import kotlinx.coroutines.launch

/** Compile-checked consumer skeletons. Replace these with owner screens; never use as live UI.
 * Inputs come from A. No feature imports, SDK access or save reads/writes.
 */
val CharacterSelectionExample: CharacterSelectionSlot = { snapshot, catalog, gate, onSelect, modifier ->
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier) {
        Text("B · 캐릭터 선택 연결 예제")
        catalog.characters.forEach { character ->
            MemorideSecondaryButton(
                text = character.name + if (character.id == snapshot.avatar.characterId) " · 선택됨" else "",
                enabled = gate.canInteract && !busy,
                onClick = {
                    if (!busy) scope.launch {
                        busy = true
                        try {
                            // A saves SelectCharacter and navigates only after Success.
                            error = when (val result = onSelect(character.id)) {
                                MutationResult.Success -> null
                                is MutationResult.Rejected -> result.reason
                                is MutationResult.StorageFailure -> result.message
                            }
                        } finally { busy = false }
                    }
                },
            )
        }
        error?.let { Text(it) }
    }
}

val IslandExample: NewIslandSlot = { _, _, gate, destination, select, start, inventory, world, modifier ->
    Column(modifier) {
        Text("B · 섬 연결 예제")
        destination?.content?.invoke()
        if (destination?.replacesActions != true) {
            MemorideSecondaryButton("캐릭터 선택", select, enabled = gate.canInteract)
            MemoridePrimaryButton("여정 출발", start, enabled = gate.canInteract)
            MemorideSecondaryButton("인벤토리", inventory, enabled = gate.canInteract)
            MemorideSecondaryButton("월드맵·도감", world, enabled = gate.canInteract)
        }
    }
}

val WorldExample: NewWorldSlot = { _, _, history, gate, close, modifier ->
    Column(modifier) {
        Text("C · 월드맵 연결 예제")
        Text(when (history) {
            RegionVisitHistory.Loading -> "기록 확인 중"
            is RegionVisitHistory.Unavailable -> history.message
            is RegionVisitHistory.Available -> "기록 있는 지역 ${history.regions.size}개"
        })
        MemorideTextButton("닫기", close, enabled = gate.canInteract)
    }
}

val ArrivalSaveExample: ArrivalSaveSlot = { state, gate, retry, dismiss, modifier ->
    MemoridePanel(modifier.padding(MemorideDimens.ScreenPadding)) {
        when (state) {
            ArrivalSaveUiState.Saving -> Text("여정 결과를 저장하고 있어요")
            is ArrivalSaveUiState.Failed -> {
                Text(state.message)
                MemoridePrimaryButton("다시 시도", retry, Modifier.fillMaxWidth(), gate.canInteract)
                MemorideTextButton("닫기", dismiss, enabled = gate.canInteract)
            }
        }
    }
}

val BriefingExample: NewBriefingSlot = { receipt, _, _, gate, island, select, modifier ->
    var busy by remember(receipt.journeyId) { mutableStateOf(false) }
    var error by remember(receipt.journeyId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun submit(action: suspend () -> MutationResult) {
        if (!busy && gate.canInteract) scope.launch {
            busy = true
            try {
                // The callback includes acknowledgement. Failure must leave this result visible.
                error = when (val result = action()) {
                    MutationResult.Success -> null
                    is MutationResult.Rejected -> result.reason
                    is MutationResult.StorageFailure -> result.message
                }
            } finally { busy = false }
        }
    }
    Column(modifier) {
        Text("PM · 결과 연결 예제 · ${receipt.source}")
        Text("거리·시간·연비·누적 주행: 미측정 값은 —")
        MemoridePrimaryButton("섬으로 가기", { submit(island) }, enabled = gate.canInteract && !busy)
        MemorideSecondaryButton("캐릭터 선택", { submit(select) }, enabled = gate.canInteract && !busy)
        error?.let { Text(it) }
    }
}
