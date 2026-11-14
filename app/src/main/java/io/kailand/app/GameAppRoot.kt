package io.kailand.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kailand.app.journey.*
import io.kailand.core.contract.*
import io.kailand.core.ui.*
import io.kailand.feature.island.GameReadiness
import io.kailand.feature.island.NewInventoryScreen
import kotlinx.coroutines.launch

/** Product routes only. Legacy demo/visit/companion/preparation routes are deliberately absent. */
@Composable
fun GameAppRoot(
    gate: InteractionGate, app: GameAppSession, owner: Any, developmentEntry: Boolean,
    navigation: ProductNavigation, screens: ProductScreenSlots? = null,
    launchNavigation: suspend () -> RequestResult = { RequestResult.Failed("내비 앱을 직접 열어 주세요.") },
) {
    // Retain feature saveable UI across temporary platform blocking, outside the gated subtree.
    val savedUi = rememberSaveableStateHolder()
    val readiness by app.game.readiness.collectAsState()
    val catalogState by app.catalog.collectAsState()
    val snapshot by app.game.state.collectAsState()
    val history by app.game.regionVisitHistory.collectAsState()
    val session by app.commands.session.collectAsState()
    val reset by app.demoResetState.collectAsState()
    val observedScreen by navigation.state.collectAsState()
    val resetScreen = observedScreen // Freeze the character route for asynchronous reset admission.
    val rootScope = rememberCoroutineScope()
    val resetCurrent = { navigation.current(resetScreen) && resetScreen.route == ProductRoute.CHARACTER && !navigation.busy.value }
    val performReset: suspend () -> MutationResult = {
        app.resetDemoFromCharacter(owner, resetCurrent) { navigation.resetAfterDemo() }
    }
    var previousRevision by remember { mutableStateOf(reset.revision) }
    LaunchedEffect(reset.revision) {
        if (previousRevision != reset.revision) {
            savedUi.removeState("product:$previousRevision")
            previousRevision = reset.revision
        }
    }
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
            MemorideSecondaryButton("다시 시도", { if (app.canUse(owner)) app.reloadCatalog() })
        }
        return
    }
    if (readiness != GameReadiness.READY || loaded == null) {
        GameMessage("나의 섬을 준비하고 있어요", "저장된 기록과 콘텐츠를 확인 중입니다.")
        return
    }
    if (reset.busy || reset.error != null) {
        GameMessage(if (reset.busy) "시연용 기록을 초기화하고 있어요" else "초기화를 마치지 못했어요",
            reset.error ?: "저장된 게임 기록과 내비 목적지를 정리하고 있습니다.") {
            if (!reset.busy) MemorideSecondaryButton("초기화 다시 시도", {
                rootScope.launch { performReset() }
            })
        }
        return
    }
    if (session.phase != GameCommandPhase.ACTIVE) {
        GameMessage("이용 상태를 정리하고 있어요", "잠시 후 다시 확인해 주세요.")
        return
    }
    savedUi.SaveableStateProvider("product:${reset.revision}") {
        ProductScreens(app, owner, loaded.catalog, snapshot, history, gate, session.generation,
            developmentEntry, navigation, screens ?: productScreenSlots(snapshot, history, onDemoReset = performReset), launchNavigation)
    }
}

