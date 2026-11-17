package com.nutria.app.feature.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.SavedRecipe
import java.io.ByteArrayOutputStream
import java.io.InputStream

object RecipeImportParser {
    const val FORMAT = "nutria.recipe.v1"
    const val MAX_FILE_SIZE_BYTES = 128 * 1024
    private const val MAX_INGREDIENTS = 100

    fun parse(input: InputStream): SavedRecipe {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4_096)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("El archivo supera el límite de 128 KiB.")
            }
            output.write(buffer, 0, count)
        }
        return parse(String(output.toByteArray(), Charsets.UTF_8))
    }

    fun parse(json: String): SavedRecipe {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("El archivo supera el límite de 128 KiB.")
        }
        val root = try {
            JsonParser.parseString(json.removePrefix("\uFEFF"))
        } catch (failure: JsonParseException) {
            throw IllegalArgumentException("El archivo no contiene un JSON válido.", failure)
        }
        if (!root.isJsonObject) invalid("La raíz del archivo debe ser un objeto JSON.")

        val rootObject = root.asJsonObject
        if (rootObject.string("format") != FORMAT) {
            invalid("El formato no es compatible. Se esperaba $FORMAT.")
        }
        val recipeObject = rootObject.required("recipe").asObject("recipe")
        val categoryName = recipeObject.string("category")
        val category = RecipeCategory.entries.firstOrNull { it.name == categoryName }
            ?: invalid("La categoría debe ser MEAL o DINNER.")
        val complexityName = recipeObject.optionalString("complexity")
        val complexity = complexityName?.let { value ->
            RecipeComplexity.entries.firstOrNull { it.name == value }
                ?: invalid("La dificultad debe ser EASY, MEDIUM, ADVANCED o null.")
        }
        val ingredientsElement = recipeObject.required("ingredients")
        if (!ingredientsElement.isJsonArray || ingredientsElement.asJsonArray.isEmpty) {
            invalid("La receta debe incluir al menos un ingrediente.")
        }
        val ingredientsArray = ingredientsElement.asJsonArray
        if (ingredientsArray.size() > MAX_INGREDIENTS) {
            invalid("El archivo admite hasta $MAX_INGREDIENTS ingredientes.")
        }
        val ingredients = ingredientsArray.mapIndexed { index, element ->
            val ingredient = element.asObject("ingredients[$index]")
            val nutrition = ingredient.required("nutritionPer100g").asObject("nutritionPer100g")
            RecipeIngredientDraft(
                name = ingredient.string("name"),
                edibleGrams = ingredient.number("edibleGrams").toString(),
                caloriesPer100Grams = nutrition.number("caloriesKcal").toString(),
                proteinPer100Grams = nutrition.number("proteinGrams").toString(),
                carbohydratesPer100Grams = nutrition.number("carbohydratesGrams").toString(),
                fatPer100Grams = nutrition.number("fatGrams").toString(),
            )
        }
        val instructionsElement = recipeObject.required("instructions")
        if (!instructionsElement.isJsonArray) invalid("instructions debe ser una lista de textos.")
        val instructions = instructionsElement.asJsonArray.mapIndexed { index, element ->
            element.asString("instructions[$index]")
        }
        val prepMinutes = recipeObject.optionalInteger("prepMinutes")?.toString().orEmpty()
        val validation = RecipeFormValidator.validate(
            RecipeFormDraft(
                name = recipeObject.string("name"),
                category = category,
                servings = recipeObject.integer("servings").toString(),
                prepMinutes = prepMinutes,
                complexity = complexity,
                ingredients = ingredients,
                instructions = instructions.joinToString("\n"),
            ),
        )
        return validation.recipe ?: invalid(validation.errors.joinToString(" "))
    }

    private fun JsonObject.required(name: String): JsonElement =
        get(name)?.takeUnless { it.isJsonNull } ?: invalid("Falta el campo «$name».")

    private fun JsonObject.string(name: String): String = required(name).asString(name).trim()

    private fun JsonObject.optionalString(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        return element.asString(name).trim()
    }

    private fun JsonObject.number(name: String): Double = required(name).asNumber(name)

    private fun JsonObject.integer(name: String): Int = required(name).asInteger(name)

    private fun JsonObject.optionalInteger(name: String): Int? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        return element.asInteger(name)
    }

    private fun JsonElement.asObject(field: String): JsonObject {
        if (!isJsonObject) invalid("«$field» debe ser un objeto.")
        return asJsonObject
    }

    private fun JsonElement.asString(field: String): String {
        if (!isJsonPrimitive || !asJsonPrimitive.isString) invalid("«$field» debe ser texto.")
        return asString
    }

    private fun JsonElement.asNumber(field: String): Double {
        if (!isJsonPrimitive || !asJsonPrimitive.isNumber) invalid("«$field» debe ser un número.")
        val value = try {
            asDouble
        } catch (failure: NumberFormatException) {
            invalid("«$field» debe ser un número válido.")
        }
        if (!value.isFinite() || value < 0.0 || value > Long.MAX_VALUE / 1_000.0) {
            invalid("«$field» está fuera del rango permitido.")
        }
        return value
    }

    private fun JsonElement.asInteger(field: String): Int {
        val value = asNumber(field)
        if (value % 1.0 != 0.0 || value > Int.MAX_VALUE) {
            invalid("«$field» debe ser un número entero válido.")
        }
        return value.toInt()
    }

    private fun invalid(message: String): Nothing = throw IllegalArgumentException(message)
}
