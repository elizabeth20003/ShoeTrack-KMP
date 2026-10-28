package com.shoetrack.presentation

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakePhotoStorage
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.SequentialIdGenerator
import com.shoetrack.domain.fakes.scheduleReminderOf
import com.shoetrack.domain.usecase.CreateLayawayUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import com.shoetrack.domain.usecase.RegisterPaymentUseCase
import com.shoetrack.domain.usecase.SavePhotoUseCase
import com.shoetrack.domain.usecase.StartLayawayUseCase
import com.shoetrack.presentation.create.NewLayawayEvent
import com.shoetrack.presentation.create.NewLayawayViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class NewLayawayViewModelTest : MainDispatcherTest() {

    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()
    private val layaways = FakeLayawayRepository()
    private val photos = FakePhotoStorage()

    private fun viewModel(): NewLayawayViewModel {
        val idGenerator = SequentialIdGenerator()
        val clock = FixedClock(Instant.parse("2026-02-01T00:00:00Z"))
        val scheduleReminder = scheduleReminderOf(layaways, customers, products)
        return NewLayawayViewModel(
            StartLayawayUseCase(
                customerRepository = customers,
                productRepository = products,
                layawayRepository = layaways,
                createLayaway = CreateLayawayUseCase(),
                registerPayment = RegisterPaymentUseCase(
                    layawayRepository = layaways,
                    processPayment = ProcessPaymentUseCase(),
                    idGenerator = idGenerator,
                    clock = clock,
                    deviceId = "device-1",
                    scheduleReminder = scheduleReminder,
                ),
                scheduleReminder = scheduleReminder,
                idGenerator = idGenerator,
                clock = clock,
            ),
            SavePhotoUseCase(photos),
        )
    }

    private fun NewLayawayViewModel.fillValidForm() {
        onCustomerNameChange("Ana Perez")
        onProductDescriptionChange("Nike Air Force 42")
        onPriceChange("100000")
    }

    @Test
    fun savingAnIncompleteFormShowsValidationAndSavesNothing() {
        val viewModel = viewModel()

        viewModel.save()

        val state = viewModel.uiState.value
        assertTrue(state.showValidation)
        assertTrue(state.isCustomerNameInvalid)
        assertTrue(state.isPriceInvalid)
        assertTrue(layaways.layaways.isEmpty())
    }

    @Test
    fun savingAValidFormStoresTheLayawayAndEmitsSaved() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidForm()

        viewModel.save()

        assertEquals(NewLayawayEvent.Saved, viewModel.events.first())
        assertEquals(1, layaways.layaways.size)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun aStorageFailureEmitsSaveFailedAndAllowsRetrying() = runTest {
        layaways.writeFailure = IllegalStateException("disk full")
        val viewModel = viewModel()
        viewModel.fillValidForm()

        viewModel.save()

        assertEquals(NewLayawayEvent.SaveFailed, viewModel.events.first())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun anInvalidPhoneBlocksSavingAndIsFlagged() {
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onCustomerPhoneChange("12345")

        viewModel.save()

        assertTrue(viewModel.uiState.value.isCustomerPhoneInvalid)
        assertTrue(layaways.layaways.isEmpty())
    }

    @Test
    fun phoneFieldKeepsAtMostTenDigits() {
        val viewModel = viewModel()

        viewModel.onCustomerPhoneChange("+57 300-123-4567-89")

        assertEquals("5730012345", viewModel.uiState.value.customerPhone)
    }

    @Test
    fun aPickedPhotoIsStoredAndSavedWithTheLayaway() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidForm()

        viewModel.onPhotoPicked(byteArrayOf(1, 2, 3))
        viewModel.save()
        viewModel.events.first()

        val photoName = viewModel.uiState.value.photoName
        assertNotNull(photoName)
        assertContentEquals(byteArrayOf(1, 2, 3), photos.photos[photoName])
        assertEquals(photoName, products.products.values.single().photoPath)
    }

    @Test
    fun aPhotoStorageFailureEmitsPhotoFailed() = runTest {
        photos.failure = IllegalStateException("disk full")
        val viewModel = viewModel()

        viewModel.onPhotoPicked(byteArrayOf(1))

        assertEquals(NewLayawayEvent.PhotoFailed, viewModel.events.first())
        assertNull(viewModel.uiState.value.photoName)
    }

    @Test
    fun moneyFieldsKeepOnlyDigits() {
        val viewModel = viewModel()

        viewModel.onPriceChange("1a2.5")

        assertEquals("125", viewModel.uiState.value.price)
    }

    @Test
    fun initialPaymentAbovePriceIsFlaggedAndBlocksSaving() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onInitialPaymentChange("200000")

        viewModel.save()

        assertTrue(viewModel.uiState.value.isInitialPaymentInvalid)
        assertTrue(layaways.layaways.isEmpty())
    }
}
