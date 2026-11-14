package io.kailand.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Animate the incoming screen only; outgoing callbacks/jobs are disposed immediately. */
@Composable
internal fun ProductScreenTransition(
    route: ProductRoute,
    contentKey: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val target = route to contentKey
    var previousRoute by remember { mutableStateOf(route) }
    val direction = remember(target) {
        if (route == ProductRoute.ISLAND && previousRoute !in setOf(ProductRoute.ISLAND, ProductRoute.CHARACTER)) -1f
        else 1f
    }
    SideEffect { previousRoute = route }
    Box(modifier.fillMaxWidth().clipToBounds()) {
        key(target) {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                progress.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = progress.value
                translationX = direction * 24.dp.toPx() * (1f - progress.value)
            }) {
                content()
            }
        }
    }
}
