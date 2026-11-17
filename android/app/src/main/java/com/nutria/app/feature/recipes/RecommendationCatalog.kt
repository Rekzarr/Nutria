package com.nutria.app.feature.recipes

import com.nutria.app.core.model.Nutrients
import com.nutria.app.core.model.RecipeCategory
import com.nutria.app.core.model.RecipeComplexity
import com.nutria.app.core.model.RecipeIngredient
import com.nutria.app.core.model.RecipeRecommendation

internal object RecommendationCatalog {
    val recipes = listOf(
        recipe(
            id = "chicken-quinoa-bowl",
            name = "Bowl de pollo, quinoa y verduras",
            category = RecipeCategory.MEAL,
            prepMinutes = 25,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Pechuga de pollo cocinada", 100.0, 165.0, 31.0, 0.0, 3.6),
                ingredient("Quinoa cocida", 100.0, 120.0, 4.4, 21.3, 1.9),
                ingredient("Calabacín y pimiento", 150.0, 35.0, 1.3, 6.8, 0.3),
                ingredient("Aceite de oliva", 4.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Saltea el calabacín y el pimiento con el aceite.",
                "Añade el pollo y la quinoa cocidos, mezcla y sirve.",
            ),
        ),
        recipe(
            id = "salmon-sweet-potato",
            name = "Salmón con boniato y espinacas",
            category = RecipeCategory.DINNER,
            prepMinutes = 35,
            complexity = RecipeComplexity.MEDIUM,
            ingredients = listOf(
                ingredient("Salmón cocinado", 90.0, 206.0, 22.0, 0.0, 13.0),
                ingredient("Boniato cocido", 150.0, 90.0, 2.0, 20.7, 0.2),
                ingredient("Espinacas", 50.0, 23.0, 2.9, 3.6, 0.4),
                ingredient("Yogur griego natural", 30.0, 59.0, 10.0, 3.6, 0.4),
                ingredient("Aceite de oliva", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Calienta el horno a 200 °C o prepara una sartén antiadherente. Corta el boniato ya cocido en rodajas.",
                "Cocina el salmón con la mitad del aceite, unos minutos por cada lado, hasta que quede hecho por dentro.",
                "En otra sartén, saltea las espinacas con el aceite restante hasta que se ablanden.",
                "Sirve el salmón con el boniato y las espinacas. Mezcla el yogur con unas gotas de limón y úsalo como salsa.",
            ),
        ),
        recipe(
            id = "mediterranean-lentils",
            name = "Ensalada mediterránea de lentejas",
            category = RecipeCategory.MEAL,
            prepMinutes = 15,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Lentejas cocidas", 150.0, 116.0, 9.0, 20.1, 0.4),
                ingredient("Tomate", 80.0, 18.0, 0.9, 3.9, 0.2),
                ingredient("Pepino", 70.0, 16.0, 0.7, 3.6, 0.1),
                ingredient("Queso feta", 25.0, 264.0, 14.0, 4.1, 21.0),
                ingredient("Aceite de oliva", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Mezcla las lentejas con el tomate y el pepino troceados.",
                "Añade el feta desmenuzado y aliña con aceite, limón y hierbas.",
            ),
        ),
        recipe(
            id = "vegetable-omelette",
            name = "Tortilla de verduras con pan integral",
            category = RecipeCategory.DINNER,
            prepMinutes = 20,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Huevo", 100.0, 143.0, 12.6, 0.7, 9.5),
                ingredient("Champiñones", 80.0, 22.0, 3.1, 3.3, 0.3),
                ingredient("Espinacas", 50.0, 23.0, 2.9, 3.6, 0.4),
                ingredient("Pan integral", 50.0, 247.0, 13.0, 41.0, 4.2),
                ingredient("Aceite de oliva", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Saltea los champiñones y las espinacas con el aceite.",
                "Añade el huevo batido, cuaja la tortilla y sirve con pan integral.",
            ),
        ),
        recipe(
            id = "chickpea-curry",
            name = "Curry suave de garbanzos y arroz",
            category = RecipeCategory.DINNER,
            prepMinutes = 30,
            complexity = RecipeComplexity.MEDIUM,
            ingredients = listOf(
                ingredient("Garbanzos cocidos", 110.0, 164.0, 8.9, 27.4, 2.6),
                ingredient("Coliflor", 120.0, 25.0, 1.9, 5.0, 0.3),
                ingredient("Tomate triturado", 100.0, 29.0, 1.4, 5.8, 0.2),
                ingredient("Leche de coco ligera", 50.0, 95.0, 1.1, 2.1, 9.5),
                ingredient("Arroz integral cocido", 80.0, 123.0, 2.7, 25.6, 1.0),
                ingredient("Mezcla de curry", 2.0, 325.0, 9.0, 58.0, 8.0),
            ),
            instructions = listOf(
                "Calienta el arroz integral cocido siguiendo las indicaciones del envase y resérvalo tapado.",
                "Separa la coliflor en ramilletes pequeños y cuécela en una sartén con un chorrito de agua hasta que esté tierna.",
                "Añade el tomate triturado y el curry; remueve y deja que la salsa reduzca unos minutos.",
                "Incorpora los garbanzos y la leche de coco ligera. Cocina a fuego suave y sirve el curry sobre el arroz.",
            ),
        ),
        recipe(
            id = "yogurt-oat-berries",
            name = "Bol de yogur, avena y frutos rojos",
            category = RecipeCategory.MEAL,
            prepMinutes = 10,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Yogur griego natural", 200.0, 59.0, 10.0, 3.6, 0.4),
                ingredient("Copos de avena", 25.0, 389.0, 16.9, 66.3, 6.9),
                ingredient("Arándanos", 80.0, 57.0, 0.7, 14.5, 0.3),
                ingredient("Semillas de chía", 8.0, 486.0, 16.5, 42.1, 30.7),
                ingredient("Almendras", 8.0, 579.0, 21.2, 21.6, 49.9),
            ),
            instructions = listOf(
                "Sirve el yogur en un bol y añade la avena y los arándanos.",
                "Termina con la chía y las almendras troceadas.",
            ),
        ),
        recipe(
            id = "wholegrain-tuna-pasta",
            name = "Pasta integral con atún y tomate",
            category = RecipeCategory.MEAL,
            prepMinutes = 20,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Pasta integral cocida", 120.0, 149.0, 5.8, 30.9, 0.9),
                ingredient("Atún al natural escurrido", 70.0, 116.0, 25.5, 0.0, 0.8),
                ingredient("Tomate triturado", 100.0, 29.0, 1.4, 5.8, 0.2),
                ingredient("Espinacas", 40.0, 23.0, 2.9, 3.6, 0.4),
                ingredient("Aceite de oliva", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Cuece la pasta integral hasta que quede al dente y reserva un poco del agua de cocción.",
                "Mientras tanto, calienta el tomate triturado con el aceite en una sartén.",
                "Añade las espinacas y remueve hasta que se ablanden; incorpora el atún escurrido.",
                "Mezcla la pasta con la salsa. Ajusta la textura con un poco del agua reservada y sirve.",
            ),
        ),
        recipe(
            id = "tofu-brown-rice-stir-fry",
            name = "Salteado de tofu, brócoli y arroz",
            category = RecipeCategory.DINNER,
            prepMinutes = 30,
            complexity = RecipeComplexity.MEDIUM,
            ingredients = listOf(
                ingredient("Tofu firme", 100.0, 144.0, 17.3, 2.8, 8.7),
                ingredient("Brócoli", 100.0, 34.0, 2.8, 6.6, 0.4),
                ingredient("Zanahoria", 50.0, 41.0, 0.9, 9.6, 0.2),
                ingredient("Arroz integral cocido", 100.0, 123.0, 2.7, 25.6, 1.0),
                ingredient("Salsa de soja", 10.0, 53.0, 8.0, 4.9, 0.6),
                ingredient("Aceite de sésamo", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Calienta el arroz integral cocido y resérvalo tapado. Escurre el tofu y córtalo en dados.",
                "Dora el tofu en una sartén amplia con el aceite de sésamo y retíralo cuando esté dorado.",
                "Corta el brócoli en ramilletes pequeños y la zanahoria en láminas finas; saltéalos hasta que estén tiernos pero firmes.",
                "Devuelve el tofu a la sartén, añade la salsa de soja y mezcla. Sirve el salteado sobre el arroz.",
            ),
        ),
        recipe(
            id = "stuffed-pepper-turkey-bulgur",
            name = "Pimiento relleno de pavo y bulgur",
            category = RecipeCategory.DINNER,
            prepMinutes = 40,
            complexity = RecipeComplexity.MEDIUM,
            ingredients = listOf(
                ingredient("Pimiento", 150.0, 31.0, 1.0, 6.0, 0.3),
                ingredient("Pavo magro cocinado", 100.0, 135.0, 29.0, 0.0, 1.6),
                ingredient("Bulgur cocido", 100.0, 83.0, 3.1, 18.6, 0.2),
                ingredient("Tomate triturado", 80.0, 29.0, 1.4, 5.8, 0.2),
                ingredient("Queso rallado", 15.0, 350.0, 25.0, 2.0, 27.0),
                ingredient("Aceite de oliva", 4.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Calienta el horno a 200 °C. Parte el pimiento por la mitad a lo largo, retira las semillas y colócalo en una fuente.",
                "Hornea las mitades vacías durante 10 minutos para que empiecen a ablandarse.",
                "Mientras tanto, mezcla el pavo cocinado y desmenuzado con el bulgur cocido y el tomate triturado.",
                "Rellena las mitades, reparte el queso por encima y hornea otros 15–20 minutos, hasta que el pimiento esté tierno.",
            ),
        ),
        recipe(
            id = "turkey-zucchini-lasagna",
            name = "Lasaña de calabacín y pavo",
            category = RecipeCategory.DINNER,
            prepMinutes = 60,
            complexity = RecipeComplexity.ADVANCED,
            ingredients = listOf(
                ingredient("Calabacín", 180.0, 17.0, 1.2, 3.1, 0.3),
                ingredient("Pavo magro cocinado", 100.0, 135.0, 29.0, 0.0, 1.6),
                ingredient("Tomate triturado", 100.0, 29.0, 1.4, 5.8, 0.2),
                ingredient("Queso ricotta", 40.0, 174.0, 11.3, 3.0, 13.0),
                ingredient("Espinacas", 30.0, 23.0, 2.9, 3.6, 0.4),
                ingredient("Queso parmesano", 8.0, 431.0, 38.0, 4.1, 29.0),
                ingredient("Aceite de oliva", 3.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Calienta el horno a 200 °C. Lava el calabacín y córtalo a lo largo en láminas finas y uniformes.",
                "Dora las láminas por tandas en una sartén antiadherente. Déjalas sobre papel de cocina para retirar el exceso de humedad.",
                "Calienta el aceite en una sartén, añade el tomate triturado y cocina a fuego medio hasta que espese ligeramente.",
                "Incorpora las espinacas y el pavo cocinado desmenuzado; cocina unos minutos y retira del fuego.",
                "En una fuente pequeña, alterna capas de calabacín, mezcla de pavo y pequeñas porciones de ricotta. Repite y termina con parmesano.",
                "Hornea durante 15–20 minutos hasta que la superficie se gratine. Deja reposar 5 minutos antes de cortar.",
            ),
        ),
        recipe(
            id = "chickpea-tomato-salad",
            name = "Ensalada rápida de garbanzos y tomate",
            category = RecipeCategory.MEAL,
            prepMinutes = 15,
            complexity = RecipeComplexity.EASY,
            ingredients = listOf(
                ingredient("Garbanzos cocidos", 100.0, 164.0, 8.9, 27.4, 2.6),
                ingredient("Tomate", 80.0, 18.0, 0.9, 3.9, 0.2),
                ingredient("Pepino", 50.0, 16.0, 0.7, 3.6, 0.1),
                ingredient("Yogur griego natural", 20.0, 59.0, 10.0, 3.6, 0.4),
                ingredient("Aceite de oliva", 2.0, 884.0, 0.0, 0.0, 100.0),
            ),
            instructions = listOf(
                "Mezcla los garbanzos con el tomate y el pepino troceados.",
                "Aliña con yogur, aceite, limón y hierbas.",
            ),
        ),
    )

    private fun recipe(
        id: String,
        name: String,
        category: RecipeCategory,
        prepMinutes: Int,
        complexity: RecipeComplexity,
        ingredients: List<RecipeIngredient>,
        instructions: List<String>,
    ) = RecipeRecommendation(
        id = id,
        name = name,
        category = category,
        prepMinutes = prepMinutes,
        complexity = complexity,
        ingredients = ingredients,
        instructions = instructions,
    )

    private fun ingredient(
        name: String,
        grams: Double,
        calories: Double,
        protein: Double,
        carbohydrates: Double,
        fat: Double,
) = RecipeIngredient(
        name = name,
        edibleGrams = grams,
        nutritionPer100Grams = Nutrients(calories, protein, carbohydrates, fat),
    )
}
