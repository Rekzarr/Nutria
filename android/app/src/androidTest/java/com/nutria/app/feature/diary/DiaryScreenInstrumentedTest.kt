package com.nutria.app.feature.diary

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutria.app.data.local.LocalDatabaseMigrations
import com.nutria.app.data.local.NutriaDatabase
import com.nutria.app.data.local.RoomDiaryRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

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
                Column {
                    DiaryScreen(viewModel)
                    WaterLogCard(viewModel)
                }
            }
        }

        composeRule.onNodeWithText("Alimento").performTextInput("Avena")
        composeRule.onNodeWithText("Calorías (kcal)").performTextInput("389")
        composeRule.onNodeWithText("Proteínas (g)").performTextInput("17")
        composeRule.onNodeWithText("Carbohidratos (g)").performTextInput("66")
        composeRule.onNodeWithText("Grasas (g)").performTextInput("7")
        composeRule.onNodeWithText("Añadir al diario").performClick()
        composeRule.onNodeWithText("+150 ml").performClick()
        composeRule.onNodeWithText("+250 ml").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Avena").assertIsDisplayed()
        composeRule.onNodeWithText("400 ml").assertIsDisplayed()

        composeRule.onNodeWithText("Deshacer última toma").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("150 ml").assertIsDisplayed()
    }
}
