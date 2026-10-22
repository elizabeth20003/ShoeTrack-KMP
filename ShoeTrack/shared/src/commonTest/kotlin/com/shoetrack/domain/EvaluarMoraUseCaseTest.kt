package com.shoetrack.domain

import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.usecase.EvaluarMoraUseCase
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class EvaluarMoraUseCaseTest {

    private val useCase = EvaluarMoraUseCase(diasLimite = 30)
    private val fechaUltimoAbono = Instant.parse("2026-01-01T00:00:00Z")

    private fun apartadoDePrueba(estado: ApartadoState) = Apartado(
        id = "ap1",
        clienteId = "cl1",
        productoId = "pr1",
        saldoPendiente = Money(10_000),
        estado = estado,
        fechaCreacion = fechaUltimoAbono,
        fechaUltimoAbono = fechaUltimoAbono,
    )

    @Test
    fun apartadoActivoConMenosDiasDelLimiteSigueActivo() {
        val ahora = fechaUltimoAbono + 29.days
        val resultado = useCase.ejecutar(apartadoDePrueba(ApartadoState.Activo), ahora)

        assertEquals(ApartadoState.Activo, resultado.estado)
    }

    @Test
    fun apartadoActivoQueLlegaAlLimiteDeDiasPasaAEnMora() {
        val ahora = fechaUltimoAbono + 30.days
        val resultado = useCase.ejecutar(apartadoDePrueba(ApartadoState.Activo), ahora)

        assertEquals(ApartadoState.EnMora, resultado.estado)
    }

    @Test
    fun apartadoCompletadoNoCambiaAunqueHayanPasadoMuchosDias() {
        val ahora = fechaUltimoAbono + 365.days
        val resultado = useCase.ejecutar(apartadoDePrueba(ApartadoState.Completado), ahora)

        assertEquals(ApartadoState.Completado, resultado.estado)
    }

    @Test
    fun apartadoCanceladoNoCambiaAunqueHayanPasadoMuchosDias() {
        val ahora = fechaUltimoAbono + 365.days
        val resultado = useCase.ejecutar(apartadoDePrueba(ApartadoState.Cancelado), ahora)

        assertEquals(ApartadoState.Cancelado, resultado.estado)
    }
}
