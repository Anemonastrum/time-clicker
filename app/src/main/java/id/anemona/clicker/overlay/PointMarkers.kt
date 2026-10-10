package id.anemona.clicker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import id.anemona.clicker.store.AppScope
import id.anemona.clicker.store.TouchPoint

/**
 * One draggable circle per configured touch point, drawn as a separate overlay window so it can
 * be moved independently of the control panel. Dragging writes the new position back to the
 * store and re-arms the clicker.
 */
class PointMarkers(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val views = ArrayList<View>()

    private fun type(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    /** Replaces all markers with one per [points]. Call after add/remove/drag. */
    fun sync(points: List<TouchPoint>) {
        clear()
        points.forEach { p -> addMarker(p) }
    }

    private fun addMarker(p: TouchPoint) {
        val view = View(context).apply {
            setBackgroundResource(android.R.drawable.presence_online)
            isClickable = true
            isFocusable = false
        }
        val params = WindowManager.LayoutParams(
            MARKER_SIZE, MARKER_SIZE,
            type(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = p.x.toInt() - MARKER_SIZE / 2
            y = p.y.toInt() - MARKER_SIZE / 2
        }
        var startX = 0f
        var startY = 0f
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    startX = event.rawX; startY = event.rawY
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    persist(params.x + MARKER_SIZE / 2, params.y + MARKER_SIZE / 2)
                    true
                }
                else -> false
            }
        }
        runCatching { windowManager.addView(view, params) }
        views += view
    }

    private fun persist(x: Int, y: Int) {
        val store = AppScope.store(context)
        val points = store.loadPoints().mapIndexed { i, p ->
            if (i == views.size - 1) p.copy(x = x.toFloat(), y = y.toFloat()) else p
        }
        store.savePoints(points)
        OverlayActions.arm(context, points)
    }

    fun clear() {
        views.forEach { v -> runCatching { windowManager.removeView(v) } }
        views.clear()
    }

    private companion object {
        const val MARKER_SIZE = 56
    }
}
