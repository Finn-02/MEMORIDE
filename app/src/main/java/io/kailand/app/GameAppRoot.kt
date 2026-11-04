package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.kailand.core.contract.*
import io.kailand.core.ui.AvatarSlot
import io.kailand.app.journey.JourneyPreparationScreen
import io.kailand.feature.briefing.BriefingScreen
import io.kailand.feature.companion.CompanionScreen
import io.kailand.feature.demo.DemoPanel
import io.kailand.feature.demo.DemoScenario
import io.kailand.feature.visit.VisitRoute
import io.kailand.feature.island.*
import io.kailand.feature.world.WorldScreen
import kotlinx.coroutines.launch

private enum class GameRoute { ISLAND, WORLD, PREPARATION, COMPANION, DEMO, VISIT }
private class GameScreenLease { var active = true }

/** Real saved state, DEMO arrival and receipt-driven briefing; platform permission remains external. */
@Composable
fun GameAppRoot(gate: InteractionGate, app: GameAppSession, owner: Any, developmentEntry: Boolean) {
    val readiness by app.game.readiness.collectAsState()
    val catalogState by app.catalog.collectAsState()
    val snapshot by app.game.state.collectAsState()
    val session by app.commands.session.collectAsState()
    val mode by app.mode.collectAsState()
    // The branch disposal also closes sheets and stops B's temporary Eat feedback on restriction.
    if (!gate.canInteract) {
        GameMessage(if (gate.state == GateState.UNKNOWN) "이용 상태 미확인" else "지금은 이용할 수 없어요",
            gate.reason ?: "주차 상태와 이용 허용을 확인해 주세요.")
        return
    }
    if (readiness == GameReadiness.READ_ERROR) {
        GameMessage("게임 기록을 읽지 못했어요", "기록을 변경하지 않았습니다. 앱을 완전히 종료한 뒤 다시 실행해 주세요.")
        return
    }
    val loaded = catalogState as? CatalogLoadState.Ready
    if (catalogState == CatalogLoadState.Failed) {
        GameMessage("콘텐츠를 불러오지 못했어요", "잠시 후 다시 시도해 주세요.") {
            Button(onClick = { if (app.canUse(owner)) app.reloadCatalog() }) { Text("다시 시도") }
        }
        return
    }
    if (readiness != GameReadiness.READY || loaded == null) {
        GameMessage("나의 섬을 준비하고 있어요", "저장된 기록과 콘텐츠를 확인 중입니다.")
        return
    }
    // Keep DemoPanel alive across reset generations so its own reset acknowledgement can finish.
    SavedGameScreens(app, owner, loaded.catalog, loaded.demoCompletion, loaded.scenarios, snapshot, gate,
        session.generation, session.phase, mode, developmentEntry)
}

