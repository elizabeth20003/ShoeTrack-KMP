package com.shoetrack.domain.service

import com.shoetrack.domain.notebook.NotebookEntry

/** Reads one layaway entry of the paper notebook straight from a photo of it. */
interface NotebookReader {
    /** Returns what could be read from [image] (JPEG bytes); throws if the photo could not be processed. */
    suspend fun read(image: ByteArray): NotebookEntry
}
