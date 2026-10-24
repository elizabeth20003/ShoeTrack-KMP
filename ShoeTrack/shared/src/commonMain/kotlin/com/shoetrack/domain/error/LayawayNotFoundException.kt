package com.shoetrack.domain.error

class LayawayNotFoundException(id: String) : NoSuchElementException("Layaway not found: $id")
