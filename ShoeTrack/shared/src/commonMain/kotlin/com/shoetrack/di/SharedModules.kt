package com.shoetrack.di

import com.shoetrack.data.id.UuidIdGenerator
import com.shoetrack.data.local.DatabaseDriverFactory
import com.shoetrack.data.photo.FileSystemPhotoStorage
import com.shoetrack.data.repository.CustomerRepositoryImpl
import com.shoetrack.data.repository.LayawayRepositoryImpl
import com.shoetrack.data.repository.ProductRepositoryImpl
import com.shoetrack.data.repository.SyncRepositoryImpl
import com.shoetrack.data.sync.LanSyncEngine
import com.shoetrack.data.sync.SyncSession
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository
import com.shoetrack.domain.repository.SyncRepository
import com.shoetrack.domain.sync.LayawayMerger
import com.shoetrack.domain.sync.SyncEngine
import com.shoetrack.domain.usecase.MergeSnapshotUseCase
import com.shoetrack.domain.service.IdGenerator
import com.shoetrack.domain.service.OverdueReminderScheduler
import com.shoetrack.domain.service.PhotoStorage
import com.shoetrack.domain.usecase.CreateLayawayUseCase
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.ObserveLayawaysUseCase
import com.shoetrack.domain.usecase.LoadPhotoUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.domain.usecase.RegisterPaymentUseCase
import com.shoetrack.domain.usecase.ScheduleOverdueReminderUseCase
import com.shoetrack.domain.usecase.SavePhotoUseCase
import com.shoetrack.domain.usecase.StartLayawayUseCase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Clock

private fun domainModule(deviceId: String): Module = module {
    single<Clock> { Clock.System }
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
            idGenerator = get(),
            clock = get(),
        )
    }
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
            module { single<OverdueReminderScheduler> { reminderScheduler } },
        )
    }
}
