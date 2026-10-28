package com.shoetrack.data.sync

import com.shoetrack.data.mapper.toDbValue
import com.shoetrack.data.mapper.toLayawayState
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Messages two devices exchange during a sync. */
@Serializable
internal sealed interface SyncMessage

@Serializable
@SerialName("snapshot")
internal data class SnapshotMessage(val snapshot: SnapshotDto) : SyncMessage

@Serializable
@SerialName("photo_request")
internal data class PhotoRequestMessage(val name: String) : SyncMessage

/** [base64] is null when the sender does not have the photo. */
@Serializable
@SerialName("photo")
internal data class PhotoMessage(val name: String, val base64: String?) : SyncMessage

@Serializable
@SerialName("done")
internal data object DoneMessage : SyncMessage

@Serializable
internal data class SnapshotDto(
    val customers: List<CustomerDto>,
    val products: List<ProductDto>,
    val layaways: List<LayawayDto>,
)

@Serializable
internal data class CustomerDto(val id: String, val name: String, val phone: String, val registeredAt: Long)

@Serializable
internal data class ProductDto(val id: String, val description: String, val photoPath: String, val totalPriceCents: Long)

@Serializable
internal data class PaymentDto(
    val id: String,
    val layawayId: String,
    val amountCents: Long,
    val paidAt: Long,
    val recordedByDeviceId: String,
)

@Serializable
internal data class LayawayDto(
    val id: String,
    val customerId: String,
    val productId: String,
    val remainingBalanceCents: Long,
    val state: String,
    val createdAt: Long,
    val lastPaymentAt: Long,
    val payments: List<PaymentDto>,
)

internal fun SyncSnapshot.toDto() = SnapshotDto(
    customers = customers.map { CustomerDto(it.id, it.name, it.phone, it.registeredAt.toEpochMilliseconds()) },
    products = products.map { ProductDto(it.id, it.description, it.photoPath, it.totalPrice.cents) },
    layaways = layaways.map { layaway ->
        LayawayDto(
            id = layaway.id,
            customerId = layaway.customerId,
            productId = layaway.productId,
            remainingBalanceCents = layaway.remainingBalance.cents,
            state = layaway.state.toDbValue(),
            createdAt = layaway.createdAt.toEpochMilliseconds(),
            lastPaymentAt = layaway.lastPaymentAt.toEpochMilliseconds(),
            payments = layaway.payments.map {
                PaymentDto(it.id, it.layawayId, it.amount.cents, it.paidAt.toEpochMilliseconds(), it.recordedByDeviceId)
            },
        )
    },
)

internal fun SnapshotDto.toDomain() = SyncSnapshot(
    customers = customers.map { Customer(it.id, it.name, it.phone, Instant.fromEpochMilliseconds(it.registeredAt)) },
    products = products.map { Product(it.id, it.description, it.photoPath, Money(it.totalPriceCents)) },
    layaways = layaways.map { layaway ->
        Layaway(
            id = layaway.id,
            customerId = layaway.customerId,
            productId = layaway.productId,
            remainingBalance = Money(layaway.remainingBalanceCents),
            state = layaway.state.toLayawayState(),
            createdAt = Instant.fromEpochMilliseconds(layaway.createdAt),
            lastPaymentAt = Instant.fromEpochMilliseconds(layaway.lastPaymentAt),
            payments = layaway.payments.map {
                Payment(it.id, it.layawayId, Money(it.amountCents), Instant.fromEpochMilliseconds(it.paidAt), it.recordedByDeviceId)
            },
        )
    },
)
