package com.shoetrack.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayDetail
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.presentation.components.AppIcon
import com.shoetrack.presentation.components.IconShape
import com.shoetrack.presentation.components.PaymentProgress
import com.shoetrack.presentation.components.SneakerIllustration
import com.shoetrack.presentation.components.colorwayFor
import com.shoetrack.presentation.components.progressColorFor
import com.shoetrack.presentation.format.format
import com.shoetrack.presentation.format.formatAsDate
import com.shoetrack.presentation.photo.ShoePhoto
import com.shoetrack.presentation.theme.ShoeTrackTheme
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.add_payment
import shoetrack.shared.generated.resources.back
import shoetrack.shared.generated.resources.cancel
import shoetrack.shared.generated.resources.confirm
import shoetrack.shared.generated.resources.error_load_failed
import shoetrack.shared.generated.resources.error_payment_failed
import shoetrack.shared.generated.resources.layaway_not_found
import shoetrack.shared.generated.resources.left_of
import shoetrack.shared.generated.resources.next_payment_in_days
import shoetrack.shared.generated.resources.next_payment_today
import shoetrack.shared.generated.resources.no_payments
import shoetrack.shared.generated.resources.paid_in_full_pill
import shoetrack.shared.generated.resources.paid_percent
import shoetrack.shared.generated.resources.payment_amount
import shoetrack.shared.generated.resources.payment_exceeds_balance
import shoetrack.shared.generated.resources.payments_title
import shoetrack.shared.generated.resources.state_cancelled
import shoetrack.shared.generated.resources.state_overdue
import kotlin.math.roundToInt
import kotlin.time.Instant

private val SheetTop = 272.dp
private val SheetOverlap = 32.dp

@Composable
fun LayawayDetailScreen(viewModel: LayawayDetailViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LayawayDetailEvent.PaymentFailed ->
                    snackbarHostState.showSnackbar(getString(Res.string.error_payment_failed))
            }
        }
    }

    LayawayDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddPayment = viewModel::openPaymentDialog,
        onPaymentInputChange = viewModel::onPaymentInputChange,
        onConfirmPayment = viewModel::confirmPayment,
        onDismissPayment = viewModel::dismissPaymentDialog,
    )
}

@Composable
internal fun LayawayDetailContent(
    uiState: LayawayDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddPayment: () -> Unit,
    onPaymentInputChange: (String) -> Unit,
    onConfirmPayment: () -> Unit,
    onDismissPayment: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val detail = uiState.detail
            when {
                uiState.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.loadFailed -> CenteredMessage(stringResource(Res.string.error_load_failed))

                detail == null -> CenteredMessage(stringResource(Res.string.layaway_not_found))

                else -> DetailBody(detail, uiState.daysUntilDue, onBack, onAddPayment)
            }
            if (detail == null) BackButton(onBack, Modifier.align(Alignment.TopStart).padding(16.dp))
        }
    }

    if (uiState.isPaymentDialogOpen) {
        PaymentDialog(
            input = uiState.paymentInput,
            canConfirm = uiState.canConfirmPayment,
            maxAmount = uiState.detail?.layaway?.remainingBalance?.format().takeIf { uiState.isPaymentTooHigh },
            onInputChange = onPaymentInputChange,
            onConfirm = onConfirmPayment,
            onDismiss = onDismissPayment,
        )
    }
}

@Composable
private fun BoxScope.CenteredMessage(text: String) {
    Text(text = text, modifier = Modifier.align(Alignment.Center).padding(32.dp))
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.back)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onBack)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(IconShape.Back, MaterialTheme.colorScheme.onSurface, size = 22.dp)
    }
}

@Composable
private fun DetailBody(detail: LayawayDetail, daysUntilDue: Int?, onBack: () -> Unit, onAddPayment: () -> Unit) {
    val layaway = detail.layaway
    val colorway = colorwayFor(layaway.id)
    BoxWithConstraints(Modifier.fillMaxSize().background(colorway.backdrop)) {
        val sheetMinHeight = maxHeight - SheetTop
        Box(Modifier.fillMaxWidth().height(SheetTop + SheetOverlap)) {
            if (detail.product.photoPath.isNotBlank()) {
                ShoePhoto(name = detail.product.photoPath, modifier = Modifier.fillMaxSize())
            } else {
                SneakerIllustration(
                    colorway = colorway,
                    modifier = Modifier.align(Alignment.Center).padding(top = 24.dp).fillMaxWidth(0.82f).rotate(-6f),
                )
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(SheetTop))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sheetMinHeight)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
            ) {
                SheetHeader(detail)
                Balance(detail)
                ProgressSection(detail)
                if (layaway.state == LayawayState.Active || layaway.state == LayawayState.Overdue) {
                    Button(
                        onClick = onAddPayment,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(stringResource(Res.string.add_payment), style = MaterialTheme.typography.titleMedium)
                    }
                }
                PaymentsSection(layaway.payments)
            }
        }
        BackButton(onBack, Modifier.align(Alignment.TopStart).padding(16.dp))
        StatusPill(layaway.state, daysUntilDue, Modifier.align(Alignment.TopEnd).padding(16.dp))
    }
}

