package com.nutria.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.MealSource
import com.nutria.app.core.model.Nutrients
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DiaryLocalStoreInstrumentedTest {
    private lateinit var database: NutriaDatabase
    private lateinit var repository: RoomDiaryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NutriaDatabase::class.java)
            .allowMainThreadQueries()
            .addMigrations(*LocalDatabaseMigrations.all)
            .build()
        repository = RoomDiaryRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun waterUndoRemovesOnlyTheLatestEventForThatLocalDay() = runBlocking {
        val date = LocalDate.of(2026, 11, 14)
        val first = repository.addWaterEvent(date, 150, Instant.parse("2026-11-14T08:00:00Z"))
        val second = repository.addWaterEvent(date, 250, Instant.parse("2026-11-14T09:00:00Z"))

        assertEquals(1L, first.sequence)
        assertEquals(2L, second.sequence)
        assertEquals(second.id, repository.undoLatestWaterEvent(date)?.id)
        assertEquals(listOf(first), repository.observeWaterEvents(date).first())
        assertNull(repository.undoLatestWaterEvent(date.minusDays(1)))
    }

    @Test
    fun historyIsDateBoundedAndPastMealsCannotBeRemovedThroughTodayScope() = runBlocking {
        val today = LocalDate.of(2026, 11, 14)
        val old = meal("old", today.minusDays(30))
        val recent = meal("recent", today.minusDays(29))
        val todayMeal = meal("today", today)
        repository.addMeal(old)
        repository.addMeal(recent)
        repository.addMeal(todayMeal)

        assertEquals(listOf(todayMeal, recent), repository.observeMealHistory(today.minusDays(29), today).first())
        repository.removeMeal(today, old.id)
        assertNotNull(repository.observeMeals(old.localDate).first().singleOrNull())
        repository.removeMeal(today, todayMeal.id)
        assertEquals(0, repository.observeMeals(today).first().size)
    }

    @Test
    fun clearingOneDayLeavesOtherDatesUntouched() = runBlocking {
        val today = LocalDate.of(2026, 11, 14)
        repository.addMeal(meal("today", today))
        repository.addMeal(meal("yesterday", today.minusDays(1)))
        repository.addWaterEvent(today, 150, Instant.parse("2026-11-14T08:00:00Z"))

        repository.clearMealsAndWater(today)

        assertEquals(0, repository.observeMeals(today).first().size)
        assertEquals(0, repository.observeWaterEvents(today).first().size)
        assertEquals(1, repository.observeMeals(today.minusDays(1)).first().size)
    }

    @Test
    fun migrationFromSchemaFiveScalesExistingMealNutritionWithoutLosingRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(5) {
                        override fun onCreate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                            database.execSQL(
                                """
                                CREATE TABLE `diary_meals` (
                                    `id` TEXT NOT NULL PRIMARY KEY,
                                    `local_date` TEXT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `calories_kcal` REAL NOT NULL,
                                    `protein_grams` REAL NOT NULL,
                                    `carbohydrates_grams` REAL NOT NULL,
                                    `fat_grams` REAL NOT NULL,
                                    `source` TEXT NOT NULL,
                                    `recorded_at_epoch_ms` INTEGER NOT NULL
                                )
                                """.trimIndent(),
                            )
                            database.execSQL(
                                "CREATE INDEX `index_diary_meals_local_date` " +
                                    "ON `diary_meals` (`local_date`)",
                            )
                            database.execSQL(
                                """
                                INSERT INTO `diary_meals` VALUES (
                                    'meal-1', '2026-11-14', 'Avena',
                                    123.4567, 12.3456, 45.6789, 0.0005,
                                    'FOOD', 1794645000000
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            database: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        try {
            LocalDatabaseMigrations.MIGRATION_5_6.migrate(helper.writableDatabase)
            val cursor = helper.writableDatabase.query(
                """
                SELECT `id`, `calories_milli_kcal`, `protein_milli_grams`,
                    `carbohydrates_milli_grams`, `fat_milli_grams`
                FROM `diary_meals`
                """.trimIndent(),
            )
            cursor.use {
                assertEquals(true, it.moveToFirst())
                assertEquals("meal-1", it.getString(0))
                assertEquals(123_457L, it.getLong(1))
                assertEquals(12_346L, it.getLong(2))
                assertEquals(45_679L, it.getLong(3))
                assertEquals(1L, it.getLong(4))
            }
        } finally {
            helper.close()
        }
    }

    private fun meal(id: String, date: LocalDate) = MealEntry(
        id = id,
        localDate = date,
        name = "Alimento $id",
        nutrients = Nutrients(100.0, 5.0, 10.0, 2.0),
        source = MealSource.FOOD,
        recordedAt = Instant.parse("2026-11-14T08:00:00Z"),
    )
}
