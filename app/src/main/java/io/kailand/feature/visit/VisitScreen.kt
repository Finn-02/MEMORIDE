package io.kailand.feature.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.InteractionGate
import io.kailand.core.ui.AvatarSlot

/** C-local binding. A retains the controller and supplies current gate/mode and B's preview. */
@Composable
fun VisitRoute(
    controller: VisitController,
    gate: InteractionGate,
    mode: DataSource,
    avatar: AvatarPreview,
    avatarSlot: AvatarSlot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentGate by rememberUpdatedState(gate)
    val currentMode by rememberUpdatedState(mode)
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var feedback by remember(controller) { mutableStateOf<String?>(null) }
    val state by controller.state.collectAsStateWithLifecycle()

    fun refreshContext() {
        foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        controller.updateContext(VisitContext(currentGate, foreground, true, currentMode))
    }
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> refreshContext() }
        lifecycle.addObserver(observer)
        refreshContext()
        onDispose {
            lifecycle.removeObserver(observer)
            controller.leaveScreen()
        }
    }
    SideEffect { refreshContext() }

    VisitScreen(
        // Do not render the previous mode's visit while SideEffect cancels that session.
        state = state.copy(visitorAvatar = avatar,
            interactionAllowed = state.interactionAllowed && foreground && state.mode == mode),
        gate = gate,
        avatarSlot = avatarSlot,
        onRequest = {
            refreshContext()
            feedback = when (val result = controller.request()) {
                is VisitRequestResult.Created -> null
                is VisitRequestResult.Rejected -> result.message
            }
        },
        onAccept = { refreshContext(); controller.accept(it) },
        onReject = { refreshContext(); controller.reject(it) },
        onWithdraw = { refreshContext(); controller.withdraw(it) },
        onClose = { refreshContext(); controller.close(it) },
        onBack = { controller.leaveScreen(); onBack() },
        feedback = feedback,
        modifier = modifier,
    )
}

