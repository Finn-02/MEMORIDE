package io.kailand.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import io.kailand.core.contract.InteractionGate
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.kailand.app.journey.NavigationDraft
import io.kailand.app.journey.NavigationDraftStore
import io.kailand.app.journey.NavigationInbox
import io.kailand.core.contract.RequestResult
import io.kailand.feature.island.BGameService
import io.kailand.feature.island.PreferencesGameSaveStore
import io.kailand.feature.island.RegionRewards
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Debug-only single launch failure, then the actual production launcher/capture and real platform gate. */
class NavigationLaunchRehearsalActivity : MainActivity() {
    private val trial by lazy { ViewModelProvider(this)[NavigationLaunchTrialModel::class.java] }
    override val gameSession get() = trial.session
    @Composable
    override fun AppContent(gate: InteractionGate) {
        val scope = rememberCoroutineScope()
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = {
                if (gameSession.canUse(gameOwner)) scope.launch {
                    gameSession.navigationCapture.reset()
                    finish()
                }
            }, enabled = gate.canInteract) { Text("시험 닫기") }
            Text("DEMO 내비 실행 시험 · 별도 저장 · 재시도는 실제 외부 내비")
            Box(Modifier.weight(1f)) { super.AppContent(gate) }
        }
    }
    override fun launchExternalNavigation(): RequestResult {
        if (!gameSession.canUse(gameOwner)) return RequestResult.Failed("현재 이용 상태를 확인해 주세요.")
        if (trial.failNext) {
            trial.failNext = false
            return RequestResult.Failed("DEMO · 내비 실행 실패 1회 · 재시도는 실제 내비 앱을 엽니다.")
        }
        return super.launchExternalNavigation()
    }
}

class NavigationLaunchTrialModel(application: Application) : AndroidViewModel(application) {
    var failNext = true
    private val file = File(application.noBackupFilesDir, "ux-nav-launch/${UUID.randomUUID()}/game.preferences_pb")
    private val inbox = NavigationInbox(object : NavigationDraftStore {
        private var draft: NavigationDraft? = null
        override suspend fun read() = draft
        override suspend fun write(draft: NavigationDraft?) { this.draft = draft }
    }, viewModelScope)
    val session = GameAppSession(application,
        suppliedGame = BGameService(PreferencesGameSaveStore(
            PreferenceDataStoreFactory.create(scope = viewModelScope) { file }), viewModelScope) {
            withContext(Dispatchers.IO) {
                application.assets.open("content/catalog.json").bufferedReader().use { RegionRewards.fromJson(it.readText()) }
            }
        }, suppliedInbox = inbox, scope = viewModelScope)
}
