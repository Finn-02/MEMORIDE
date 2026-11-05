package io.kailand.feature.briefing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.ArrivalReceipt
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult
import kotlinx.coroutines.launch

/** Immutable result display. AppRoot must check current gate/foreground/ID in both callbacks. */
@Composable
fun BriefingScreen(
    receipt: ArrivalReceipt,
    catalog: Catalog,
    avatar: AvatarPreview,
    gate: InteractionGate,
    onDismiss: suspend () -> MutationResult,
    onOpenItems: suspend () -> MutationResult,
    modifier: Modifier = Modifier,
) {
    // A new receipt or gate disposes the old scope and state, even if its callback ignores cancellation.
    key(receipt, gate) {
        BriefingSession(receipt, catalog, avatar, gate, onDismiss, onOpenItems, modifier)
    }
}

@Composable
private fun BriefingSession(
    receipt: ArrivalReceipt,
    catalog: Catalog,
    avatar: AvatarPreview,
    gate: InteractionGate,
    onDismiss: suspend () -> MutationResult,
    onOpenItems: suspend () -> MutationResult,
    modifier: Modifier,
) {
    val actions = remember { BriefingActions(gate) }
    val state by actions.state.collectAsState()
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val openItems by rememberUpdatedState(onOpenItems)
    DisposableEffect(actions) { onDispose { actions.deactivate() } }
    val dispatch: (BriefingAction) -> Unit = { action ->
        scope.launch { actions.perform(action, if (action == BriefingAction.DISMISS) dismiss else openItems) }
    }
    // Always consume Back here; restricted/busy sessions cannot route around acknowledgement.
    BackHandler { dispatch(BriefingAction.DISMISS) }
    Surface(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        if (!gate.canInteract) {
            Box(Modifier.padding(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("결과 확인이 잠시 보류됐어요", style = MaterialTheme.typography.headlineSmall)
                    Text("이용 가능한 상태가 확인되면 이동 기록을 볼 수 있어요.")
                }
            }
        } else {
            val text = remember(receipt, catalog, avatar) { formatBriefing(receipt, catalog, avatar) }
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(text.source, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text.title, style = MaterialTheme.typography.headlineMedium)
                }
                item {
                    BriefingCard("함께한 친구") {
                        Text(text.character, style = MaterialTheme.typography.titleLarge)
                        Text(text.outfit?.let { "착용 · $it" } ?: "기본 모습")
                        Text(text.greeting)
                    }
                }
                if (text.elapsed != null || text.distance != null) {
                    item {
                        BriefingCard("이동 기록") {
                            text.elapsed?.let { Text("시간 · $it") }
                            text.distance?.let {
                                Text("거리 · $it")
                                Text("거리 표시는 단위를 줄여 내림한 값이에요.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    BriefingCard("섬에 남은 기록") {
                        Text(text.growth)
                        text.discovery?.let { Text(it) }
                        if (text.souvenirs.isEmpty()) {
                            Text("추가 기념품 없음")
                        } else {
                            Text("기념품 기록", style = MaterialTheme.typography.titleSmall)
                            text.souvenirs.forEach { Text("• $it") }
                            Text("이 여정에 저장된 기록이에요. 다시 보아도 새로 지급되지 않아요.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (text.notices.isNotEmpty()) {
                    item { text.notices.forEach { Text(it, color = MaterialTheme.colorScheme.error) } }
                }
                item {
                    Column(
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.error?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { state.failed?.let(dispatch) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("다시 시도") }
                        }
                        if (state.pending != null) Text("확인 내용을 처리하고 있어요…")
                        if (state.completed) Text("확인이 완료됐어요.")
                        val enabled = state.pending == null && !state.completed
                        Button(
                            onClick = { dispatch(BriefingAction.OPEN_ITEMS) },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) { Text("아이템 보기") }
                        OutlinedButton(
                            onClick = { dispatch(BriefingAction.DISMISS) },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) { Text("닫기") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
