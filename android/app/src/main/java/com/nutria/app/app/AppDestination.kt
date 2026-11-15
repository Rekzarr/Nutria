package com.nutria.app.app

import androidx.annotation.StringRes
import com.nutria.app.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val iconRes: Int
) {
    INICIO("inicio", R.string.destination_home, R.string.icon_home),
    COMIDA("comida", R.string.destination_food, R.string.icon_food),
    RECETAS("recetas", R.string.destination_recipes, R.string.icon_recipes),
    FOTO("foto", R.string.destination_photo, R.string.icon_photo),
    CICLO("ciclo", R.string.destination_cycle, R.string.icon_cycle),
    PLAN("plan", R.string.destination_plan, R.string.icon_plan),
    ACTIVIDAD("actividad", R.string.destination_activity, R.string.icon_activity),
    AJUSTES("ajustes", R.string.destination_settings, R.string.icon_settings);

    companion object {
        val primary: List<AppDestination> = listOf(
            INICIO,
            COMIDA,
            RECETAS,
            FOTO,
            CICLO,
            PLAN,
            ACTIVIDAD
        )

        fun fromRoute(route: String?): AppDestination =
            when (route) {
                "registrar" -> COMIDA
                else -> entries.firstOrNull { it.route == route } ?: INICIO
            }
    }
}

/**
 * Keeps destination navigation deterministic and prevents a drawer/deep-link action from
 * pushing a second copy of a destination that is already in the back stack.
 */
object NavigationHistory {
    fun navigate(stack: List<String>, route: String): List<String> {
        val current = normalize(stack)
        val destination = AppDestination.fromRoute(route).route
        if (current.last() == destination) return current

        val existingIndex = current.indexOf(destination)
        return if (existingIndex >= 0) {
            current.take(existingIndex + 1)
        } else {
            current + destination
        }
    }

    fun back(stack: List<String>): List<String> {
        val current = normalize(stack)
        return if (current.size > 1) current.dropLast(1) else current
    }

    private fun normalize(stack: List<String>): List<String> {
        val validRoutes = stack
            .map { AppDestination.fromRoute(it).route }
            .distinctInOrder()
        return validRoutes.ifEmpty { listOf(AppDestination.INICIO.route) }
    }

    private fun List<String>.distinctInOrder(): List<String> {
        val seen = mutableSetOf<String>()
        return filter { seen.add(it) }
    }
}
