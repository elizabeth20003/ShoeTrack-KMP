package com.shoetrack.data.mapper

import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.model.Cliente
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Producto
import kotlin.time.Instant
import com.shoetrack.db.Abono as AbonoRow
import com.shoetrack.db.Apartado as ApartadoRow
import com.shoetrack.db.Cliente as ClienteRow
import com.shoetrack.db.Producto as ProductoRow

// Conversión entre las filas generadas por SQLDelight (tipos primitivos) y los
// modelos del dominio. Las fechas se guardan como milisegundos desde epoch y el
// dinero como centavos.

internal fun ClienteRow.toDomain(): Cliente = Cliente(
    id = id,
    nombre = nombre,
    telefono = telefono,
    fechaRegistro = Instant.fromEpochMilliseconds(fechaRegistro),
)

internal fun ProductoRow.toDomain(): Producto = Producto(
    id = id,
    descripcion = descripcion,
    fotoPath = fotoPath,
    precioTotal = Money(precioTotalCentavos),
)

internal fun AbonoRow.toDomain(): Abono = Abono(
    id = id,
    apartadoId = apartadoId,
    monto = Money(montoCentavos),
    fecha = Instant.fromEpochMilliseconds(fecha),
    registradoPorDeviceId = registradoPorDeviceId,
)

internal fun ApartadoRow.toDomain(abonos: List<Abono> = emptyList()): Apartado = Apartado(
    id = id,
    clienteId = clienteId,
    productoId = productoId,
    saldoPendiente = Money(saldoPendienteCentavos),
    estado = estado.toApartadoState(),
    fechaCreacion = Instant.fromEpochMilliseconds(fechaCreacion),
    fechaUltimoAbono = Instant.fromEpochMilliseconds(fechaUltimoAbono),
    abonos = abonos,
)

internal fun ApartadoState.toDbValue(): String = when (this) {
    ApartadoState.Activo -> ESTADO_ACTIVO
    ApartadoState.EnMora -> ESTADO_EN_MORA
    ApartadoState.Completado -> ESTADO_COMPLETADO
    ApartadoState.Cancelado -> ESTADO_CANCELADO
}

internal fun String.toApartadoState(): ApartadoState = when (this) {
    ESTADO_ACTIVO -> ApartadoState.Activo
    ESTADO_EN_MORA -> ApartadoState.EnMora
    ESTADO_COMPLETADO -> ApartadoState.Completado
    ESTADO_CANCELADO -> ApartadoState.Cancelado
    else -> throw IllegalStateException("Estado de apartado desconocido en la base de datos: $this")
}

private const val ESTADO_ACTIVO = "ACTIVO"
private const val ESTADO_EN_MORA = "EN_MORA"
private const val ESTADO_COMPLETADO = "COMPLETADO"
private const val ESTADO_CANCELADO = "CANCELADO"
