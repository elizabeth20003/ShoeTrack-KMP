package com.shoetrack.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Producto
import com.shoetrack.domain.usecase.CrearApartadoUseCase
import com.shoetrack.domain.usecase.ProcesarAbonoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Prueba la capa de datos con una base SQLite en memoria, sin UI ni dispositivo. */
class ApartadoRepositoryImplTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        ShoeTrackDatabase.Schema.create(it)
    }
    private val repositorio = ApartadoRepositoryImpl(
        database = ShoeTrackDatabase(driver),
        deviceId = "dispositivo-test",
        dispatcher = Dispatchers.Unconfined,
    )

    private val ahora = Instant.parse("2026-01-01T00:00:00Z")
    private val producto = Producto(
        id = "pr1",
        descripcion = "Nike Air Force 42",
        fotoPath = "fotos/pr1.jpg",
        precioTotal = Money(10_000),
    )

    @AfterTest
    fun cerrarBaseDeDatos() {
        driver.close()
    }

    private fun nuevoApartado(id: String = "ap1"): Apartado =
        CrearApartadoUseCase().ejecutar(id, clienteId = "cl1", producto = producto, ahora = ahora)

    @Test
    fun crearYObtenerPorIdDevuelveElMismoApartado() {
        runBlocking {
            val apartado = nuevoApartado()
            repositorio.crear(apartado)

            assertEquals(apartado, repositorio.obtenerPorId("ap1"))
        }
    }

    @Test
    fun obtenerPorIdDeUnApartadoInexistenteDevuelveNull() {
        runBlocking {
            assertNull(repositorio.obtenerPorId("no-existe"))
        }
    }

    @Test
    fun registrarAbonoGuardaElSaldoElEstadoYElHistorial() {
        runBlocking {
            repositorio.crear(nuevoApartado())
            val abono = Abono("ab1", "ap1", Money(4_000), ahora, "dispositivo-test")
            val actualizado = ProcesarAbonoUseCase().ejecutar(nuevoApartado(), abono)

            repositorio.registrarAbono(actualizado, abono)
            val recuperado = repositorio.obtenerPorId("ap1")

            assertEquals(Money(6_000), recuperado?.saldoPendiente)
            assertEquals(ApartadoState.Activo, recuperado?.estado)
            assertEquals(listOf(abono), recuperado?.abonos)
        }
    }

    @Test
    fun unAbonoQueCompletaElSaldoDejaElApartadoCompletado() {
        runBlocking {
            repositorio.crear(nuevoApartado())
            val abono = Abono("ab1", "ap1", Money(10_000), ahora, "dispositivo-test")
            val actualizado = ProcesarAbonoUseCase().ejecutar(nuevoApartado(), abono)

            repositorio.registrarAbono(actualizado, abono)

            assertEquals(ApartadoState.Completado, repositorio.obtenerPorId("ap1")?.estado)
        }
    }

    @Test
    fun obtenerPorEstadoDevuelveSoloLosApartadosDeEseEstado() {
        runBlocking {
            repositorio.crear(nuevoApartado("ap1"))
            repositorio.crear(nuevoApartado("ap2"))
            repositorio.actualizarEstado(nuevoApartado("ap2").copy(estado = ApartadoState.EnMora))

            val enMora = repositorio.obtenerPorEstado(ApartadoState.EnMora)
            val activos = repositorio.obtenerPorEstado(ApartadoState.Activo)

            assertEquals(listOf("ap2"), enMora.map { it.id })
            assertEquals(listOf("ap1"), activos.map { it.id })
        }
    }

    @Test
    fun losApartadosNuevosQuedanPendientesDeSincronizar() {
        runBlocking {
            repositorio.crear(nuevoApartado())

            val pendientes = repositorio.obtenerPendientesDeSync()

            assertTrue(pendientes.any { it.id == "ap1" })
        }
    }
}
