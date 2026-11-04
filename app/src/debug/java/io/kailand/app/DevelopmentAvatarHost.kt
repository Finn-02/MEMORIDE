package io.kailand.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.ui.AvatarSlot
import io.kailand.feature.island.AvatarView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A owns this debug host. B can supply its gallery as content without owning routing or gates. */
@Composable
internal fun DevelopmentAvatarHost(
    gate: InteractionGate,
    onBack: () -> Unit,
    content: @Composable (Catalog, AvatarPreview, AvatarSlot) -> Unit = { _, avatar, slot ->
        Text("연결 확인 · tori / idle / 의상 없음")
        slot(avatar, "idle", Modifier.size(128.dp))
        Text("B의 36개 조합 갤러리는 아직 연결되지 않았습니다.")
    },
) {
    Surface(Modifier.fillMaxSize()) {
        if (!gate.canInteract) {
            Column(Modifier.padding(24.dp)) {
                Text("캐릭터 개발 화면을 사용할 수 없습니다.")
                Text(gate.reason ?: "이용 상태 미확인")
            }
        } else {
            // Leaving this branch disposes the gallery and its temporary animation/loading state.
            AllowedAvatarHost(onBack, content)
        }
    }
}

@Composable
private fun AllowedAvatarHost(
    onBack: () -> Unit,
    content: @Composable (Catalog, AvatarPreview, AvatarSlot) -> Unit,
) {
    val assets = LocalContext.current.applicationContext.assets
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(assets, attempt) {
        failed = false
        catalog = null
        try {
            catalog = withContext(Dispatchers.IO) {
                decodeCatalog(CatalogAssets(assets).readCatalogJson())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("B04 · 캐릭터 이미지 확인", style = MaterialTheme.typography.titleLarge)
        Text("일반 Android debug 전용 · 샘플 외형 · 게임 저장/보상 없음")
        Button(onClick = onBack) { Text("개발 화면으로 돌아가기") }
        val loaded = catalog
        when {
            failed -> {
                Text("Catalog를 불러오지 못했습니다.")
                Button(onClick = { attempt++ }) { Text("다시 시도") }
            }
            loaded == null -> Text("Catalog 준비 중…")
            else -> {
                val slot: AvatarSlot = { avatar, expression, modifier ->
                    AvatarView(avatar, loaded, expression, modifier)
                }
                content(loaded, AvatarPreview("tori", null), slot)
            }
        }
    }
}

