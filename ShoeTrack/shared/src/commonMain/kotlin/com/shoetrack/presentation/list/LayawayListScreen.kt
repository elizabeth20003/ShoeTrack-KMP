package com.shoetrack.presentation.list

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.LayawayTotals
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.totals
import com.shoetrack.domain.sync.SyncStatus
import com.shoetrack.presentation.components.AppIcon
import com.shoetrack.presentation.components.IconShape
import com.shoetrack.presentation.components.PaymentProgress
import com.shoetrack.presentation.components.SneakerIllustration
import com.shoetrack.presentation.components.colorwayFor
import com.shoetrack.presentation.components.progressColorFor
import com.shoetrack.presentation.format.format
import com.shoetrack.presentation.format.formatAsTime
import com.shoetrack.presentation.photo.ShoePhoto
import com.shoetrack.presentation.theme.ShoeTrackTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.badge_paid
import shoetrack.shared.generated.resources.clear
import shoetrack.shared.generated.resources.empty_message
import shoetrack.shared.generated.resources.empty_title
import shoetrack.shared.generated.resources.error_load_failed
import shoetrack.shared.generated.resources.filter_active
import shoetrack.shared.generated.resources.filter_all
import shoetrack.shared.generated.resources.filter_overdue
import shoetrack.shared.generated.resources.filter_paid
import shoetrack.shared.generated.resources.layaways_title
import shoetrack.shared.generated.resources.new_layaway
import shoetrack.shared.generated.resources.no_results
import shoetrack.shared.generated.resources.owed_summary
import shoetrack.shared.generated.resources.search_hint
import shoetrack.shared.generated.resources.state_overdue
import shoetrack.shared.generated.resources.sync_failed
import shoetrack.shared.generated.resources.sync_in_progress
import shoetrack.shared.generated.resources.sync_nearby
import shoetrack.shared.generated.resources.sync_nearby_synced
import shoetrack.shared.generated.resources.sync_no_devices
import shoetrack.shared.generated.resources.total_owed
import kotlin.math.roundToInt
import kotlin.time.Instant

private val PhotoSize = 96.dp
private val FullWidth = GridItemSpan(1)

@Composable
fun LayawayListScreen(
    viewModel: LayawayListViewModel,
    onNewLayaway: () -> Unit,
    onOpenLayaway: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    LayawayListContent(
        uiState, query, syncStatus, viewModel::onQueryChange, viewModel::onFilterChange, onNewLayaway, onOpenLayaway,
    )
}

@Composable
internal fun LayawayListContent(
    uiState: LayawayListUiState,
    query: String,
    syncStatus: SyncStatus,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LayawayFilter) -> Unit,
    onNewLayaway: () -> Unit,
    onOpenLayaway: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewLayaway,
                containerColor = ShoeTrackTheme.colors.sun,
                contentColor = ShoeTrackTheme.colors.onSun,
                shape = RoundedCornerShape(20.dp),
            ) {
                AppIcon(IconShape.Plus, ShoeTrackTheme.colors.onSun)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.new_layaway), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                LayawayListUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                LayawayListUiState.Error ->
                    Text(
                        text = stringResource(Res.string.error_load_failed),
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )

                is LayawayListUiState.Loaded ->
                    LayawayGrid(uiState, query, syncStatus, onQueryChange, onFilterChange, onOpenLayaway)
            }
        }
    }
}

@Composable
private fun LayawayGrid(
    state: LayawayListUiState.Loaded,
    query: String,
    syncStatus: SyncStatus,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LayawayFilter) -> Unit,
    onOpenLayaway: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { FullWidth }) { Header(syncStatus) }
        if (!state.hasAnyLayaways) {
            item(span = { FullWidth }) {
                EmptyState(stringResource(Res.string.empty_title), stringResource(Res.string.empty_message))
            }
        } else {
            item(span = { FullWidth }) { OwedHero(state.totals) }
            item(span = { FullWidth }) { SearchField(query, onQueryChange) }
            item(span = { FullWidth }) { FilterChips(state.filter, onFilterChange) }
            layawayCards(state.layaways, onOpenLayaway)
        }
    }
}

private fun LazyGridScope.layawayCards(layaways: List<LayawaySummary>, onOpen: (String) -> Unit) {
    if (layaways.isEmpty()) {
        item(span = { FullWidth }) { EmptyState(stringResource(Res.string.no_results), null) }
        return
    }
    items(layaways, key = { it.id }) { layaway ->
        LayawayCard(layaway, onClick = { onOpen(layaway.id) })
    }
}

@Composable
private fun Header(status: SyncStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.layaways_title),
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 1,
            softWrap = false,
        )
        // The title never wraps: the pill takes whatever width is left and shortens its text.
        Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterEnd) {
            SyncPill(status)
        }
    }
}

