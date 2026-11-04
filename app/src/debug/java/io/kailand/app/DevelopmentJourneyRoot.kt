package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kailand.app.journey.DemoJourneyAdapter
import io.kailand.app.journey.JourneyPreparationScreen
import io.kailand.core.contract.*
import io.kailand.feature.companion.CompanionScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Only DevelopmentActivity installs this in-memory sample; release/real entry never do. */
@Composable
internal fun DevelopmentJourneyRoot(gate: InteractionGate, model: DevelopmentJourneyModel) {
    val assets = LocalContext.current.assets
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestGate by rememberUpdatedState(gate)
    var data by remember { mutableStateOf<JourneySampleData?>(null) }
    var loadError by remember { mutableStateOf(false) }
    LaunchedEffect(assets) {
        try {
            data = withContext(Dispatchers.IO) {
                val catalog = Json.parseToJsonElement(assets.open("content/catalog.json").bufferedReader().use { it.readText() }).jsonObject
                val destinations = catalog.getValue("regions").jsonArray.map { value ->
                    val region = value.jsonObject
                    DestinationSpec(region.getValue("id").jsonPrimitive.content, region.getValue("name").jsonPrimitive.content,
                        region.getValue("latitude").jsonPrimitive.double, region.getValue("longitude").jsonPrimitive.double)
                }
                val scenarios = Json.parseToJsonElement(assets.open("fixtures/scenarios.json").bufferedReader().use { it.readText() })
                    .jsonObject.getValue("scenarios").jsonArray
                val arrival = scenarios.first { it.jsonObject.getValue("id").jsonPrimitive.content == "D03" }
                    .jsonObject.getValue("arrival").jsonObject
                JourneySampleData(destinations, DemoInput.Complete(arrival.getValue("completedAtEpochMs").jsonPrimitive.long,
                    arrival.getValue("elapsedSeconds").jsonPrimitive.longOrNull, arrival.getValue("distanceMeters").jsonPrimitive.doubleOrNull))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = true }
    }
    val sample = data
    if (sample == null) {
        Surface(Modifier.fillMaxSize()) {
            Text(if (loadError) "여정 샘플을 불러오지 못했어요." else "여정 샘플 준비 중…", Modifier.safeDrawingPadding().padding(24.dp))
        }
        return
    }
    model.owner = lifecycle
    model.permittedNow = { latestGate.canInteract && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
    DisposableEffect(model, lifecycle) {
        onDispose { if (model.owner === lifecycle) model.permittedNow = { false } }
    }
    val adapter = model.journey(sample)
    val snapshot by adapter.state.collectAsState()
    AppRoot(gate, developmentEntry = true,
        journeyScreen = { back, companion ->
            JourneyPreparationScreen(adapter, adapter, sample.destinations, gate,
                if (model.missingMeasurements) sample.completion.copy(elapsedSeconds = null, distanceMeters = null) else sample.completion,
                back, companion, developmentControls = {
                    HorizontalDivider()
                    Text("A02 개발 샘플 · 실제 저장/보상 없음", style = MaterialTheme.typography.labelLarge)
                    Text("완료 시각·거리·시간은 fixture의 모의 값입니다.", style = MaterialTheme.typography.bodySmall)
                    Row {
                        Checkbox(model.failNext, { model.failNext = it })
                        Text("다음 모의 저장 실패")
                    }
                    Row {
                        Checkbox(model.missingMeasurements, { model.missingMeasurements = it })
                        Text("시간·거리 측정 없음")
                    }
                    Text("직전 모의 결과 · ${model.boundary.message}")
                })
        },
        companionScreen = { back ->
            BackHandler { back() }
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text("A02 개발 샘플 · 실제 저장/보상 없음", Modifier.padding(12.dp))
                CompanionScreen(snapshot, AvatarPreview("tori", null), gate,
                    avatarSlot = { _, _, modifier -> Box(modifier) { Text("임시 외형 · 토리") } },
                    onBack = back, onHideChanged = {}, modifier = Modifier.weight(1f))
            }
        })
}

internal data class JourneySampleData(val destinations: List<DestinationSpec>, val completion: DemoInput.Complete)

/** Retains the debug session across configuration changes; process death starts a new journey. */
internal class DevelopmentJourneyModel : ViewModel() {
    var owner: Lifecycle? = null
    var permittedNow: () -> Boolean = { false }
    var failNext by mutableStateOf(false)
    var missingMeasurements by mutableStateOf(false)
    val boundary = DevelopmentArrivalBoundary()
    private var adapter: DemoJourneyAdapter? = null

    fun journey(sample: JourneySampleData): DemoJourneyAdapter = adapter ?: DemoJourneyAdapter(
        viewModelScope, sample.destinations.mapNotNull { it.regionId }.toSet(),
        { permittedNow() },
        { command ->
            val shouldFail = failNext
            failNext = false
            delay(750)
            if (shouldFail) ArrivalResult.StorageFailure("Explicit debug sample failure") else boundary.complete(command)
        }, prepare = { delay(400) },
    ).also { adapter = it }
}

/** Simulates acknowledgement only. No growth, item policy, GameService or disk writes. */
internal class DevelopmentArrivalBoundary {
    private val receipts = mutableMapOf<String, ArrivalReceipt>()
    var message by mutableStateOf("모의 도착 처리 전")
        private set

    fun complete(command: ArrivalCommand): ArrivalResult {
        val original = receipts[command.journeyId]
        if (original != null) {
            message = "원본 모의 영수증 반환 · 같은 여정 ID · 실제 지급 없음"
            return ArrivalResult.AlreadyApplied(original)
        }
        val receipt = ArrivalReceipt(command.journeyId, command.source, command.kind, command.regionId,
            command.completedAtEpochMs, command.elapsedSeconds, command.distanceMeters, 0, 0, null, emptyList())
        receipts[command.journeyId] = receipt
        message = "모의 도착 처리 완료 · 실제 저장/지급 없음"
        return ArrivalResult.Applied(receipt)
    }
}
