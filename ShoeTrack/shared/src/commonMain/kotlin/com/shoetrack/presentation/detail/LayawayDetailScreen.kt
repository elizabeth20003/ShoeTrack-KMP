package com.shoetrack.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
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
import com.shoetrack.presentation.common.color
import com.shoetrack.presentation.common.label
import com.shoetrack.presentation.format.format
import com.shoetrack.presentation.format.formatAsDate
import com.shoetrack.presentation.photo.ShoePhoto
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.add_payment
import shoetrack.shared.generated.resources.back
import shoetrack.shared.generated.resources.balance_label
import shoetrack.shared.generated.resources.cancel
import shoetrack.shared.generated.resources.confirm
import shoetrack.shared.generated.resources.error_load_failed
import shoetrack.shared.generated.resources.error_payment_failed
import shoetrack.shared.generated.resources.layaway_details_title
import shoetrack.shared.generated.resources.layaway_not_found
import shoetrack.shared.generated.resources.no_payments
import shoetrack.shared.generated.resources.payment_amount
import shoetrack.shared.generated.resources.payment_exceeds_balance
import shoetrack.shared.generated.resources.payments_title
import shoetrack.shared.generated.resources.total_price_label
import kotlin.time.Instant

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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.layaway_details_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val detail = uiState.detail
            when {
                uiState.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.loadFailed -> CenteredMessage(stringResource(Res.string.error_load_failed))

                detail == null -> CenteredMessage(stringResource(Res.string.layaway_not_found))

                else -> DetailBody(detail, onAddPayment)
            }
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
private fun DetailBody(detail: LayawayDetail, onAddPayment: () -> Unit) {
    val layaway = detail.layaway
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (detail.product.photoPath.isNotBlank()) {
            item {
                ShoePhoto(
                    name = detail.product.photoPath,
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.product.description,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOf(detail.customer.name, detail.customer.phone).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(layaway.state.label()),
                    style = MaterialTheme.typography.labelLarge,
                    color = layaway.state.color(),
                )
                Text(
                    text = stringResource(Res.string.total_price_label, detail.product.totalPrice.format()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.balance_label, layaway.remainingBalance.format()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (layaway.state == LayawayState.Active || layaway.state == LayawayState.Overdue) {
            item {
                Button(onClick = onAddPayment, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.add_payment))
                }
            }
        }
        item {
            Text(
                text = stringResource(Res.string.payments_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            HorizontalDivider()
        }
        if (layaway.payments.isEmpty()) {
            item { Text(stringResource(Res.string.no_payments)) }
        } else {
            items(layaway.payments.asReversed(), key = { it.id }) { payment ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(payment.paidAt.formatAsDate())
                    Text(payment.amount.format(), fontWeight = FontWeight.Medium)
                }
            }
        }
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
    MaterialTheme {
        LayawayDetailContent(
            uiState = LayawayDetailUiState(detail = detail, isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onAddPayment = {},
            onPaymentInputChange = {},
            onConfirmPayment = {},
            onDismissPayment = {},
        )
    }
}
