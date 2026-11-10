package io.kailand.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Official Pleos 4.5:1 target for these actual text/background pairs.
 * Numerical palette checks do not certify rendered assets or physical display legibility.
 */
class MemorideContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + .05f) / (dark + .05f)
    }

    @Test fun primaryTextAndSupportingTextMeetContrastAtBothGradientEnds() {
        for (background in listOf(MemorideColors.Navy, MemorideColors.OceanAccessible)) {
            for (text in listOf(MemorideColors.OnPrimary, MemorideColors.PrimarySupportingText)) {
                assertTrue("Primary text must be opaque", text.alpha == 1f)
                assertTrue("Primary contrast ${ratio(text, background)}", ratio(text, background) >= 4.5f)
            }
        }
    }

    @Test fun disabledPrimaryTextMeetsContrastWithoutRelyingOnOpacity() {
        assertTrue(ratio(MemorideColors.DisabledPrimaryText, MemorideColors.ProgressEmpty) >= 4.5f)
    }

    @Test fun secondaryAndPanelTextMeetContrastOnTheirSurfaces() {
        for (background in listOf(MemorideColors.Surface, MemorideColors.SurfaceSubtle, MemorideColors.SurfaceLocked)) {
            assertTrue(ratio(MemorideColors.TextPrimary, background) >= 4.5f)
            assertTrue(ratio(MemorideColors.TextSecondary, background) >= 4.5f)
        }
    }
}
