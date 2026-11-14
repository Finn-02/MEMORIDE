package io.kailand.feature.briefing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.GateState
import io.kailand.core.ui.ArrivalSaveSlot
import io.kailand.core.ui.ArrivalSaveUiState
import io.kailand.core.ui.MemorideBrushes
import io.kailand.core.ui.MemorideColors
import io.kailand.core.ui.MemorideDimens
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideScreen
import io.kailand.core.ui.MemorideTextButton
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** UX-P02 S06. Pure display: no timer, tap-to-succeed, storage or retry command lives here.
 * A owns the state, retries the original command and routes after dismiss. [Failed.message]
 * is A's factual description; this screen does not claim a reward was or was not granted.
 */
@Composable
fun ArrivalSaveScreen(
    state: ArrivalSaveUiState,
    gate: InteractionGate,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    applyInsets: Boolean = true,
) {
    val guard = remember { SaveStatusInputGuard() }
    var locked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentGate by rememberUpdatedState(gate)
    // A newly observed state or gate always re-arms input.
    LaunchedEffect(state, gate) { guard.release(); locked = false }
    val submit: (() -> Unit) -> Unit = { callback ->
        if (guard.tryEnter(currentGate.canInteract)) {
            locked = true
            try {
                callback()
            } finally {
                // Normally A's next state (Saving) releases via LaunchedEffect above. If an identical
                // Failed is conflated and no change is observed, re-arm input only (never success).
                val entry = guard.generation
                scope.launch {
                    delay(INPUT_REARM_FALLBACK_MS)
                    guard.releaseIf(entry)
                    locked = guard.busy
                }
            }
        }
    }
    val enabled = gate.canInteract && !locked
    BackHandler {
        // Saving: consume Back so the pending save is not abandoned silently.
        if (state is ArrivalSaveUiState.Failed) submit(onDismiss)
    }
    MemorideScreen(modifier, applyInsets) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= MemorideDimens.WideBreakpoint && maxWidth > maxHeight
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.widthIn(max = if (wide) 560.dp else 520.dp).fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (state) {
                        ArrivalSaveUiState.Saving -> {
                            StaticProgress()
                            StatusText("여정 결과를 저장하고 있어요", "잠시만 기다려 주세요.")
                        }
                        is ArrivalSaveUiState.Failed -> {
                            AlertMark()
                            StatusText("결과 저장을 마치지 못했어요",
                                state.message.ifBlank { "저장 결과를 확인하지 못했어요. 다시 시도해 주세요." })
                        }
                    }
                    if (!gate.canInteract) Text("이용 가능한 상태가 확인되면 다시 시도할 수 있어요.",
                        color = MemorideColors.TextSecondary, textAlign = TextAlign.Center)
                }
                if (state is ArrivalSaveUiState.Failed) {
                    Column(
                        Modifier.widthIn(max = if (wide) 480.dp else 440.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MemoridePrimaryButton(
                            text = "다시 시도",
                            onClick = { submit(onRetry) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            supportingText = "같은 여정 내용으로 다시 저장",
                        )
                        MemorideTextButton(
                            text = "닫기",
                            onClick = { submit(onDismiss) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                        )
                    }
                }
            }
        }
    }
}

/** Input re-arm only; it never completes, fails or retries a save. */
internal const val INPUT_REARM_FALLBACK_MS = 1_000L

/** Adapter for A's AppRoot wiring. */
val ArrivalSaveScreenSlot: ArrivalSaveSlot = { state, gate, retry, dismiss, modifier ->
    ArrivalSaveScreen(state, gate, retry, dismiss, modifier)
}

@Composable
private fun StatusText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium.koreanPhrases(), textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() })
    Text(body, style = MaterialTheme.typography.bodyLarge.koreanPhrases(), color = MemorideColors.TextSecondary,
        textAlign = TextAlign.Center)
}

/** Fixed (non-animated) progress mark per S06: empty ring with a gradient arc. */
@Composable
private fun StaticProgress() {
    Canvas(Modifier.size(96.dp).padding(8.dp)) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(MemorideColors.ProgressEmpty, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(MemorideBrushes.Progress, -90f, 250f, false, Offset(inset, inset), arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun AlertMark() {
    Box(Modifier.size(96.dp).padding(8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawCircle(MemorideColors.ChipBackground) }
        Text("!", style = MaterialTheme.typography.headlineLarge, color = MemorideColors.ChipText)
    }
}

private val previewGate = InteractionGate(GateState.PARKED_ALLOWED)

@Preview(name = "S06 saving wide", widthDp = 1280, heightDp = 720)
@Preview(name = "S06 saving narrow", widthDp = 640, heightDp = 720)
@Preview(name = "S06 saving portrait", widthDp = 720, heightDp = 1280)
@Composable
private fun ArrivalSavingPreview() {
    MemorideTheme { ArrivalSaveScreen(ArrivalSaveUiState.Saving, previewGate, {}, {}) }
}

@Preview(name = "S06 failed wide", widthDp = 1280, heightDp = 720)
@Preview(name = "S06 failed split", widthDp = 841, heightDp = 1210)
@Preview(name = "S06 failed narrow", widthDp = 640, heightDp = 720)
@Preview(name = "S06 failed phone large text", widthDp = 411, heightDp = 891, fontScale = 2f)
@Composable
private fun ArrivalFailedPreview() {
    MemorideTheme {
        ArrivalSaveScreen(ArrivalSaveUiState.Failed("저장 결과를 확인하지 못했어요. 같은 여정으로 다시 시도해 주세요."),
            previewGate, {}, {})
    }
}
