package com.shoetrack.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.Money
import com.shoetrack.presentation.common.color
import com.shoetrack.presentation.common.label
import com.shoetrack.presentation.format.format
import com.shoetrack.presentation.format.formatAsDate
import com.shoetrack.presentation.photo.ShoePhoto
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.balance_label
import shoetrack.shared.generated.resources.empty_message
import shoetrack.shared.generated.resources.empty_title
import shoetrack.shared.generated.resources.error_load_failed
import shoetrack.shared.generated.resources.last_payment_label
import shoetrack.shared.generated.resources.layaways_title
import shoetrack.shared.generated.resources.new_layaway
import kotlin.time.Instant

@Composable
fun LayawayListScreen(
    viewModel: LayawayListViewModel,
    onNewLayaway: () -> Unit,
    onOpenLayaway: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LayawayListContent(uiState, onNewLayaway, onOpenLayaway)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LayawayListContent(
    uiState: LayawayListUiState,
    onNewLayaway: () -> Unit,
    onOpenLayaway: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(Res.string.layaways_title)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewLayaway) {
                Text(stringResource(Res.string.new_layaway))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                LayawayListUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                LayawayListUiState.Error ->
                    Text(
                        text = stringResource(Res.string.error_load_failed),
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )

                is LayawayListUiState.Loaded ->
                    if (uiState.layaways.isEmpty()) {
                        EmptyState()
                    } else {
                        LayawayList(uiState.layaways, onOpenLayaway)
                    }
            }
        }
    }
}

@Composable
private fun LayawayList(layaways: List<LayawaySummary>, onOpenLayaway: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(layaways, key = { it.id }) { layaway ->
            LayawayCard(layaway, onClick = { onOpenLayaway(layaway.id) })
        }
    }
}

@Composable
private fun LayawayCard(layaway: LayawaySummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (layaway.productPhotoPath.isNotBlank()) {
                ShoePhoto(
                    name = layaway.productPhotoPath,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            LayawayCardText(layaway, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LayawayCardText(layaway: LayawaySummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = layaway.productDescription,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(layaway.state.label()),
                style = MaterialTheme.typography.labelLarge,
                color = layaway.state.color(),
            )
        }
        Text(text = layaway.customerName, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(Res.string.balance_label, layaway.remainingBalance.format()),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(Res.string.last_payment_label, layaway.lastPaymentAt.formatAsDate()),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.empty_message), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun LayawayListContentPreview() {
    val samples = listOf(
        LayawaySummary(
            id = "1",
            customerName = "Ana Perez",
            productDescription = "Nike Air Force 42",
            productPhotoPath = "",
            remainingBalance = Money.fromUnits(180_000),
            state = LayawayState.Active,
            lastPaymentAt = Instant.parse("2026-10-01T00:00:00Z"),
        ),
        LayawaySummary(
            id = "2",
            customerName = "Luis Gomez",
            productDescription = "Adidas Samba 40",
            productPhotoPath = "",
            remainingBalance = Money.fromUnits(95_000),
            state = LayawayState.Overdue,
            lastPaymentAt = Instant.parse("2026-08-15T00:00:00Z"),
        ),
    )
    MaterialTheme {
        LayawayListContent(LayawayListUiState.Loaded(samples), onNewLayaway = {}, onOpenLayaway = {})
    }
}
