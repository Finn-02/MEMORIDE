package io.kailand.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kailand.app.journey.ArrivalConfirmationPopup
import io.kailand.core.contract.*
import io.kailand.core.ui.MemoridePleosFonts
import io.kailand.core.ui.MemoridePrimaryButton
import io.kailand.core.ui.MemorideTheme
import io.kailand.core.ui.memorideModalBackdrop
import io.kailand.feature.island.IslandHomeStage
import io.kailand.feature.island.islandPresentation

/** Read-only UI fixture. No platform gate input, GameService, save or arrival command exists here. */
class ArrivalModalPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalog = assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8).use { decodeCatalog(it.readText()) }
        val region = catalog.regions.first { it.id == "wonju" }
        val variant = intent.getStringExtra("variant") ?: "reward"
        val snapshot = GameSnapshot(AvatarPreview("tori", null), 1,
            if (variant == "collected") region.itemIds.toSet() else emptySet(),
            emptySet(), emptyMap(), null)
        val destination = DestinationSpec(if (variant == "unknown") null else region.id,
            if (variant == "unknown") "등록되지 않은 목적지" else region.name, region.latitude, region.longitude)
        setContent {
            MemorideTheme {
                var visible by remember { mutableStateOf(false) }
                var status by remember { mutableStateOf("UI 검수 표본 · 저장 및 실제 도착 처리 없음") }
                LaunchedEffect(Unit) { visible = true }
                Row(Modifier.fillMaxSize().then(memorideModalBackdrop(visible))
                    .background(Color(0xFFF3F6F9)).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IslandHomeStage(snapshot, catalog, islandPresentation(snapshot, catalog), false,
                        Modifier.weight(3f).fillMaxHeight())
                    Column(Modifier.weight(2f).fillMaxHeight().background(Color.White).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        Text("MEMORIDE · UI 검수 표본", fontFamily = MemoridePleosFonts.Body, fontSize = 24.sp)
                        Text(status, fontFamily = MemoridePleosFonts.Body, fontSize = 22.sp)
                        Text("토리의 섬", fontFamily = MemoridePleosFonts.Body, fontSize = 40.sp)
                        Text(destination.label, fontFamily = MemoridePleosFonts.Body, fontSize = 56.sp)
                        MemoridePrimaryButton("팝업 다시 열기", { visible = true })
                    }
                }
                ArrivalConfirmationPopup(visible, destination, catalog, snapshot, enabled = true,
                    saving = variant == "busy", error = null,
                    onBack = { visible = false; status = "뒤로 가기 표본" },
                    onContinue = { visible = false; status = "아직 아니에요 · 대기 화면 유지 표본" },
                    onArrive = { visible = false; status = "도착 콜백 실행 표본 · 저장 없음" },
                    address = if (variant == "address") "강원특별자치도 원주시" else null)
            }
        }
    }
}
