package io.kailand.feature.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.core.contract.*
import io.kailand.core.ui.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A adapter must validate its current gate/foreground/generation and isolated profile at every
 * callback. RequestResult.Accepted never means prepared, app-opened, arrival or storage success.
 * C-local proposal only: not wired into AppRoot or existing DemoPanel.
 */
@Composable
fun UxDemoPanel(
    catalog: Catalog,
    gate: InteractionGate,
    developmentEnvironment: Boolean,
    isolatedEnvironment: Boolean,
    onPrepare: suspend (UxDemoRequest) -> RequestResult,
    onOpenApp: suspend (UxDemoRequest, sessionId: String) -> RequestResult,
    readEvidence: () -> UxDemoEvidence?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedName by rememberSaveable { mutableStateOf(UxDemoPreset.NEW_REGION.name) }
    var requestToken by rememberSaveable { mutableStateOf<String?>(null) }
    var preparedSession by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var operation by remember { mutableStateOf<Job?>(null) }
    var epoch by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val visible = gate.canInteract && foreground && developmentEnvironment && isolatedEnvironment
    val currentVisible by rememberUpdatedState(visible)
    val currentPrepare by rememberUpdatedState(onPrepare)
    val currentOpen by rememberUpdatedState(onOpenApp)
    val currentRead by rememberUpdatedState(readEvidence)
    val currentCatalog by rememberUpdatedState(catalog)
    fun observe(): UxDemoEvidence? = try { currentRead() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    val selected = UxDemoPreset.valueOf(selectedName)
    val request = requestToken?.let { UxDemoRequest(selected, it) }
    LaunchedEffect(visible) {
        if (!visible) {
            // Keep the request for manual evidence re-validation after using the app.
            // Never resume a suspended operation automatically.
            epoch++
            preparedSession = null
            operation?.cancel()
            operation = null
            busy = false
            message = "시연 제어 중단 · 복귀 후 준비 상태를 다시 확인해 주세요."
        }
    }
    MemorideScreen(modifier, applyInsets = false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("개발·리허설 · 네 시연", style = MaterialTheme.typography.headlineSmall)
            if (!visible) Text(gate.reason ?: "주차·이용 허용과 별도 개발 시험 환경이 필요해요.")
            else {
                Text("실제 사용자 저장을 초기화하지 않아요. 플랫폼 이용 제한은 그대로 적용돼요.")
                UxDemoPreset.entries.forEach { preset ->
                    MemorideSecondaryButton((if (selected == preset) "✓ " else "") + preset.title, {
                        if (currentVisible && !busy) {
                            selectedName = preset.name; requestToken = null; preparedSession = null; message = null
                        }
                    }, Modifier.fillMaxWidth(), enabled = !busy)
                }
                MemoridePanel(Modifier.fillMaxWidth()) {
                    Text(selected.title, style = MaterialTheme.typography.titleLarge)
                    selected.guide.forEachIndexed { i, step -> Text("${i + 1}. $step") }
                    Text("각 단계는 직접 조작합니다. 저장 중 타이머·화면 탭으로 성공을 만들지 않아요.")
                }
                MemoridePrimaryButton("별도 시험 상태 준비 요청", {
                    if (currentVisible && !busy) {
                        val next = UxDemoRequest(selected, UUID.randomUUID().toString())
                        val capturedEpoch = epoch
                        requestToken = next.requestToken; preparedSession = null; busy = true
                        operation = scope.launch {
                            try {
                                val result = currentPrepare(next)
                                if (currentVisible && epoch == capturedEpoch && requestToken == next.requestToken) message = when (result) {
                                    RequestResult.Accepted -> "요청 접수 · 준비 결과를 별도로 확인해 주세요."
                                    is RequestResult.Failed -> result.message
                                }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { if (currentVisible && epoch == capturedEpoch) message = "준비 요청 결과를 확인할 수 없어요." }
                            finally { if (epoch == capturedEpoch && requestToken == next.requestToken) { busy = false; operation = null } }
                        }
                    }
                }, Modifier.fillMaxWidth(), enabled = !busy)
                if (request != null) {
                    MemorideSecondaryButton("준비 결과 확인", {
                        if (currentVisible && !busy) {
                            val evidence = observe()
                            val check = checkUxDemoPreparation(request, evidence, currentCatalog)
                            preparedSession = if (check.verdict == UxDemoVerdict.PASS) evidence?.sessionId else null
                            message = "준비: ${check.verdict} · ${check.message}"
                        }
                    }, Modifier.fillMaxWidth(), enabled = !busy)
                    if (preparedSession != null) {
                        MemorideSecondaryButton("준비한 앱 흐름 열기", {
                            if (currentVisible && !busy) {
                                val evidence = observe()
                                val check = checkUxDemoPreparation(request, evidence, currentCatalog)
                                val session = preparedSession
                                if (check.verdict != UxDemoVerdict.PASS || session == null || evidence?.sessionId != session) {
                                    preparedSession = null; message = "시험 환경이 바뀌었어요. 준비 상태를 다시 확인해 주세요."
                                } else if (evidence.finished) {
                                    message = "이미 끝난 시연이에요. 실제 시연 결과를 확인해 주세요."
                                } else {
                                    val capturedEpoch = epoch
                                    busy = true
                                    operation = scope.launch {
                                        try {
                                            val result = currentOpen(request, session)
                                            if (currentVisible && epoch == capturedEpoch && requestToken == request.requestToken) message = when (result) {
                                                RequestResult.Accepted -> "열기 요청 접수 · 안내 순서대로 앱을 직접 조작해 주세요."
                                                is RequestResult.Failed -> result.message
                                            }
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { if (currentVisible && epoch == capturedEpoch) message = "앱 열기 결과를 확인할 수 없어요." }
                                        finally { if (epoch == capturedEpoch && requestToken == request.requestToken) { busy = false; operation = null } }
                                    }
                                }
                            }
                        }, Modifier.fillMaxWidth(), enabled = !busy)
                        MemorideSecondaryButton("실제 시연 결과 확인", {
                            if (currentVisible && !busy) {
                                val evidence = observe()
                                message = if (evidence?.sessionId != preparedSession) "시험 환경이 바뀌었어요. 다시 준비해 주세요."
                                    else checkUxDemoResult(request, evidence, currentCatalog).let { "${it.verdict} · ${it.message}" }
                            }
                        }, Modifier.fillMaxWidth(), enabled = !busy)
                    }
                }
                message?.let { Text(it) }
                MemorideTextButton("시연 제어 닫기", { if (currentVisible && !busy) onClose() }, enabled = !busy)
            }
        }
    }
}
