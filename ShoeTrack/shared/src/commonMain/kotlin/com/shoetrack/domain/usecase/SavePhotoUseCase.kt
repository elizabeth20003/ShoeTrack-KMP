package com.shoetrack.domain.usecase

import com.shoetrack.domain.service.PhotoStorage

class SavePhotoUseCase(private val photoStorage: PhotoStorage) {

    suspend operator fun invoke(imageBytes: ByteArray): String = photoStorage.save(imageBytes)
}
