package com.shoetrack.presentation.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.back

/** A round back button with the standard 48 dp touch target. */
@Composable
fun BackButton(onBack: () -> Unit, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.back)
    IconButton(
        onClick = onBack,
        colors = IconButtonDefaults.iconButtonColors(containerColor = containerColor),
        modifier = modifier.semantics { contentDescription = label },
    ) {
        AppIcon(IconShape.Back, contentColor, size = 22.dp)
    }
}
