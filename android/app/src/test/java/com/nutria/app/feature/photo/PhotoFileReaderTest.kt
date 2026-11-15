package com.nutria.app.feature.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class PhotoFileReaderTest {
    @Test
    fun acceptsImagesAtServerUploadLimit() {
        val bytes = ByteArrayInputStream(ByteArray(MAX_PHOTO_BYTES) { 7 }).readPhotoBytes()

        assertEquals(MAX_PHOTO_BYTES, bytes.size)
        assertEquals(7, bytes.last().toInt())
    }

    @Test
    fun rejectsImagesLargerThanServerUploadLimit() {
        val failure = runCatching {
            ByteArrayInputStream(ByteArray(MAX_PHOTO_BYTES + 1)).readPhotoBytes()
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("La imagen supera el límite de 10 MiB.", failure?.message)
    }
}
