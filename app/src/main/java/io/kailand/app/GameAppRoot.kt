package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.ui.AvatarSlot
import io.kailand.app.journey.JourneyPreparationScreen
import io.kailand.feature.briefing.BriefingScreen
import io.kailand.feature.companion.CompanionScreen
import io.kailand.feature.island.*
import io.kailand.feature.world.WorldScreen

private enum class GameRoute { ISLAND, WORLD, PREPARATION, COMPANION, DEMO }
private class GameScreenLease { var active = true }

/** Real saved state, DEMO arrival and receipt-driven briefing; platform permission remains external. */
@Composable
fun GameAppRoot(gate: InteractionGate, app: GameAppSession, owner: Any, developmentEntry: Boolean) {
    val readiness by app.game.readiness.collectAsState()
    val catalogState by app.catalog.collectAsState()
    val snapshot by app.game.state.collectAsState()
    val session by app.commands.session.collectAsState()
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
    if (session.phase != GameCommandPhase.ACTIVE) {
        GameMessage("게임 기록을 정리하고 있어요", "정리가 끝날 때까지 기다려 주세요.")
        return
    }
    key(session.generation) {
        SavedGameScreens(app, owner, loaded.catalog, loaded.demoCompletion, snapshot, gate,
            session.generation, developmentEntry)
    }
}

@Composable
private fun SavedGameScreens(
    app: GameAppSession, owner: Any, catalog: Catalog, completion: DemoInput.Complete, snapshot: GameSnapshot,
    gate: InteractionGate, generation: Long, developmentEntry: Boolean,
) {
    var routeName by rememberSaveable { mutableStateOf(GameRoute.ISLAND.name) }
    var selectedRegion by rememberSaveable { mutableStateOf<String?>(null) }
    var itemsOpen by remember { mutableStateOf(false) }
    var navigationVersion by remember { mutableLongStateOf(0) }
    val lease = remember { GameScreenLease() }
    DisposableEffect(lease) { onDispose { lease.active = false } }
    val route = GameRoute.valueOf(routeName)
    fun allowed() = lease.active && app.canUse(owner) && app.commands.session.value.generation == generation
    val closer = remember(app, owner, generation, lease) {
        ReceiptCloseCoordinator(app.game, app.commands, generation) { allowed() }
    }
    val heldReceipt by closer.heldReceipt.collectAsState()
    val adapter = remember(app, owner, catalog, generation, developmentEntry) {
        if (developmentEntry) app.demoJourney(owner, catalog) else null
    }
    val journeySnapshot = adapter?.state?.collectAsState()?.value
    val callbackRoute = routeName
    val callbackVersion = navigationVersion
    val demoControls = remember(adapter, callbackVersion, lease) {
        adapter?.let { app.screenDemoControls(it) {
            allowed() && routeName == GameRoute.PREPARATION.name && navigationVersion == callbackVersion &&
                app.game.state.value.pendingReceipt == null && closer.heldReceipt.value == null
        } }
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
    if (receipt != null) {
        BriefingScreen(receipt, catalog, snapshot.avatar, gate,
            onDismiss = { closer.close(receipt) { navigate(GameRoute.ISLAND) } },
            onOpenItems = { closer.close(receipt) { navigate(GameRoute.ISLAND); itemsOpen = true } })
        return
    }
    BackHandler(enabled = route != GameRoute.ISLAND && !itemsOpen) {
        navigate(GameRoute.ISLAND)
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (developmentEntry) Text("개발 화면 · 실제 로컬 게임 기록 사용", Modifier.padding(8.dp))
        if (route != GameRoute.ISLAND) {
            TextButton(onClick = { navigate(GameRoute.ISLAND) }) { Text("섬으로 돌아가기") }
        }
        Box(Modifier.weight(1f)) {
            when (route) {
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
                GameRoute.PREPARATION -> if (demoControls != null) {
                    JourneyPreparationScreen(demoControls, demoControls,
                        catalog.regions.map { DestinationSpec(it.id, it.name, it.latitude, it.longitude) },
                        gate, completion, onBack = { navigate(GameRoute.ISLAND) },
                        onOpenCompanion = { navigate(GameRoute.COMPANION) },
                        initialRegionId = selectedRegion,
                        developmentControls = { Text("DEMO 날짜·시간·거리로 실제 로컬 진행을 저장합니다.") })
                } else {
                    GameMessage("여정 준비", "실제 차량 여정 연결을 준비하고 있어요.")
                }
                GameRoute.COMPANION -> if (journeySnapshot != null) {
                    val avatarSlot: AvatarSlot = { avatar, expression, modifier ->
                        AvatarView(avatar, catalog, expression, modifier)
                    }
                    CompanionScreen(journeySnapshot, snapshot.avatar, gate, avatarSlot,
                        onBack = { navigate(GameRoute.PREPARATION) }, onHideChanged = {})
                } else GameMessage("동행 화면", "먼저 여정을 준비해 주세요.")
                GameRoute.DEMO -> GameMessage("DEMO", "시연 제어 화면은 아직 연결 전입니다.")
            }
        }
    }
    if (itemsOpen) {
        InventorySheet(snapshot, catalog, gate, onAction, onClose = { itemsOpen = false })
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
