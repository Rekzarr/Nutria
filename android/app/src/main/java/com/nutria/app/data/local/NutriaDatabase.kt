package com.nutria.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nutria.app.feature.activity.HeartRateSampleEntity
import com.nutria.app.feature.activity.WorkoutDao
import com.nutria.app.feature.activity.WorkoutEventEntity
import com.nutria.app.feature.activity.WorkoutSessionEntity

/**
 * App-private database containing the shared bootstrap and feature-owned local records.
 */
@Entity(tableName = "storage_metadata")
internal data class StorageMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "initialized_at_epoch_ms") val initializedAtEpochMs: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
internal interface StorageMetadataDao {
    @Query("SELECT * FROM storage_metadata WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int = StorageMetadataEntity.SINGLETON_ID): StorageMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(record: StorageMetadataEntity)
}

@Database(
    entities = [
        StorageMetadataEntity::class,
        MealRecordEntity::class,
        WaterEventEntity::class,
        CycleSettingsEntity::class,
        PeriodRecordEntity::class,
        CycleDayEntity::class,
        WorkoutSessionEntity::class,
        WorkoutEventEntity::class,
        HeartRateSampleEntity::class,
        NutritionGoalsEntity::class,
    ],
    version = LocalDatabaseMigrations.SCHEMA_VERSION,
    exportSchema = true,
)
internal abstract class NutriaDatabase : RoomDatabase() {
    abstract fun storageMetadataDao(): StorageMetadataDao
    abstract fun diaryDao(): DiaryDao
    abstract fun cycleDao(): CycleDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionGoalsDao(): NutritionGoalsDao
}

/**
 * Versioned migration registry. The initial schema is version 1; feature tasks own
 * their schema additions and must append explicit migrations rather than use a
 * destructive fallback.
 */
internal object LocalDatabaseMigrations {
    const val SCHEMA_VERSION = 6
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `diary_meals` (
                    `id` TEXT NOT NULL,
                    `local_date` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `calories_kcal` REAL NOT NULL,
                    `protein_grams` REAL NOT NULL,
                    `carbohydrates_grams` REAL NOT NULL,
                    `fat_grams` REAL NOT NULL,
                    `source` TEXT NOT NULL,
                    `recorded_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_meals_local_date` ON `diary_meals` (`local_date`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `water_events` (
                    `id` TEXT NOT NULL,
                    `local_date` TEXT NOT NULL,
                    `amount_millilitres` INTEGER NOT NULL,
                    `sequence` INTEGER NOT NULL,
                    `recorded_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_events_local_date_sequence` ON `water_events` (`local_date`, `sequence`)")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cycle_settings` (
                    `id` INTEGER NOT NULL PRIMARY KEY CHECK(`id` = 1),
                    `cycle_length_days` INTEGER NOT NULL CHECK(`cycle_length_days` BETWEEN 21 AND 45),
                    `period_length_days` INTEGER NOT NULL CHECK(`period_length_days` BETWEEN 2 AND 10),
                    CHECK(`period_length_days` < `cycle_length_days`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO `cycle_settings` (`id`, `cycle_length_days`, `period_length_days`)
                VALUES (1, 28, 5)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `period_records` (
                    `id` TEXT NOT NULL,
                    `start_date` TEXT NOT NULL,
                    `end_date` TEXT,
                    PRIMARY KEY(`id`),
                    CHECK(`end_date` IS NULL OR `end_date` >= `start_date`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_period_records_start_date` " +
                    "ON `period_records` (`start_date`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cycle_days` (
                    `local_date` TEXT NOT NULL,
                    `flow` TEXT NOT NULL CHECK(`flow` IN ('NONE', 'SPOTTING', 'LIGHT', 'MEDIUM', 'HEAVY')),
                    `pain` INTEGER NOT NULL CHECK(`pain` BETWEEN 0 AND 10),
                    `mood` TEXT NOT NULL CHECK(`mood` IN ('GOOD', 'STABLE', 'TIRED', 'SENSITIVE', 'IRRITABLE')),
                    `note` TEXT NOT NULL DEFAULT '' CHECK(length(`note`) <= 160),
                    PRIMARY KEY(`local_date`)
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sessions` (
                    `id` TEXT NOT NULL,
                    `started_at_epoch_ms` INTEGER NOT NULL,
                    `ended_at_epoch_ms` INTEGER,
                    `local_date` TEXT NOT NULL,
                    `zone_id` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `end_reason` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_sessions_local_date_started_at_epoch_ms` " +
                    "ON `workout_sessions` (`local_date`, `started_at_epoch_ms`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_events` (
                    `id` TEXT NOT NULL,
                    `session_id` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `occurred_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_events_session_id_occurred_at_epoch_ms` " +
                    "ON `workout_events` (`session_id`, `occurred_at_epoch_ms`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `heart_rate_samples` (
                    `id` TEXT NOT NULL,
                    `session_id` TEXT NOT NULL,
                    `received_at_epoch_ms` INTEGER NOT NULL,
                    `beats_per_minute` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_heart_rate_samples_session_id_received_at_epoch_ms` " +
                    "ON `heart_rate_samples` (`session_id`, `received_at_epoch_ms`)",
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `nutrition_goals` (
                    `id` INTEGER NOT NULL PRIMARY KEY CHECK(`id` = 1),
                    `calories_kcal` INTEGER NOT NULL CHECK(`calories_kcal` > 0),
                    `water_millilitres` INTEGER NOT NULL
                        CHECK(`water_millilitres` >= 50 AND `water_millilitres` % 50 = 0),
                    `protein_milli_grams` INTEGER NOT NULL CHECK(`protein_milli_grams` > 0),
                    `carbohydrates_milli_grams` INTEGER NOT NULL CHECK(`carbohydrates_milli_grams` > 0),
                    `fat_milli_grams` INTEGER NOT NULL CHECK(`fat_milli_grams` > 0)
                )
                """.trimIndent(),
            )
        }
    }

    internal val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `diary_meals_scaled` (
                    `id` TEXT NOT NULL,
                    `local_date` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `calories_milli_kcal` INTEGER NOT NULL,
                    `protein_milli_grams` INTEGER NOT NULL,
                    `carbohydrates_milli_grams` INTEGER NOT NULL,
                    `fat_milli_grams` INTEGER NOT NULL,
                    `source` TEXT NOT NULL,
                    `recorded_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `diary_meals_scaled` (
                    `id`, `local_date`, `name`, `calories_milli_kcal`,
                    `protein_milli_grams`, `carbohydrates_milli_grams`,
                    `fat_milli_grams`, `source`, `recorded_at_epoch_ms`
                )
                SELECT
                    `id`, `local_date`, `name`,
                    CAST(ROUND(`calories_kcal` * 1000.0) AS INTEGER),
                    CAST(ROUND(`protein_grams` * 1000.0) AS INTEGER),
                    CAST(ROUND(`carbohydrates_grams` * 1000.0) AS INTEGER),
                    CAST(ROUND(`fat_grams` * 1000.0) AS INTEGER),
                    `source`, `recorded_at_epoch_ms`
                FROM `diary_meals`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `diary_meals`")
            db.execSQL("ALTER TABLE `diary_meals_scaled` RENAME TO `diary_meals`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_diary_meals_local_date` " +
                    "ON `diary_meals` (`local_date`)",
            )
        }
    }

    val all: Array<Migration> =
        arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
