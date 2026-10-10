package id.anemona.clicker.store

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Persistence for points + settings. One SharedPreferences file, plain primitives + a
 * hand-rolled JSON blob for the point list. Deliberately no serialization framework: the
 * model is tiny and adding one is not worth a Gradle plugin.
 */
class Store(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // region settings

    var overlayScale: Float
        get() = prefs.getFloat(KEY_OVERLAY_SCALE, 1.0f).coerceIn(0.8f, 2.0f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_SCALE, value.coerceIn(0.8f, 2.0f)).apply()

    var ntpEnabled: Boolean
        get() = prefs.getBoolean(KEY_NTP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NTP_ENABLED, value).apply()

    var ntpServer: String
        get() = prefs.getString(KEY_NTP_SERVER, "pool.ntp.org") ?: "pool.ntp.org"
        set(value) = prefs.edit().putString(KEY_NTP_SERVER, value.trim()).apply()

    var ntpTimeoutMs: Int
        get() = prefs.getInt(KEY_NTP_TIMEOUT, 3000).coerceIn(500, 30000)
        set(value) = prefs.edit().putInt(KEY_NTP_TIMEOUT, value.coerceIn(500, 30000)).apply()

    var ntpRefreshMinutes: Int
        get() = prefs.getInt(KEY_NTP_REFRESH, 30).coerceIn(1, 24 * 60)
        set(value) = prefs.edit().putInt(KEY_NTP_REFRESH, value.coerceIn(1, 24 * 60)).apply()

    var compensateNetworkDelay: Boolean
        get() = prefs.getBoolean(KEY_COMPENSATE_NET, true)
        set(value) = prefs.edit().putBoolean(KEY_COMPENSATE_NET, value).apply()

    var defaultRepeat: Int
        get() = prefs.getInt(KEY_DEF_REPEAT, 1).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_DEF_REPEAT, value.coerceAtLeast(0)).apply()

    var defaultIntervalMs: Long
        get() = prefs.getLong(KEY_DEF_INTERVAL, 100L).coerceAtLeast(0L)
        set(value) = prefs.edit().putLong(KEY_DEF_INTERVAL, value.coerceAtLeast(0L)).apply()

    var defaultDelayMs: Long
        get() = prefs.getLong(KEY_DEF_DELAY, 0L).coerceAtLeast(0L)
        set(value) = prefs.edit().putLong(KEY_DEF_DELAY, value.coerceAtLeast(0L)).apply()

    var defaultHoldMs: Long
        get() = prefs.getLong(KEY_DEF_HOLD, 40L).coerceAtLeast(1L)
        set(value) = prefs.edit().putLong(KEY_DEF_HOLD, value.coerceAtLeast(1L)).apply()

    /** HH:MM:SS.mmm string as entered by the user. */
    var startTimeText: String
        get() = prefs.getString(KEY_START_TIME, "00:00:00.000") ?: "00:00:00.000"
        set(value) = prefs.edit().putString(KEY_START_TIME, value).apply()

    // endregion

    // region points

    fun loadPoints(): List<TouchPoint> {
        val raw = prefs.getString(KEY_POINTS, null) ?: return emptyList()
        return runCatching { decodePoints(raw) }.onFailure {
            Log.w(TAG, "bad points blob, dropping", it)
        }.getOrDefault(emptyList())
    }

    fun savePoints(points: List<TouchPoint>) {
        prefs.edit().putString(KEY_POINTS, encodePoints(points)).apply()
    }

    // endregion

    private fun encodePoints(points: List<TouchPoint>): String =
        buildString {
            append('[')
            points.forEachIndexed { i, p ->
                if (i > 0) append(',')
                append('{')
                append("\"x\":").append(p.x.toLong()).append(',')
                append("\"y\":").append(p.y.toLong()).append(',')
                append("\"repeat\":").append(p.repeat).append(',')
                append("\"interval\":").append(p.interval).append(',')
                append("\"delay\":").append(p.delay).append(',')
                append("\"hold\":").append(p.hold)
                append('}')
            }
            append(']')
        }

    private fun decodePoints(raw: String): List<TouchPoint> {
        val list = ArrayList<TouchPoint>()
        var i = 0
        val defaults = defaults()
        while (i < raw.length) {
            val open = raw.indexOf('{', i)
            if (open < 0) break
            val close = raw.indexOf('}', open)
            if (close < 0) break
            val body = raw.substring(open + 1, close)
            val num = { k: String -> Regex("\"$k\":(-?\\d+)").find(body)?.groupValues?.get(1)?.toFloatOrNull() }
            list.add(
                TouchPoint(
                    x = num("x") ?: 0f,
                    y = num("y") ?: 0f,
                    repeat = num("repeat")?.toInt() ?: defaults.repeat,
                    interval = num("interval")?.toLong() ?: defaults.interval,
                    delay = num("delay")?.toLong() ?: defaults.delay,
                    hold = num("hold")?.toLong() ?: defaults.hold,
                )
            )
            i = close + 1
        }
        return list
    }

    private fun defaults(): TouchPoint = TouchPoint(
        x = 0f, y = 0f,
        repeat = defaultRepeat.coerceAtLeast(1),
        interval = defaultIntervalMs,
        delay = defaultDelayMs,
        hold = defaultHoldMs,
    )

    companion object {
        private const val TAG = "Store"
        private const val NAME = "settings"
        private const val KEY_OVERLAY_SCALE = "overlay_scale"
        private const val KEY_NTP_ENABLED = "ntp_enabled"
        private const val KEY_NTP_SERVER = "ntp_server"
        private const val KEY_NTP_TIMEOUT = "ntp_timeout"
        private const val KEY_NTP_REFRESH = "ntp_refresh_min"
        private const val KEY_COMPENSATE_NET = "compensate_net_delay"
        private const val KEY_DEF_REPEAT = "def_repeat"
        private const val KEY_DEF_INTERVAL = "def_interval"
        private const val KEY_DEF_DELAY = "def_delay"
        private const val KEY_DEF_HOLD = "def_hold"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_POINTS = "points"
    }
}
