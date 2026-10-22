package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.model.Producto
import kotlin.time.Instant

class CrearApartadoUseCase {

    fun ejecutar(
        id: String,
        clienteId: String,
        producto: Producto,
        ahora: Instant
    ): Apartado {
        require(producto.precioTotal.esPositivo) {
            "El precio del producto debe ser mayor a cero"
        }

        return Apartado(
            id = id,
            clienteId = clienteId,
            productoId = producto.id,
            saldoPendiente = producto.precioTotal,
            estado = ApartadoState.Activo,
            fechaCreacion = ahora,
            fechaUltimoAbono = ahora,
            abonos = emptyList()
        )
    }
}