@Composable
private fun StatusPill(state: LayawayState, daysUntilDue: Int?, modifier: Modifier = Modifier) {
    val colors = ShoeTrackTheme.colors
    val text = when {
        state == LayawayState.Overdue -> stringResource(Res.string.state_overdue)
        state == LayawayState.Completed -> stringResource(Res.string.paid_in_full_pill)
        state == LayawayState.Cancelled -> stringResource(Res.string.state_cancelled)
        daysUntilDue == null -> return
        daysUntilDue == 0 -> stringResource(Res.string.next_payment_today)
        else -> pluralStringResource(Res.plurals.next_payment_in_days, daysUntilDue, daysUntilDue)
    }
    val (background, content) = when (state) {
        LayawayState.Overdue -> colors.overdueBadge to colors.onOverdueBadge
        LayawayState.Completed -> colors.doneBadge to colors.onDoneBadge
        LayawayState.Cancelled -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        LayawayState.Active -> colors.sun to colors.onSun
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SheetHeader(detail: LayawayDetail) {
    Text(text = detail.customer.name, style = MaterialTheme.typography.headlineLarge)
    Text(
        text = listOf(detail.product.description, detail.customer.phone).filter { it.isNotBlank() }.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Balance(detail: LayawayDetail) {
    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = detail.layaway.remainingBalance.format(), style = ShoeTrackTheme.money.large)
        Text(
            text = stringResource(Res.string.left_of, detail.product.totalPrice.format()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun ProgressSection(detail: LayawayDetail) {
    val layaway = detail.layaway
    val fraction = if (detail.product.totalPrice.isPositive) {
        (1f - layaway.remainingBalance.cents.toFloat() / detail.product.totalPrice.cents).coerceIn(0f, 1f)
    } else {
        0f
    }
    val color = progressColorFor(layaway.state == LayawayState.Overdue, layaway.state == LayawayState.Completed)
    val percent = stringResource(Res.string.paid_percent, "${(fraction * 100).roundToInt()}%")
    Column(Modifier.padding(top = 12.dp, bottom = 20.dp)) {
        PaymentProgress(fraction, color, thickness = 12.dp, description = percent)
        Text(
            text = percent,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PaymentsSection(payments: List<Payment>) {
    Text(
        text = stringResource(Res.string.payments_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
    )
    if (payments.isEmpty()) {
        Text(
            text = stringResource(Res.string.no_payments),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        payments.asReversed().forEach { PaymentTicket(it) }
    }
}

/** A payment shaped like a ticket stub: a notch cut into each side. */
@Composable
private fun PaymentTicket(payment: Payment) {
    val sheetColor = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                val radius = 7.dp.toPx()
                drawCircle(sheetColor, radius, Offset(0f, size.height / 2))
                drawCircle(sheetColor, radius, Offset(size.width, size.height / 2))
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(payment.paidAt.formatAsDate(), style = MaterialTheme.typography.titleMedium)
        Text(payment.amount.format(), style = ShoeTrackTheme.money.small)
    }
}

@Composable
private fun PaymentDialog(
    input: String,
    canConfirm: Boolean,
    maxAmount: String?,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_payment)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text(stringResource(Res.string.payment_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = maxAmount != null,
                supportingText = maxAmount?.let { max ->
                    { Text(stringResource(Res.string.payment_exceeds_balance, max)) }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun LayawayDetailContentPreview() {
    val date = Instant.parse("2026-10-01T00:00:00Z")
    val layaway = Layaway(
        id = "1",
        customerId = "c1",
        productId = "p1",
        remainingBalance = Money.fromUnits(150_000),
        state = LayawayState.Active,
        createdAt = date,
        lastPaymentAt = date,
        payments = listOf(Payment("pay1", "1", Money.fromUnits(50_000), date, "device")),
    )
    val detail = LayawayDetail(
        layaway = layaway,
        customer = Customer("c1", "Ana Perez", "3001234567", date),
        product = Product("p1", "Nike Air Force 42", "", Money.fromUnits(200_000)),
    )
    ShoeTrackTheme {
        LayawayDetailContent(
            uiState = LayawayDetailUiState(detail = detail, isLoading = false, daysUntilDue = 18),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onAddPayment = {},
            onPaymentInputChange = {},
            onConfirmPayment = {},
            onDismissPayment = {},
        )
    }
}
