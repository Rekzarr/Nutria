package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.withTransaction
import com.nutria.app.core.data.CalendarRepository
import com.nutria.app.core.model.CalendarAssignment
import com.nutria.app.core.model.CalendarSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@Entity(
    tableName = "calendar_assignments",
    primaryKeys = ["local_date", "slot"],
    foreignKeys = [
        ForeignKey(
            entity = SavedRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recipe_id"])],
)
internal data class CalendarAssignmentEntity(
    @ColumnInfo(name = "local_date") val localDate: String,
    val slot: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String,
)

@Dao
internal interface CalendarDao {
    @Query(
        "SELECT * FROM calendar_assignments " +
            "WHERE local_date BETWEEN :fromInclusive AND :throughInclusive " +
            "ORDER BY local_date, slot",
    )
    fun observeAssignments(fromInclusive: String, throughInclusive: String): Flow<List<CalendarAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAssignment(assignment: CalendarAssignmentEntity)

    @Query(
        "DELETE FROM calendar_assignments " +
            "WHERE local_date BETWEEN :fromInclusive AND :throughInclusive",
    )
    suspend fun deleteAssignments(fromInclusive: String, throughInclusive: String)

    @Insert
    suspend fun insertAssignments(assignments: List<CalendarAssignmentEntity>)
}

internal class CalendarRoomRepository(
    private val databaseProvider: EncryptedDatabaseProvider,
) : CalendarRepository {
    override fun observeAssignments(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
    ): Flow<List<CalendarAssignment>> = flow {
        require(fromInclusive <= throughInclusive)
        emitAll(
            databaseProvider.open().calendarDao()
                .observeAssignments(fromInclusive.toString(), throughInclusive.toString())
                .map { assignments -> assignments.map(CalendarAssignmentEntity::toModel) },
        )
    }

    override suspend fun saveAssignment(assignment: CalendarAssignment) {
        databaseProvider.open().calendarDao().saveAssignment(assignment.toEntity())
    }

    override suspend fun replaceAssignments(
        fromInclusive: LocalDate,
        throughInclusive: LocalDate,
        assignments: List<CalendarAssignment>,
    ) {
        require(fromInclusive <= throughInclusive)
        require(assignments.all { it.localDate in fromInclusive..throughInclusive })
        require(assignments.map { it.localDate to it.slot }.distinct().size == assignments.size)

        val database = databaseProvider.open()
        val dao = database.calendarDao()
        database.withTransaction {
            dao.deleteAssignments(fromInclusive.toString(), throughInclusive.toString())
            if (assignments.isNotEmpty()) {
                dao.insertAssignments(assignments.map(CalendarAssignment::toEntity))
            }
        }
    }
}

private fun CalendarAssignment.toEntity() = CalendarAssignmentEntity(
    localDate = localDate.toString(),
    slot = slot.name,
    recipeId = recipeId,
)

private fun CalendarAssignmentEntity.toModel() = CalendarAssignment(
    localDate = LocalDate.parse(localDate),
    slot = CalendarSlot.valueOf(slot),
    recipeId = recipeId,
)
