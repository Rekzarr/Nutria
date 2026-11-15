package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.nutria.app.core.data.CycleRepository
import com.nutria.app.core.model.CycleDayEntry
import com.nutria.app.core.model.CycleFlow
import com.nutria.app.core.model.CycleMood
import com.nutria.app.core.model.CycleSettings
import com.nutria.app.core.model.PeriodRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@Entity(tableName = "cycle_settings")
internal data class CycleSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "cycle_length_days") val cycleLengthDays: Int,
    @ColumnInfo(name = "period_length_days") val periodLengthDays: Int,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "period_records",
    indices = [Index(value = ["start_date"], unique = true)],
)
internal data class PeriodRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?,
)

@Entity(tableName = "cycle_days")
internal data class CycleDayEntity(
    @PrimaryKey @ColumnInfo(name = "local_date") val localDate: String,
    val flow: String,
    val pain: Int,
    val mood: String,
    @ColumnInfo(defaultValue = "''") val note: String,
)

@Dao
internal interface CycleDao {
    @Query("SELECT * FROM cycle_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: Int = CycleSettingsEntity.SINGLETON_ID): Flow<CycleSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSettingsIfMissing(settings: CycleSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: CycleSettingsEntity)

    @Query("SELECT * FROM period_records ORDER BY start_date DESC")
    fun observePeriods(): Flow<List<PeriodRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePeriod(period: PeriodRecordEntity)

    @Query("DELETE FROM period_records WHERE id = :periodId")
    suspend fun deletePeriod(periodId: String)

    @Query(
        "SELECT * FROM cycle_days WHERE local_date BETWEEN :fromDate AND :throughDate " +
            "ORDER BY local_date DESC",
    )
    fun observeEntries(fromDate: String, throughDate: String): Flow<List<CycleDayEntity>>

    @Query(
        "SELECT * FROM cycle_days WHERE local_date <= :throughDate " +
            "ORDER BY local_date DESC LIMIT :limit",
    )
    fun observeRecentEntries(throughDate: String, limit: Int): Flow<List<CycleDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEntry(entry: CycleDayEntity)
}

/**
 * Cycle data is stored only through the app-private encrypted Room database.
 * The repository has no network client and never emits note text to logs.
 */
internal class CycleRoomRepository(
    private val databaseProvider: EncryptedDatabaseProvider,
) : CycleRepository {
    override fun observeSettings(): Flow<CycleSettings> = flow {
        val dao = databaseProvider.open().cycleDao()
        dao.insertSettingsIfMissing(
            CycleSettingsEntity(
                cycleLengthDays = CycleSettings.DEFAULT_CYCLE_LENGTH_DAYS,
                periodLengthDays = CycleSettings.DEFAULT_PERIOD_LENGTH_DAYS,
            ),
        )
        emitAll(dao.observeSettings().map { entity -> entity?.toModel() ?: CycleSettings() })
    }

    override fun observePeriods(): Flow<List<PeriodRecord>> = flow {
        emitAll(databaseProvider.open().cycleDao().observePeriods().map { records ->
            records.map(PeriodRecordEntity::toModel)
        })
    }

    override fun observeEntries(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
    ): Flow<List<CycleDayEntry>> = flow {
        emitAll(
            databaseProvider.open().cycleDao()
                .observeEntries(fromInclusive.toString(), throughInclusive.toString())
                .map { entries -> entries.map(CycleDayEntity::toModel) },
        )
    }

    override fun observeRecentEntries(
        throughInclusive: LocalDate,
        limit: Int,
    ): Flow<List<CycleDayEntry>> {
        require(limit > 0)
        return flow {
            emitAll(
                databaseProvider.open().cycleDao()
                    .observeRecentEntries(throughInclusive.toString(), limit)
                    .map { entries -> entries.map(CycleDayEntity::toModel) },
            )
        }
    }

    override suspend fun saveSettings(settings: CycleSettings) {
        databaseProvider.open().cycleDao().saveSettings(settings.toEntity())
    }

    override suspend fun savePeriod(period: PeriodRecord) {
        databaseProvider.open().cycleDao().savePeriod(period.toEntity())
    }

    override suspend fun deletePeriod(periodId: String) {
        databaseProvider.open().cycleDao().deletePeriod(periodId)
    }

    override suspend fun saveDayEntry(entry: CycleDayEntry) {
        val normalized = entry.copy(note = entry.note.trim())
        databaseProvider.open().cycleDao().saveEntry(normalized.toEntity())
    }
}

private fun CycleSettingsEntity.toModel() = CycleSettings(
    cycleLengthDays = cycleLengthDays,
    periodLengthDays = periodLengthDays,
)

private fun CycleSettings.toEntity() = CycleSettingsEntity(
    cycleLengthDays = cycleLengthDays,
    periodLengthDays = periodLengthDays,
)

private fun PeriodRecordEntity.toModel() = PeriodRecord(
    id = id,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
)

private fun PeriodRecord.toEntity() = PeriodRecordEntity(
    id = id,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
)

private fun CycleDayEntity.toModel() = CycleDayEntry(
    date = LocalDate.parse(localDate),
    flow = CycleFlow.valueOf(flow),
    pain = pain,
    mood = CycleMood.valueOf(mood),
    note = note,
)

private fun CycleDayEntry.toEntity() = CycleDayEntity(
    localDate = date.toString(),
    flow = flow.name,
    pain = pain,
    mood = mood.name,
    note = note,
)
