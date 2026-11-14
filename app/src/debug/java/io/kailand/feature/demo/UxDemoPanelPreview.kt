package io.kailand.feature.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.kailand.app.decodeCatalog
import io.kailand.core.contract.*
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Preview(name = "UX-C03 네 시연 · 연결 대기", widthDp = 1280, heightDp = 720, showBackground = true)
@Preview(name = "UX-C03 분할 창", widthDp = 640, heightDp = 720, showBackground = true)
@Preview(name = "UX-C03 큰 글자 보조", widthDp = 411, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
fun UxDemoPanelPreview() {
    val assets = LocalContext.current.assets
    var error by remember { mutableStateOf(false) }
    val catalog by produceState<Catalog?>(null, assets) {
        value = withContext(Dispatchers.IO) {
            try {
                assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8).use { decodeCatalog(it.readText()) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true; null }
        }
    }
    MemorideTheme {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text("UI SAMPLE · 실제 저장/내비 없음 · A 콜백 미연결")
            val loaded = catalog
            if (loaded == null) Text(if (error) "Catalog를 읽지 못했어요." else "샘플 불러오는 중")
            else UxDemoPanel(loaded, InteractionGate(GateState.PARKED_ALLOWED), true, true,
                onPrepare = { RequestResult.Failed("A의 별도 시험 환경 준비 연결 대기 · 실행 NOT RUN") },
                onOpenApp = { _, _ -> RequestResult.Failed("A 앱 연결 대기 · 실행 NOT RUN") },
                readEvidence = { null }, onClose = {}, modifier = Modifier.weight(1f))
        }
    }
}
