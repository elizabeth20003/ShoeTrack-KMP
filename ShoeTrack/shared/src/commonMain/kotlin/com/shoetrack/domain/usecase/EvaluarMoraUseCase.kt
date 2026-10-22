package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import kotlin.time.Instant

/**
 * Determina si un apartado activo debe pasar a estado EnMora
 * según los días transcurridos desde el último abono.
 */
class EvaluarMoraUseCase(
    private val diasLimite: Int = 30
) {

    fun ejecutar(apartado: Apartado, ahora: Instant): Apartado {
        if (apartado.estado != ApartadoState.Activo) return apartado

        val diasSinAbono = (ahora - apartado.fechaUltimoAbono).inWholeDays
        return if (diasSinAbono >= diasLimite) {
            apartado.copy(estado = ApartadoState.EnMora)
        } else {
            apartado
        }
    }
}
