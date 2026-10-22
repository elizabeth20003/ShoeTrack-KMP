package com.shoetrack.domain

import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.usecase.ProcesarAbonoUseCase
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProcesarAbonoUseCaseTest {

    private val useCase = ProcesarAbonoUseCase()
    private val fechaBase = Instant.parse("2026-01-01T00:00:00Z")

    private fun apartadoDePrueba(
        saldo: Long,
        estado: ApartadoState = ApartadoState.Activo,
    ) = Apartado(
        id = "ap1",
        clienteId = "cl1",
        productoId = "pr1",
        saldoPendiente = Money(saldo),
        estado = estado,
        fechaCreacion = fechaBase,
        fechaUltimoAbono = fechaBase,
    )

    private fun abonoDePrueba(monto: Long) = Abono(
        id = "ab1",
        apartadoId = "ap1",
        monto = Money(monto),
        fecha = fechaBase,
        registradoPorDeviceId = "device1",
    )

    @Test
    fun abonoParcialReduceElSaldoYMantieneEstadoActivo() {
        val resultado = useCase.ejecutar(apartadoDePrueba(saldo = 10_000), abonoDePrueba(3_000))

        assertEquals(7_000, resultado.saldoPendiente.centavos)
        assertEquals(ApartadoState.Activo, resultado.estado)
    }

    @Test
    fun abonoQueCompletaElSaldoExactoPasaACompletado() {
        val resultado = useCase.ejecutar(apartadoDePrueba(saldo = 5_000), abonoDePrueba(5_000))

        assertEquals(0, resultado.saldoPendiente.centavos)
        assertEquals(ApartadoState.Completado, resultado.estado)
    }

    @Test
    fun abonoMayorAlSaldoTambienPasaACompletado() {
        val resultado = useCase.ejecutar(apartadoDePrueba(saldo = 3_000), abonoDePrueba(5_000))

        assertEquals(ApartadoState.Completado, resultado.estado)
    }

    @Test
    fun elAbonoSeAgregaAlHistorial() {
        val abono = abonoDePrueba(3_000)
        val resultado = useCase.ejecutar(apartadoDePrueba(saldo = 10_000), abono)

        assertEquals(listOf(abono), resultado.abonos)
    }

    @Test
    fun noSePuedeAbonarAUnApartadoCompletado() {
        val apartado = apartadoDePrueba(saldo = 0, estado = ApartadoState.Completado)

        assertFailsWith<IllegalArgumentException> {
            useCase.ejecutar(apartado, abonoDePrueba(1_000))
        }
    }

    @Test
    fun noSePuedeAbonarAUnApartadoCancelado() {
        val apartado = apartadoDePrueba(saldo = 5_000, estado = ApartadoState.Cancelado)

        assertFailsWith<IllegalArgumentException> {
            useCase.ejecutar(apartado, abonoDePrueba(1_000))
        }
    }

    @Test
    fun apartadoEnMoraVuelveAActivoSiSeAbonaYNoCompletaElSaldo() {
        val apartado = apartadoDePrueba(saldo = 10_000, estado = ApartadoState.EnMora)
        val resultado = useCase.ejecutar(apartado, abonoDePrueba(2_000))

        assertEquals(ApartadoState.Activo, resultado.estado)
    }
}
