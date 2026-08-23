package com.hibiki.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SquareCropTransform(
    val userScale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val viewportSize: Float,
)

object SquareCropper {
    const val MAX_DECODE_SIZE = 2048
    const val OUTPUT_SIZE = 1024

    fun decode(context: Context, uri: Uri, maxSize: Int = MAX_DECODE_SIZE): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val sample = max(1, largest / maxSize)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        } ?: error("Impossibile leggere l'immagine")
    }

    fun minUserScale(bitmap: Bitmap): Float = 1f

    fun displayedScale(bitmap: Bitmap, viewportSize: Float, userScale: Float): Float {
        val minSide = min(bitmap.width, bitmap.height).coerceAtLeast(1)
        return (viewportSize / minSide) * userScale.coerceAtLeast(1f)
    }

    fun crop(bitmap: Bitmap, transform: SquareCropTransform): Bitmap {
        val scale = displayedScale(bitmap, transform.viewportSize, transform.userScale)
        val displayedW = bitmap.width * scale
        val displayedH = bitmap.height * scale
        val originX = (transform.viewportSize - displayedW) / 2f + transform.offsetX
        val originY = (transform.viewportSize - displayedH) / 2f + transform.offsetY
        val srcSize = (transform.viewportSize / scale).roundToInt().coerceAtLeast(1)
        val srcLeft = ((0f - originX) / scale).roundToInt()
        val srcTop = ((0f - originY) / scale).roundToInt()
        val left = srcLeft.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
        val top = srcTop.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        val width = srcSize.coerceAtMost(bitmap.width - left).coerceAtLeast(1)
        val height = srcSize.coerceAtMost(bitmap.height - top).coerceAtLeast(1)
        val side = min(width, height)
        val square = Bitmap.createBitmap(bitmap, left, top, side, side)
        if (side == OUTPUT_SIZE) return square
        return Bitmap.createScaledBitmap(square, OUTPUT_SIZE, OUTPUT_SIZE, true).also {
            if (it != square) square.recycle()
        }
    }
}
