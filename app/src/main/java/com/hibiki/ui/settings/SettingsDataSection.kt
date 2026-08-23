package com.hibiki.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hibiki.data.arashi.ArashiExportContract
import com.hibiki.data.arashi.model.ArashiExportType
import com.hibiki.data.backup.BackupConstants
import com.hibiki.domain.model.LastArashiExport
import com.hibiki.ui.components.DetailSection
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.components.HibikiConfirmDialog
import com.hibiki.ui.components.HibikiDialog
import com.hibiki.ui.theme.Cyberpunk
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsDataSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf(false) }
    var showArashiChoice by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveExportToUri(context, uri)
        } else {
            viewModel.cancelPendingExport()
        }
    }

    LaunchedEffect(uiState.backupState) {
        if (uiState.backupState is BackupState.ExportReady) {
            exportLauncher.launch(BackupConstants.defaultExportFileName())
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri)
        }
    }

    val arashiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onArashiActivityResult(
            context = context,
            resultCode = result.resultCode,
            resultJson = result.data?.getStringExtra(ArashiExportContract.EXTRA_IMPORT_RESULT),
            errorMessage = result.data?.getStringExtra(ArashiExportContract.EXTRA_IMPORT_ERROR),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.arashiLaunch.collect { intent ->
            runCatching { arashiLauncher.launch(intent) }
                .onFailure { viewModel.onArashiLaunchFailed(context, it) }
        }
    }

    if (showImportConfirm) {
        HibikiConfirmDialog(
            title = "Importare il backup?",
            message = "Questa operazione cancella tutti i dati locali (frasi, contesti, audio e immagini) e li sostituisce con il contenuto del file selezionato. Le impostazioni dell'app non vengono modificate. L'operazione non può essere annullata.",
            onConfirm = {
                showImportConfirm = false
                viewModel.clearBackupState()
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            },
            onDismiss = { showImportConfirm = false },
            confirmStyle = HibikiButtonStyles.Destructive,
        )
    }

    if (showArashiChoice) {
        ArashiExportChoiceDialog(
            hasPreviousExport = uiState.lastArashiExport != null,
            onPartial = {
                showArashiChoice = false
                viewModel.clearArashiExportState()
                viewModel.startArashiExport(context, ArashiExportType.PARTIAL)
            },
            onFull = {
                showArashiChoice = false
                viewModel.clearArashiExportState()
                viewModel.startArashiExport(context, ArashiExportType.FULL)
            },
            onDismiss = { showArashiChoice = false },
        )
    }

    DetailSection(
        title = "Backup e ripristino",
        modifier = modifier,
    ) {
        Text(
            text = "Esporta o ripristina frasi, contesti, audio e immagini. L'importazione sostituisce completamente i dati locali attuali.",
            style = MaterialTheme.typography.bodySmall,
            color = Cyberpunk.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HibikiButton(
                modifier = Modifier.weight(1f),
                text = "ESPORTA",
                onClick = {
                    viewModel.clearBackupState()
                    viewModel.startExport(context)
                },
                enabled = uiState.backupState !is BackupState.Exporting &&
                    uiState.backupState !is BackupState.ExportReady &&
                    uiState.backupState !is BackupState.Importing &&
                    uiState.arashiExportState !is ArashiExportState.Preparing,
                style = HibikiButtonStyles.Primary,
                fillMaxWidth = false,
            )
            HibikiButton(
                modifier = Modifier.weight(1f),
                text = "IMPORTA",
                onClick = { showImportConfirm = true },
                enabled = uiState.backupState !is BackupState.Exporting &&
                    uiState.backupState !is BackupState.Importing &&
                    uiState.arashiExportState !is ArashiExportState.Preparing,
                style = HibikiButtonStyles.Destructive,
                fillMaxWidth = false,
            )
        }

        when (val backupState = uiState.backupState) {
            is BackupState.Exporting -> SettingsStatusLine(
                text = "Esportazione… ${backupState.message}",
                color = Cyberpunk.NeonCyan,
            )
            BackupState.ExportReady -> SettingsStatusLine(
                text = "Esportazione… Seleziona destinazione…",
                color = Cyberpunk.NeonCyan,
            )
            is BackupState.Importing -> SettingsStatusLine(
                text = "Importazione… ${backupState.message}",
                color = Cyberpunk.NeonMagenta,
            )
            is BackupState.Success -> SettingsStatusLine(
                text = backupState.message,
                color = Cyberpunk.NeonLime,
            )
            is BackupState.Error -> SettingsStatusLine(
                text = backupState.message,
                color = Cyberpunk.NeonMagenta,
            )
            BackupState.Idle -> Unit
        }

        HibikiButton(
            modifier = Modifier.padding(top = 16.dp),
            text = "ESPORTA IN ARASHI",
            onClick = { showArashiChoice = true },
            enabled = uiState.backupState !is BackupState.Exporting &&
                uiState.backupState !is BackupState.Importing &&
                uiState.arashiExportState !is ArashiExportState.Preparing,
            style = HibikiButtonStyles.Violet,
        )
        Text(
            text = lastArashiExportLabel(uiState.lastArashiExport),
            style = MaterialTheme.typography.bodySmall,
            color = Cyberpunk.TextMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
        when (val arashiState = uiState.arashiExportState) {
            ArashiExportState.Preparing -> SettingsStatusLine(
                text = "Preparazione export verso Arashi…",
                color = Cyberpunk.NeonCyan,
            )
            is ArashiExportState.Success -> SettingsStatusLine(
                text = arashiState.summary,
                color = Cyberpunk.NeonLime,
            )
            is ArashiExportState.Error -> SettingsStatusLine(
                text = arashiState.message,
                color = Cyberpunk.NeonMagenta,
            )
            ArashiExportState.Idle -> Unit
        }
    }
}

@Composable
private fun ArashiExportChoiceDialog(
    hasPreviousExport: Boolean,
    onPartial: () -> Unit,
    onFull: () -> Unit,
    onDismiss: () -> Unit,
) {
    HibikiDialog(onDismissRequest = onDismiss) {
        Text(
            text = "Esporta in Arashi",
            style = MaterialTheme.typography.titleMedium,
            color = Cyberpunk.TextPrimary,
        )
        Text(
            text = if (hasPreviousExport) {
                "Scegli se inviare solo le frasi nuove o modificate, oppure tutte."
            } else {
                "Non esiste un export precedente: verrà inviato l'elenco completo."
            },
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Cyberpunk.TextMuted,
        )
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HibikiButton(
                text = if (hasPreviousExport) "EXPORT PARZIALE" else "TUTTE LE FRASI",
                onClick = onPartial,
                style = HibikiButtonStyles.Primary,
            )
            HibikiButton(
                text = "EXPORT TOTALE",
                onClick = onFull,
                style = HibikiButtonStyles.Secondary,
            )
            HibikiButton(
                text = "ANNULLA",
                onClick = onDismiss,
                style = HibikiButtonStyles.Cancel,
            )
        }
    }
}

private fun lastArashiExportLabel(last: LastArashiExport?): String {
    if (last == null) return "Mai esportato in Arashi"
    val formatted = LAST_EXPORT_FORMAT.format(Instant.ofEpochMilli(last.exportedAtEpochMs))
    return "Ultimo export riuscito:\n$formatted\n${last.phraseCount} frasi inviate"
}

private val LAST_EXPORT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun SettingsStatusLine(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}
