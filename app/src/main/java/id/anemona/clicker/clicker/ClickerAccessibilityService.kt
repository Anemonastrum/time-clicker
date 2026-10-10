package id.anemona.clicker.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import id.anemona.clicker.ntp.ClockSync
import id.anemona.clicker.store.AppScope
import id.anemona.clicker.store.ClickerConfig
import id.anemona.clicker.store.TouchPoint
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Accessibility service that performs the taps. Only dispatches gestures — it never reads screen
 * content. Scheduling is driven by [SystemClock.elapsedRealtime] anchored to the NTP-corrected
 * wall clock so firing follows real time, not a countdown.
 */
class ClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClickerService"

        @Volatile
        private var instance: ClickerAccessibilityService? = null
        val isRunning: Boolean get() = instance != null

        /** Latest armed config, set from the overlay before [start]. */
        @Volatile
        var pendingConfig: ClickerConfig? = null

        @Volatile
        var running: Boolean = false
            private set

        private val stateListeners = mutableSetOf<(Boolean) -> Unit>()

        fun observeRunning(listener: (Boolean) -> Unit) {
            synchronized(stateListeners) { stateListeners += listener }
            listener(running)
        }

        fun stopObserving(listener: (Boolean) -> Unit) {
            synchronized(stateListeners) { stateListeners -= listener }
        }

        private fun publishRunning(v: Boolean) {
            running = v
            val snapshot = synchronized(stateListeners) { stateListeners.toList() }
            snapshot.forEach { runCatching { it(v) } }
        }

        /** Start the armed engine. Returns false if the service isn't connected. */
        fun start(): Boolean {
            val svc = instance ?: return false
            val cfg = pendingConfig
            if (cfg == null || cfg.points.isEmpty()) {
                Log.w(TAG, "start: no points configured")
                return false
            }
            svc.engine.start(cfg)
            publishRunning(true)
            return true
        }

        fun stop() {
            instance?.engine?.stop()
            publishRunning(false)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var engine: Engine
    val clockSync: ClockSync by lazy { ClockSync(this) }

    override fun onServiceConnected() {
        instance = this
        engine = Engine(this, ::dispatch)
        Log.i(TAG, "connected")
        clockSync.sync()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        clockSync.stop()
        stop()
        return super.onUnbind(intent)
    }

    private fun dispatch(point: TouchPoint, done: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            done(); return
        }
        val path = Path().apply { moveTo(point.x, point.y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, point.hold)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = done()
            override fun onCancelled(g: GestureDescription?) {
                Log.w(TAG, "gesture cancelled at ${point.x},${point.y}")
                done()
            }
        }, handler)
        if (!ok) done()
    }

    // region engine

    /**
     * Runs one config: wait for the wall-clock start time, then loop the points.
     * All waits are on the main thread handler; nothing blocks.
     */
    private class Engine(
        private val service: ClickerAccessibilityService,
        private val dispatch: (TouchPoint, () -> Unit) -> Unit,
    ) {
        private val stopped = AtomicBoolean(false)
        private val handler = Handler(Looper.getMainLooper())

        fun start(cfg: ClickerConfig) {
            stopped.set(false)
            scheduleStart(cfg)
        }

        fun stop() {
            stopped.set(true)
            handler.removeCallbacksAndMessages(null)
        }

        private fun scheduleStart(cfg: ClickerConfig) {
            val now = nowCorrected()
            var target = startOfTodayCorrected() + cfg.startMsOfDay
            // If the time already passed today, arm for tomorrow.
            while (target <= now) target += DAY_MS

            val delay = target - now
            Log.i(TAG, "arming for ${cfg.startMsOfDay}ms-of-day, firing in ${delay}ms")
            handler.postDelayed({
                if (stopped.get()) return@postDelayed
                runLoop(cfg, pass = 0)
            }, delay)
        }

        private fun runLoop(cfg: ClickerConfig, pass: Int) {
            if (stopped.get()) return
            if (cfg.loops > 0 && pass >= cfg.loops) {
                Log.i(TAG, "all loops done")
                publishRunning(false)
                return
            }
            runPoint(cfg, index = 0, pass)
        }

        private fun runPoint(cfg: ClickerConfig, index: Int, pass: Int) {
            if (stopped.get()) return
            if (index >= cfg.points.size) {
                // One full pass complete; loop again after the trailing delay.
                handler.postDelayed({ runLoop(cfg, pass + 1) }, 0L)
                return
            }
            val p = cfg.points[index]
            val taps = if (p.isInfinite) Int.MAX_VALUE else p.repeat.coerceAtLeast(1)
            tapRepeatedly(p, remaining = taps) {
                handler.postDelayed({ runPoint(cfg, index + 1, pass) }, p.delay)
            }
        }

        private fun tapRepeatedly(p: TouchPoint, remaining: Int, onDone: () -> Unit) {
            if (stopped.get() || remaining <= 0) {
                onDone(); return
            }
            dispatch(p) {
                if (stopped.get()) return@dispatch
                handler.postDelayed({ tapRepeatedly(p, remaining - 1, onDone) }, p.interval)
            }
        }

        private fun nowCorrected(): Long {
            val offset = service.clockSync.offsetMs ?: 0.0
            return System.currentTimeMillis() + offset.toLong()
        }

        private fun startOfTodayCorrected(): Long {
            return (nowCorrected() / DAY_MS) * DAY_MS
        }
    }

    // endregion
}

private const val DAY_MS = 24L * 60 * 60 * 1000
