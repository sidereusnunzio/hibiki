package com.hibiki.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import com.hibiki.HibikiApplication
import com.hibiki.MainActivity
import com.hibiki.R
import com.hibiki.domain.model.OverlayStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayWindow: OverlayWindow? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container().overlayController.stopListen()
            stopSelf()
            return START_NOT_STICKY
        }
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        startAsForeground()
        if (resultCode != 0 && data != null && mediaProjection == null) {
            startProjection(resultCode, data)
        }
        showOverlay()
        running = true
        return START_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopOverlay = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_notification, getString(R.string.overlay_notification_stop), stopOverlay)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        }
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = manager.getMediaProjection(resultCode, data)
            ?: return
        mediaProjection = projection
        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            },
            Handler(Looper.getMainLooper()),
        )
        val metrics = resources.displayMetrics
        val reader = ImageReader.newInstance(
            (metrics.widthPixels / 4).coerceAtLeast(320),
            (metrics.heightPixels / 4).coerceAtLeast(180),
            PixelFormat.RGBA_8888,
            2,
        )
        reader.setOnImageAvailableListener(
            { available -> available.acquireLatestImage()?.close() },
            Handler(Looper.getMainLooper()),
        )
        imageReader = reader
        virtualDisplay = projection.createVirtualDisplay(
            "hibiki-audio-only",
            reader.width,
            reader.height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )
        container().audioCaptureRepository.attachProjection(projection)
    }

    private fun showOverlay() {
        if (overlayWindow != null) return
        val app = application as HibikiApplication
        val controller = app.container.overlayController
        val settings = app.container.settingsRepository
        overlayWindow = OverlayWindow(this) { x, y ->
            scope.launch { settings.setOverlayPosition(x, y) }
        }
        scope.launch {
            controller.hydrate()
            controller.observeUpdates(scope)
            val prefs = settings.preferences.first()
            overlayWindow?.attach(prefs.overlayX, prefs.overlayY) {
                val uiState by controller.state.collectAsState()
                val appPrefs by settings.preferences.collectAsState(
                    initial = com.hibiki.domain.model.AppPreferences(),
                )
                OverlayPanel(
                    state = uiState,
                    displayPrefs = appPrefs.overlayDisplay,
                    onDrag = { dx, dy -> overlayWindow?.updatePositionBy(dx, dy) },
                    onToggleCollapsed = { controller.setCollapsed(!uiState.collapsed, scope) },
                    onSelectContext = { controller.selectContext(it, scope) },
                    onSelectSubject = { controller.selectSubject(it, scope) },
                    onListenToggle = {
                        when (uiState.stage) {
                            OverlayStage.LISTENING -> controller.stopListen()
                            OverlayStage.IDLE -> controller.startListen(scope)
                            OverlayStage.RESULT, OverlayStage.ERROR -> {
                                controller.resetToIdle()
                                controller.startListen(scope)
                            }
                            else -> Unit
                        }
                    },
                    onPlay = {
                        uiState.result?.phrase?.audioPath?.let { app.container.phraseAudioPlayer.play(it) }
                    },
                    onClose = {
                        controller.stopListen()
                        stopSelf()
                    },
                    onDropdownFocus = { open -> overlayWindow?.setFocusable(open) },
                )
            }
        }
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        overlayWindow?.dismiss()
        overlayWindow = null
        container().audioCaptureRepository.detachProjection()
        virtualDisplay?.release()
        imageReader?.close()
        runCatching { mediaProjection?.stop() }
        super.onDestroy()
    }

    private fun container() = (application as HibikiApplication).container

    companion object {
        const val ACTION_STOP = "com.hibiki.overlay.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "hibiki_overlay"
        private const val NOTIFICATION_ID = 41

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