@Composable
private fun SyncPill(status: SyncStatus) {
    val text = when {
        status.hasFailed -> stringResource(Res.string.sync_failed)
        status.isSyncing -> stringResource(Res.string.sync_in_progress)
        status.nearbyDevices == 0 -> stringResource(Res.string.sync_no_devices)
        status.lastSyncAt == null -> stringResource(Res.string.sync_nearby, status.nearbyDevices)
        else -> stringResource(Res.string.sync_nearby_synced, status.nearbyDevices, status.lastSyncAt.formatAsTime())
    }
    val connected = status.nearbyDevices > 0 && !status.hasFailed
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dot = if (connected) ShoeTrackTheme.colors.live else MaterialTheme.colorScheme.outline
        Box(
            Modifier.size(8.dp).alpha(if (connected) pulse else 1f).clip(CircleShape).background(dot),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OwedHero(totals: LayawayTotals) {
    val colors = ShoeTrackTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.hero)
            .padding(start = 20.dp, top = 18.dp, bottom = 20.dp, end = 20.dp),
    ) {
        SneakerIllustration(
            colorway = colors.colorways.first(),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-6).dp).width(130.dp).rotate(-9f),
        )
        Column {
            Text(
                text = stringResource(Res.string.total_owed),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onHero.copy(alpha = 0.8f),
            )
            Text(
                text = totals.owed.format(),
                style = ShoeTrackTheme.money.large,
                color = colors.onHero,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(Res.string.owed_summary, totals.openCount, totals.overdueCount),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onHero.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.search_hint)) },
        leadingIcon = { AppIcon(IconShape.Search, MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable { onQueryChange("") }.padding(12.dp),
                )
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FilterChips(selected: LayawayFilter, onSelect: (LayawayFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LayawayFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Text(
                text = stringResource(filter.label()),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) ShoeTrackTheme.colors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) ShoeTrackTheme.colors.hero else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

private fun LayawayFilter.label(): StringResource = when (this) {
    LayawayFilter.All -> Res.string.filter_all
    LayawayFilter.Active -> Res.string.filter_active
    LayawayFilter.Overdue -> Res.string.filter_overdue
    LayawayFilter.Paid -> Res.string.filter_paid
}

@Composable
private fun LayawayCard(layaway: LayawaySummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ShoeTrackTheme.colors
    val isOverdue = layaway.state == LayawayState.Overdue
    val isPaid = layaway.state == LayawayState.Completed
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(PhotoSize).clip(MaterialTheme.shapes.medium)) {
            CardPicture(layaway)
        }
        Column(Modifier.weight(1f).height(PhotoSize), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = layaway.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = layaway.productDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when {
                    isOverdue -> Badge(
                        stringResource(Res.string.state_overdue), colors.overdueBadge, colors.onOverdueBadge,
                    )
                    isPaid -> Badge(stringResource(Res.string.badge_paid), colors.doneBadge, colors.onDoneBadge)
                }
            }
            Text(text = layaway.remainingBalance.format(), style = ShoeTrackTheme.money.medium)
            val progressColor = progressColorFor(isOverdue, isPaid)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentProgress(layaway.paidFraction, progressColor, Modifier.weight(1f))
                Text(
                    text = "${(layaway.paidFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor,
                )
            }
        }
    }
}

@Composable
private fun CardPicture(layaway: LayawaySummary) {
    if (layaway.productPhotoPath.isNotBlank()) {
        ShoePhoto(name = layaway.productPhotoPath, modifier = Modifier.fillMaxSize())
        return
    }
    val colorway = colorwayFor(layaway.id)
    Box(Modifier.fillMaxSize().background(colorway.backdrop), contentAlignment = Alignment.Center) {
        SneakerIllustration(colorway, Modifier.fillMaxWidth(0.8f))
    }
}

@Composable
private fun Badge(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun EmptyState(title: String, message: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SneakerIllustration(
            colorway = ShoeTrackTheme.colors.colorways.first(),
            modifier = Modifier.width(180.dp).rotate(-6f),
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            totalPrice = Money.fromUnits(300_000),
            state = LayawayState.Active,
            lastPaymentAt = Instant.parse("2026-10-01T00:00:00Z"),
        ),
        LayawaySummary(
            id = "2",
            customerName = "Luis Gomez",
            productDescription = "Adidas Samba 40",
            productPhotoPath = "",
            remainingBalance = Money.fromUnits(95_000),
            totalPrice = Money.fromUnits(310_000),
            state = LayawayState.Overdue,
            lastPaymentAt = Instant.parse("2026-08-15T00:00:00Z"),
        ),
    )
    ShoeTrackTheme {
        LayawayListContent(
            uiState = LayawayListUiState.Loaded(samples, hasAnyLayaways = true, totals = samples.totals()),
            query = "",
            syncStatus = SyncStatus(nearbyDevices = 1, lastSyncAt = Instant.parse("2026-10-01T10:30:00Z")),
            onQueryChange = {},
            onFilterChange = {},
            onNewLayaway = {},
            onOpenLayaway = {},
        )
    }
}
