package com.hibiki.ui.contexts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hibiki.ui.components.AppPage
import com.hibiki.ui.components.AppPageBackAction
import com.hibiki.ui.components.HibikiButton
import com.hibiki.ui.components.HibikiButtonStyles
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.cyberpunkOutlinedTextFieldColors

@Composable
fun SubjectEditScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SubjectEditViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppPage(
        title = if (state.id.isBlank()) "Nuovo personaggio" else "Modifica personaggio",
        kanji = "名",
        actions = { AppPageBackAction(onBack) },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::setDisplayName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome") },
                colors = cyberpunkOutlinedTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.japaneseName,
                onValueChange = viewModel::setJapaneseName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome giapponese") },
                colors = cyberpunkOutlinedTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prompt aggiuntivo") },
                minLines = 4,
                colors = cyberpunkOutlinedTextFieldColors(),
            )
            state.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Cyberpunk.NeonMagenta)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HibikiButton("SALVA", { viewModel.save(onDone) }, HibikiButtonStyles.Primary)
        }
    }
}
