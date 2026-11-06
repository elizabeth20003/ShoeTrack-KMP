package com.shoetrack.di

import com.shoetrack.data.ai.ClaudeNotebookReader
import com.shoetrack.data.id.UuidIdGenerator
import com.shoetrack.data.local.DatabaseDriverFactory
import com.shoetrack.data.ocr.createTextRecognizer
import com.shoetrack.data.photo.FileSystemPhotoStorage
import com.shoetrack.data.repository.AiKeyRepositoryImpl
import com.shoetrack.data.repository.CustomerRepositoryImpl
import com.shoetrack.data.repository.LayawayRepositoryImpl
import com.shoetrack.data.repository.ProductRepositoryImpl
import com.shoetrack.data.repository.SyncRepositoryImpl
import com.shoetrack.data.sync.LanSyncEngine
import com.shoetrack.data.sync.SyncSession
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.presentation.create.NewLayawayViewModel
import com.shoetrack.presentation.detail.LayawayDetailViewModel
import com.shoetrack.presentation.list.LayawayListViewModel
import com.shoetrack.presentation.scan.ScanViewModel
import com.shoetrack.domain.repository.AiKeyRepository
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository
import com.shoetrack.domain.repository.SyncRepository
import com.shoetrack.domain.sync.LayawayMerger
import com.shoetrack.domain.sync.SyncEngine
import com.shoetrack.domain.usecase.MergeSnapshotUseCase
import com.shoetrack.domain.usecase.SearchCustomersUseCase
import com.shoetrack.domain.service.IdGenerator
import com.shoetrack.domain.service.NotebookReader
import com.shoetrack.domain.service.OverdueReminderScheduler
import com.shoetrack.domain.service.PhotoStorage
import com.shoetrack.domain.service.TextRecognizer
import com.shoetrack.domain.usecase.CreateLayawayUseCase
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.ObserveLayawaysUseCase
import com.shoetrack.domain.usecase.PairWithDeviceUseCase
import com.shoetrack.domain.usecase.LoadPhotoUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.domain.usecase.RegisterPaymentUseCase
import com.shoetrack.domain.usecase.ScheduleOverdueReminderUseCase
import com.shoetrack.domain.usecase.SavePhotoUseCase
import com.shoetrack.domain.usecase.StartLayawayUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

private const val AI_REQUEST_TIMEOUT_MILLIS = 60_000L

private fun domainModule(deviceId: String): Module = module {
    single<Clock> { Clock.System }
    single<TextRecognizer> { createTextRecognizer() }
    single<IdGenerator> { UuidIdGenerator() }

    single { OverduePolicy() }

    factory { CreateLayawayUseCase() }
    factory { ProcessPaymentUseCase() }
    factory { EvaluateOverdueUseCase(policy = get()) }
    factory {
        ScheduleOverdueReminderUseCase(
            getLayawayDetail = get(),
            scheduler = get(),
            policy = get(),
            clock = get(),
        )
    }

    factory {
        RegisterPaymentUseCase(
            layawayRepository = get(),
            processPayment = get(),
            idGenerator = get(),
            clock = get(),
            deviceId = deviceId,
            scheduleReminder = get(),
        )
    }
    factory {
        StartLayawayUseCase(
            customerRepository = get(),
            productRepository = get(),
            layawayRepository = get(),
            createLayaway = get(),
            registerPayment = get(),
            scheduleReminder = get(),
            refreshOverdue = get(),
            idGenerator = get(),
            clock = get(),
        )
    }
    factory { SearchCustomersUseCase(customerRepository = get()) }
    single { LayawayMerger() }
    factory {
        MergeSnapshotUseCase(
            syncRepository = get(),
            merger = get(),
            evaluateOverdue = get(),
            scheduleReminder = get(),
            clock = get(),
        )
    }
    factory { PairWithDeviceUseCase(syncEngine = get()) }
    factory { RefreshOverdueUseCase(layawayRepository = get(), evaluateOverdue = get(), clock = get()) }
    factory { ObserveLayawaysUseCase(get()) }
    factory { SavePhotoUseCase(get()) }
    factory { LoadPhotoUseCase(get()) }
    factory {
        GetLayawayDetailUseCase(
            layawayRepository = get(),
            customerRepository = get(),
            productRepository = get(),
        )
    }
}

private fun dataModule(
    driverFactory: DatabaseDriverFactory,
    photosDirectory: String,
    deviceId: String,
): Module = module {
    single<PhotoStorage> { FileSystemPhotoStorage(photosDirectory, idGenerator = get()) }
    single<ShoeTrackDatabase> { ShoeTrackDatabase(driverFactory.createDriver()) }
    single<CustomerRepository> { CustomerRepositoryImpl(get()) }
    single<AiKeyRepository> { AiKeyRepositoryImpl(get()) }
    single {
        HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = AI_REQUEST_TIMEOUT_MILLIS }
        }
    }
    single<NotebookReader> { ClaudeNotebookReader(client = get(), keys = get(), clock = get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<SyncEngine> {
        LanSyncEngine(
            deviceId = deviceId,
            session = SyncSession(syncRepository = get(), mergeSnapshot = get(), photoStorage = get()),
            clock = get(),
        )
    }
    single<SyncRepository> { SyncRepositoryImpl(database = get(), deviceId = deviceId, clock = get()) }
    single<LayawayRepository> {
        LayawayRepositoryImpl(database = get(), deviceId = deviceId, clock = get())
    }
}

// ViewModels are factories: each screen gets its own, and the ViewModel store keeps it alive.
private fun presentationModule(): Module = module {
    factory {
        LayawayListViewModel(
            observeLayaways = get(),
            refreshOverdue = get(),
            syncEngine = get(),
            pairWithDevice = get(),
        )
    }
    factory { params ->
        NewLayawayViewModel(
            startLayaway = get(),
            savePhoto = get(),
            searchCustomers = get(),
            prefill = params.getOrNull<NotebookEntry>(),
        )
    }
    factory { params ->
        LayawayDetailViewModel(
            layawayId = params.get<String>(),
            getLayawayDetail = get(),
            registerPayment = get(),
            overduePolicy = get(),
            clock = get(),
        )
    }
    factory { ScanViewModel(recognizer = get(), cloudReader = get(), keys = get(), clock = get()) }
}

fun initKoin(
    driverFactory: DatabaseDriverFactory,
    reminderScheduler: OverdueReminderScheduler,
    photosDirectory: String,
    deviceId: String,
) {
    startKoin {
        modules(
            domainModule(deviceId),
            dataModule(driverFactory, photosDirectory, deviceId),
            presentationModule(),
            module { single<OverdueReminderScheduler> { reminderScheduler } },
        )
    }
}
