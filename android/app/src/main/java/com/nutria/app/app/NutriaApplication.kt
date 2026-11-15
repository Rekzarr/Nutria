package com.nutria.app.app

import android.app.Application
import com.nutria.app.core.data.CycleRepository
import com.nutria.app.core.data.DiaryRepository
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.data.SettingsPreferencesRepository
import com.nutria.app.data.local.CycleRoomRepository
import com.nutria.app.data.local.EncryptedDatabaseProvider
import com.nutria.app.data.local.GoalsRoomRepository
import com.nutria.app.data.local.PreferencesStore
import com.nutria.app.data.local.RoomDiaryRepository
import com.nutria.app.data.remote.ConfiguredNutriaApiClientFactory
import com.nutria.app.feature.activity.WorkoutRoomRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NutriaApplication : Application() {
    private val databaseProvider by lazy { EncryptedDatabaseProvider(this) }
    private val repositoryMutex = Mutex()
    private var cachedDiaryRepository: DiaryRepository? = null
    private var cachedCycleRepository: CycleRepository? = null
    private val cachedWorkoutRepository: WorkoutRoomRepository by lazy {
        WorkoutRoomRepository { databaseProvider.open() }
    }
    private val cachedGoalsRepository: GoalsRepository by lazy {
        GoalsRoomRepository(databaseProvider)
    }
    private val cachedSettingsPreferences: SettingsPreferencesRepository by lazy {
        PreferencesStore(applicationContext)
    }

    suspend fun diaryRepository(): DiaryRepository = repositoryMutex.withLock {
        cachedDiaryRepository ?: RoomDiaryRepository(databaseProvider.open())
            .also { cachedDiaryRepository = it }
    }

    suspend fun cycleRepository(): CycleRepository = repositoryMutex.withLock {
        cachedCycleRepository ?: CycleRoomRepository(databaseProvider)
            .also { cachedCycleRepository = it }
    }

    fun goalsRepository(): GoalsRepository = cachedGoalsRepository

    internal fun workoutRepository(): WorkoutRoomRepository = cachedWorkoutRepository

    fun settingsPreferencesRepository(): SettingsPreferencesRepository = cachedSettingsPreferences

    fun apiClientFactory() = ConfiguredNutriaApiClientFactory(cachedSettingsPreferences)
}
