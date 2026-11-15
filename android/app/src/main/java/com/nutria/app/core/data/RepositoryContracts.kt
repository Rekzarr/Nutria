package com.nutria.app.core.data

import com.nutria.app.core.model.CalendarAssignment
import com.nutria.app.core.model.CycleDayEntry
import com.nutria.app.core.model.CycleSettings
import com.nutria.app.core.model.HeartRateBandState
import com.nutria.app.core.model.HeartRateReading
import com.nutria.app.core.model.HealthConnectStepsState
import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.NutritionGoals
import com.nutria.app.core.model.PeriodRecord
import com.nutria.app.core.model.RecommendationFilter
import com.nutria.app.core.model.SavedRecipe
import com.nutria.app.core.model.WaterEvent
import com.nutria.app.core.model.WorkoutEvent
import com.nutria.app.core.model.WorkoutEndStatus
import com.nutria.app.core.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Domain-facing persistence contracts. Implementations belong in data/local or
 * platform adapters; domain and UI code must not depend on Room or Android APIs.
 */
interface DiaryRepository {
    fun observeMeals(date: LocalDate): Flow<List<MealEntry>>
    fun observeMealHistory(fromInclusive: LocalDate, throughInclusive: LocalDate): Flow<List<MealEntry>>
    suspend fun addMeal(entry: MealEntry)
    suspend fun removeMeal(date: LocalDate, mealId: String)

    fun observeWaterEvents(date: LocalDate): Flow<List<WaterEvent>>
    suspend fun addWaterEvent(date: LocalDate, amountMillilitres: Int, recordedAt: Instant): WaterEvent
    suspend fun undoLatestWaterEvent(date: LocalDate): WaterEvent?

    /** Deletes only this day's meals and water events, atomically. */
    suspend fun clearMealsAndWater(date: LocalDate)
}

interface GoalsRepository {
    fun observeGoals(): Flow<NutritionGoals>
    suspend fun saveGoals(goals: NutritionGoals)
}

interface RecipeRepository {
    fun observeRecipes(): Flow<List<SavedRecipe>>
    suspend fun saveRecipe(recipe: SavedRecipe)
    suspend fun updateRecipe(recipe: SavedRecipe)

    /** Must atomically remove the recipe and its calendar assignments. */
    suspend fun deleteRecipe(recipeId: String)
}

interface CalendarRepository {
    fun observeAssignments(fromInclusive: LocalDate, throughInclusive: LocalDate): Flow<List<CalendarAssignment>>
    suspend fun saveAssignment(assignment: CalendarAssignment)

    /** Replaces exactly the supplied date range, leaving other plans untouched. */
    suspend fun replaceAssignments(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
        assignments: List<CalendarAssignment>,
    )
}

interface CycleRepository {
    fun observeSettings(): Flow<CycleSettings>
    fun observePeriods(): Flow<List<PeriodRecord>>
    fun observeEntries(fromInclusive: LocalDate, throughInclusive: LocalDate): Flow<List<CycleDayEntry>>
    fun observeRecentEntries(throughInclusive: LocalDate, limit: Int): Flow<List<CycleDayEntry>>
    suspend fun saveSettings(settings: CycleSettings)
    suspend fun savePeriod(period: PeriodRecord)
    suspend fun deletePeriod(periodId: String)
    suspend fun saveDayEntry(entry: CycleDayEntry)
}

/** Health Connect is read-only and its returned values must remain transient. */
interface HealthConnectStepsRepository {
    suspend fun readDailySteps(localDate: LocalDate): HealthConnectStepsState
}

/**
 * A platform adapter for explicit BLE connect/disconnect and received standard
 * Heart Rate GATT notifications. It does not start or imply a workout.
 */
interface HeartRateBandRepository {
    val state: StateFlow<HeartRateBandState>
    val readings: Flow<HeartRateReading>
    suspend fun connect()
    suspend fun disconnect()
}

interface WorkoutRepository {
    fun observeSessions(localDate: LocalDate): Flow<List<WorkoutSession>>
    fun observeRecentSessions(limit: Int = 20): Flow<List<WorkoutSessionSummary>>
    fun observeEvents(sessionId: String): Flow<List<WorkoutEvent>>
    fun observeReadings(sessionId: String): Flow<List<HeartRateReading>>

    /** Caller invokes only after the user explicitly starts a workout. */
    suspend fun startWorkout(localDate: LocalDate, zoneId: ZoneId, startedAt: Instant): WorkoutSession

    /** Accept only values emitted by the connected band's GATT notification stream. */
    suspend fun appendReceivedReading(sessionId: String, reading: HeartRateReading)
    /** Persist the observed event and mark an active session interrupted atomically. */
    suspend fun recordInterruption(sessionId: String, event: WorkoutEvent)

    /** Resume only after an explicit user-confirmed reconnection; retain the gap event. */
    suspend fun recordExplicitReconnection(sessionId: String, event: WorkoutEvent)

    suspend fun finishWorkout(
        sessionId: String,
        endedAt: Instant,
        status: WorkoutEndStatus,
    )

    suspend fun deleteWorkout(sessionId: String)
}

data class WorkoutSessionSummary(
    val session: WorkoutSession,
    val sampleCount: Int,
    val hadInterruption: Boolean,
    val recoveryDetected: Boolean,
    val interruptionObservedAt: Instant?,
)

enum class AppTheme { SAGE, OCEAN, LAVENDER, CORAL }

data class ClientPreferences(
    val theme: AppTheme = AppTheme.SAGE,
    val recommendationFilter: RecommendationFilter = RecommendationFilter(),
    val thirdMealEnabled: Boolean = false,
    val serviceBaseUrl: String? = null,
)

/** Contains display/connection preferences only; never diary, cycle or workout data. */
interface ClientPreferencesRepository {
    fun observePreferences(): Flow<ClientPreferences>
    suspend fun savePreferences(preferences: ClientPreferences)
}

data class SettingsPreferences(
    val theme: AppTheme = AppTheme.SAGE,
    val serviceBaseUrl: String? = null,
)

/** Settings-only view of the non-health preferences; no API credential belongs here. */
interface SettingsPreferencesRepository {
    fun observeSettings(): Flow<SettingsPreferences>
    suspend fun saveSettings(preferences: SettingsPreferences)
}
