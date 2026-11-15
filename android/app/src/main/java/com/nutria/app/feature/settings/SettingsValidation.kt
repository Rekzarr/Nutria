package com.nutria.app.feature.settings

import com.nutria.app.core.model.NutritionGoals
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class NutritionGoalDraft(
    val calories: String,
    val waterMillilitres: String,
    val proteinGrams: String,
    val carbohydratesGrams: String,
    val fatGrams: String,
)

data class GoalDraftResult(
    val goals: NutritionGoals?,
    val errors: Map<String, String>,
)

object GoalDraftValidator {
    const val CALORIES = "calories"
    const val WATER = "water"
    const val PROTEIN = "protein"
    const val CARBOHYDRATES = "carbohydrates"
    const val FAT = "fat"

    fun validate(draft: NutritionGoalDraft): GoalDraftResult {
        val errors = linkedMapOf<String, String>()
        val calories = draft.calories.trim().toIntOrNull()
            ?.takeIf { it > 0 }
            ?: run {
                errors[CALORIES] = "Introduce un número entero mayor que cero."
                null
            }
        val water = draft.waterMillilitres.trim().toIntOrNull()
            ?.takeIf { it >= 50 && it % 50 == 0 }
            ?: run {
                errors[WATER] = "El objetivo debe ser de al menos 50 ml y avanzar en pasos de 50 ml."
                null
            }
        val protein = draft.proteinGrams.parsePositiveDecimal()
            ?: run {
                errors[PROTEIN] = "Introduce una cantidad mayor que cero."
                null
            }
        val carbohydrates = draft.carbohydratesGrams.parsePositiveDecimal()
            ?: run {
                errors[CARBOHYDRATES] = "Introduce una cantidad mayor que cero."
                null
            }
        val fat = draft.fatGrams.parsePositiveDecimal()
            ?: run {
                errors[FAT] = "Introduce una cantidad mayor que cero."
                null
            }

        if (errors.isNotEmpty()) return GoalDraftResult(null, errors)
        val goals = runCatching {
            NutritionGoals(
                caloriesKcal = requireNotNull(calories),
                waterMillilitres = requireNotNull(water),
                proteinGrams = requireNotNull(protein),
                carbohydratesGrams = requireNotNull(carbohydrates),
                fatGrams = requireNotNull(fat),
            )
        }.getOrNull()
        return GoalDraftResult(goals, emptyMap())
    }

    private fun String.parsePositiveDecimal(): Double? =
        trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 && it <= Long.MAX_VALUE / 1_000.0 }
}

sealed interface ServiceUrlValidation {
    data class Valid(val normalizedUrl: String?) : ServiceUrlValidation
    data class Invalid(val message: String) : ServiceUrlValidation
}

object ServiceUrlValidator {
    /**
     * Cleartext is accepted only for an explicitly enabled local-debug connection, matching the
     * existing API client's HTTPS policy. The value is a service base URL, never a credential.
     */
    fun validate(rawValue: String, allowLocalHttpForDebug: Boolean): ServiceUrlValidation {
        val raw = rawValue.trim()
        if (raw.isEmpty()) return ServiceUrlValidation.Valid(null)
        val url = raw.toHttpUrlOrNull()
            ?: return ServiceUrlValidation.Invalid("Escribe una URL válida, por ejemplo https://nutria.ejemplo.")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            return ServiceUrlValidation.Invalid("La URL del servicio no debe incluir credenciales.")
        }
        if (url.encodedQuery != null || url.encodedFragment != null) {
            return ServiceUrlValidation.Invalid("La URL base no debe incluir parámetros ni fragmentos.")
        }
        val host = url.host.trim('[', ']').lowercase()
        val isLoopbackHost = host == "localhost" || host.startsWith("127.") || host == "::1"
        if (!url.isHttps && !(allowLocalHttpForDebug && isLoopbackHost && url.scheme == "http")) {
            return ServiceUrlValidation.Invalid(
                "La conexión debe usar HTTPS. HTTP solo se permite para depuración local.",
            )
        }
        return ServiceUrlValidation.Valid(url.toString())
    }
}
