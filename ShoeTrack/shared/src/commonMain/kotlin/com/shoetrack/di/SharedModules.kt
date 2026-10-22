package com.shoetrack.di

import com.shoetrack.data.local.DatabaseDriverFactory
import com.shoetrack.data.repository.ApartadoRepositoryImpl
import com.shoetrack.data.repository.ClienteRepositoryImpl
import com.shoetrack.data.repository.ProductoRepositoryImpl
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.repository.ApartadoRepository
import com.shoetrack.domain.repository.ClienteRepository
import com.shoetrack.domain.repository.ProductoRepository
import com.shoetrack.domain.usecase.CrearApartadoUseCase
import com.shoetrack.domain.usecase.EvaluarMoraUseCase
import com.shoetrack.domain.usecase.ProcesarAbonoUseCase
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

private val domainModule: Module = module {
    factory { CrearApartadoUseCase() }
    factory { ProcesarAbonoUseCase() }
    factory { EvaluarMoraUseCase() }
}

private fun dataModule(driverFactory: DatabaseDriverFactory, deviceId: String): Module = module {
    single<ShoeTrackDatabase> { ShoeTrackDatabase(driverFactory.createDriver()) }
    single<ClienteRepository> { ClienteRepositoryImpl(get()) }
    single<ProductoRepository> { ProductoRepositoryImpl(get()) }
    single<ApartadoRepository> { ApartadoRepositoryImpl(database = get(), deviceId = deviceId) }
}

/**
 * Punto de entrada de la inyección de dependencias. Cada app (Android, iOS,
 * escritorio) lo llama una sola vez al arrancar, pasando su propio
 * [DatabaseDriverFactory] y el identificador único de ese dispositivo.
 */
fun initKoin(driverFactory: DatabaseDriverFactory, deviceId: String) {
    startKoin {
        modules(domainModule, dataModule(driverFactory, deviceId))
    }
}
