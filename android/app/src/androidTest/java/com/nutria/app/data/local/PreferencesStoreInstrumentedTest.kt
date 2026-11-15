package com.nutria.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.core.data.AppTheme
import com.nutria.app.core.data.SettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesStoreInstrumentedTest {
    @Test
    fun preferencesPersistOnlyNonHealthSettings() = runBlocking {
        val store = PreferencesStore(InstrumentationRegistry.getInstrumentation().targetContext)

        store.setTheme(NutriaTheme.SAGE)
        store.setRecommendationFilters(emptySet())
        store.setBreakfastEnabled(false)
        assertFalse(store.preferences.first().breakfastEnabled)
        store.setServiceBaseUrl(null)
        store.setTheme(NutriaTheme.OCEAN)
        store.setRecommendationFilters(setOf("vegetarian"))
        store.setBreakfastEnabled(true)
        store.setServiceBaseUrl("https://nutria.example.test")

        val stored = store.preferences.first()
        assertEquals(NutriaTheme.OCEAN, stored.theme)
        assertEquals(setOf("vegetarian"), stored.recommendationFilters)
        assertEquals(true, stored.breakfastEnabled)
        assertEquals("https://nutria.example.test", stored.serviceBaseUrl)

        store.saveSettings(
            SettingsPreferences(
                theme = AppTheme.CORAL,
                serviceBaseUrl = "https://api.nutria.example.test/",
            ),
        )
        val settings = store.observeSettings().first()
        assertEquals(AppTheme.CORAL, settings.theme)
        assertEquals("https://api.nutria.example.test/", settings.serviceBaseUrl)
    }
}
