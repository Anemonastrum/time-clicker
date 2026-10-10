package id.anemona.clicker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.anemona.clicker.clicker.ClickerAccessibilityService
import id.anemona.clicker.overlay.OverlayService
import id.anemona.clicker.store.AppScope

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AppScope.store(this)
        setContent {
            AppScaffold(
                startTime = store.startTimeText,
                onStartTimeChanged = { store.startTimeText = it },
                overlayScale = store.overlayScale,
                onOverlayScaleChanged = { store.overlayScale = it },
                ntpEnabled = store.ntpEnabled,
                onNtpEnabledChanged = { store.ntpEnabled = it },
                ntpServer = store.ntpServer,
                onNtpServerChanged = { store.ntpServer = it },
                ntpTimeout = store.ntpTimeoutMs,
                onNtpTimeoutChanged = { store.ntpTimeoutMs = it },
                refreshMinutes = store.ntpRefreshMinutes,
                onRefreshMinutesChanged = { store.ntpRefreshMinutes = it },
                compensateNet = store.compensateNetworkDelay,
                onCompensateNetChanged = { store.compensateNetworkDelay = it },
                accessibilityGranted = { ClickerAccessibilityService.isRunning },
                overlayShown = { OverlayService.isShowing },
                onEnableAccessibility = { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onEnableOverlay = { OverlayService.show(this) },
                onHideOverlay = { OverlayService.hide(this) },
            )
        }
    }
}
