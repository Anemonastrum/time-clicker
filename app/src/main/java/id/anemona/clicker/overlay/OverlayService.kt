package id.anemona.clicker.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import id.anemona.clicker.R
import id.anemona.clicker.ntp.ClockSync

/**
 * Hosts the floating panel plus one draggable marker per configured touch point. Owns the
 * [WindowManager] params, so dragging a marker is just a param update.
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIF_ID = 1

        @Volatile private var instance: OverlayService? = null
        val isShowing: Boolean get() = instance != null

        fun show(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun hide(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val clockSync: ClockSync by lazy { ClockSync(this) }
    private val markers: PointMarkers by lazy { PointMarkers(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        addOverlay()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.overlay_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.overlay_channel_description)
                    setShowBadge(false)
                }
            )
        }
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun addOverlay() {
        if (!canDrawOverlays()) {
            Log.w(TAG, "no SYSTEM_ALERT_WINDOW permission")
            stopSelf(); return
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 120
        }
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent { OverlayPanel() }
        }
        composeView = view
        runCatching { windowManager.addView(view, params) }
            .onFailure { Log.e(TAG, "addView failed", it); stopSelf(); return }

        // The panel is its own drag handle: pointer drags move the window.
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { touchStartX = event.rawX; touchStartY = event.rawY; false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    touchStartX = event.rawX; touchStartY = event.rawY
                    moveBy(dx, dy); true
                }
                else -> false
            }
        }
    }

    private var touchStartX = 0f
    private var touchStartY = 0f

    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        runCatching { windowManager.updateViewLayout(composeView, params) }
    }

    @Composable
    private fun OverlayPanel() {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        OverlayContent(
            scale = id.anemona.clicker.store.AppScope.store(this).overlayScale,
            onMove = ::moveBy,
            clockSync = clockSync,
            onAdd = {
                val points = OverlayActions.addPoint(ctx)
                markers.sync(points)
                android.widget.Toast.makeText(ctx, "Point added", android.widget.Toast.LENGTH_SHORT).show()
            },
            onRemove = {
                val points = OverlayActions.removePoint(ctx)
                markers.sync(points)
            },
            onStart = {
                OverlayActions.arm(ctx)
                if (!OverlayActions.start()) {
                    android.widget.Toast.makeText(ctx, "Enable the accessibility service first", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onStop = { OverlayActions.stop() },
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        markers.clear()
        composeView?.let { v -> runCatching { windowManager.removeView(v) } }
        composeView = null
        clockSync.stop()
        if (instance === this) instance = null
        super.onDestroy()
    }
}
