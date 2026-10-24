package com.shoetrack.domain.usecase

import com.shoetrack.domain.service.PhotoStorage

class LoadPhotoUseCase(private val photoStorage: PhotoStorage) {

    suspend operator fun invoke(name: String): ByteArray? = photoStorage.load(name)
}
