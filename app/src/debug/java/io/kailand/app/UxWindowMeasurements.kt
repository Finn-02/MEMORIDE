package io.kailand.app

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowInsets
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject

/** Debug-only readout from actual platform APIs. Never writes display, gear or UX restrictions.
 * Bounds are screen px; density converts dimensions to dp. mm cannot be inferred from these values.
 */
internal class UxWindowMeasurements(private val activity: Activity) : ViewTreeObserver.OnGlobalLayoutListener {
    private var viewport = IntSize.Zero
    private var lastValue: String? = null
    private var attached = false

    fun start() {
        if (!attached) {
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
            attached = true
        }
        activity.window.decorView.post { publish() }
    }

    fun stop() {
        if (attached) activity.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        attached = false
        viewport = IntSize.Zero
    }

    fun viewportMeasured(size: IntSize) { viewport = size; publish() }

    override fun onGlobalLayout() = publish()

    private fun publish() {
        if (!attached) return
        val decor = activity.window.decorView
        if (decor.width <= 0 || decor.height <= 0) return
        val density = activity.resources.displayMetrics.density
        val config = activity.resources.configuration
        val visible = Rect().also(decor::getWindowVisibleDisplayFrame)
        val bounds = if (Build.VERSION.SDK_INT >= 30) activity.windowManager.currentWindowMetrics.bounds else visible
        fun rect(value: Rect) = JSONObject().put("left", value.left).put("top", value.top)
            .put("right", value.right).put("bottom", value.bottom)
        val data = JSONObject()
            .put("kind", "PLEOS_WINDOW_MEASUREMENT")
            .put("requestedOrientation", activity.requestedOrientation)
            .put("boundsPx", rect(bounds))
            .put("windowWidthDp", bounds.width() / density)
            .put("windowHeightDp", bounds.height() / density)
            .put("decorWidthPx", decor.width).put("decorHeightPx", decor.height)
            .put("visibleFramePx", rect(visible))
            .put("density", density).put("densityDpi", config.densityDpi)
            .put("fontScale", config.fontScale)
            .put("orientation", config.orientation)
        if (viewport != IntSize.Zero) data.put("composeViewportWidthPx", viewport.width)
            .put("composeViewportHeightPx", viewport.height)
        val insets = decor.rootWindowInsets
        if (Build.VERSION.SDK_INT >= 30 && insets != null) {
            fun inset(type: Int): JSONObject {
                val value = insets.getInsets(type)
                return JSONObject().put("left", value.left).put("top", value.top)
                    .put("right", value.right).put("bottom", value.bottom)
            }
            data.put("systemBarsPx", inset(WindowInsets.Type.systemBars()))
                .put("displayCutoutPx", inset(WindowInsets.Type.displayCutout()))
                .put("imePx", inset(WindowInsets.Type.ime()))
                .put("imeVisible", insets.isVisible(WindowInsets.Type.ime()))
        }
        val serialized = data.toString()
        if (serialized != lastValue) {
            lastValue = serialized
            Log.i("MemorideWindow", serialized)
        }
    }
}
