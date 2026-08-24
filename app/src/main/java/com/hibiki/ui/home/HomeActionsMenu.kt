package com.hibiki.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hibiki.R
import com.hibiki.ui.components.AppPageAction
import com.hibiki.ui.theme.Cyberpunk

@Composable
fun HomeActionsMenu(
    onOpenContexts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppPageAction(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.home_actions),
                tint = Cyberpunk.TextPrimary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Cyberpunk.PanelElevated,
        ) {
            HomeActionMenuItem(
                label = "Contesti",
                icon = Icons.Filled.Category,
                onClick = {
                    expanded = false
                    onOpenContexts()
                },
            )
            HomeActionMenuItem(
                label = "Impostazioni",
                icon = Icons.Filled.Settings,
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
            HorizontalDivider(color = Cyberpunk.GridLine)
            HomeActionMenuItem(
                label = stringResource(R.string.home_about),
                icon = Icons.Filled.Info,
                onClick = {
                    expanded = false
                    onOpenAbout()
                },
            )
        }
    }
}

@Composable
private fun HomeActionMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = label, color = Cyberpunk.TextPrimary) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Cyberpunk.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        },
        onClick = onClick,
    )
}
