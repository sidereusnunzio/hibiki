package com.hibiki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hibiki.R
import com.hibiki.ui.theme.Cyberpunk

/** Sfondo pietra scura (stesso asset e scrim di Arashi). */
@Composable
fun StoneBackground(
    scrimAlpha: Float = 0.45f,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_dark_stone),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cyberpunk.withAlpha(Cyberpunk.Void, scrimAlpha)),
        )
        content()
    }
}