/** Purpose-first local simulation UI; callbacks carry the displayed request's identity. */
@Composable
fun VisitScreen(
    state: VisitState,
    gate: InteractionGate,
    avatarSlot: AvatarSlot,
    onRequest: () -> Unit,
    onAccept: (VisitToken) -> Unit,
    onReject: (VisitToken) -> Unit,
    onWithdraw: (VisitToken) -> Unit,
    onClose: (VisitToken) -> Unit,
    onBack: () -> Unit,
    feedback: String? = null,
    modifier: Modifier = Modifier,
) {
    val token = state.request?.token
    var receiverView by rememberSaveable { mutableStateOf(false) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    val currentGate by rememberUpdatedState(gate)
    val currentAllowed by rememberUpdatedState(state.interactionAllowed)
    fun permitted() = currentGate.canInteract && currentAllowed
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onBack, Modifier.heightIn(min = 56.dp)) { Text("뒤로") }
            Text("DEMO · 가상 방문", style = MaterialTheme.typography.headlineSmall)
            Text("주차 상태의 모의 체험 · 실제 차량 간 통신이 아니에요.")
            if (!gate.canInteract || !state.interactionAllowed) {
                Text("가상 방문 중단", style = MaterialTheme.typography.titleLarge)
                Text(if (state.phase == VisitPhase.CANCELLED) phaseLabel(state)
                    else "현재 요청과 방문 표시를 중단했어요.")
                Text(gate.reason ?: "이용 가능 상태와 활성 화면을 확인한 뒤 다시 요청해 주세요.")
            } else {
                Text("내 차 → 가상 앞차", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { if (permitted()) receiverView = false },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("내 차 시점") }
                    OutlinedButton(onClick = { if (permitted()) receiverView = true },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("가상 앞차 시점") }
                }
                Text(if (receiverView) "현재: 수신자 가상 앞차" else "현재: 발신자 내 차")
                Text("목적: ${VisitController.PURPOSE}")
                Text(phaseLabel(state), style = MaterialTheme.typography.titleMedium)
                feedback?.let { Text(it) }
                if (!state.active && !receiverView) {
                    Text("같은 대상 재요청은 30초 간격이에요. 거절해도 불이익이 없어요.")
                    Button(onClick = { if (permitted()) onRequest() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text("가상 방문 요청")
                    }
                }
                if (!state.active && receiverView) Text("내 차 시점에서 새 가상 방문을 요청할 수 있어요.")
                if (state.phase == VisitPhase.REQUESTED && token != null) {
                    state.remainingResponseMs?.let { Text("응답 대기: ${(it + 999) / 1_000}초 남음") }
                    Text("15초 안에 응답이 없으면 만료돼요. 무응답은 동의가 아니에요.")
                    if (receiverView) {
                        Text("뒤 차량의 캐릭터가 주행 관련 메시지를 전하고 싶어 해요. 받아보시겠어요?")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { if (permitted()) onAccept(token) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("수락") }
                            Button(onClick = { if (permitted()) onReject(token) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("거절") }
                        }
                    } else {
                        Text("요청을 보냈어요.")
                        Text("가상 앞차 시점에서 수락·거절을 체험할 수 있어요.")
                        OutlinedButton(onClick = { if (permitted()) onWithdraw(token) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text("요청 철회")
                        }
                    }
                }
                if (state.canShowVisitor(gate) && token != null) {
                    Text("가상 앞차가 메시지 열람을 수락했어요.")
                    OutlinedButton(onClick = { if (permitted()) hidden = !hidden },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(if (hidden) "캐릭터 다시 표시" else "캐릭터 숨기기")
                    }
                    Box(
                        Modifier.size(160.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.canShowVisitor(gate, hidden)) {
                            state.visitorAvatar?.let { avatarSlot(it, "idle", Modifier.fillMaxSize()) }
                                ?: Text("외형 정보 대기 중")
                        } else Text("캐릭터 숨김")
                    }
                    Text(VisitController.MESSAGE)
                    Text("메시지 열람 동의이며 운전 행동을 약속하는 것은 아니에요.")
                    Button(onClick = { if (permitted()) onClose(token) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("대화 닫기") }
                    if (!receiverView) {
                        OutlinedButton(onClick = { if (permitted()) onWithdraw(token) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("방문 철회") }
                    }
                }
            }
        }
    }
}

/** Consent and current session identity are required even when a caller supplies an avatar. */
internal fun VisitState.canShowVisitor(gate: InteractionGate, hidden: Boolean = false): Boolean =
    !hidden && interactionAllowed && gate.canInteract && phase == VisitPhase.VISITING &&
        request?.token?.generation == generation

private fun phaseLabel(state: VisitState) = when (state.phase) {
    VisitPhase.IDLE -> "가상 방문을 요청할 수 있어요"
    VisitPhase.REQUESTED -> "응답을 기다리고 있어요"
    VisitPhase.ACCEPTED -> "수락한 메시지를 열고 있어요"
    VisitPhase.VISITING -> "동의한 가상 방문 중"
    VisitPhase.REJECTED -> "요청이 거절됐어요"
    VisitPhase.EXPIRED -> "요청 시간이 만료됐어요"
    VisitPhase.WITHDRAWN -> "요청 또는 방문을 철회했어요"
    VisitPhase.CANCELLED -> when (state.cancelReason) {
        VisitCancelReason.UNKNOWN, VisitCancelReason.RESTRICTED -> "이용 제한으로 방문을 종료했어요"
        VisitCancelReason.BACKGROUND, VisitCancelReason.SCREEN_LEFT -> "화면 이탈로 방문을 종료했어요"
        VisitCancelReason.MODE_CHANGED -> "모드 전환으로 방문을 종료했어요"
        VisitCancelReason.RESET -> "초기화로 방문을 종료했어요"
        null -> "가상 방문을 종료했어요"
    }
    VisitPhase.COMPLETED -> "대화를 마쳤어요"
}
