package com.nutria.app.app

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SpanishFormats {
    private val locale = Locale.forLanguageTag("es-ES")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatNumber(value: Double): String =
        NumberFormat.getNumberInstance(locale)
            .apply { maximumFractionDigits = 2 }
            .format(value)

    fun formatInteger(value: Long): String =
        NumberFormat.getIntegerInstance(locale).format(value)

    fun formatTime(value: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        value.atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm", locale))
}
