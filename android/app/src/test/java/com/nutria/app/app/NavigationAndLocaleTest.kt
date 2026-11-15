package com.nutria.app.app

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationAndLocaleTest {
    @Test
    fun navigationToCurrentRouteDoesNotAddDuplicate() {
        assertEquals(
            listOf("inicio", "comida"),
            NavigationHistory.navigate(listOf("inicio", "comida"), "comida")
        )
    }

    @Test
    fun selectingAnExistingDestinationReturnsToItsExistingBackStackEntry() {
        assertEquals(
            listOf("inicio", "comida"),
            NavigationHistory.navigate(listOf("inicio", "comida", "recetas"), "comida")
        )
    }

    @Test
    fun backReturnsToPreviousDestinationWithoutDuplicateEntries() {
        assertEquals(
            listOf("inicio", "comida"),
            NavigationHistory.back(listOf("inicio", "comida", "recetas"))
        )
        assertEquals(
            listOf("inicio"),
            NavigationHistory.back(listOf("inicio"))
        )
    }

    @Test
    fun unknownRoutesResolveToHome() {
        assertEquals(AppDestination.INICIO, AppDestination.fromRoute("missing"))
        assertEquals(listOf("inicio"), NavigationHistory.navigate(emptyList(), "missing"))
    }

    @Test
    fun legacyFoodEntryRouteResolvesToComida() {
        assertEquals(AppDestination.COMIDA, AppDestination.fromRoute("registrar"))
    }

    @Test
    fun dateFormattingUsesSpanishDayMonthYearPresentation() {
        assertEquals(
            "14/11/2026",
            SpanishFormats.formatDate(LocalDate.of(2026, 11, 14))
        )
    }

    @Test
    fun numberFormattingUsesSpanishDecimalAndGroupingSeparators() {
        assertEquals("12.345,6", SpanishFormats.formatNumber(12_345.6))
    }

    @Test
    fun stepCountAndRefreshTimeUseSpanishFormatting() {
        assertEquals("12.345", SpanishFormats.formatInteger(12_345))
        assertEquals(
            "13:45",
            SpanishFormats.formatTime(
                Instant.parse("2026-11-14T12:45:00Z"),
                ZoneId.of("Europe/Madrid")
            )
        )
    }

}
