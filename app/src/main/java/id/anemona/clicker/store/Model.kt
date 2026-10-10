package id.anemona.clicker.store

/**
 * One tap target shown on the overlay.
 *
 * @param x,y      Screen coordinates (px, from top-left).
 * @param repeat   Taps delivered per loop pass. 0 = unlimited.
 * @param interval Pause between consecutive taps of this point, ms.
 * @param delay    Extra pause after this point before moving on, ms.
 * @param hold     Duration the pointer is pressed, ms.
 */
data class TouchPoint(
    val x: Float,
    val y: Float,
    val repeat: Int = 1,
    val interval: Long = 100L,
    val delay: Long = 0L,
    val hold: Long = 40L,
) {
    val isInfinite: Boolean get() = repeat <= 0
}

/**
 * @param startMsOfDay Wall-clock start time as ms-of-day; firing waits for the clock to reach it.
 * @param loops        Total passes across all points. 0 = unlimited.
 */
data class ClickerConfig(
    val points: List<TouchPoint> = emptyList(),
    val startMsOfDay: Long = 0L,
    val loops: Int = 0,
)
