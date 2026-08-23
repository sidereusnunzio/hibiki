package com.hibiki

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hibiki.overlay.OverlayService
import com.hibiki.ui.boot.ConnectingScreen
import com.hibiki.ui.navigation.HibikiNavHost
import com.hibiki.ui.theme.HibikiTheme

class MainActivity : ComponentActivity() {
    private var overlayGranted by mutableStateOf(false)
    private var recordGranted by mutableStateOf(false)
    private var overlayRunning by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshPermissions()
        if (recordGranted && overlayGranted) requestProjection()
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            OverlayService.start(this, result.resultCode, data)
            overlayRunning = true
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissions()
        setContent {
            val skipBoot = intent.getBooleanExtra(EXTRA_SKIP_BOOT, false)
            var connected by rememberSaveable { mutableStateOf(skipBoot) }
            HibikiTheme {
                Crossfade(
                    targetState = connected,
                    animationSpec = tween(durationMillis = 450),
                    label = "bootToHome",
                ) { isConnected ->
                    if (!isConnected) {
                        ConnectingScreen(onFinished = { connected = true })
                    } else {
                        HibikiNavHost(
                            onStartOverlay = ::onStartOverlay,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        overlayRunning = OverlayService.running
    }

    private fun refreshPermissions() {
        overlayGranted = Settings.canDrawOverlays(this)
        recordGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun onStartOverlay() {
        if (OverlayService.running) {
            OverlayService.stop(this)
            overlayRunning = false
            return
        }
        if (!overlayGranted) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                ),
            )
            return
        }
        val needed = buildList {
            if (!recordGranted) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
            return
        }
        requestProjection()
    }

    private fun requestProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val intent = if (Build.VERSION.SDK_INT >= 34) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
        projectionLauncher.launch(intent)
    }

    companion object {
        const val EXTRA_SKIP_BOOT = "skip_boot"

        fun openHome(context: Context) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_SKIP_BOOT, true)
                },
            )
        }
    }
}
