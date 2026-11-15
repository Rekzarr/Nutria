package com.nutria.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationRegistryTest {
    @Test
    fun featureMigrationsAreRegisteredInContinuousVersionOrder() {
        assertTrue(LocalDatabaseMigrations.SCHEMA_VERSION >= 6)
        assertTrue(LocalDatabaseMigrations.all.isNotEmpty())
        assertTrue(LocalDatabaseMigrations.all.asList().zipWithNext().all { (first, next) ->
            first.endVersion == next.startVersion
        })
        assertEquals(1, LocalDatabaseMigrations.all.first().startVersion)
        assertEquals(LocalDatabaseMigrations.SCHEMA_VERSION, LocalDatabaseMigrations.all.last().endVersion)
        assertTrue(LocalDatabaseMigrations.all.any { it.startVersion == 4 && it.endVersion == 5 })
        assertTrue(LocalDatabaseMigrations.all.any { it.startVersion == 5 && it.endVersion == 6 })
    }
}
