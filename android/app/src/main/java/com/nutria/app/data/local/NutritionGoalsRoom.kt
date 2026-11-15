package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.model.NutritionGoals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToLong

@Entity(tableName = "nutrition_goals")
internal data class NutritionGoalsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Int,
    @ColumnInfo(name = "water_millilitres") val waterMillilitres: Int,
    @ColumnInfo(name = "protein_milli_grams") val proteinMilliGrams: Long,
    @ColumnInfo(name = "carbohydrates_milli_grams") val carbohydratesMilliGrams: Long,
    @ColumnInfo(name = "fat_milli_grams") val fatMilliGrams: Long,
) {
    fun toDomain() = NutritionGoals(
        caloriesKcal = caloriesKcal,
        waterMillilitres = waterMillilitres,
        proteinGrams = proteinMilliGrams / 1_000.0,
        carbohydratesGrams = carbohydratesMilliGrams / 1_000.0,
        fatGrams = fatMilliGrams / 1_000.0,
    )

    companion object {
        const val SINGLETON_ID = 1

        fun fromDomain(goals: NutritionGoals): NutritionGoalsEntity {
            require(goals.waterMillilitres >= 50 && goals.waterMillilitres % 50 == 0) {
                "El objetivo de agua debe ser de al menos 50 ml y avanzar en pasos de 50 ml."
            }
            return NutritionGoalsEntity(
                caloriesKcal = goals.caloriesKcal,
                waterMillilitres = goals.waterMillilitres,
                proteinMilliGrams = goals.proteinGrams.toScaledInteger(),
                carbohydratesMilliGrams = goals.carbohydratesGrams.toScaledInteger(),
                fatMilliGrams = goals.fatGrams.toScaledInteger(),
            )
        }

        private fun Double.toScaledInteger(): Long {
            require(isFinite() && this > 0.0 && this <= Long.MAX_VALUE / 1_000.0)
            return (this * 1_000.0).roundToLong()
        }
    }
}

@Dao
internal interface NutritionGoalsDao {
    @Query("SELECT * FROM nutrition_goals WHERE id = :id LIMIT 1")
    fun observe(id: Int = NutritionGoalsEntity.SINGLETON_ID): Flow<NutritionGoalsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(goals: NutritionGoalsEntity)
}

/** Goal records are stored in the encrypted Room database, not in preference storage. */
internal class GoalsRoomRepository(
    private val databaseProvider: EncryptedDatabaseProvider,
) : GoalsRepository {
    override fun observeGoals(): Flow<NutritionGoals> = flow {
        val dao = databaseProvider.open().nutritionGoalsDao()
        emitAll(dao.observe().map { it?.toDomain() ?: NutritionGoals() })
    }

    override suspend fun saveGoals(goals: NutritionGoals) {
        databaseProvider.open().nutritionGoalsDao().save(NutritionGoalsEntity.fromDomain(goals))
    }
}
