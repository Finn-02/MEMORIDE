package io.kailand.feature.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.GameAction
import io.kailand.core.contract.GameSnapshot
import io.kailand.core.contract.InteractionGate
import io.kailand.core.contract.MutationResult

/** The caller renders this after game readiness and keys it by the active game generation. */
@Composable
fun IslandScreen(
    snapshot: GameSnapshot,
    catalog: Catalog,
    gate: InteractionGate,
    onAction: suspend (GameAction) -> MutationResult,
    onPrepareJourney: () -> Unit,
    onOpenWorld: () -> Unit,
    onOpenDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = remember(snapshot, catalog) { islandPresentation(snapshot, catalog) }
    val actions = rememberIslandActions(snapshot, view, gate, onAction)
    var inventoryOpen by remember { mutableStateOf(false) }
    val enabled = actions.enabled && !actions.busy && !inventoryOpen
    val fontScale = LocalDensity.current.fontScale

    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 640.dp && fontScale <= 1.2f
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("나의 섬", style = MaterialTheme.typography.headlineSmall)
                IslandFeedback(actions, gate)
                if (view.hasCatalogIssues) Text("일부 콘텐츠 정보를 확인할 수 없어요.")

                val scene: @Composable (Modifier) -> Unit = { sceneModifier ->
                    Column(sceneModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IslandAvatar(snapshot.avatar, catalog, view.characterName, actions.eating)
                        Text("의상 · ${view.outfitStatus}")
                        IslandGrowthCard(view.growth)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("섬 장식", style = MaterialTheme.typography.titleMedium)
                                view.slots.forEach { slot -> Text("${slot.label} · ${slot.status}") }
                            }
                        }
                    }
                }
                val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
                    Column(controlsModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("캐릭터 선택", style = MaterialTheme.typography.titleMedium)
                        if (view.characters.isEmpty()) Text("선택할 수 있는 캐릭터가 없어요.")
                        view.characters.forEach { character ->
                            OutlinedButton(
                                onClick = {
                                    if (!inventoryOpen) actions.submit(GameAction.SelectCharacter(character.id))
                                },
                                enabled = enabled && !character.selected,
                                modifier = Modifier.islandTouchTarget(),
                            ) { Text(if (character.selected) "${character.name} · 선택됨" else character.name) }
                        }
                        Button(
                            onClick = {
                                if (actions.enabled && !actions.busy && !inventoryOpen) inventoryOpen = true
                            },
                            enabled = enabled,
                            modifier = Modifier.islandTouchTarget(),
                        ) { Text("인벤토리 열기") }
                        IslandNavigation("여정 준비", enabled) {
                            if (actions.enabled && !actions.busy && !inventoryOpen) onPrepareJourney()
                        }
                        IslandNavigation("월드맵", enabled) {
                            if (actions.enabled && !actions.busy && !inventoryOpen) onOpenWorld()
                        }
                        IslandNavigation("DEMO", enabled) {
                            if (actions.enabled && !actions.busy && !inventoryOpen) onOpenDemo()
                        }
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        scene(Modifier.weight(1f))
                        controls(Modifier.weight(1f))
                    }
                } else {
                    scene(Modifier.fillMaxWidth())
                    controls(Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (inventoryOpen) {
        IslandInventoryModal(view, gate, actions, onClose = {
            inventoryOpen = false
        })
    }
}

@Composable
private fun IslandNavigation(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.islandTouchTarget()) {
        Text(label)
    }
}

@Composable
private fun IslandAvatar(avatar: AvatarPreview, catalog: Catalog, characterName: String, eating: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarView(avatar, catalog, if (eating) "eat" else "idle")
            Text(characterName, style = MaterialTheme.typography.headlineMedium)
            Text(if (eating) "냠냠 · 식사 중" else "나의 캐릭터")
        }
    }
}

@Composable
private fun IslandGrowthCard(growth: IslandGrowth?) {
    val garden = MaterialTheme.colorScheme.primaryContainer
    val detail = MaterialTheme.colorScheme.onPrimaryContainer
    val accent = MaterialTheme.colorScheme.tertiary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                growth?.let { "성장 ${it.ordinal}단계 · ${it.label}" } ?: "성장 상태 확인 필요",
                style = MaterialTheme.typography.titleMedium,
            )
            if (growth != null) {
                Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                    val w = size.width
                    val h = size.height
                    drawOval(garden, Offset(w * .12f, h * .2f), Size(w * .76f, h * .7f))
                    if (growth >= IslandGrowth.PATH) {
                        drawLine(detail, Offset(w * .32f, h * .76f), Offset(w * .68f, h * .35f), 3.dp.toPx())
                    }
                    if (growth >= IslandGrowth.FLOWERS) {
                        listOf(.34f, .44f, .54f).forEach { x ->
                            drawCircle(accent, 4.dp.toPx(), Offset(w * x, h * .36f))
                        }
                    }
                    if (growth >= IslandGrowth.REST_AREA) {
                        drawRect(detail, Offset(w * .61f, h * .49f), Size(w * .16f, h * .18f))
                    }
                }
            }
        }
    }
}

internal fun Modifier.islandTouchTarget(): Modifier = fillMaxWidth().heightIn(min = 56.dp)

@Composable
internal fun IslandFeedback(actions: IslandUiActions, gate: InteractionGate) {
    if (!actions.enabled) Text(gate.reason ?: "이용 가능한 활성 화면에서 다시 시도해 주세요.")
    if (actions.busy) Text("처리 중…")
    actions.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
