package id.anemona.clicker.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.anemona.clicker.clicker.ClickerAccessibilityService
import id.anemona.clicker.ntp.ClockSync
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The floating panel. Minimized = clock (with ms) + network RTT; expanded = icon-only buttons
 * for add/remove touch point, start and stop. Whole panel is a drag handle.
 *
 * ponytail: no per-point editor on the panel — repeat/interval/delay are the app defaults and
 * can be promoted to a per-point sheet when a second caller needs them.
 */
@Composable
fun OverlayContent(
    scale: Float,
    onMove: (dx: Float, dy: Float) -> Unit,
    clockSync: ClockSync,
    onAdd: () -> Unit,
    onRemove: () -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val controller = remember { ThemeController() }
    var expanded by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf("--:--:--.---") }
    var rtt by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        ClickerAccessibilityService.observeRunning { running = it }
    }
    LaunchedEffect(Unit) {
        clockSync.observe { state ->
            rtt = (state as? ClockSync.State.Synced)?.rttMs?.toFloat()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            clock = TIME_FORMAT.format(Date())
            delay(50)
        }
    }

    MiuixTheme(controller = controller) {
        Card(
            modifier = Modifier.pointerInput(expanded) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onMove(drag.x, drag.y)
                }
            },
            cornerRadius = (16 * scale).dp,
            insideMargin = PaddingValues((12 * scale).dp),
        ) {
            if (expanded) {
                ExpandedBar(
                    scale = scale,
                    running = running,
                    onAdd = onAdd,
                    onRemove = onRemove,
                    onStart = onStart,
                    onStop = onStop,
                    onCollapse = { expanded = false },
                )
            } else {
                MiniClock(
                    scale = scale,
                    clock = clock,
                    rtt = rtt,
                    onExpand = { expanded = true },
                )
            }
        }
    }
}

@Composable
private fun MiniClock(
    scale: Float,
    clock: String,
    rtt: Float?,
    onExpand: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
    ) {
        Text(
            text = clock,
            fontSize = (14f * scale).sp,
            color = MiuixTheme.colorScheme.onSurface,
        )
        if (rtt != null) {
            Text(
                text = "${rtt.roundToInt()}ms",
                fontSize = (10f * scale).sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        IconButton(onClick = onExpand) {
            Icon(imageVector = MiuixIcons.Basic.ArrowRight, contentDescription = "Expand")
        }
    }
}

@Composable
private fun ExpandedBar(
    scale: Float,
    running: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCollapse: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAdd, enabled = !running) {
                Icon(MiuixIcons.Add, "Add touch point")
            }
            IconButton(onClick = onRemove, enabled = !running) {
                Icon(MiuixIcons.Remove, "Remove touch point")
            }
            Spacer(Modifier.width((4 * scale).dp))
            IconButton(onClick = onStart, enabled = !running) {
                Icon(MiuixIcons.Play, "Start")
            }
            IconButton(onClick = onStop, enabled = running) {
                Icon(MiuixIcons.Pause, "Stop")
            }
            Spacer(Modifier.width((4 * scale).dp))
            IconButton(onClick = onCollapse) {
                Icon(MiuixIcons.Basic.Close, "Collapse")
            }
        }
    }
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
