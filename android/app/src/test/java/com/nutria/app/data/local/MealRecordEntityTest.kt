package com.nutria.app.data.local

import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.MealSource
import com.nutria.app.core.model.Nutrients
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MealRecordEntityTest {
    @Test
    fun nutritionIsStoredAsMilliUnitsAndRoundTripsAtThatPrecision() {
        val entry = MealEntry(
            id = "meal-1",
            localDate = LocalDate.of(2026, 11, 14),
            name = "Avena",
            nutrients = Nutrients(
                caloriesKcal = 123.4567,
                proteinGrams = 12.3456,
                carbohydratesGrams = 45.6789,
                fatGrams = 0.0005,
            ),
            source = MealSource.FOOD,
            recordedAt = Instant.parse("2026-11-14T08:30:00Z"),
        )

        val persisted = MealRecordEntity.fromDomain(entry)
        val restored = persisted.toDomain()

        assertEquals(123_457L, persisted.caloriesMilliKcal)
        assertEquals(12_346L, persisted.proteinMilliGrams)
        assertEquals(45_679L, persisted.carbohydratesMilliGrams)
        assertEquals(1L, persisted.fatMilliGrams)
        assertEquals(123.457, restored.nutrients.caloriesKcal, 0.0)
        assertEquals(12.346, restored.nutrients.proteinGrams, 0.0)
        assertEquals(45.679, restored.nutrients.carbohydratesGrams, 0.0)
        assertEquals(0.001, restored.nutrients.fatGrams, 0.0)
        assertEquals(entry.id, restored.id)
        assertEquals(entry.localDate, restored.localDate)
        assertEquals(entry.name, restored.name)
    }
}
