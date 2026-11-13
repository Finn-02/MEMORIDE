package io.kailand.core.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** Native backdrop blur. Software-rendered / pre-Android 12 windows use a darker scrim. */
@Composable
private fun modalBlurSupported() = Build.VERSION.SDK_INT >= 31 && LocalView.current.isHardwareAccelerated

/** Apply to the screen behind [MemorideModal], never to its foreground card. */
@Composable
fun memorideModalBackdrop(visible: Boolean): Modifier {
    val radius by animateFloatAsState(if (visible) 20f else 0f, tween(200), label = "modal-backdrop-blur")
    return if (modalBlurSupported()) Modifier.blur(radius.dp) else Modifier
}

/** Reusable full-window modal. Outside taps are intercepted by the dialog and never dismiss it. */
@Composable
fun MemorideModal(
    visible: Boolean,
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val supportsBlur = modalBlurSupported()
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible
    val transition = updateTransition(transitionState, label = "memoride-modal")
    val opacity by transition.animateFloat({ tween(200) }, label = "opacity") { if (it) 1f else 0f }
    val scale by transition.animateFloat({ tween(200) }, label = "scale") { if (it) 1f else .96f }
    if (transition.currentState || transition.targetState) {
        Dialog(
            onDismissRequest = { if (visible) onBack() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
                dismissOnClickOutside = false, dismissOnBackPress = true),
        ) {
            val view = LocalView.current
            SideEffect {
                // The custom scrim is the only dim layer; avoid Android's additional default dim.
                (view.parent as? DialogWindowProvider)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            BoxWithConstraints(Modifier.fillMaxSize()
                .background(Color(0xFF1E2B3A).copy(alpha = (if (supportsBlur) .38f else .55f) * opacity)),
                contentAlignment = Alignment.Center) {
                // Larger arrival card at the target landscape size; compact windows keep safe margins.
                val width = if (maxWidth >= 1000.dp) minOf(800.dp, maxWidth * .7f) else minOf(800.dp, maxWidth - 32.dp)
                Surface(Modifier.width(width).heightIn(max = maxHeight - 32.dp)
                    .graphicsLayer { alpha = opacity; scaleX = scale; scaleY = scale }
                    .semantics { paneTitle = title },
                    shape = RoundedCornerShape(40.dp), color = Color.White,
                    shadowElevation = 24.dp) {
                    content()
                }
            }
        }
    }
}
