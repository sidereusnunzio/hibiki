package com.hibiki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.theme.Cyberpunk

val AppPageHorizontalPadding = 16.dp
val AppPageTopPadding = 12.dp
val AppPageActionSize = 32.dp

@Composable
fun AppPage(
    title: String,
    modifier: Modifier = Modifier,
    kanji: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppPageHorizontalPadding)
            .padding(top = AppPageTopPadding, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppPageTitleRow(title = title, kanji = kanji, modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(colorStops = Cyberpunk.TitleUnderlineGradient)),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            content()
        }
    }
}

@Composable
fun AppPageOverlayActions(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppPageHorizontalPadding)
            .padding(top = AppPageTopPadding),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions()
    }
}

@Composable
fun AppPageAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AppPageActionSize),
    ) {
        content()
    }
}

@Composable
fun AppPageBackAction(onBack: () -> Unit) {
    AppPageAction(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Indietro",
            tint = Cyberpunk.TextPrimary,
        )
    }
}

@Composable
fun AppPageTitleRow(
    title: String,
    modifier: Modifier = Modifier,
    kanji: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (kanji != null) {
            Text(
                text = kanji,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp,
                color = Cyberpunk.NeonCyan,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Cyberpunk.NeonCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Cyberpunk.MutedCyan,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
fun FieldValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(label)
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            color = if (accent) Cyberpunk.NeonCyan else Cyberpunk.TextPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
