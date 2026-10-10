package id.anemona.clicker.overlay

import android.content.Context
import id.anemona.clicker.clicker.ClickerAccessibilityService
import id.anemona.clicker.store.AppScope
import id.anemona.clicker.store.ClickerConfig
import id.anemona.clicker.store.TouchPoint
import id.anemona.clicker.ui.parseTimeOfDay

/**
 * The overlay's actions, backed by the shared store. Adding a point drops it at screen centre;
 * the point markers are dragged directly, so positioning happens on the markers, not the panel.
 * Every mutation re-arms [ClickerAccessibilityService.pendingConfig] so Start is always current.
 */
object OverlayActions {

    fun addPoint(context: Context): List<TouchPoint> {
        val store = AppScope.store(context)
        val size = screenCenter(context)
        val points = store.loadPoints() + TouchPoint(
            x = size.first,
            y = size.second,
            repeat = store.defaultRepeat.coerceAtLeast(1),
            interval = store.defaultIntervalMs,
            delay = store.defaultDelayMs,
            hold = store.defaultHoldMs,
        )
        store.savePoints(points)
        arm(context, points)
        return points
    }

    fun removePoint(context: Context): List<TouchPoint> {
        val store = AppScope.store(context)
        val points = store.loadPoints().dropLast(1)
        store.savePoints(points)
        arm(context, points)
        return points
    }

    fun arm(context: Context, points: List<TouchPoint> = AppScope.store(context).loadPoints()) {
        val store = AppScope.store(context)
        ClickerAccessibilityService.pendingConfig = ClickerConfig(
            points = points,
            startMsOfDay = parseTimeOfDay(store.startTimeText) ?: 0L,
            loops = 0,
        )
    }

    fun start(): Boolean = ClickerAccessibilityService.start()

    fun stop() = ClickerAccessibilityService.stop()

    private fun screenCenter(context: Context): Pair<Float, Float> {
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
            .defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels / 2f to metrics.heightPixels / 2f
    }
}
