package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.nutria.app.core.data.DiaryRepository
import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.MealSource
import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.WaterEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToLong

@Entity(
    tableName = "diary_meals",
    indices = [Index(value = ["local_date"])],
)
internal data class MealRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "local_date") val localDate: String,
    val name: String,
    @ColumnInfo(name = "calories_milli_kcal") val caloriesMilliKcal: Long,
    @ColumnInfo(name = "protein_milli_grams") val proteinMilliGrams: Long,
    @ColumnInfo(name = "carbohydrates_milli_grams") val carbohydratesMilliGrams: Long,
    @ColumnInfo(name = "fat_milli_grams") val fatMilliGrams: Long,
    val source: String,
    @ColumnInfo(name = "recorded_at_epoch_ms") val recordedAtEpochMs: Long,
) {
    fun toDomain() = MealEntry(
        id = id,
        localDate = LocalDate.parse(localDate),
        name = name,
        nutrients = Nutrients(
            caloriesKcal = caloriesMilliKcal / MILLI_UNITS_PER_UNIT,
            proteinGrams = proteinMilliGrams / MILLI_UNITS_PER_UNIT,
            carbohydratesGrams = carbohydratesMilliGrams / MILLI_UNITS_PER_UNIT,
            fatGrams = fatMilliGrams / MILLI_UNITS_PER_UNIT,
        ),
        source = runCatching { MealSource.valueOf(source) }.getOrDefault(MealSource.FOOD),
        recordedAt = Instant.ofEpochMilli(recordedAtEpochMs),
    )

    companion object {
        fun fromDomain(entry: MealEntry) = MealRecordEntity(
            id = entry.id,
            localDate = entry.localDate.toString(),
            name = entry.name.trim(),
            caloriesMilliKcal = entry.nutrients.caloriesKcal.toMilliUnits(),
            proteinMilliGrams = entry.nutrients.proteinGrams.toMilliUnits(),
            carbohydratesMilliGrams = entry.nutrients.carbohydratesGrams.toMilliUnits(),
            fatMilliGrams = entry.nutrients.fatGrams.toMilliUnits(),
            source = entry.source.name,
            recordedAtEpochMs = entry.recordedAt.toEpochMilli(),
        )

        private const val MILLI_UNITS_PER_UNIT = 1_000.0

        private fun Double.toMilliUnits(): Long {
            require(isFinite() && this >= 0.0 && this <= Long.MAX_VALUE / MILLI_UNITS_PER_UNIT)
            return (this * MILLI_UNITS_PER_UNIT).roundToLong()
        }
    }
}

@Entity(
    tableName = "water_events",
    indices = [Index(value = ["local_date", "sequence"])],
)
internal data class WaterEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "amount_millilitres") val amountMillilitres: Int,
    val sequence: Long,
    @ColumnInfo(name = "recorded_at_epoch_ms") val recordedAtEpochMs: Long,
) {
    fun toDomain() = WaterEvent(
        id = id,
        localDate = LocalDate.parse(localDate),
        amountMillilitres = amountMillilitres,
        sequence = sequence,
        recordedAt = Instant.ofEpochMilli(recordedAtEpochMs),
    )
}

@Dao
internal interface DiaryDao {
    @Query(
        """
        SELECT * FROM diary_meals
        WHERE local_date = :date
        ORDER BY recorded_at_epoch_ms DESC, id DESC
        """,
    )
    fun observeMeals(date: String): Flow<List<MealRecordEntity>>

    @Query(
        """
        SELECT * FROM diary_meals
        WHERE local_date BETWEEN :fromInclusive AND :throughInclusive
        ORDER BY local_date DESC, recorded_at_epoch_ms DESC, id DESC
        """,
    )
    fun observeMealHistory(fromInclusive: String, throughInclusive: String): Flow<List<MealRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeal(meal: MealRecordEntity)

    @Query("DELETE FROM diary_meals WHERE id = :mealId AND local_date = :date")
    suspend fun deleteMeal(date: String, mealId: String): Int

    @Query("DELETE FROM diary_meals WHERE local_date = :date")
    suspend fun deleteMeals(date: String)

    @Query(
        """
        SELECT * FROM water_events
        WHERE local_date = :date
        ORDER BY sequence ASC
        """,
    )
    fun observeWaterEvents(date: String): Flow<List<WaterEventEntity>>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM water_events WHERE local_date = :date")
    suspend fun maxWaterSequence(date: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWaterEvent(event: WaterEventEntity)

    @Query(
        """
        SELECT * FROM water_events
        WHERE local_date = :date
        ORDER BY sequence DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestWaterEvent(date: String): WaterEventEntity?

    @Query("DELETE FROM water_events WHERE id = :eventId")
    suspend fun deleteWaterEvent(eventId: String)

    @Query("DELETE FROM water_events WHERE local_date = :date")
    suspend fun deleteWaterEvents(date: String)
}

/**
 * Room-backed local diary. Event sequence allocation and undo are serialized with
 * transactions so undo always removes exactly the latest increment for that day.
 */
internal class RoomDiaryRepository(
    private val database: NutriaDatabase,
    private val dao: DiaryDao = database.diaryDao(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) : DiaryRepository {
    private val writeMutex = Mutex()

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        dao.observeMeals(date.toString()).map { meals -> meals.map(MealRecordEntity::toDomain) }

    override fun observeMealHistory(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
    ): Flow<List<MealEntry>> =
        dao.observeMealHistory(fromInclusive.toString(), throughInclusive.toString())
            .map { meals -> meals.map(MealRecordEntity::toDomain) }

    override suspend fun addMeal(entry: MealEntry) = withContext(Dispatchers.IO) {
        dao.insertMeal(MealRecordEntity.fromDomain(entry))
    }

    override suspend fun removeMeal(date: LocalDate, mealId: String) {
        withContext(Dispatchers.IO) {
        dao.deleteMeal(date.toString(), mealId)
        }
    }

    override fun observeWaterEvents(date: LocalDate): Flow<List<WaterEvent>> =
        dao.observeWaterEvents(date.toString()).map { events -> events.map(WaterEventEntity::toDomain) }

    override suspend fun addWaterEvent(
        date: LocalDate,
        amountMillilitres: Int,
        recordedAt: Instant,
    ): WaterEvent = writeMutex.withLock {
        require(amountMillilitres == WATER_INCREMENT_SMALL || amountMillilitres == WATER_INCREMENT_LARGE)
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val nextSequence = dao.maxWaterSequence(date.toString()) + 1
                val event = WaterEventEntity(
                    id = idGenerator(),
                    localDate = date.toString(),
                    amountMillilitres = amountMillilitres,
                    sequence = nextSequence,
                    recordedAtEpochMs = recordedAt.toEpochMilli(),
                )
                dao.insertWaterEvent(event)
                event.toDomain()
            }
        }
    }

    override suspend fun undoLatestWaterEvent(date: LocalDate): WaterEvent? = writeMutex.withLock {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.findLatestWaterEvent(date.toString())?.also { latest ->
                    dao.deleteWaterEvent(latest.id)
                }?.toDomain()
            }
        }
    }

    override suspend fun clearMealsAndWater(date: LocalDate) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.deleteMeals(date.toString())
            dao.deleteWaterEvents(date.toString())
        }
    }

    private companion object {
        const val WATER_INCREMENT_SMALL = 150
        const val WATER_INCREMENT_LARGE = 250
    }
}
