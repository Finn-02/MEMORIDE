package io.kailand.feature.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.kailand.R
import io.kailand.core.ui.MemorideBrandLogo
import io.kailand.core.ui.MemorideJourneySteps
import io.kailand.core.ui.MemorideJourneyLayout

/** Decorative artwork for waiting mode only, never live driving or navigation telemetry. */
@Composable
internal fun IslandJourneyCarStage(label: String, animate: Boolean, modifier: Modifier = Modifier, characterName: String? = null) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp)).background(
        Brush.verticalGradient(listOf(Color(0xFFCDEBFA), Color(0xFFF9F1E7))))) {
        val roomy = maxHeight >= 500.dp && maxWidth >= 560.dp
        val fontScale = LocalDensity.current.fontScale
        Column(Modifier.fillMaxSize().padding(if (roomy) MemorideJourneyLayout.OuterInset else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (roomy) MemorideJourneyLayout.SectionGap else 12.dp)) {
            if (roomy) {
                Row(Modifier.fillMaxWidth().height(MemorideJourneyLayout.headerHeight(fontScale)), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MemorideBrandLogo(Modifier.width(276.dp), markSize = 60.dp, wordmarkSize = 38)
                    IslandWaitingStageChip(label, animate, Modifier.weight(1f), characterName = characterName)
                }
            } else IslandWaitingStageChip(label, animate, Modifier.fillMaxWidth(), characterName = characterName)
            Image(painterResource(R.drawable.memoride_journey_car_v1), "MEMORIDE 자동차 여정 일러스트",
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)),
                contentScale = if (roomy) ContentScale.Crop else ContentScale.Fit)
            // Separate from the image; bottom padding also contains the footer shadow.
            Surface(Modifier.fillMaxWidth().height(MemorideJourneyLayout.footerHeight(fontScale, compact = !roomy)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = .96f), shadowElevation = 4.dp) {
                MemorideJourneySteps(Modifier.padding(horizontal = if (roomy) 20.dp else 12.dp, vertical = 12.dp), compact = !roomy)
            }
        }
    }
}
