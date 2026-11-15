package com.nutria.app.data.remote

import com.nutria.app.core.data.AppTheme
import com.nutria.app.core.data.SettingsPreferences
import com.nutria.app.core.data.SettingsPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConfiguredNutriaApiClientFactoryTest {
    @Test
    fun createsClientOnlyWhenAServiceUrlWasConfigured() = runBlocking {
        val settings = FakeSettingsPreferencesRepository()
        val factory = ConfiguredNutriaApiClientFactory(settings)

        assertNull(factory.createOrNull())
        settings.saveSettings(
            SettingsPreferences(AppTheme.SAGE, "https://nutria.example.test/"),
        )
        assertNotNull(factory.createOrNull())
    }

    private class FakeSettingsPreferencesRepository : SettingsPreferencesRepository {
        private val value = MutableStateFlow(SettingsPreferences())
        override fun observeSettings(): Flow<SettingsPreferences> = value
        override suspend fun saveSettings(preferences: SettingsPreferences) {
            value.value = preferences
        }
    }
}
