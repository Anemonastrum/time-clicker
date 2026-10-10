package id.anemona.clicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Validates "HH:MM:SS.mmm" strictly and reports the ms-of-day, or null when invalid.
 * Not a countdown: the clicker waits for the wall clock to hit this time.
 */
internal fun parseTimeOfDay(raw: String): Long? {
    val match = Regex("^(\\d{1,2}):(\\d{2}):(\\d{2})\\.(\\d{1,3})$").matchEntire(raw.trim())
        ?: return null
    val (h, m, s, ms) = match.destructured
    val hour = h.toIntOrNull() ?: return null
    val minute = m.toIntOrNull() ?: return null
    val sec = s.toIntOrNull() ?: return null
    val millis = ms.padEnd(3, '0').toIntOrNull() ?: return null
    if (hour > 23 || minute > 59 || sec > 59 || millis > 999) return null
    return ((hour * 3600L + minute * 60L + sec) * 1000L) + millis
}

@Composable
@Suppress("LongParameterList")
internal fun SettingsBody(
    padding: PaddingValues,
    startTime: String,
    onStartTimeChanged: (String) -> Unit,
    overlayScale: Float,
    onOverlayScaleChanged: (Float) -> Unit,
    ntpEnabled: Boolean,
    onNtpEnabledChanged: (Boolean) -> Unit,
    ntpServer: String,
    onNtpServerChanged: (String) -> Unit,
    ntpTimeout: Int,
    onNtpTimeoutChanged: (Int) -> Unit,
    refreshMinutes: Int,
    onRefreshMinutesChanged: (Int) -> Unit,
    compensateNet: Boolean,
    onCompensateNetChanged: (Boolean) -> Unit,
    accessibilityGranted: Boolean,
    overlayShown: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
    onHideOverlay: () -> Unit,
) {
    // ponytail: a single screen of cards; no navigation until a second screen exists.
    val timeState = remember { TextFieldState(startTime) }
    val serverState = remember { TextFieldState(ntpServer) }
    var timeError by remember { mutableStateOf(false) }

    // Push external changes (e.g. overlay edits) back into the fields.
    LaunchedEffect(startTime) { if (timeState.text.toString() != startTime) timeState.text = startTime }
    LaunchedEffect(ntpServer) { if (serverState.text.toString() != ntpServer) serverState.text = ntpServer }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!accessibilityGranted) {
            PermissionCard(onClick = onEnableAccessibility)
        }

        SmallTitle(text = "Execution start time")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextField(
                    state = timeState,
                    label = "HH:MM:SS.mmm",
                    onValueChange = {
                        val parsed = parseTimeOfDay(it)
                        timeError = parsed == null
                        if (parsed != null) onStartTimeChanged(it)
                    },
                )
                if (timeError) {
                    Text(
                        text = "Use the format HH:MM:SS.mmm — e.g. 14:30:00.000",
                        color = MiuixTheme.colorScheme.onError,
                    )
                } else {
                    Text(
                        text = "Fires at this wall-clock time, not after a countdown.",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        SmallTitle(text = "Clock sync")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchRow(
                    title = "Sync clock with NTP",
                    subtitle = "Query a time server and correct for network delay before firing",
                    checked = ntpEnabled,
                    onCheckedChange = onNtpEnabledChanged,
                )
                TextField(
                    state = serverState,
                    label = "NTP server",
                    onValueChange = { onNtpServerChanged(it) },
                )
                SliderRow(
                    label = "NTP timeout",
                    value = "${ntpTimeout}ms",
                    current = ntpTimeout.toFloat(),
                    range = 500f..30000f,
                    onChange = { onNtpTimeoutChanged(it.toInt()) },
                )
                SliderRow(
                    label = "Re-sync interval",
                    value = "${refreshMinutes}min",
                    current = refreshMinutes.toFloat(),
                    range = 1f..180f,
                    onChange = { onRefreshMinutesChanged(it.toInt()) },
                )
                SwitchRow(
                    title = "Compensate network delay",
                    subtitle = "Subtract half the measured round-trip from the fire time",
                    checked = compensateNet,
                    onCheckedChange = onCompensateNetChanged,
                )
            }
        }

        SmallTitle(text = "Overlay")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SliderRow(
                    label = "Overlay size",
                    value = "${(overlayScale * 100).toInt()}%",
                    current = overlayScale,
                    range = 0.8f..2.0f,
                    onChange = onOverlayScaleChanged,
                )
                Button(
                    onClick = if (overlayShown) onHideOverlay else onEnableOverlay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (overlayShown) "Hide overlay" else "Show overlay")
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = title)
            Text(
                text = subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label)
            Text(text = value, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Slider(value = current, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun PermissionCard(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Accessibility permission needed")
            Text(
                text = "Anemona Clicker needs the accessibility service enabled so it can dispatch the taps you configured.",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(onClick = onClick) { Text("Grant") }
        }
    }
}
