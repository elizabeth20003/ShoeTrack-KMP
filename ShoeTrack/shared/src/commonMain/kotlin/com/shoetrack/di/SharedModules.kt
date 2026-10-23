package com.shoetrack.di

import com.shoetrack.data.local.DatabaseDriverFactory
import com.shoetrack.data.repository.CustomerRepositoryImpl
import com.shoetrack.data.repository.LayawayRepositoryImpl
import com.shoetrack.data.repository.ProductRepositoryImpl
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository
import com.shoetrack.domain.usecase.CreateLayawayUseCase
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

private val domainModule: Module = module {
    factory { CreateLayawayUseCase() }
    factory { ProcessPaymentUseCase() }
    factory { EvaluateOverdueUseCase() }
}

private fun dataModule(driverFactory: DatabaseDriverFactory, deviceId: String): Module = module {
    single<ShoeTrackDatabase> { ShoeTrackDatabase(driverFactory.createDriver()) }
    single<CustomerRepository> { CustomerRepositoryImpl(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<LayawayRepository> { LayawayRepositoryImpl(database = get(), deviceId = deviceId) }
}

/**
 * Starts dependency injection. Each app calls this once at launch with its own
 * [DatabaseDriverFactory] and a unique id for the device.
 */
fun initKoin(driverFactory: DatabaseDriverFactory, deviceId: String) {
    startKoin {
        modules(domainModule, dataModule(driverFactory, deviceId))
    }
}