@Composable
private fun ProductScreens(
    app: GameAppSession, owner: Any, catalog: Catalog, snapshot: GameSnapshot, history: RegionVisitHistory,
    gate: InteractionGate, generation: Long, developmentEntry: Boolean,
    navigation: ProductNavigation, screens: ProductScreenSlots,
    launchNavigation: suspend () -> RequestResult,
) {
    val observedScreen by navigation.state.collectAsState()
    val screen = observedScreen // Freeze the lease; delegated State getters would refresh stale callbacks.
    val selecting by navigation.busy.collectAsState()
    val scope = rememberCoroutineScope()
    val lease = remember(generation) { ProductScreenLease() }
    DisposableEffect(lease) { onDispose { lease.active = false } }
    fun active() = lease.active && app.canUse(owner) && app.commands.session.value.generation == generation
    fun current() = active() && navigation.current(screen)
    val closer = remember(app, owner, generation, lease) {
        ReceiptCloseCoordinator(app.game, app.commands, generation) { active() }
    }
    val heldReceipt by closer.heldReceipt.collectAsState()
    val receipt = snapshot.pendingReceipt ?: heldReceipt
    SideEffect {
        val latest = app.game.state.value.pendingReceipt ?: closer.heldReceipt.value
        if (latest != null) navigation.presentReceipt(latest.journeyId) else navigation.receiptAbsent()
    }
    fun noReceipt() = app.game.state.value.pendingReceipt == null && closer.heldReceipt.value == null
    fun open(target: ProductRoute) {
        if (current() && noReceipt()) navigation.open(screen, target)
    }
    val onAction: suspend (GameAction) -> MutationResult = { action ->
        app.commands.apply(generation, action) {
            current() && noReceipt() && screen.route == ProductRoute.INVENTORY
        }
    }
    val select: suspend (String) -> MutationResult = { id ->
        navigation.selectCharacter(screen, {
            current() && noReceipt() && app.commands.canRoute(generation) { current() }
        }) {
            if (catalog.characters.count { it.id == id } != 1) MutationResult.Rejected("캐릭터를 다시 확인해 주세요.")
            else app.commands.apply(generation, GameAction.SelectCharacter(id)) { current() && noReceipt() }
        }
    }
    fun closeResult(target: ProductRoute): suspend () -> MutationResult = {
        val displayed = receipt
        if (displayed == null) MutationResult.Rejected("확인할 기록이 없어요.")
        else closer.close(displayed, { current() && screen.route == ProductRoute.BRIEFING }) {
            app.finishManualReceipt(displayed.journeyId)
            navigation.acknowledged(screen, displayed.journeyId, target)
        }
    }
    val inbox by app.navigationInbox.state.collectAsState()
    val connection by app.navigationCapture.status.collectAsState()
    val manual = app.manualJourney(owner, catalog)
    val manualState = manual?.state?.collectAsState()?.value
    val saving = manual?.busy?.collectAsState()?.value ?: false
    val saveResult = manual?.result?.collectAsState()?.value
    var error by remember { mutableStateOf<String?>(null) }
    fun show(result: RequestResult) { error = (result as? RequestResult.Failed)?.message }
    fun depart() {
        if (current() && noReceipt()) scope.launch {
            show(navigation.depart(screen, { current() && noReceipt() }, launchNavigation))
        }
    }
    fun finish(draft: NavigationDraft) {
        if (!current() || !noReceipt() || !app.navigationInbox.current(draft) ||
            draft.destination(catalog).regionId == null) return
        scope.launch {
            val switched = app.changeMode(owner, DataSource.USER_CONFIRMED)
            // changeMode intentionally advances generation. Recheck the owner and the original screen.
            if (switched != MutationResult.Success || !app.canUse(owner) || !navigation.current(screen) ||
                !noReceipt() || !app.navigationInbox.current(draft)) return@launch
            val runtime = app.manualJourney(owner, catalog) ?: return@launch
            val adopted = app.adoptNavigation(owner, catalog, runtime, draft) {
                app.canUse(owner) && navigation.current(screen) && noReceipt()
            }
            show(adopted)
            if (adopted == RequestResult.Accepted && navigation.open(screen, ProductRoute.CONFIRMATION)) {
                val questionScreen = navigation.state.value
                runtime.requestReview { app.canUse(owner) && navigation.current(questionScreen) && noReceipt() }
            }
        }
    }
    val draft = inbox.draft
    val usable = draft != null && app.navigationInbox.current(draft) &&
        draft.destination(catalog).regionId != null
    LaunchedEffect(draft, usable, screen) {
        if (usable && current() && noReceipt()) {
            navigation.destinationObserved("${draft.id}:${draft.revision}")
        }
    }
    // Presentation only: preserve a saved destination card while the existing refresh is in flight.
    // Usability/currentness and every action permission below retain their original checks.
    val waitingDestinationLabel = draft?.takeIf {
        it.usable(System.currentTimeMillis())
    }?.let { saved ->
        saved.label.takeIf { it.isNotBlank() }
            ?: catalog.regions.singleOrNull { it.id == saved.destination(catalog).regionId }?.name?.takeIf { it.isNotBlank() }
    }
    val failedSave = manualState?.phase == ManualJourneyPhase.CONFIRMED &&
        (saveResult is ArrivalResult.StorageFailure || saveResult is ArrivalResult.Rejected)
    // An empty inbox is the ordinary home, including immediately after demo reset.
    val destination = if (failedSave) IslandDestinationSlot(true) {
        MemoridePrimaryButton("저장 다시 시도", { open(ProductRoute.SAVE) }, Modifier.fillMaxWidth(), enabled = current())
    } else if (draft != null && draft.status != NavigationDraftStatus.CONSUMED) {
        IslandDestinationSlot(usable, destinationLabel = waitingDestinationLabel) {
            NavigationDraftCard(inbox, connection, current(),
                onRefresh = { if (current() && noReceipt()) app.navigationCapture.refresh() }, onFinish = ::finish, canFinish = usable,
                waitingLabel = waitingDestinationLabel, snapshot = snapshot, catalog = catalog)
        }
    } else null

    BackHandler {
        when {
            receipt != null -> scope.launch { closeResult(ProductRoute.ISLAND)() }
            selecting || saving -> Unit
            screen.route == ProductRoute.CHARACTER -> Unit // cannot bypass required selection
            screen.route == ProductRoute.CONFIRMATION -> {
                manualState?.confirmation?.let { question -> scope.launch {
                    show(manual?.choose(question.token, ManualJourneyChoice.CONTINUE, ::current)
                        ?: RequestResult.Failed("목적지를 다시 확인해 주세요."))
                    open(ProductRoute.ISLAND)
                } }
            }
            else -> open(ProductRoute.ISLAND)
        }
    }
    val confirmationVisible = receipt == null && screen.route == ProductRoute.CONFIRMATION
    val question = manualState?.confirmation
    val continueJourney: () -> Unit = {
        if (question == null) open(ProductRoute.ISLAND)
        else scope.launch {
            val answer = manual?.choose(question.token, ManualJourneyChoice.CONTINUE, ::current)
            if (answer != null) show(answer)
            if (answer == RequestResult.Accepted) open(ProductRoute.ISLAND)
        }
    }
    val visualRoute = when {
        receipt != null -> ProductRoute.BRIEFING
        screen.route == ProductRoute.CONFIRMATION -> ProductRoute.ISLAND
        else -> screen.route
    }
    val visualKey: Any? = receipt?.journeyId
        ?: if (visualRoute == ProductRoute.ISLAND) waitingDestinationLabel != null else null
    MemorideScreen {
        Column(Modifier.fillMaxSize().then(memorideModalBackdrop(confirmationVisible))) {
            if (developmentEntry) Text("개발 화면 · 실제 로컬 게임 기록 사용", Modifier.padding(8.dp))
            ProductScreenTransition(visualRoute, visualKey, Modifier.weight(1f)) {
                if (receipt != null) {
                    // Existing pending receipts take precedence over cold-start selection; never discard them.
                    key(receipt.journeyId) {
                        screens.briefing(receipt, catalog, snapshot.avatar, gate,
                            closeResult(ProductRoute.ISLAND), closeResult(ProductRoute.CHARACTER), Modifier)
                    }
                } else when (screen.route) {
                    ProductRoute.CHARACTER -> screens.character(snapshot, catalog, gate, select, Modifier)
                    ProductRoute.ISLAND -> screens.island(snapshot, catalog, gate, destination,
                        { open(ProductRoute.CHARACTER) }, ::depart,
                        { open(ProductRoute.INVENTORY) }, { open(ProductRoute.WORLD) }, Modifier)
                    ProductRoute.WORLD -> screens.world(snapshot, catalog,
                        history,
                        gate, { open(ProductRoute.ISLAND) }, Modifier)
                    ProductRoute.INVENTORY -> {
                        NewInventoryScreen(snapshot, catalog, gate, onAction, { open(ProductRoute.ISLAND) })
                    }
                    ProductRoute.NAVIGATION_FAILURE -> ProductColumn {
                        Text("내비를 직접 열어 주세요", style = MaterialTheme.typography.headlineMedium)
                        Text("현재 앱에서 내비를 여는 연결을 사용할 수 없어요. 내비 앱을 직접 이용한 뒤 주차 후 MEMORIDE를 열어 주세요.")
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        MemoridePrimaryButton("다시 시도", ::depart, Modifier.fillMaxWidth(), enabled = current() && !selecting)
                        MemorideSecondaryButton("섬으로 돌아가기", { open(ProductRoute.ISLAND) }, Modifier.fillMaxWidth())
                    }
                    ProductRoute.CONFIRMATION -> screens.island(snapshot, catalog, gate, destination,
                        { open(ProductRoute.CHARACTER) }, ::depart,
                        { open(ProductRoute.INVENTORY) }, { open(ProductRoute.WORLD) }, Modifier)
                    ProductRoute.SAVE -> screens.save(
                        if (saving) ArrivalSaveUiState.Saving else ArrivalSaveUiState.Failed(
                            "저장을 마치지 못했어요. 최초 확인 내용으로 다시 시도해 주세요."), gate,
                        { if (current() && manual != null && !manual.busy.value) scope.launch { show(manual.retry(::current)) } },
                        { if (manual?.busy?.value != true) open(ProductRoute.ISLAND) }, Modifier)
                    ProductRoute.BRIEFING -> ProductColumn {
                        Text("기록 확인을 마쳤어요")
                        MemorideSecondaryButton("섬으로 돌아가기", { open(ProductRoute.ISLAND) })
                    }
                }
            }
        }
        ArrivalConfirmationPopup(
            visible = confirmationVisible, destination = question?.destination, catalog = catalog, snapshot = snapshot,
            enabled = current() && question != null && manual?.isNavigationJourney == true,
            saving = saving, error = error, onBack = continueJourney,
            onContinue = {
                if (question != null && manual != null) scope.launch {
                    val answer = manual.choose(question.token, ManualJourneyChoice.CONTINUE, ::current)
                    show(answer)
                    if (answer == RequestResult.Accepted) open(ProductRoute.ISLAND)
                }
            },
            onArrive = {
                if (question != null && manual != null && current() && navigation.open(screen, ProductRoute.SAVE)) {
                    val savingScreen = navigation.state.value
                    scope.launch { show(manual.choose(question.token, ManualJourneyChoice.ARRIVED) {
                        active() && navigation.current(savingScreen) && noReceipt()
                    }) }
                }
            },
        )
    }
}

private class ProductScreenLease { var active = true }

@Composable
private fun GameMessage(title: String, message: String, content: @Composable () -> Unit = {}) {
    MemorideScreen {
        ProductColumn {
            Text("MEMORIDE", style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message)
            content()
        }
    }
}
