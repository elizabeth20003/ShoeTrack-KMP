package com.shoetrack.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.presentation.components.BackButton
import com.shoetrack.presentation.photo.SCAN_PHOTO_SIDE_PX
import com.shoetrack.presentation.photo.rememberPhotoPicker
import com.shoetrack.presentation.theme.ShoeTrackTheme
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.ai_enabled
import shoetrack.shared.generated.resources.ai_hint
import shoetrack.shared.generated.resources.ai_key_label
import shoetrack.shared.generated.resources.ai_key_remove
import shoetrack.shared.generated.resources.ai_key_save
import shoetrack.shared.generated.resources.ai_title
import shoetrack.shared.generated.resources.ai_unavailable
import shoetrack.shared.generated.resources.choose_photo
import shoetrack.shared.generated.resources.scan_failed
import shoetrack.shared.generated.resources.scan_hint
import shoetrack.shared.generated.resources.scan_no_layaway
import shoetrack.shared.generated.resources.scan_no_layaway_hint
import shoetrack.shared.generated.resources.scan_notebook
import shoetrack.shared.generated.resources.scan_nothing_found
import shoetrack.shared.generated.resources.scan_reading
import shoetrack.shared.generated.resources.scan_result_title
import shoetrack.shared.generated.resources.take_photo

@Composable
fun ScanScreen(viewModel: ScanViewModel, onBack: () -> Unit, onEntryRead: (NotebookEntry) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val photoPicker =
        rememberPhotoPicker(
            onPhotoPicked = viewModel::onPhotoPicked,
            maxSidePx = SCAN_PHOTO_SIDE_PX,
        )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanEvent.EntryRead -> onEntryRead(event.entry)
            }
        }
    }

    ScanContent(
        uiState = uiState,
        onBack = onBack,
        onTakePhoto = photoPicker::takePhoto,
        onPickPhoto = photoPicker::pickFromGallery,
        onSaveKey = viewModel::onAiKeySaved,
        onRemoveKey = viewModel::onAiKeyRemoved,
    )
}

@Composable
internal fun ScanContent(
    uiState: ScanUiState,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSaveKey: (String) -> Unit,
    onRemoveKey: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(
                    onBack,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(Res.string.scan_notebook), style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = stringResource(Res.string.scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onTakePhoto,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(Res.string.take_photo)) }
                OutlinedButton(
                    onClick = onPickPhoto,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(Res.string.choose_photo)) }
            }
            if (uiState.aiUnavailable) {
                Text(
                    text = stringResource(Res.string.ai_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ScanResult(uiState)
            AiKeyCard(hasKey = uiState.hasAiKey, onSave = onSaveKey, onRemove = onRemoveKey)
        }
    }
}

@Composable
private fun AiKeyCard(hasKey: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.ai_title), style = MaterialTheme.typography.titleMedium)
        if (hasKey) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(Res.string.ai_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) { Text(stringResource(Res.string.ai_key_remove)) }
            }
        } else {
            var key by remember { mutableStateOf("") }
            Text(
                text = stringResource(Res.string.ai_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(Res.string.ai_key_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(key) },
                enabled = key.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(Res.string.ai_key_save)) }
        }
    }
}

@Composable
private fun ScanResult(uiState: ScanUiState) {
    when {
        uiState.isReading ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(Res.string.scan_reading))
            }

        uiState.hasFailed ->
            Text(
                text = stringResource(Res.string.scan_failed),
                color = MaterialTheme.colorScheme.error,
            )

        uiState.foundNothing ->
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(Res.string.scan_no_layaway), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.scan_no_layaway_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(Res.string.scan_result_title), style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(
                        text = uiState.text.orEmpty().ifBlank { stringResource(Res.string.scan_nothing_found) },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

        uiState.text != null ->
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(Res.string.scan_result_title), style = MaterialTheme.typography.titleMedium)
                SelectionContainer {
                    Text(
                        text = uiState.text.ifBlank { stringResource(Res.string.scan_nothing_found) },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
    }
}

@Preview
@Composable
private fun ScanContentPreview() {
    ShoeTrackTheme {
        ScanContent(
            uiState = ScanUiState(text = "03/10/26 Alba Calambas\nAbona 30.000 Saldo 110.000"),
            onBack = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onSaveKey = {},
            onRemoveKey = {},
        )
    }
}
