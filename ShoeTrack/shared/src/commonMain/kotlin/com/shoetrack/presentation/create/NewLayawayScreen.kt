package com.shoetrack.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.model.Customer
import com.shoetrack.presentation.photo.ShoePhoto
import com.shoetrack.presentation.photo.rememberPhotoPicker
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.back
import shoetrack.shared.generated.resources.choose_photo
import shoetrack.shared.generated.resources.customer_name
import shoetrack.shared.generated.resources.customer_phone
import shoetrack.shared.generated.resources.error_photo_failed
import shoetrack.shared.generated.resources.existing_customer
import shoetrack.shared.generated.resources.is_existing_customer
import shoetrack.shared.generated.resources.error_save_failed
import shoetrack.shared.generated.resources.initial_payment
import shoetrack.shared.generated.resources.initial_payment_exceeds_price
import shoetrack.shared.generated.resources.invalid_phone
import shoetrack.shared.generated.resources.new_layaway
import shoetrack.shared.generated.resources.price
import shoetrack.shared.generated.resources.product_description
import shoetrack.shared.generated.resources.required_field
import shoetrack.shared.generated.resources.save
import shoetrack.shared.generated.resources.take_photo

@Composable
fun NewLayawayScreen(
    viewModel: NewLayawayViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val photoPicker = rememberPhotoPicker(onPhotoPicked = viewModel::onPhotoPicked)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                NewLayawayEvent.Saved -> onSaved()
                NewLayawayEvent.SaveFailed ->
                    snackbarHostState.showSnackbar(getString(Res.string.error_save_failed))

                NewLayawayEvent.PhotoFailed ->
                    snackbarHostState.showSnackbar(getString(Res.string.error_photo_failed))
            }
        }
    }

    NewLayawayContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onCustomerNameChange = viewModel::onCustomerNameChange,
        onCustomerSelected = viewModel::onCustomerSelected,
        onCustomerPhoneChange = viewModel::onCustomerPhoneChange,
        onProductDescriptionChange = viewModel::onProductDescriptionChange,
        onPriceChange = viewModel::onPriceChange,
        onInitialPaymentChange = viewModel::onInitialPaymentChange,
        onTakePhoto = photoPicker::takePhoto,
        onPickPhoto = photoPicker::pickFromGallery,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewLayawayContent(
    uiState: NewLayawayUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerSelected: (Customer) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onProductDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onInitialPaymentChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.new_layaway)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoSection(
                photoName = uiState.photoName,
                onTakePhoto = onTakePhoto,
                onPickPhoto = onPickPhoto,
            )
            FormField(
                label = stringResource(Res.string.customer_name),
                value = uiState.customerName,
                onValueChange = onCustomerNameChange,
                errorMessage = if (uiState.isCustomerNameInvalid) stringResource(Res.string.required_field) else null,
            )
            CustomerSuggestions(
                suggestions = uiState.customerSuggestions,
                isExistingCustomerSelected = uiState.selectedCustomerId != null,
                onSelected = onCustomerSelected,
            )
            FormField(
                label = stringResource(Res.string.customer_phone),
                value = uiState.customerPhone,
                onValueChange = onCustomerPhoneChange,
                keyboardType = KeyboardType.Phone,
                errorMessage = if (uiState.isCustomerPhoneInvalid) stringResource(Res.string.invalid_phone) else null,
            )
            FormField(
                label = stringResource(Res.string.product_description),
                value = uiState.productDescription,
                onValueChange = onProductDescriptionChange,
                errorMessage = if (uiState.isProductDescriptionInvalid) stringResource(Res.string.required_field) else null,
            )
            FormField(
                label = stringResource(Res.string.price),
                value = uiState.price,
                onValueChange = onPriceChange,
                keyboardType = KeyboardType.Number,
                errorMessage = if (uiState.isPriceInvalid) stringResource(Res.string.required_field) else null,
            )
            FormField(
                label = stringResource(Res.string.initial_payment),
                value = uiState.initialPayment,
                onValueChange = onInitialPaymentChange,
                keyboardType = KeyboardType.Number,
                errorMessage = if (uiState.isInitialPaymentInvalid) {
                    stringResource(Res.string.initial_payment_exceeds_price)
                } else {
                    null
                },
            )
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}

@Composable
private fun PhotoSection(photoName: String?, onTakePhoto: () -> Unit, onPickPhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photoName != null) {
            ShoePhoto(
                name = photoName,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.take_photo))
            }
            OutlinedButton(onClick = onPickPhoto, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.choose_photo))
            }
        }
    }
}

@Composable
private fun CustomerSuggestions(
    suggestions: List<Customer>,
    isExistingCustomerSelected: Boolean,
    onSelected: (Customer) -> Unit,
) {
    if (isExistingCustomerSelected) {
        Text(
            text = stringResource(Res.string.existing_customer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (suggestions.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = stringResource(Res.string.is_existing_customer),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )
                suggestions.forEach { customer ->
                    TextButton(onClick = { onSelected(customer) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${customer.name} · ${customer.phone}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun NewLayawayContentPreview() {
    MaterialTheme {
        NewLayawayContent(
            uiState = NewLayawayUiState(customerName = "Ana Perez", showValidation = true),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onCustomerNameChange = {},
            onCustomerSelected = {},
            onCustomerPhoneChange = {},
            onProductDescriptionChange = {},
            onPriceChange = {},
            onInitialPaymentChange = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onSave = {},
        )
    }
}
