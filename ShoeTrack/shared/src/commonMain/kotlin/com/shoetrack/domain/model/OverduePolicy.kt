package com.shoetrack.domain.model

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Decides when a layaway counts as overdue: [overdueAfterDays] days after its last payment. */
class OverduePolicy(val overdueAfterDays: Int = 30) {

    fun dueAt(layaway: Layaway): Instant = layaway.lastPaymentAt + overdueAfterDays.days

    fun isOverdue(layaway: Layaway, now: Instant): Boolean = now >= dueAt(layaway)
}
