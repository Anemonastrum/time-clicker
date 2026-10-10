package id.anemona.clicker.ntp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import id.anemona.clicker.store.AppScope
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the current NTP offset so the clicker can fire on corrected time, and exposes the last
 * measured RTT for the overlay clock. Re-syncs on a schedule when [sync] is called.
 */
class ClockSync(private val context: Context) {

    sealed interface State {
        data object Idle : State
        data object Syncing : State
        data class Synced(val offsetMs: Double, val rttMs: Double) : State
        data class Failed(val why: String) : State
    }

    private val _state = AtomicReference<State>(State.Idle)
    val state: State get() = _state.get()

    /** True when an offset is usable for scheduling. */
    val offsetMs: Double?
        get() = (state as? State.Synced)?.offsetMs

    private val client = NtpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var resyncJob: Runnable? = null
    private val listeners = mutableSetOf<(State) -> Unit>()

    fun observe(listener: (State) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun stopObserving(listener: (State) -> Unit) {
        listeners -= listener
    }

    private fun publish(s: State) {
        _state.set(s)
        listeners.forEach { runCatching { it(s) } }
    }

    /**
     * Runs one query, then schedules the next one [Store.ntpRefreshMinutes] later.
     */
    fun sync() {
        val store = AppScope.store(context)
        if (!store.ntpEnabled) {
            publish(State.Failed("NTP sync disabled"))
            return
        }
        if (state is State.Syncing) return
        publish(State.Syncing)
        client.query(store.ntpServer, store.ntpTimeoutMs) { result ->
            if (result == null) {
                publish(State.Failed("No reply from ${store.ntpServer}"))
            } else {
                publish(State.Synced(result.offsetMs, result.rttMs))
                Log.i(TAG, "synced offset=${result.offsetMs}ms rtt=${result.rttMs}ms")
            }
            scheduleNext()
        }
    }

    private fun scheduleNext() {
        resyncJob?.let { handler.removeCallbacks(it) }
        val delayMin = AppScope.store(context).ntpRefreshMinutes
        val r = Runnable { sync() }
        resyncJob = r
        handler.postDelayed(r, delayMin * 60_000L)
    }

    fun stop() {
        resyncJob?.let { handler.removeCallbacks(it) }
        resyncJob = null
        publish(State.Idle)
    }

    companion object {
        private const val TAG = "ClockSync"
    }
}
