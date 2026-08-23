package com.hibiki.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hibiki.data.media.SquareCropTransform
import com.hibiki.data.media.SquareCropper
import com.hibiki.ui.theme.Cyberpunk
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SquareThumb(
    path: String?,
    modifier: Modifier = Modifier.size(48.dp),
    contentDescription: String? = null,
    showPlaceholder: Boolean = true,
) {
    val shape = RoundedCornerShape(4.dp)
    val bitmap = remember(path) {
        path?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)
        }
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(Cyberpunk.PanelElevated)
            .border(1.dp, Cyberpunk.GridLine, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else if (showPlaceholder) {
            Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = contentDescription,
                tint = Cyberpunk.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun SquareImagePicker(
    imagePath: String?,
    onCropped: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    label: String = "Immagine",
) {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingUri = uri
    }

    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        loadError = null
        runCatching { SquareCropper.decode(context, uri) }
            .onSuccess { sourceBitmap = it }
            .onFailure { loadError = it.message ?: "Immagine non valida" }
        pendingUri = null
    }

    sourceBitmap?.let { bitmap ->
        SquareCropDialog(
            bitmap = bitmap,
            onConfirm = { cropped ->
                sourceBitmap = null
                onCropped(cropped)
            },
            onDismiss = { sourceBitmap = null },
        )
    }

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Cyberpunk.MutedCyan,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        SquareThumb(
            path = imagePath,
            contentDescription = label,
            modifier = Modifier
                .size(size)
                .clickable { picker.launch("image/*") },
        )
        loadError?.let {
            Text(it, color = Cyberpunk.NeonMagenta, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SquareCropDialog(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    var userScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportPx by remember { mutableFloatStateOf(0f) }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Ritaglia",
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .heightIn(max = 360.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Cyberpunk.Void)
                .border(1.dp, Cyberpunk.GridLine, RoundedCornerShape(8.dp))
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val sizePx = minOf(size.width, size.height).toFloat()
                        userScale = (userScale * zoom).coerceIn(1f, 6f)
                        val nextScale = SquareCropper.displayedScale(bitmap, sizePx, userScale)
                        val displayedW = bitmap.width * nextScale
                        val displayedH = bitmap.height * nextScale
                        val maxX = ((displayedW - sizePx) / 2f).coerceAtLeast(0f)
                        val maxY = ((displayedH - sizePx) / 2f).coerceAtLeast(0f)
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                },
        ) {
            val sizePx = size.minDimension
            viewportPx = sizePx
            val scale = SquareCropper.displayedScale(bitmap, sizePx, userScale)
            val displayedW = bitmap.width * scale
            val displayedH = bitmap.height * scale
            val left = (size.width - displayedW) / 2f + offset.x
            val top = (size.height - displayedH) / 2f + offset.y
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(displayedW.roundToInt().coerceAtLeast(1), displayedH.roundToInt().coerceAtLeast(1)),
            )
        }
        HibikiButton(
            text = "CONFERMA",
            onClick = {
                val cropped = SquareCropper.crop(
                    bitmap,
                    SquareCropTransform(
                        userScale = userScale,
                        offsetX = offset.x,
                        offsetY = offset.y,
                        viewportSize = viewportPx.coerceAtLeast(1f),
                    ),
                )
                onConfirm(cropped)
            },
            style = HibikiButtonStyles.Primary,
            modifier = Modifier.padding(top = 12.dp),
        )
        HibikiButton(
            text = "ANNULLA",
            onClick = onDismiss,
            style = HibikiButtonStyles.Cancel,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
