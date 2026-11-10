package com.shoetrack.presentation.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.sync.PairingCode
import com.shoetrack.domain.sync.QrCode
import com.shoetrack.presentation.list.PairingState
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.close
import shoetrack.shared.generated.resources.pair_code_label
import shoetrack.shared.generated.resources.pair_connect
import shoetrack.shared.generated.resources.pair_connecting
import shoetrack.shared.generated.resources.pair_failed
import shoetrack.shared.generated.resources.pair_hint
import shoetrack.shared.generated.resources.pair_invalid
import shoetrack.shared.generated.resources.pair_no_network
import shoetrack.shared.generated.resources.pair_paired
import shoetrack.shared.generated.resources.pair_scan
import shoetrack.shared.generated.resources.pair_title

/** Shows this device's QR and reads the QR of another one, so both start syncing. */
@Composable
fun PairingDialog(code: PairingCode?, state: PairingState, onPair: (String) -> Unit, onDismiss: () -> Unit) {
    val scanner = rememberQrScanner(onPair)
    var typedCode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pair_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.pair_hint), style = MaterialTheme.typography.bodyMedium)
                if (code == null) {
                    Text(stringResource(Res.string.pair_no_network), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val qr = remember(code) { QrCode.encode(code.encode()) }
                    QrImage(qr, Modifier.size(QR_SIZE))
                    SelectionContainer {
                        Text(code.encode(), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (scanner != null) {
                    Button(onClick = scanner::scan, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.pair_scan))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = typedCode,
                        onValueChange = { typedCode = it },
                        label = { Text(stringResource(Res.string.pair_code_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { onPair(typedCode) }, enabled = typedCode.isNotBlank()) {
                        Text(stringResource(Res.string.pair_connect))
                    }
                }
                val message =
                    when (state) {
                        PairingState.Idle -> null
                        PairingState.Connecting -> Res.string.pair_connecting
                        PairingState.Paired -> Res.string.pair_paired
                        PairingState.Failed -> Res.string.pair_failed
                        PairingState.Invalid -> Res.string.pair_invalid
                    }
                if (message != null) Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}

@Composable
private fun QrImage(qr: QrCode, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color.White)
        val cells = qr.size + 2 * QUIET_ZONE
        val cell = size.width / cells
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (qr.isDark(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + QUIET_ZONE) * cell, (y + QUIET_ZONE) * cell),
                        // A hair of overlap avoids visible seams between neighbouring cells.
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}

private const val QUIET_ZONE = 4
private val QR_SIZE = 200.dp
