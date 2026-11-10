package com.shoetrack.presentation

import com.shoetrack.domain.fakes.FakeAiKeyRepository
import com.shoetrack.domain.fakes.FakeNotebookReader
import com.shoetrack.domain.fakes.FakeTextRecognizer
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.presentation.scan.ScanEvent
import com.shoetrack.presentation.scan.ScanViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ScanViewModelTest : MainDispatcherTest() {
    private val recognizer = FakeTextRecognizer()
    private val clock = FixedClock(Instant.parse("2026-11-04T15:00:00Z"))
    private val cloudReader = FakeNotebookReader()
    private val keys = FakeAiKeyRepository()
    private lateinit var viewModel: ScanViewModel

    // Built after the Main dispatcher is replaced: the ViewModel starts a coroutine as soon as it exists.
    @BeforeTest
    fun createViewModel() {
        viewModel = ScanViewModel(recognizer, cloudReader, keys, clock)
    }

    @Test
    fun startsWithoutAnyText() {
        assertNull(viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isReading)
    }

    @Test
    fun aReadLayawayLineIsHandedOverToTheForm() = runTest {
        recognizer.text = "03/10/26 Alba Calambas Maxten gris T=140.000 Abona 30.000 Saldo 110.000"

        viewModel.onPhotoPicked(byteArrayOf(1))

        val event = viewModel.events.first() as ScanEvent.EntryRead
        assertEquals("Alba Calambas", event.entry.customerName)
        assertEquals(Money.fromUnits(140_000), event.entry.totalPrice)
        assertEquals(LocalDate(2026, 10, 3), event.entry.lastPaymentDate)
    }

    @Test
    fun aDateAfterTodayIsDroppedBecauseItWasMisread() = runTest {
        recognizer.text = "03/12/26 Alba Calambas Maxten gris T=140.000"

        viewModel.onPhotoPicked(byteArrayOf(1))

        val event = viewModel.events.first() as ScanEvent.EntryRead
        assertNull(event.entry.lastPaymentDate)
    }

    @Test
    fun textWithoutALayawayLineKeepsTheScreenOpenWithWhatWasRead() {
        recognizer.text = "hello"

        viewModel.onPhotoPicked(byteArrayOf(1))

        assertTrue(viewModel.uiState.value.foundNothing)
        assertEquals("hello", viewModel.uiState.value.text)
    }

    @Test
    fun reportsAFailureWhenThePhotoCannotBeRead() {
        recognizer.fails = true

        viewModel.onPhotoPicked(byteArrayOf(1))

        assertTrue(viewModel.uiState.value.hasFailed)
        assertNull(viewModel.uiState.value.text)
    }

    @Test
    fun withoutAKeyThePhoneReadsThePhotoAndTheCloudIsNotUsed() = runTest {
        recognizer.text = "03/10/26 Alba Calambas Maxten gris T=140.000"

        viewModel.onPhotoPicked(byteArrayOf(1))

        viewModel.events.first()
        assertEquals(0, cloudReader.reads)
        assertFalse(viewModel.uiState.value.hasAiKey)
    }

    @Test
    fun withAKeyTheCloudReadingFillsTheForm() = runTest {
        keys.stored = "key"
        cloudReader.entry = NotebookEntry(customerName = "Paola Flor", totalPrice = Money.fromUnits(80_000))
        val viewModel = ScanViewModel(recognizer, cloudReader, keys, clock)

        viewModel.onPhotoPicked(byteArrayOf(1))

        val event = viewModel.events.first() as ScanEvent.EntryRead
        assertEquals("Paola Flor", event.entry.customerName)
        assertFalse(viewModel.uiState.value.aiUnavailable)
    }

    @Test
    fun whenTheCloudFailsThePhoneReadsThePhotoInstead() = runTest {
        keys.stored = "key"
        cloudReader.fails = true
        recognizer.text = "03/10/26 Alba Calambas Maxten gris T=140.000"
        val viewModel = ScanViewModel(recognizer, cloudReader, keys, clock)

        viewModel.onPhotoPicked(byteArrayOf(1))

        val event = viewModel.events.first() as ScanEvent.EntryRead
        assertEquals("Alba Calambas", event.entry.customerName)
        assertTrue(viewModel.uiState.value.aiUnavailable)
    }

    @Test
    fun savingAndRemovingTheKeyIsReflectedInTheState() {
        viewModel.onAiKeySaved(" secret ")
        assertTrue(viewModel.uiState.value.hasAiKey)
        assertEquals("secret", keys.stored)

        viewModel.onAiKeyRemoved()
        assertFalse(viewModel.uiState.value.hasAiKey)
    }
}
