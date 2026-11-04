package io.kailand.app

import android.content.Context
import io.kailand.core.contract.*
import io.kailand.feature.island.GameReadiness
import io.kailand.feature.island.createBGameService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState
    data class Ready(val catalog: Catalog) : CatalogLoadState
    data object Failed : CatalogLoadState
}

/** Process-owned service. Activity recreation never creates another GameService or write mutex. */
class GameAppSession internal constructor(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val game = createBGameService(app, scope)
    private var activeOwner: Any? = null
    private var inputs = GameCommandContext()
    val commands = GameCommandCoordinator(game, { game.readiness.value == GameReadiness.READY }, { inputs })
    private val mutableCatalog = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Loading)
    val catalog = mutableCatalog.asStateFlow()
    private var catalogJob: Job? = null

    init { reloadCatalog() }

    fun reloadCatalog() {
        if (catalogJob?.isActive == true) return
        catalogJob = scope.launch {
            mutableCatalog.value = CatalogLoadState.Loading
            try {
                mutableCatalog.value = CatalogLoadState.Ready(withContext(Dispatchers.IO) {
                    decodeCatalog(CatalogAssets(app.assets).readCatalogJson())
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableCatalog.value = CatalogLoadState.Failed
            }
        }
    }

    /** Main-thread lifecycle input. A background entry cannot revoke a newer foreground owner. */
    fun updateHost(owner: Any, gate: InteractionGate, foreground: Boolean, mode: DataSource) {
        if (foreground) {
            activeOwner = owner
            inputs = GameCommandContext(gate, true, mode)
        } else if (activeOwner === owner) {
            inputs = inputs.copy(gate = gate, foreground = false)
            activeOwner = null
        }
    }

    fun canUse(owner: Any): Boolean = activeOwner === owner && inputs.foreground && inputs.gate.canInteract &&
        game.readiness.value == GameReadiness.READY && commands.session.value.phase == GameCommandPhase.ACTIVE
}
