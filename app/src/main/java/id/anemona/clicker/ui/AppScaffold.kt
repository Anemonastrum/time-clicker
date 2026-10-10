package id.anemona.clicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * The app's settings screen. The user asked for in-app settings covering the execution start
 * time, network-delay calculation and the clock; the overlay's own controls live on the overlay.
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
fun AppScaffold(
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
    accessibilityGranted: () -> Boolean,
    overlayShown: () -> Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
    onHideOverlay: () -> Unit,
) {
    val controller = remember { ThemeController() }
    MiuixTheme(controller = controller) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = "Anemona Clicker",
                )
            }
        ) { padding ->
            SettingsBody(
                padding = padding,
                startTime = startTime,
                onStartTimeChanged = onStartTimeChanged,
                overlayScale = overlayScale,
                onOverlayScaleChanged = onOverlayScaleChanged,
                ntpEnabled = ntpEnabled,
                onNtpEnabledChanged = onNtpEnabledChanged,
                ntpServer = ntpServer,
                onNtpServerChanged = onNtpServerChanged,
                ntpTimeout = ntpTimeout,
                onNtpTimeoutChanged = onNtpTimeoutChanged,
                refreshMinutes = refreshMinutes,
                onRefreshMinutesChanged = onRefreshMinutesChanged,
                compensateNet = compensateNet,
                onCompensateNetChanged = onCompensateNetChanged,
                accessibilityGranted = accessibilityGranted(),
                overlayShown = overlayShown(),
                onEnableAccessibility = onEnableAccessibility,
                onEnableOverlay = onEnableOverlay,
                onHideOverlay = onHideOverlay,
            )
        }
    }
}
