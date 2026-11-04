package com.shoetrack.domain.notebook

import com.shoetrack.domain.model.Money
import kotlinx.datetime.LocalDate

/**
 * What could be read of one layaway line of the paper notebook. Handwriting is easy to misread,
 * so every field may be missing and the user always reviews the result before saving.
 */
data class NotebookEntry(
    val customerName: String = "",
    val productDescription: String = "",
    val totalPrice: Money? = null,
    /** How much of the price is already paid. */
    val paidAmount: Money? = null,
    /** The date written at the start of the line, which is the date of the last payment. */
    val lastPaymentDate: LocalDate? = null,
)