@Composable
private fun SavedGameScreens(
    app: GameAppSession, owner: Any, catalog: Catalog, completion: DemoInput.Complete,
    scenarios: List<DemoScenario>, snapshot: GameSnapshot, gate: InteractionGate, generation: Long,
    phase: GameCommandPhase, mode: DataSource, developmentEntry: Boolean,
) {
    var routeName by rememberSaveable {
        mutableStateOf(if (phase in setOf(GameCommandPhase.RESETTING, GameCommandPhase.RESET_FAILED))
            GameRoute.DEMO.name else GameRoute.ISLAND.name)
    }
    var selectedRegion by rememberSaveable { mutableStateOf<String?>(null) }
    var itemsOpen by remember { mutableStateOf(false) }
    var navigationVersion by remember { mutableLongStateOf(0) }
    var briefingOpen by remember { mutableStateOf(false) }
    var modeError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lease = remember(generation) { GameScreenLease() }
    DisposableEffect(lease) { onDispose { lease.active = false } }
    val route = GameRoute.valueOf(routeName)
    fun allowed() = lease.active && app.canUse(owner) && app.commands.session.value.generation == generation
    val closer = remember(app, owner, generation, lease) {
        ReceiptCloseCoordinator(app.game, app.commands, generation) { allowed() }
    }
    val heldReceipt by closer.heldReceipt.collectAsState()
    val hostReady = app.canHost(owner)
    val simulation = remember(app, owner, catalog, scenarios, hostReady) {
        app.simulation(owner, catalog, scenarios)
    }
    if (simulation == null) {
        GameMessage("활성 화면을 확인하고 있어요", "잠시 후 다시 확인해 주세요.")
        return
    }
    val journeySnapshot by simulation.journey.state.collectAsState()
    val runnerState by simulation.runner.state.collectAsState()
    LaunchedEffect(generation) { itemsOpen = false; briefingOpen = false; selectedRegion = null }
    val callbackRoute = routeName
    val callbackVersion = navigationVersion
    val demoControls = remember(simulation, callbackVersion, lease) {
        app.screenDemoControls(simulation) {
            allowed() && routeName == GameRoute.PREPARATION.name && navigationVersion == callbackVersion &&
                app.game.state.value.pendingReceipt == null && closer.heldReceipt.value == null
        }
    }
    fun navigate(target: GameRoute) {
        if (allowed()) {
            navigationVersion++
            itemsOpen = false
            routeName = target.name
        }
    }
    val onAction: suspend (GameAction) -> MutationResult = { action ->
        app.commands.apply(generation, action) {
            allowed() && routeName == callbackRoute && navigationVersion == callbackVersion &&
                (route != GameRoute.WORLD || itemsOpen)
                && app.game.state.value.pendingReceipt == null && closer.heldReceipt.value == null
        }
    }
    val receipt = snapshot.pendingReceipt ?: heldReceipt
    val demoRoute = route == GameRoute.DEMO
    SideEffect {
        simulation.setScreens(
            demo = demoRoute && phase !in setOf(GameCommandPhase.CHANGING_MODE, GameCommandPhase.MODE_FAILED),
            visit = route == GameRoute.VISIT && phase == GameCommandPhase.ACTIVE && receipt == null)
    }
    val closeToIsland: suspend () -> MutationResult = {
        if (receipt == null) MutationResult.Rejected("확인할 기록이 없어요.")
        else closer.close(receipt) { briefingOpen = false; navigate(GameRoute.ISLAND) }
    }
    val closeToItems: suspend () -> MutationResult = {
        if (receipt == null) MutationResult.Rejected("확인할 기록이 없어요.")
        else closer.close(receipt) { briefingOpen = false; navigate(GameRoute.ISLAND); itemsOpen = true }
    }
    if (phase == GameCommandPhase.ACTIVE && receipt != null && !demoRoute) {
        BriefingScreen(receipt, catalog, snapshot.avatar, gate,
            onDismiss = closeToIsland, onOpenItems = closeToItems)
        return
    }
    BackHandler(enabled = phase != GameCommandPhase.ACTIVE || (route != GameRoute.ISLAND && !itemsOpen)) {
        if (phase == GameCommandPhase.ACTIVE) navigate(GameRoute.ISLAND)
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (developmentEntry) Text("개발 화면 · 실제 로컬 게임 기록 사용", Modifier.padding(8.dp))
        Text(if (mode == DataSource.DEMO) "DEMO · 모의 여정 입력" else "PLEOS · 실제 여정 입력 대기",
            Modifier.padding(horizontal = 12.dp))
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DataSource.DEMO, DataSource.PLEOS).forEach { target ->
                FilterChip(selected = mode == target,
                    enabled = hostReady && phase in setOf(GameCommandPhase.ACTIVE, GameCommandPhase.MODE_FAILED),
                    onClick = { scope.launch {
                        modeError = null
                        modeError = when (val result = app.changeMode(owner, target)) {
                            MutationResult.Success -> null
                            is MutationResult.Rejected -> result.reason
                            is MutationResult.StorageFailure -> result.message
                        }
                    } }, label = { Text("${target.name} 모드") })
            }
            TextButton(onClick = { navigate(GameRoute.VISIT) }, enabled = allowed()) { Text("가상 방문") }
        }
        modeError?.let { Text(it, Modifier.padding(horizontal = 12.dp)) }
        if (route != GameRoute.ISLAND) {
            TextButton(onClick = { navigate(GameRoute.ISLAND) }, enabled = allowed()) { Text("섬으로 돌아가기") }
        }
        Box(Modifier.weight(1f)) {
            if (phase in setOf(GameCommandPhase.CHANGING_MODE, GameCommandPhase.MODE_FAILED)) {
                GameMessage(if (phase == GameCommandPhase.MODE_FAILED) "모드 전환을 다시 확인해 주세요" else "입력 모드를 바꾸고 있어요",
                    if (phase == GameCommandPhase.MODE_FAILED) "위에서 입력 모드를 다시 선택해 주세요." else "이전 시연과 방문을 정리하고 있어요.")
            } else if (phase != GameCommandPhase.ACTIVE && !demoRoute) {
                GameMessage("게임 기록을 정리하고 있어요", "정리가 끝날 때까지 기다려 주세요.")
            } else when (route) {
                GameRoute.DEMO -> Column(Modifier.fillMaxSize()) {
                    Text("D04 원본 반복은 브리핑 확인 전에 실행해 주세요.", Modifier.padding(horizontal = 12.dp))
                    if (receipt != null) Button(onClick = { briefingOpen = true },
                        enabled = allowed() && !runnerState.busy, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text("저장된 브리핑 보기")
                    }
                    DemoPanel(simulation.runner, gate, mode, Modifier.weight(1f))
                }
                else -> key(generation) { when (route) {
                GameRoute.ISLAND -> IslandScreen(snapshot, catalog, gate, onAction,
                    onPrepareJourney = { if (allowed()) { selectedRegion = null; navigate(GameRoute.PREPARATION) } },
                    onOpenWorld = { navigate(GameRoute.WORLD) },
                    onOpenDemo = { navigate(GameRoute.DEMO) })
                GameRoute.WORLD -> WorldScreen(snapshot, catalog, gate,
                    onSelectRegion = { id ->
                        if (allowed() && catalog.regions.count { it.id == id } == 1) {
                            selectedRegion = id; navigate(GameRoute.PREPARATION)
                        }
                    }, onOpenItems = { if (allowed()) itemsOpen = true })
                GameRoute.PREPARATION -> if (mode == DataSource.DEMO) {
                    JourneyPreparationScreen(demoControls, demoControls,
                        catalog.regions.map { DestinationSpec(it.id, it.name, it.latitude, it.longitude) },
                        gate, completion, onBack = { navigate(GameRoute.ISLAND) },
                        onOpenCompanion = { navigate(GameRoute.COMPANION) },
                        initialRegionId = selectedRegion,
                        developmentControls = { Text("DEMO 날짜·시간·거리로 실제 로컬 진행을 저장합니다.") })
                } else {
                    GameMessage("여정 준비", "실제 차량 여정 연결을 준비하고 있어요.")
                }
                GameRoute.COMPANION -> if (mode == DataSource.DEMO) {
                    val avatarSlot: AvatarSlot = { avatar, expression, modifier ->
                        AvatarView(avatar, catalog, expression, modifier)
                    }
                    CompanionScreen(journeySnapshot, snapshot.avatar, gate, avatarSlot,
                        onBack = { navigate(GameRoute.PREPARATION) }, onHideChanged = {})
                } else GameMessage("동행 화면", "먼저 여정을 준비해 주세요.")
                GameRoute.VISIT -> Column(Modifier.fillMaxSize()) {
                    TextButton(onClick = { if (allowed()) simulation.visit.restartDemo() }, enabled = allowed()) {
                        Text("방문만 다시 준비")
                    }
                    val avatarSlot: AvatarSlot = { avatar, expression, modifier ->
                        AvatarView(avatar, catalog, expression, modifier)
                    }
                    VisitRoute(simulation.visit, gate, mode, snapshot.avatar, avatarSlot,
                        onBack = { navigate(GameRoute.ISLAND) }, modifier = Modifier.weight(1f))
                }
                GameRoute.DEMO -> Unit // Kept outside generation keys above.
                } }
            }
        }
    }
    if (itemsOpen) {
        key(generation) { InventorySheet(snapshot, catalog, gate, onAction, onClose = { itemsOpen = false }) }
    }
    if (phase == GameCommandPhase.ACTIVE && demoRoute && receipt != null && briefingOpen && !runnerState.busy) {
        Dialog(onDismissRequest = { scope.launch { closeToIsland() } },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BriefingScreen(receipt, catalog, snapshot.avatar, gate,
                onDismiss = closeToIsland, onOpenItems = closeToItems)
        }
    }
}

@Composable
private fun GameMessage(title: String, message: String, content: @Composable () -> Unit = {}) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MEMORIDE", style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message)
            content()
        }
    }
}
