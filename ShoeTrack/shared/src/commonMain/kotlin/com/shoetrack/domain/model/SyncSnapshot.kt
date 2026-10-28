package com.shoetrack.domain.model

/**
 * Everything a device knows, in the form it is exchanged with another device.
 * Each layaway carries its full payment history.
 */
data class SyncSnapshot(
    val customers: List<Customer> = emptyList(),
    val products: List<Product> = emptyList(),
    val layaways: List<Layaway> = emptyList(),
)
