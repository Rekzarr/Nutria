package com.nutria.app.feature.photo

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val MAX_PHOTO_BYTES = 10 * 1024 * 1024

internal fun InputStream.readPhotoBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (output.size() + count > MAX_PHOTO_BYTES) {
            throw IllegalArgumentException("La imagen supera el límite de 10 MiB.")
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
