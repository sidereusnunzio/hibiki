package com.hibiki

import android.Manifest
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hibiki.overlay.OverlayService
import com.hibiki.ui.boot.ConnectingScreen
import com.hibiki.ui.navigation.HibikiNavHost
import com.hibiki.ui.theme.Cyberpunk
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
            var connected by rememberSaveable { mutableStateOf(false) }
            HibikiTheme {
                Crossfade(
                    targetState = connected,
                    animationSpec = tween(durationMillis = 450),
                    label = "bootToHome",
                ) { isConnected ->
                    if (!isConnected) {
                        ConnectingScreen(onFinished = { connected = true })
                    } else {
                        Scaffold(
                            containerColor = Cyberpunk.Void,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                HibikiNavHost(
                                    onStartOverlay = ::onStartOverlay,
                                )
                            }
                        }
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
}
