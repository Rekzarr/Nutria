package com.nutria.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.nutria.app.core.data.SettingsPreferences
import com.nutria.app.core.data.SettingsPreferencesRepository
import com.nutria.app.core.data.AppTheme

private val Context.nutriaPreferencesDataStore by preferencesDataStore(name = "nutria_preferences")

internal enum class NutriaTheme(val storageValue: String) {
    SAGE("sage"),
    OCEAN("ocean"),
    LAVENDER("lavender"),
    CORAL("coral"),
}

/**
 * DataStore intentionally exposes only display and connection preferences. Health,
 * nutrition, cycle, recipe, meal, water, and workout records belong in encrypted Room.
 */
internal data class NonHealthPreferences(
    val theme: NutriaTheme = NutriaTheme.SAGE,
    val recommendationFilters: Set<String> = emptySet(),
    val breakfastEnabled: Boolean = false,
    val serviceBaseUrl: String? = null,
)

internal class PreferencesStore(context: Context) : SettingsPreferencesRepository {
    private val store = context.applicationContext.nutriaPreferencesDataStore

    val preferences: Flow<NonHealthPreferences> = store.data.map { values ->
        NonHealthPreferences(
            theme = NutriaTheme.entries.firstOrNull { it.storageValue == values[THEME] }
                ?: NutriaTheme.SAGE,
            recommendationFilters = values[RECOMMENDATION_FILTERS].orEmpty(),
            breakfastEnabled = values[BREAKFAST_ENABLED] ?: false,
            serviceBaseUrl = values[SERVICE_BASE_URL]?.takeIf(String::isNotBlank),
        )
    }

    override fun observeSettings(): Flow<SettingsPreferences> = preferences.map { value ->
        SettingsPreferences(
            theme = AppTheme.valueOf(value.theme.name),
            serviceBaseUrl = value.serviceBaseUrl,
        )
    }

    override suspend fun saveSettings(preferences: SettingsPreferences) {
        store.edit { values ->
            values[THEME] = NutriaTheme.valueOf(preferences.theme.name).storageValue
            val baseUrl = preferences.serviceBaseUrl?.trim()?.takeIf(String::isNotEmpty)
            if (baseUrl == null) {
                values.remove(SERVICE_BASE_URL)
            } else {
                values[SERVICE_BASE_URL] = baseUrl
            }
        }
    }

    suspend fun setTheme(theme: NutriaTheme) {
        store.edit { it[THEME] = theme.storageValue }
    }

    suspend fun setRecommendationFilters(filters: Set<String>) {
        store.edit { it[RECOMMENDATION_FILTERS] = filters }
    }

    suspend fun setBreakfastEnabled(enabled: Boolean) {
        store.edit { it[BREAKFAST_ENABLED] = enabled }
    }

    suspend fun setServiceBaseUrl(baseUrl: String?) {
        store.edit { values ->
            if (baseUrl.isNullOrBlank()) {
                values.remove(SERVICE_BASE_URL)
            } else {
                values[SERVICE_BASE_URL] = baseUrl.trim()
            }
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val RECOMMENDATION_FILTERS = stringSetPreferencesKey("recommendation_filters")
        val BREAKFAST_ENABLED = booleanPreferencesKey("breakfast_enabled")
        val SERVICE_BASE_URL = stringPreferencesKey("service_base_url")
    }
}
