package com.shoetrack.presentation.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoetrack.domain.model.Customer
import com.shoetrack.presentation.components.AppIcon
import com.shoetrack.presentation.components.IconShape
import com.shoetrack.presentation.components.SneakerIllustration
import com.shoetrack.presentation.photo.ShoePhoto
import com.shoetrack.presentation.photo.rememberPhotoPicker
import com.shoetrack.presentation.theme.Colorway
import com.shoetrack.presentation.theme.ShoeTrackTheme
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.back
import shoetrack.shared.generated.resources.choose_photo
import shoetrack.shared.generated.resources.customer_name
import shoetrack.shared.generated.resources.customer_phone
import shoetrack.shared.generated.resources.error_photo_failed
import shoetrack.shared.generated.resources.error_save_failed
import shoetrack.shared.generated.resources.existing_customer
import shoetrack.shared.generated.resources.initial_payment
import shoetrack.shared.generated.resources.initial_payment_exceeds_price
import shoetrack.shared.generated.resources.invalid_phone
import shoetrack.shared.generated.resources.is_existing_customer
import shoetrack.shared.generated.resources.new_layaway
import shoetrack.shared.generated.resources.optional
import shoetrack.shared.generated.resources.photo_hint
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
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { SaveBar(enabled = !uiState.isSaving, onSave = onSave) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(onBack)
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    label = stringResource(Res.string.price),
                    value = uiState.price,
                    onValueChange = onPriceChange,
                    keyboardType = KeyboardType.Number,
                    errorMessage = if (uiState.isPriceInvalid) stringResource(Res.string.required_field) else null,
                    modifier = Modifier.weight(1f),
                )
                FormField(
                    label = stringResource(Res.string.initial_payment),
                    value = uiState.initialPayment,
                    onValueChange = onInitialPaymentChange,
                    keyboardType = KeyboardType.Number,
                    placeholder = stringResource(Res.string.optional),
                    errorMessage = if (uiState.isInitialPaymentInvalid) {
                        stringResource(Res.string.initial_payment_exceeds_price)
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    val backLabel = stringResource(Res.string.back)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(IconShape.Back, MaterialTheme.colorScheme.onSurface, size = 22.dp)
        }
        Text(stringResource(Res.string.new_layaway), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Button(
            onClick = onSave,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) {
            Text(stringResource(Res.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A camera-style viewfinder: tap the shutter to take a photo, or choose one from the gallery. */
@Composable
private fun PhotoSection(photoName: String?, onTakePhoto: () -> Unit, onPickPhoto: () -> Unit) {
    val frame = ShoeTrackTheme.colors.sun
    val shutterLabel = stringResource(Res.string.take_photo)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(ShoeTrackTheme.colors.tag),
    ) {
        if (photoName != null) {
            ShoePhoto(name = photoName, modifier = Modifier.fillMaxSize())
        } else {
            SneakerIllustration(
                colorway = Colorway(Color.Transparent, Color.White, Color.White, Color.White),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp).width(190.dp).alpha(0.16f),
            )
            Text(
                text = stringResource(Res.string.photo_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 112.dp),
            )
        }
        Canvas(Modifier.matchParentSize()) { drawCornerBrackets(frame) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(56.dp)
                .border(4.dp, Color.White, CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onTakePhoto)
                .semantics { contentDescription = shutterLabel },
        )
        Text(
            text = stringResource(Res.string.choose_photo),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 26.dp)
                .clip(CircleShape)
                .clickable(onClick = onPickPhoto)
                .padding(8.dp),
        )
    }
}

private fun DrawScope.drawCornerBrackets(color: Color) {
    val inset = 18.dp.toPx()
    val arm = 26.dp.toPx()
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val w = size.width
    val h = size.height
    fun bracket(cornerX: Float, cornerY: Float, dx: Float, dy: Float) = drawPath(
        Path().apply {
            moveTo(cornerX + dx * arm, cornerY)
            lineTo(cornerX, cornerY)
            lineTo(cornerX, cornerY + dy * arm)
        },
        color,
        style = stroke,
    )
    bracket(inset, inset, 1f, 1f)
    bracket(w - inset, inset, -1f, 1f)
    bracket(inset, h - inset, 1f, -1f)
    bracket(w - inset, h - inset, -1f, -1f)
}

@Composable
private fun CustomerSuggestions(
    suggestions: List<Customer>,
    isExistingCustomerSelected: Boolean,
    onSelected: (Customer) -> Unit,
) {
    if (isExistingCustomerSelected) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(IconShape.Check, ShoeTrackTheme.colors.progressDone, size = 18.dp)
            Text(
                text = stringResource(Res.string.existing_customer),
                style = MaterialTheme.typography.labelLarge,
                color = ShoeTrackTheme.colors.progressDone,
            )
        }
    }
    if (suggestions.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 6.dp),
        ) {
            Text(
                text = stringResource(Res.string.is_existing_customer),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            suggestions.forEach { customer ->
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSelected(customer) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(customer.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    errorMessage: String? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            errorContainerColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun NewLayawayContentPreview() {
    ShoeTrackTheme {
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
