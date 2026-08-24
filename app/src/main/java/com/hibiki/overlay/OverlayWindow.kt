package com.hibiki.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hibiki.ui.theme.HibikiTheme

class OverlayWindow(
    private val context: Context,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun attach(
        initialX: Int,
        initialY: Int,
        widthDp: Int = EXPANDED_WIDTH_DP,
        content: @Composable () -> Unit,
    ) {
        if (view != null) return
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val params = WindowManager.LayoutParams(
            dp(widthDp),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            overlayFlags(focusable = false),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        layoutParams = params
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayWindow)
            setViewTreeViewModelStoreOwner(this@OverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@OverlayWindow)
            setContent { HibikiTheme { content() } }
        }
        view = composeView
        windowManager.addView(composeView, params)
    }

    fun setWidthDp(widthDp: Int) {
        val params = layoutParams ?: return
        val widthPx = dp(widthDp)
        if (params.width == widthPx) return
        params.width = widthPx
        view?.let { windowManager.updateViewLayout(it, params) }
    }

    fun updatePositionBy(dx: Int, dy: Int) {
        val params = layoutParams ?: return
        params.x += dx
        params.y += dy
        view?.let { windowManager.updateViewLayout(it, params) }
        onPositionChanged(params.x, params.y)
    }

    fun setFocusable(focusable: Boolean) {
        val params = layoutParams ?: return
        params.flags = overlayFlags(focusable)
        view?.let { windowManager.updateViewLayout(it, params) }
    }

    fun dismiss() {
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }

    private fun overlayFlags(focusable: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!focusable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        return flags
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    companion object {
        const val EXPANDED_WIDTH_DP = 320
        const val COLLAPSED_WIDTH_DP = 120
        const val PHRASE_WIDTH_DP = 320
        const val PHRASE_OFFSET_Y_DP = 220
    }
}
