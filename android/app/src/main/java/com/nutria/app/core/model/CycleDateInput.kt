package com.nutria.app.core.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Strict Spanish calendar-date input; malformed dates never roll into another month. */
object CycleDateInput {
    private val formatter = DateTimeFormatter
        .ofPattern("dd/MM/uuuu", Locale.forLanguageTag("es-ES"))
        .withResolverStyle(ResolverStyle.STRICT)

    fun parse(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim(), formatter) }.getOrNull()
}
