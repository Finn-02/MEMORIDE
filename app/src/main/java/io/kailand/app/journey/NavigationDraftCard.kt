package io.kailand.app.journey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationDraftCard(state: NavigationInboxState, connection: String, enabled: Boolean,
    onRefresh: () -> Unit, onFinish: (NavigationDraft) -> Unit, canFinish: Boolean = true) {
    val draft = state.draft
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("내비 목적지 · ${draft?.label ?: "수신 대기"}", style = MaterialTheme.typography.titleMedium)
            Text(state.error ?: if (state.saving) "목적지를 보관하고 있어요…" else when (draft?.status) {
                NavigationDraftStatus.CANCELLED -> "안내가 취소됐어요. 이 목적지로 완료하지 않습니다."
                NavigationDraftStatus.CHANGING -> "변경된 목적지를 확인하고 있어요."
                NavigationDraftStatus.CONSUMED -> "이미 마무리한 목적지예요."
                NavigationDraftStatus.NEEDS_REVIEW -> "마지막 목적지를 보관했어요. 도착 여부를 직접 확인해 주세요."
                NavigationDraftStatus.ACTIVE -> if (draft.usable(System.currentTimeMillis()))
                    "목적지 정보만 보관했어요. 주차 후 이동을 마무리해 주세요." else "오래된 목적지예요. 다시 조회해 주세요."
                null -> connection
            })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onRefresh, enabled = enabled && state.ready && !state.saving) { Text("다시 조회") }
                if (draft != null) Button(onClick = { onFinish(draft) },
                    enabled = enabled && canFinish && state.ready && !state.saving && state.error == null && draft.usable(System.currentTimeMillis())) {
                    Text("이동 마무리")
                }
            }
        }
    }
}
