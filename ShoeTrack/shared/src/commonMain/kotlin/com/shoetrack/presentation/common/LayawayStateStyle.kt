package com.shoetrack.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shoetrack.domain.model.LayawayState
import org.jetbrains.compose.resources.StringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.state_active
import shoetrack.shared.generated.resources.state_cancelled
import shoetrack.shared.generated.resources.state_completed
import shoetrack.shared.generated.resources.state_overdue

internal fun LayawayState.label(): StringResource = when (this) {
    LayawayState.Active -> Res.string.state_active
    LayawayState.Overdue -> Res.string.state_overdue
    LayawayState.Completed -> Res.string.state_completed
    LayawayState.Cancelled -> Res.string.state_cancelled
}

@Composable
internal fun LayawayState.color(): Color = when (this) {
    LayawayState.Active -> MaterialTheme.colorScheme.primary
    LayawayState.Overdue -> MaterialTheme.colorScheme.error
    LayawayState.Completed -> MaterialTheme.colorScheme.tertiary
    LayawayState.Cancelled -> MaterialTheme.colorScheme.outline
}
