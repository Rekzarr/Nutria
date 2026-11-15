package com.nutria.app.data.remote

import com.nutria.app.BuildConfig
import com.nutria.app.core.data.SettingsPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Creates a client from the user-configured service URL at the moment of use.
 * The mobile app never accepts or stores an OpenAI/provider credential.
 */
class ConfiguredNutriaApiClientFactory(
    private val preferencesRepository: SettingsPreferencesRepository,
) {
    suspend fun createOrNull(): NutriaApiClient? {
        val baseUrl = preferencesRepository.observeSettings().first().serviceBaseUrl
            ?.takeIf(String::isNotBlank)
            ?: return null
        return NutriaApiClient(
            baseUrl = baseUrl,
            allowCleartextForLocalDebug = BuildConfig.DEBUG,
        )
    }
}
