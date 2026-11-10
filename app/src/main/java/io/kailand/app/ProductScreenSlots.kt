package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.ui.*
import io.kailand.feature.island.AvatarView
import io.kailand.feature.island.CharacterSelectionScreen
import io.kailand.feature.island.NewIslandScreen
import io.kailand.feature.world.WorldMapScreen
import kotlinx.coroutines.launch

/** Owner screens wired by A. PM result/save adapters remain pending PR #26 integration. */
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
    val briefing: NewBriefingSlot = BriefingRoutingAdapter,
    val save: ArrivalSaveSlot = SaveRoutingAdapter,
)

private val BriefingRoutingAdapter: NewBriefingSlot = { receipt, catalog, avatar, gate, island, select, modifier ->
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun submit(action: suspend () -> MutationResult) {
        if (!busy) scope.launch {
            busy = true
            try { error = action().message() } finally { busy = false }
        }
    }
    BackHandler { submit(island) }
    ProductColumn(modifier) {
        Text(if (receipt.kind != JourneyKind.REGION || receipt.regionId == null) "보관된 이동 기록"
            else if (receipt.discoveredRegionId != null) "새 지역을 발견했어요" else "다시 찾아왔어요",
            style = MaterialTheme.typography.headlineMedium)
        Text(if (receipt.source == DataSource.DEMO) "DEMO · 모의 이동 기록"
            else if (receipt.source == DataSource.USER_CONFIRMED) "사용자 확인 · 실제 방문 인증 아님" else "PLEOS 이동 기록")
        AvatarView(avatar, catalog, "idle", Modifier.size(160.dp))
        catalog.regions.singleOrNull { it.id == receipt.regionId }?.let { Text(it.name) }
        Text("성장 ${receipt.growthBefore} → ${receipt.growthAfter}")
        receipt.grantedItemIds.mapNotNull { id -> catalog.items.singleOrNull { it.id == id }?.name }
            .forEach { Text(it) }
        Text("이동 거리 — · 이동 시간 — · 연비 — · 누적 주행 —")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        MemoridePrimaryButton("섬으로 돌아가기", { submit(island) }, Modifier.fillMaxWidth(), enabled = gate.canInteract && !busy)
        MemorideSecondaryButton("캐릭터 선택", { submit(select) }, Modifier.fillMaxWidth(), enabled = gate.canInteract && !busy)
    }
}

private val SaveRoutingAdapter: ArrivalSaveSlot = { state, gate, retry, dismiss, modifier ->
    BackHandler { if (state is ArrivalSaveUiState.Failed && gate.canInteract) dismiss() }
    ProductColumn(modifier) {
        Text(if (state == ArrivalSaveUiState.Saving) "이동 기록을 저장하고 있어요" else "저장을 마치지 못했어요",
            style = MaterialTheme.typography.headlineMedium)
        if (state is ArrivalSaveUiState.Failed) {
            Text(state.message)
            MemoridePrimaryButton("다시 시도", retry, Modifier.fillMaxWidth(), enabled = gate.canInteract)
            MemorideSecondaryButton("닫기", dismiss, Modifier.fillMaxWidth(), enabled = gate.canInteract)
        }
    }
}

@Composable
internal fun ProductColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

private fun MutationResult.message(): String? = when (this) {
    MutationResult.Success -> null
    is MutationResult.Rejected -> reason
    is MutationResult.StorageFailure -> message
}
