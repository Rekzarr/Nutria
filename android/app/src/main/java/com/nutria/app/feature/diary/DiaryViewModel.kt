package com.nutria.app.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutria.app.core.data.DiaryRepository
import com.nutria.app.core.model.MealEntry
import com.nutria.app.core.model.MealSource
import com.nutria.app.core.model.WaterEvent
import com.nutria.app.core.model.Nutrients
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class DiaryUiState(
    val date: LocalDate,
    val isLoading: Boolean = false,
    val todayMeals: List<MealEntry> = emptyList(),
    val mealHistory: List<MealEntry> = emptyList(),
    val waterEvents: List<com.nutria.app.core.model.WaterEvent> = emptyList(),
    val storageError: String? = null,
    val successMessage: String? = null,
) {
    val waterMillilitres: Int get() = waterEvents.sumOf { it.amountMillilitres }
    val canUndoWater: Boolean get() = waterEvents.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val repository: DiaryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val currentDate = MutableStateFlow(LocalDate.now(clock))
    private val storageError = MutableStateFlow<String?>(null)
    private val successMessage = MutableStateFlow<String?>(null)

    private val diaryData = currentDate.flatMapLatest { date ->
        combine(
            repository.observeMeals(date),
            repository.observeMealHistory(date.minusDays(HISTORY_DAYS.toLong() - 1L), date),
            repository.observeWaterEvents(date),
        ) { meals, history, water ->
            DiaryData(date, meals, history, water)
        }
    }.catch { failure ->
        if (failure is CancellationException) throw failure
        storageError.value = STORAGE_FAILURE_MESSAGE
        emit(DiaryData(currentDate.value, emptyList(), emptyList(), emptyList()))
    }

    val state: StateFlow<DiaryUiState> = combine(
        diaryData,
        storageError,
        successMessage,
    ) { data, error, success ->
        DiaryUiState(
            date = data.date,
            todayMeals = data.todayMeals,
            mealHistory = data.history,
            waterEvents = data.waterEvents,
            storageError = error,
            successMessage = success,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = DiaryUiState(date = LocalDate.now(clock), isLoading = true),
    )

    fun refreshCurrentDate() {
        currentDate.value = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
    }

    suspend fun addFood(entry: ValidatedFoodEntry): Boolean = mutate(
        success = "${entry.name} se añadió a tu día.",
    ) {
        repository.addMeal(
            MealEntry(
                id = UUID.randomUUID().toString(),
                localDate = currentDate.value,
                name = entry.name,
                nutrients = entry.nutrients,
                source = MealSource.FOOD,
                recordedAt = Instant.now(clock),
            ),
        )
    }

    suspend fun addPhotoMeal(name: String, nutrients: Nutrients): Boolean {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty() && normalizedName.length <= MAX_MEAL_NAME_LENGTH)
        return mutate(success = "$normalizedName se añadió a tu día.") {
            repository.addMeal(
                MealEntry(
                    id = UUID.randomUUID().toString(),
                    localDate = currentDate.value,
                    name = normalizedName,
                    nutrients = nutrients,
                    source = MealSource.PHOTO,
                    recordedAt = Instant.now(clock),
                ),
            )
        }
    }

    suspend fun addWater(amountMillilitres: Int): Boolean {
        require(amountMillilitres == SMALL_WATER_INCREMENT || amountMillilitres == LARGE_WATER_INCREMENT)
        return mutate(
            success = "$amountMillilitres ml de agua añadidos.",
        ) {
            repository.addWaterEvent(currentDate.value, amountMillilitres, Instant.now(clock))
        }
    }

    suspend fun undoLatestWater(): Boolean {
        storageError.value = null
        successMessage.value = null
        return try {
            val removed = repository.undoLatestWaterEvent(currentDate.value)
            if (removed == null) {
                successMessage.value = "No hay ninguna toma de agua para deshacer."
                return false
            }
            successMessage.value = "${removed.amountMillilitres} ml de la última toma se han eliminado."
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageError.value = STORAGE_FAILURE_MESSAGE
            false
        }
    }

    suspend fun removeMeal(meal: MealEntry): Boolean {
        if (meal.localDate != currentDate.value) return false
        return mutate(success = "Comida eliminada del registro.") {
            repository.removeMeal(currentDate.value, meal.id)
        }
    }

    fun clearStatus() {
        storageError.value = null
        successMessage.value = null
    }

    private suspend fun mutate(success: String, action: suspend () -> Unit): Boolean {
        storageError.value = null
        successMessage.value = null
        return try {
            action()
            successMessage.value = success
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            storageError.value = STORAGE_FAILURE_MESSAGE
            false
        }
    }

    private data class DiaryData(
        val date: LocalDate,
        val todayMeals: List<MealEntry>,
        val history: List<MealEntry>,
        val waterEvents: List<WaterEvent>,
    )

    companion object {
        const val HISTORY_DAYS = 30
        const val SMALL_WATER_INCREMENT = 150
        const val LARGE_WATER_INCREMENT = 250
        const val MAX_MEAL_NAME_LENGTH = 80
        const val STORAGE_FAILURE_MESSAGE =
            "No se pudo guardar en este dispositivo. No se ha confirmado el cambio."
    }
}

class DiaryViewModelFactory(
    private val repository: DiaryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DiaryViewModel::class.java))
        return DiaryViewModel(repository) as T
    }
}
