package com.shoetrack.data.mapper

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import kotlin.time.Instant
import com.shoetrack.db.Customer as CustomerRow
import com.shoetrack.db.Layaway as LayawayRow
import com.shoetrack.db.Payment as PaymentRow
import com.shoetrack.db.Product as ProductRow

// Maps SQLDelight rows to domain models.
// Dates are stored as epoch milliseconds and money as cents.

internal fun CustomerRow.toDomain(): Customer = Customer(
    id = id,
    name = name,
    phone = phone,
    registeredAt = Instant.fromEpochMilliseconds(registeredAt),
)

internal fun ProductRow.toDomain(): Product = Product(
    id = id,
    description = description,
    photoPath = photoPath,
    totalPrice = Money(totalPriceCents),
)

internal fun PaymentRow.toDomain(): Payment = Payment(
    id = id,
    layawayId = layawayId,
    amount = Money(amountCents),
    paidAt = Instant.fromEpochMilliseconds(paidAt),
    recordedByDeviceId = recordedByDeviceId,
)

internal fun LayawayRow.toDomain(payments: List<Payment> = emptyList()): Layaway = Layaway(
    id = id,
    customerId = customerId,
    productId = productId,
    remainingBalance = Money(remainingBalanceCents),
    state = state.toLayawayState(),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    lastPaymentAt = Instant.fromEpochMilliseconds(lastPaymentAt),
    payments = payments,
)

internal fun LayawayState.toDbValue(): String = when (this) {
    LayawayState.Active -> STATE_ACTIVE
    LayawayState.Overdue -> STATE_OVERDUE
    LayawayState.Completed -> STATE_COMPLETED
    LayawayState.Cancelled -> STATE_CANCELLED
}

internal fun String.toLayawayState(): LayawayState = when (this) {
    STATE_ACTIVE -> LayawayState.Active
    STATE_OVERDUE -> LayawayState.Overdue
    STATE_COMPLETED -> LayawayState.Completed
    STATE_CANCELLED -> LayawayState.Cancelled
    else -> error("Unknown layaway state in the database: $this")
}

private const val STATE_ACTIVE = "ACTIVE"
private const val STATE_OVERDUE = "OVERDUE"
private const val STATE_COMPLETED = "COMPLETED"
private const val STATE_CANCELLED = "CANCELLED"
