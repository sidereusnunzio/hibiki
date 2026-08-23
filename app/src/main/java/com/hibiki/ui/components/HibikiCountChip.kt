package com.hibiki.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibiki.ui.theme.Cyberpunk
import java.util.Locale

@Composable
fun HibikiCountChip(
    label: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Cyberpunk.MutedCyan,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(999.dp),
            color = Cyberpunk.PanelElevated,
        ) {
            Text(
                text = label.uppercase(Locale.ITALY),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor,
            )
        }
    }
}
