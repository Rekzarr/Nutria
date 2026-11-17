package com.nutria.app.feature.diary

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutria.app.data.local.LocalDatabaseMigrations
import com.nutria.app.data.local.NutriaDatabase
import com.nutria.app.data.local.RoomDiaryRepository
import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.MealSource
import com.nutria.app.core.model.Nutrients
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DiaryScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: NutriaDatabase
    private lateinit var viewModel: DiaryViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NutriaDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(*LocalDatabaseMigrations.all)
            .build()
        viewModel = DiaryViewModel(
            repository = RoomDiaryRepository(database),
            clock = Clock.fixed(
                Instant.parse("2026-11-14T12:00:00Z"),
                ZoneId.of("Europe/Madrid"),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun userCanAddFoodAndWaterThenUndoOnlyTheLatestWaterEvent() {
        composeRule.setContent {
            MaterialTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DiaryScreen(viewModel)
                    WaterLogCard(viewModel)
                }
            }
        }

        composeRule.onNodeWithText("Alimento").performScrollTo().performTextInput("Avena")
        composeRule.onNodeWithText("Calorías (kcal)").performScrollTo().performTextInput("389")
        composeRule.onNodeWithText("Proteínas (g)").performScrollTo().performTextInput("17")
        composeRule.onNodeWithText("Carbohidratos (g)").performScrollTo().performTextInput("66")
        composeRule.onNodeWithText("Grasas (g)").performScrollTo().performTextInput("7")
        composeRule.onNodeWithText("Añadir al diario").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Avena se añadió a tu día.")[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("+150 ml").performScrollTo().performClick()
        composeRule.onNodeWithText("+250 ml").performScrollTo().performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Avena").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("400 ml").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Deshacer última toma").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("150 ml").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("150 ml").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun summaryStreakCountsMealAndWaterDaysAndStopsAtTheFirstGap() = runBlocking {
        val repository = RoomDiaryRepository(database)
        val today = LocalDate.now(Clock.fixed(
            Instant.parse("2026-11-14T12:00:00Z"),
            ZoneId.of("Europe/Madrid"),
        ))
        repository.addMeal(
            MealEntry(
                id = "meal-today",
                localDate = today,
                name = "Comida de hoy",
                nutrients = Nutrients.ZERO,
                source = MealSource.FOOD,
                recordedAt = Instant.parse("2026-11-14T12:00:00Z"),
            ),
        )
        repository.addWaterEvent(today.minusDays(1), 150, Instant.parse("2026-11-13T12:00:00Z"))
        repository.addMeal(
            MealEntry(
                id = "meal-before",
                localDate = today.minusDays(2),
                name = "Comida anterior",
                nutrients = Nutrients.ZERO,
                source = MealSource.FOOD,
                recordedAt = Instant.parse("2026-11-12T12:00:00Z"),
            ),
        )

        val state = DiaryViewModel(
            repository = repository,
            clock = Clock.fixed(
                Instant.parse("2026-11-14T12:00:00Z"),
                ZoneId.of("Europe/Madrid"),
            ),
        ).state.first { !it.isLoading }

        assertEquals(3, state.careStreakDays)
        assertEquals(1, state.todayMeals.size)
    }

    @Test
    fun clearingTodayRequiresConfirmationAndLeavesOtherDaysUntouched() = runBlocking {
        val repository = RoomDiaryRepository(database)
        val today = LocalDate.now(Clock.fixed(
            Instant.parse("2026-11-14T12:00:00Z"),
            ZoneId.of("Europe/Madrid"),
        ))
        repository.addMeal(meal("meal-today", today))
        repository.addMeal(meal("meal-yesterday", today.minusDays(1)))
        repository.addWaterEvent(today, 150, Instant.parse("2026-11-14T08:00:00Z"))
        repository.addWaterEvent(today.minusDays(1), 250, Instant.parse("2026-11-13T08:00:00Z"))

        composeRule.setContent {
            MaterialTheme {
                WaterLogCard(viewModel)
            }
        }

        composeRule.onNodeWithText("150 ml").assertIsDisplayed()
        composeRule.onNodeWithText("Borrar comidas y agua de hoy").performClick()
        composeRule.onNodeWithText("Se eliminarán todas las comidas y tomas de agua de hoy. No se puede deshacer. No se modificarán tus recetas, planes ni registros de otros días.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()
        assertEquals(1, repository.observeMeals(today).first().size)
        assertEquals(1, repository.observeWaterEvents(today).first().size)

        composeRule.onNodeWithText("Borrar comidas y agua de hoy").performClick()
        composeRule.onNodeWithText("Borrar datos de hoy").performClick()
        composeRule.waitForIdle()

        assertEquals(0, repository.observeMeals(today).first().size)
        assertEquals(0, repository.observeWaterEvents(today).first().size)
        assertEquals(1, repository.observeMeals(today.minusDays(1)).first().size)
        assertEquals(1, repository.observeWaterEvents(today.minusDays(1)).first().size)
    }

    private fun meal(id: String, date: LocalDate) = MealEntry(
        id = id,
        localDate = date,
        name = "Alimento $id",
        nutrients = Nutrients.ZERO,
        source = MealSource.FOOD,
        recordedAt = Instant.parse("2026-11-14T08:00:00Z"),
    )
}
