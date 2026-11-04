package io.kailand.feature.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.core.contract.*
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Isolated debug UI sample. No AppRoot, GameService, persistence or actual gate input. */
@Preview(name = "C04 UI SAMPLE · 저장 없음", widthDp = 420, heightDp = 900, showBackground = true)
@Composable
fun DemoPanelInteractivePreview() {
    val assets = LocalContext.current.assets
    val loaded by produceState<PreviewLoad>(PreviewLoad(), assets) {
        value = withContext(Dispatchers.IO) {
            try {
                val plans = assets.open("fixtures/scenarios.json").bufferedReader(Charsets.UTF_8)
                    .use { DemoScenarios.fromJson(it.readText()) }
                val json = assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8)
                    .use { Json.parseToJsonElement(it.readText()).jsonObject }
                fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
                val catalog = Catalog(json.text("contentVersion"), json.text("description"), emptyList(),
                    emptyMap(), emptyMap(), json.getValue("regions").jsonArray.map { raw ->
                        raw.jsonObject.let { RegionDefinition(it.text("id"), it.text("name"), it.text("theme"),
                            it.getValue("latitude").jsonPrimitive.double, it.getValue("longitude").jsonPrimitive.double,
                            it.getValue("itemIds").jsonArray.map { id -> id.jsonPrimitive.content }) }
                    }, emptyList(), GameRules(3, 1, "Asia/Seoul", emptyList()))
                PreviewLoad(runner = previewRunner(plans, catalog))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { PreviewLoad(error = "${failure.javaClass.simpleName}: ${failure.message}") }
        }
    }
    MemorideTheme {
        Column(Modifier.fillMaxSize()) {
            Text("UI SAMPLE · 가짜 포트 · 실제 저장/보상 없음")
            Text("D03으로 요청→READY→출발 체험. 완료는 연결 대기로 멈춰요.")
            val runner = loaded.runner
            when {
                loaded.error != null -> Text("시연 파일을 읽지 못했어요: ${loaded.error}")
                runner == null -> Text("미리보기 불러오는 중 · Interactive 모드로 실행해 주세요.")
                else -> DemoPanel(runner, InteractionGate(GateState.PARKED_ALLOWED), DataSource.DEMO,
                    Modifier.weight(1f))
            }
        }
    }
}

private data class PreviewLoad(val runner: DemoScenarioRunner? = null, val error: String? = null)

private fun previewRunner(plans: List<DemoScenario>, catalog: Catalog): DemoScenarioRunner {
    val empty = JourneySnapshot(null, DataSource.DEMO, JourneyKind.REGION, JourneyPhase.IDLE, 0, null)
    val state = MutableStateFlow(empty)
    var sequence = 0
    val unavailable = RequestResult.Failed("UI SAMPLE: A/B 실제 연결 대기 · 저장·보상은 실행하지 않았어요.")
    val journey = object : JourneyPort {
        override val state = state
        override suspend fun requestDestination(destination: DestinationSpec, kind: JourneyKind): RequestResult {
            state.value = JourneySnapshot("ui-sample-${++sequence}", DataSource.DEMO, kind,
                JourneyPhase.READY, 0, destination)
            return RequestResult.Accepted
        }
        override suspend fun cancel(): RequestResult = unavailable
        override suspend fun retryArrival(): RequestResult = unavailable
    }
    val demo = object : DemoControlPort {
        override suspend fun dispatch(input: DemoInput): RequestResult = when (input) {
            DemoInput.StartTravel -> {
                state.value = state.value.copy(phase = JourneyPhase.TRAVELLING)
                RequestResult.Accepted
            }
            DemoInput.Cancel -> {
                state.value = state.value.copy(phase = JourneyPhase.CANCELLED)
                RequestResult.Accepted
            }
            else -> unavailable
        }
    }
    val sample = GameSnapshot(AvatarPreview("tori", null), 0, emptySet(), emptySet(), emptyMap(), null)
    return DemoScenarioRunner(plans, catalog, journey, demo, { sample }, { null }, {
        // Only this isolated fake journey is reset. No live game state exists in this preview.
        state.value = empty
        RequestResult.Accepted
    })
}
