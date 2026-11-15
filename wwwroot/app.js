"use strict";

const STORAGE_KEY = "nutria.v1";
const APP_THEMES = new Set(["sage", "ocean", "lavender", "coral"]);
const RECIPE_COMPLEXITY_ORDER = { easy: 0, medium: 1, advanced: 2 };
const RECIPE_COMPLEXITY_LABELS = { easy: "Fácil", medium: "Media", advanced: "Avanzada" };
const RECIPE_COMPLEXITY_FILTERS = new Set(["all", ...Object.keys(RECIPE_COMPLEXITY_ORDER)]);
const RECIPE_PREP_TIMES = [15, 30, 45, 60];
const RECIPE_CATEGORIES = new Set(["all", "meal", "dinner"]);
const DEFAULTS = {
  goals: { calories: 2000, water: 2000, protein: 120, carbs: 250, fat: 65 },
  days: {},
  recipes: [],
  periods: [],
  cycleSettings: { cycleLength: 28, periodLength: 5 },
  cycleDays: {},
  recommendationSettings: { maxPrepMinutes: 30, complexity: "all", category: "all" },
  theme: "sage",
  calendar: { thirdMealEnabled: false, viewMonth: "", months: {} }
};

const $ = (selector) => document.querySelector(selector);
const today = new Date();
const dayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
const numberFormat = new Intl.NumberFormat("es-ES", { maximumFractionDigits: 0 });
const monthFormat = new Intl.DateTimeFormat("es-ES", { month: "long", year: "numeric" });
const TAB_IDS = new Set(["inicio", "comida", "recetas", "foto", "ciclo", "agua"]);
const ingredientSearchResults = new WeakMap();
const INGREDIENT_NUTRIENTS = [
  { key: "calories", input: "ingredientCalories", label: "Kcal / 100 g" },
  { key: "protein", input: "ingredientProtein", label: "Proteínas / 100 g" },
  { key: "carbs", input: "ingredientCarbs", label: "Carbohidratos / 100 g" },
  { key: "fat", input: "ingredientFat", label: "Grasas / 100 g" }
];
const RECOMMENDED_RECIPES = [
  {
    id: "chicken-quinoa-bowl",
    name: "Bowl de pollo, quinoa y verduras",
    category: "meal",
    prepMinutes: 25,
    complexity: "easy",
    ingredients: [
      { name: "Pechuga de pollo cocinada", grams: 100, nutrition: { calories: 165, protein: 31, carbs: 0, fat: 3.6 } },
      { name: "Quinoa cocida", grams: 100, nutrition: { calories: 120, protein: 4.4, carbs: 21.3, fat: 1.9 } },
      { name: "Calabacín y pimiento", grams: 150, nutrition: { calories: 35, protein: 1.3, carbs: 6.8, fat: 0.3 } },
      { name: "Aceite de oliva", grams: 4, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: ["Saltea el calabacín y el pimiento con el aceite.", "Añade el pollo y la quinoa cocidos, mezcla y sirve."]
  },
  {
    id: "salmon-sweet-potato",
    name: "Salmón con boniato y espinacas",
    category: "dinner",
    prepMinutes: 35,
    complexity: "medium",
    ingredients: [
      { name: "Salmón cocinado", grams: 90, nutrition: { calories: 206, protein: 22, carbs: 0, fat: 13 } },
      { name: "Boniato cocido", grams: 150, nutrition: { calories: 90, protein: 2, carbs: 20.7, fat: 0.2 } },
      { name: "Espinacas", grams: 50, nutrition: { calories: 23, protein: 2.9, carbs: 3.6, fat: 0.4 } },
      { name: "Yogur griego natural", grams: 30, nutrition: { calories: 59, protein: 10, carbs: 3.6, fat: 0.4 } },
      { name: "Aceite de oliva", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: [
      "Calienta el horno a 200 °C o prepara una sartén antiadherente. Corta el boniato ya cocido en rodajas.",
      "Cocina el salmón con la mitad del aceite, unos minutos por cada lado, hasta que quede hecho por dentro.",
      "En otra sartén, saltea las espinacas con el aceite restante hasta que se ablanden.",
      "Sirve el salmón con el boniato y las espinacas. Mezcla el yogur con unas gotas de limón y úsalo como salsa."
    ]
  },
  {
    id: "mediterranean-lentils",
    name: "Ensalada mediterránea de lentejas",
    category: "meal",
    prepMinutes: 15,
    complexity: "easy",
    ingredients: [
      { name: "Lentejas cocidas", grams: 150, nutrition: { calories: 116, protein: 9, carbs: 20.1, fat: 0.4 } },
      { name: "Tomate", grams: 80, nutrition: { calories: 18, protein: 0.9, carbs: 3.9, fat: 0.2 } },
      { name: "Pepino", grams: 70, nutrition: { calories: 16, protein: 0.7, carbs: 3.6, fat: 0.1 } },
      { name: "Queso feta", grams: 25, nutrition: { calories: 264, protein: 14, carbs: 4.1, fat: 21 } },
      { name: "Aceite de oliva", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: ["Mezcla las lentejas con el tomate y el pepino troceados.", "Añade el feta desmenuzado y aliña con aceite, limón y hierbas."]
  },
  {
    id: "vegetable-omelette",
    name: "Tortilla de verduras con pan integral",
    category: "dinner",
    prepMinutes: 20,
    complexity: "easy",
    ingredients: [
      { name: "Huevo", grams: 100, nutrition: { calories: 143, protein: 12.6, carbs: 0.7, fat: 9.5 } },
      { name: "Champiñones", grams: 80, nutrition: { calories: 22, protein: 3.1, carbs: 3.3, fat: 0.3 } },
      { name: "Espinacas", grams: 50, nutrition: { calories: 23, protein: 2.9, carbs: 3.6, fat: 0.4 } },
      { name: "Pan integral", grams: 50, nutrition: { calories: 247, protein: 13, carbs: 41, fat: 4.2 } },
      { name: "Aceite de oliva", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: ["Saltea los champiñones y las espinacas con el aceite.", "Añade el huevo batido, cuaja la tortilla y sirve con pan integral."]
  },
  {
    id: "chickpea-curry",
    name: "Curry suave de garbanzos y arroz",
    category: "dinner",
    prepMinutes: 30,
    complexity: "medium",
    ingredients: [
      { name: "Garbanzos cocidos", grams: 110, nutrition: { calories: 164, protein: 8.9, carbs: 27.4, fat: 2.6 } },
      { name: "Coliflor", grams: 120, nutrition: { calories: 25, protein: 1.9, carbs: 5, fat: 0.3 } },
      { name: "Tomate triturado", grams: 100, nutrition: { calories: 29, protein: 1.4, carbs: 5.8, fat: 0.2 } },
      { name: "Leche de coco ligera", grams: 50, nutrition: { calories: 95, protein: 1.1, carbs: 2.1, fat: 9.5 } },
      { name: "Arroz integral cocido", grams: 80, nutrition: { calories: 123, protein: 2.7, carbs: 25.6, fat: 1 } },
      { name: "Mezcla de curry", grams: 2, nutrition: { calories: 325, protein: 9, carbs: 58, fat: 8 } }
    ],
    instructions: [
      "Calienta el arroz integral cocido siguiendo las indicaciones del envase y resérvalo tapado.",
      "Separa la coliflor en ramilletes pequeños y cuécela en una sartén con un chorrito de agua hasta que esté tierna.",
      "Añade el tomate triturado y el curry; remueve y deja que la salsa reduzca unos minutos.",
      "Incorpora los garbanzos y la leche de coco ligera. Cocina a fuego suave y sirve el curry sobre el arroz."
    ]
  },
  {
    id: "yogurt-oat-berries",
    name: "Bol de yogur, avena y frutos rojos",
    category: "meal",
    prepMinutes: 10,
    complexity: "easy",
    ingredients: [
      { name: "Yogur griego natural", grams: 200, nutrition: { calories: 59, protein: 10, carbs: 3.6, fat: 0.4 } },
      { name: "Copos de avena", grams: 25, nutrition: { calories: 389, protein: 16.9, carbs: 66.3, fat: 6.9 } },
      { name: "Arándanos", grams: 80, nutrition: { calories: 57, protein: 0.7, carbs: 14.5, fat: 0.3 } },
      { name: "Semillas de chía", grams: 8, nutrition: { calories: 486, protein: 16.5, carbs: 42.1, fat: 30.7 } },
      { name: "Almendras", grams: 8, nutrition: { calories: 579, protein: 21.2, carbs: 21.6, fat: 49.9 } }
    ],
    instructions: ["Sirve el yogur en un bol y añade la avena y los arándanos.", "Termina con la chía y las almendras troceadas."]
  },
  {
    id: "wholegrain-tuna-pasta",
    name: "Pasta integral con atún y tomate",
    category: "meal",
    prepMinutes: 20,
    complexity: "easy",
    ingredients: [
      { name: "Pasta integral cocida", grams: 120, nutrition: { calories: 149, protein: 5.8, carbs: 30.9, fat: 0.9 } },
      { name: "Atún al natural escurrido", grams: 70, nutrition: { calories: 116, protein: 25.5, carbs: 0, fat: 0.8 } },
      { name: "Tomate triturado", grams: 100, nutrition: { calories: 29, protein: 1.4, carbs: 5.8, fat: 0.2 } },
      { name: "Espinacas", grams: 40, nutrition: { calories: 23, protein: 2.9, carbs: 3.6, fat: 0.4 } },
      { name: "Aceite de oliva", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: [
      "Cuece la pasta integral hasta que quede al dente y reserva un poco del agua de cocción.",
      "Mientras tanto, calienta el tomate triturado con el aceite en una sartén.",
      "Añade las espinacas y remueve hasta que se ablanden; incorpora el atún escurrido.",
      "Mezcla la pasta con la salsa. Ajusta la textura con un poco del agua reservada y sirve."
    ]
  },
  {
    id: "tofu-brown-rice-stir-fry",
    name: "Salteado de tofu, brócoli y arroz",
    category: "dinner",
    prepMinutes: 30,
    complexity: "medium",
    ingredients: [
      { name: "Tofu firme", grams: 100, nutrition: { calories: 144, protein: 17.3, carbs: 2.8, fat: 8.7 } },
      { name: "Brócoli", grams: 100, nutrition: { calories: 34, protein: 2.8, carbs: 6.6, fat: 0.4 } },
      { name: "Zanahoria", grams: 50, nutrition: { calories: 41, protein: 0.9, carbs: 9.6, fat: 0.2 } },
      { name: "Arroz integral cocido", grams: 100, nutrition: { calories: 123, protein: 2.7, carbs: 25.6, fat: 1 } },
      { name: "Salsa de soja", grams: 10, nutrition: { calories: 53, protein: 8, carbs: 4.9, fat: 0.6 } },
      { name: "Aceite de sésamo", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: [
      "Calienta el arroz integral cocido y resérvalo tapado. Escurre el tofu y córtalo en dados.",
      "Dora el tofu en una sartén amplia con el aceite de sésamo y retíralo cuando esté dorado.",
      "Corta el brócoli en ramilletes pequeños y la zanahoria en láminas finas; saltéalos hasta que estén tiernos pero firmes.",
      "Devuelve el tofu a la sartén, añade la salsa de soja y mezcla. Sirve el salteado sobre el arroz."
    ]
  },
  {
    id: "stuffed-pepper-turkey-bulgur",
    name: "Pimiento relleno de pavo y bulgur",
    category: "dinner",
    prepMinutes: 40,
    complexity: "medium",
    ingredients: [
      { name: "Pimiento", grams: 150, nutrition: { calories: 31, protein: 1, carbs: 6, fat: 0.3 } },
      { name: "Pavo magro cocinado", grams: 100, nutrition: { calories: 135, protein: 29, carbs: 0, fat: 1.6 } },
      { name: "Bulgur cocido", grams: 100, nutrition: { calories: 83, protein: 3.1, carbs: 18.6, fat: 0.2 } },
      { name: "Tomate triturado", grams: 80, nutrition: { calories: 29, protein: 1.4, carbs: 5.8, fat: 0.2 } },
      { name: "Queso rallado", grams: 15, nutrition: { calories: 350, protein: 25, carbs: 2, fat: 27 } },
      { name: "Aceite de oliva", grams: 4, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: [
      "Calienta el horno a 200 °C. Parte el pimiento por la mitad a lo largo, retira las semillas y colócalo en una fuente.",
      "Hornea las mitades vacías durante 10 minutos para que empiecen a ablandarse.",
      "Mientras tanto, mezcla el pavo cocinado y desmenuzado con el bulgur cocido y el tomate triturado.",
      "Rellena las mitades, reparte el queso por encima y hornea otros 15–20 minutos, hasta que el pimiento esté tierno."
    ]
  },
  {
    id: "turkey-zucchini-lasagna",
    name: "Lasaña de calabacín y pavo",
    category: "dinner",
    prepMinutes: 60,
    complexity: "advanced",
    ingredients: [
      { name: "Calabacín", grams: 180, nutrition: { calories: 17, protein: 1.2, carbs: 3.1, fat: 0.3 } },
      { name: "Pavo magro cocinado", grams: 100, nutrition: { calories: 135, protein: 29, carbs: 0, fat: 1.6 } },
      { name: "Tomate triturado", grams: 100, nutrition: { calories: 29, protein: 1.4, carbs: 5.8, fat: 0.2 } },
      { name: "Queso ricotta", grams: 40, nutrition: { calories: 174, protein: 11.3, carbs: 3, fat: 13 } },
      { name: "Espinacas", grams: 30, nutrition: { calories: 23, protein: 2.9, carbs: 3.6, fat: 0.4 } },
      { name: "Queso parmesano", grams: 8, nutrition: { calories: 431, protein: 38, carbs: 4.1, fat: 29 } },
      { name: "Aceite de oliva", grams: 3, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: [
      "Calienta el horno a 200 °C. Lava el calabacín y córtalo a lo largo en láminas finas y uniformes.",
      "Dora las láminas por tandas en una sartén antiadherente. Déjalas sobre papel de cocina para retirar el exceso de humedad.",
      "Calienta el aceite en una sartén, añade el tomate triturado y cocina a fuego medio hasta que espese ligeramente.",
      "Incorpora las espinacas y el pavo cocinado desmenuzado; cocina unos minutos y retira del fuego.",
      "En una fuente pequeña, alterna capas de calabacín, mezcla de pavo y pequeñas porciones de ricotta. Repite y termina con parmesano.",
      "Hornea durante 15–20 minutos hasta que la superficie se gratine. Deja reposar 5 minutos antes de cortar y servir."
    ]
  },
  {
    id: "chickpea-tomato-salad",
    name: "Ensalada rápida de garbanzos y tomate",
    category: "meal",
    prepMinutes: 15,
    complexity: "easy",
    ingredients: [
      { name: "Garbanzos cocidos", grams: 100, nutrition: { calories: 164, protein: 8.9, carbs: 27.4, fat: 2.6 } },
      { name: "Tomate", grams: 80, nutrition: { calories: 18, protein: 0.9, carbs: 3.9, fat: 0.2 } },
      { name: "Pepino", grams: 50, nutrition: { calories: 16, protein: 0.7, carbs: 3.6, fat: 0.1 } },
      { name: "Yogur griego natural", grams: 20, nutrition: { calories: 59, protein: 10, carbs: 3.6, fat: 0.4 } },
      { name: "Aceite de oliva", grams: 2, nutrition: { calories: 884, protein: 0, carbs: 0, fat: 100 } }
    ],
    instructions: ["Mezcla los garbanzos con el tomate y el pepino troceados.", "Aliña con yogur, aceite, limón y hierbas."]
  }
];
const MEAL_SLOTS = [
  { id: "meal", label: "Comida" },
  { id: "dinner", label: "Cena" },
  { id: "breakfast", label: "Desayuno" }
];
let state = loadState();
applyTheme(state.theme);
let calendarViewMonth = state.calendar.viewMonth || dayKey.slice(0, 7);
let toastTimeout;
let displayedRecommendationIds = [];

function loadState() {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (!stored || typeof stored !== "object") return structuredClone(DEFAULTS);
    return {
      goals: { ...DEFAULTS.goals, ...(stored.goals || {}) },
      days: stored.days && typeof stored.days === "object" ? stored.days : {},
      recipes: Array.isArray(stored.recipes) ? stored.recipes : [],
      periods: Array.isArray(stored.periods) ? stored.periods : [],
      cycleSettings: {
        cycleLength: Number.isInteger(stored.cycleSettings?.cycleLength)
          && stored.cycleSettings.cycleLength >= 21 && stored.cycleSettings.cycleLength <= 45
          ? stored.cycleSettings.cycleLength
          : DEFAULTS.cycleSettings.cycleLength,
        periodLength: Number.isInteger(stored.cycleSettings?.periodLength)
          && stored.cycleSettings.periodLength >= 2 && stored.cycleSettings.periodLength <= 10
          ? stored.cycleSettings.periodLength
          : DEFAULTS.cycleSettings.periodLength
      },
      cycleDays: stored.cycleDays && typeof stored.cycleDays === "object" ? stored.cycleDays : {},
      recommendationSettings: {
        maxPrepMinutes: RECIPE_PREP_TIMES.includes(stored.recommendationSettings?.maxPrepMinutes)
          ? stored.recommendationSettings.maxPrepMinutes
          : DEFAULTS.recommendationSettings.maxPrepMinutes,
        complexity: RECIPE_COMPLEXITY_FILTERS.has(stored.recommendationSettings?.complexity)
          ? stored.recommendationSettings.complexity
          : Object.hasOwn(RECIPE_COMPLEXITY_ORDER, stored.recommendationSettings?.maxComplexity)
            ? stored.recommendationSettings.maxComplexity
            : DEFAULTS.recommendationSettings.complexity,
        category: RECIPE_CATEGORIES.has(stored.recommendationSettings?.category)
          ? stored.recommendationSettings.category
          : DEFAULTS.recommendationSettings.category
      },
      theme: APP_THEMES.has(stored.theme) ? stored.theme : DEFAULTS.theme,
      calendar: {
        thirdMealEnabled: stored.calendar?.thirdMealEnabled === true,
        viewMonth: typeof stored.calendar?.viewMonth === "string" ? stored.calendar.viewMonth : "",
        months: stored.calendar?.months && typeof stored.calendar.months === "object" ? stored.calendar.months : {}
      }
    };
  } catch (error) {
    console.error("No se pudo leer el registro guardado.", error);
    return structuredClone(DEFAULTS);
  }
}

function saveState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (error) {
    console.error("No se pudo guardar el registro.", error);
    showToast("No se pudieron guardar los cambios en este dispositivo.");
  }
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
}

function getDay() {
  if (!state.days[dayKey]) state.days[dayKey] = { water: 0, meals: [] };
  if (!Number.isFinite(state.days[dayKey].water)) state.days[dayKey].water = 0;
  if (!Array.isArray(state.days[dayKey].waterEntries)) {
    state.days[dayKey].waterEntries = state.days[dayKey].water > 0 ? [state.days[dayKey].water] : [];
  }
  if (!Array.isArray(state.days[dayKey].meals)) state.days[dayKey].meals = [];
  return state.days[dayKey];
}

function mealTotals() {
  return getDay().meals.reduce((total, meal) => {
    total.calories += meal.calories;
    total.protein += meal.protein;
    total.carbs += meal.carbs;
    total.fat += meal.fat;
    return total;
  }, { calories: 0, protein: 0, carbs: 0, fat: 0 });
}

function setProgress(selector, value, goal) {
  const progress = $(selector);
  progress.style.width = `${Math.min(100, Math.max(0, goal > 0 ? (value / goal) * 100 : 0))}%`;
}

function render() {
  const day = getDay();
  const totals = mealTotals();
  $("#calories-consumed").textContent = numberFormat.format(Math.round(totals.calories));
  $("#calories-remaining").textContent = numberFormat.format(Math.max(0, Math.round(state.goals.calories - totals.calories)));
  $("#calorie-goal-label").textContent = numberFormat.format(state.goals.calories);
  $("#water-consumed").textContent = numberFormat.format(day.water);
  $("#undo-water").disabled = day.water <= 0;
  $("#water-goal-label").textContent = numberFormat.format(state.goals.water);
  $("#protein-values").textContent = `${numberFormat.format(Math.round(totals.protein))} / ${numberFormat.format(state.goals.protein)} g`;
  $("#carbs-values").textContent = `${numberFormat.format(Math.round(totals.carbs))} / ${numberFormat.format(state.goals.carbs)} g`;
  $("#fat-values").textContent = `${numberFormat.format(Math.round(totals.fat))} / ${numberFormat.format(state.goals.fat)} g`;
  setProgress("#calorie-progress", totals.calories, state.goals.calories);
  setProgress("#water-progress", day.water, state.goals.water);
  setProgress("#protein-progress", totals.protein, state.goals.protein);
  setProgress("#carbs-progress", totals.carbs, state.goals.carbs);
  setProgress("#fat-progress", totals.fat, state.goals.fat);
  renderWaterGlasses(day.water);
  renderMeals(day.meals, dayKey);
  renderMealHistory();
  renderRecipes();
  renderCycle();
  renderCalendar();
  $("#meal-count-label").textContent = day.meals.length === 0
    ? "Tu día empieza aquí"
    : `${day.meals.length} ${day.meals.length === 1 ? "comida registrada" : "comidas registradas"}`;
  $("#streak-count").textContent = calculateStreak();
}

function calculateStreak() {
  const date = new Date(today);
  let streak = 0;
  while (true) {
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    const day = state.days[key];
    if (!day || (!day.water && !(day.meals || []).length)) return streak;
    streak += 1;
    date.setDate(date.getDate() - 1);
  }
}

function renderWaterGlasses(amount) {
  const filled = Math.min(8, Math.floor(amount / 250));
  const glasses = Array.from({ length: 8 }, (_, index) => index < filled ? "●" : "○").join(" ");
  $("#water-glasses").textContent = glasses;
}

function renderMeals(meals, dateKey) {
  const container = $("#meal-list");
  if (meals.length === 0) {
    container.innerHTML = '<div class="empty-state"><span class="empty-icon">✳</span><strong>Aún no hay comidas</strong><span>Empieza añadiendo algo que hayas disfrutado.</span><a href="#comida" data-tab="comida" class="inline-link">Registrar una comida <span>→</span></a></div>';
    return;
  }
  container.replaceChildren(...meals.map((meal) => createMealEntry(meal, dateKey)));
}

function createMealEntry(meal, dateKey) {
  const row = document.createElement("div");
  row.className = "meal-entry";
  const symbol = document.createElement("span");
  symbol.className = "meal-symbol";
  symbol.textContent = meal.source === "photo" ? "▧" : meal.source === "recipe" ? "▤" : "✳";
  const info = document.createElement("div");
  info.className = "meal-entry-info";
  const name = document.createElement("strong");
  name.textContent = meal.name;
  const macros = document.createElement("span");
  macros.textContent = `P ${numberFormat.format(Math.round(meal.protein))} g · C ${numberFormat.format(Math.round(meal.carbs))} g · G ${numberFormat.format(Math.round(meal.fat))} g`;
  info.append(name, macros);
  const calories = document.createElement("span");
  calories.className = "meal-entry-kcal";
  calories.textContent = `${numberFormat.format(Math.round(meal.calories))} kcal`;
  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "remove-entry";
  remove.setAttribute("aria-label", `Quitar ${meal.name}`);
  remove.textContent = "×";
  remove.addEventListener("click", () => {
    const day = state.days[dateKey];
    if (!day || !Array.isArray(day.meals)) return;
    day.meals = day.meals.filter((entry) => entry.id !== meal.id);
    saveState();
    render();
    showToast("Comida eliminada del registro.");
  });
  row.append(symbol, info, calories, remove);
  return row;
}

function renderMealHistory() {
  const container = $("#meal-history");
  const historyDays = [];
  const date = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  for (let offset = 0; offset < 30; offset += 1) {
    const dateKey = dateKeyFor(date.getFullYear(), date.getMonth(), date.getDate());
    const meals = state.days[dateKey]?.meals;
    if (Array.isArray(meals) && meals.length) historyDays.push({ dateKey, meals });
    date.setDate(date.getDate() - 1);
  }

  if (!historyDays.length) {
    container.innerHTML = '<p class="history-empty">Todavía no hay comidas registradas en los últimos 30 días.</p>';
    return;
  }

  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1);
  const yesterdayKey = dateKeyFor(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate());
  const dateFormat = new Intl.DateTimeFormat("es-ES", {
    weekday: "long",
    day: "numeric",
    month: "long"
  });
  container.replaceChildren(...historyDays.map(({ dateKey, meals }) => {
    const [year, month, day] = dateKey.split("-").map(Number);
    const entryDate = new Date(year, month - 1, day);
    const dateLabel = dateKey === dayKey
      ? "Hoy"
      : dateKey === yesterdayKey
        ? "Ayer"
        : dateFormat.format(entryDate).replace(/^./, (character) => character.toLocaleUpperCase("es-ES"));
    const group = document.createElement("section");
    group.className = "history-day";
    const heading = document.createElement("h3");
    heading.textContent = dateLabel;
    const entries = document.createElement("div");
    entries.className = "history-meals";
    entries.replaceChildren(...meals.map((meal) => createMealEntry(meal, dateKey)));
    group.append(heading, entries);
    return group;
  }));
}

function isValidDateKey(dateKey) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateKey);
  return Boolean(match)
    && dateKeyFor(Number(match[1]), Number(match[2]) - 1, Number(match[3])) === dateKey;
}

function formatPeriodDate(dateKey) {
  const [year, month, day] = dateKey.split("-").map(Number);
  return new Intl.DateTimeFormat("es-ES", {
    day: "numeric",
    month: "long",
    year: "numeric"
  }).format(new Date(year, month - 1, day));
}

function addDaysToDateKey(dateKey, days) {
  const [year, month, day] = dateKey.split("-").map(Number);
  return dateKeyFor(year, month - 1, day + days);
}

function daysBetweenDateKeys(from, to) {
  const [fromYear, fromMonth, fromDay] = from.split("-").map(Number);
  const [toYear, toMonth, toDay] = to.split("-").map(Number);
  return Math.round((Date.UTC(toYear, toMonth - 1, toDay)
    - Date.UTC(fromYear, fromMonth - 1, fromDay)) / 86400000);
}

function getCycleStatus() {
  const latestPeriod = state.periods
    .filter((period) => period && isValidDateKey(period.start) && period.start <= dayKey)
    .sort((left, right) => right.start.localeCompare(left.start))[0];
  if (!latestPeriod) return null;

  const hasEnd = isValidDateKey(latestPeriod.end) && latestPeriod.end >= latestPeriod.start;
  const cycleDay = daysBetweenDateKeys(latestPeriod.start, dayKey) + 1;
  const isOngoing = (!hasEnd && cycleDay <= state.cycleSettings.periodLength)
    || (hasEnd && latestPeriod.end >= dayKey);
  const cycleLength = state.cycleSettings.cycleLength;
  const expectedStart = addDaysToDateKey(latestPeriod.start, cycleLength);
  const daysLate = Math.max(0, daysBetweenDateKeys(expectedStart, dayKey));
  const ovulationDay = cycleLength - 14;
  const ovulationStart = Math.max(state.cycleSettings.periodLength + 1, ovulationDay - 1);
  const ovulationEnd = Math.max(ovulationStart, ovulationDay + 1);
  let phase;
  let title;
  let description;

  if (isOngoing || cycleDay <= state.cycleSettings.periodLength) {
    phase = "menstrual";
    title = "Estás con la regla";
    description = isOngoing
      ? "Según tu registro, la menstruación sigue en curso."
      : "Por el día del ciclo, esta podría ser la fase menstrual.";
  } else if (cycleDay > cycleLength) {
    phase = "late";
    title = "Posible retraso";
    description = "La fecha estimada ya pasó y aún no has registrado un nuevo inicio.";
  } else if (cycleDay >= ovulationStart && cycleDay <= ovulationEnd) {
    phase = "ovulatory";
    title = "Ventana ovulatoria estimada";
    description = "Es una aproximación de calendario; no confirma la ovulación.";
  } else if (cycleDay < ovulationStart) {
    phase = "follicular";
    title = "Fase folicular estimada";
    description = "Etapa aproximada entre la menstruación y la ventana ovulatoria.";
  } else {
    phase = "luteal";
    title = "Fase lútea estimada";
    description = "Etapa aproximada antes de la fecha estimada del siguiente periodo.";
  }

  return {
    phase,
    title,
    description,
    cycleDay,
    cycleLength,
    expectedStart,
    daysLate,
    isOngoing
  };
}

function renderCycle() {
  const status = getCycleStatus();
  const ring = $("#cycle-ring");
  const progress = status ? Math.min(100, status.cycleDay / status.cycleLength * 100) : 0;
  ring.dataset.phase = status?.phase || "unknown";
  ring.style.setProperty("--cycle-progress", `${progress}%`);
  ring.setAttribute("aria-label", status
    ? `${status.title}, día ${status.cycleDay} de un ciclo estimado de ${status.cycleLength} días`
    : "Aún no hay fechas para estimar la fase del ciclo");
  $("#cycle-day-number").textContent = status ? status.cycleDay : "—";
  $("#cycle-day-caption").textContent = status ? `día del ciclo · ${status.cycleLength} días estimados` : "registra el inicio";
  $("#cycle-phase-title").textContent = status?.title || "Añade una fecha de inicio";
  $("#cycle-phase-description").textContent = status?.description
    || "Con una fecha registrada mostraremos una fase aproximada y la próxima fecha estimada.";
  const estimate = $("#cycle-next-estimate");
  if (!status) {
    estimate.textContent = "Aún no hay una fecha estimada.";
  } else if (status.isOngoing) {
    estimate.textContent = `Siguiente inicio estimado: ${formatPeriodDate(status.expectedStart)}.`;
  } else if (status.daysLate > 0) {
    estimate.textContent = `Posible retraso: ${status.daysLate} ${status.daysLate === 1 ? "día" : "días"} desde ${formatPeriodDate(status.expectedStart)}.`;
  } else if (status.expectedStart === dayKey) {
    estimate.textContent = "La siguiente menstruación se estima para hoy; aún no consta como iniciada.";
  } else {
    const daysUntil = daysBetweenDateKeys(dayKey, status.expectedStart);
    estimate.textContent = `Siguiente inicio estimado: ${formatPeriodDate(status.expectedStart)} (en ${daysUntil} ${daysUntil === 1 ? "día" : "días"}).`;
  }

  const container = $("#period-list");
  const periods = state.periods
    .filter((period) => period && isValidDateKey(period.start))
    .sort((left, right) => right.start.localeCompare(left.start));
  if (!periods.length) {
    container.innerHTML = '<p class="period-empty">Todavía no hay periodos registrados.</p>';
  } else {
    container.replaceChildren(...periods.map((period) => {
      const hasEnd = isValidDateKey(period.end) && period.end >= period.start;
      const entry = document.createElement("article");
      entry.className = "period-entry";
      const info = document.createElement("div");
      info.className = "period-entry-info";
      const title = document.createElement("h3");
      const periodDay = daysBetweenDateKeys(period.start, dayKey) + 1;
      title.textContent = hasEnd
        ? "Menstruación registrada"
        : periodDay <= state.cycleSettings.periodLength ? "Menstruación en curso" : "Fin no confirmada";
      const dates = document.createElement("p");
      if (hasEnd) {
        const duration = daysBetweenDateKeys(period.start, period.end) + 1;
        dates.textContent = `Del ${formatPeriodDate(period.start)} al ${formatPeriodDate(period.end)} · ${duration} ${duration === 1 ? "día" : "días"}`;
      } else {
        dates.textContent = `Desde el ${formatPeriodDate(period.start)}`;
      }
      info.append(title, dates);
      const actions = document.createElement("div");
      actions.className = "period-entry-actions";
      if (!hasEnd) {
        const finish = document.createElement("button");
        finish.type = "button";
        finish.className = "secondary-button";
        finish.textContent = "Finalizar hoy";
        finish.addEventListener("click", () => {
          if (period.start > dayKey) return;
          period.end = dayKey;
          saveState();
          renderCycle();
          showToast("Se ha guardado la fecha de fin.");
        });
        actions.append(finish);
      }
      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "period-delete";
      remove.setAttribute("aria-label", `Eliminar registro del ${formatPeriodDate(period.start)}`);
      remove.textContent = "×";
      remove.addEventListener("click", () => {
        if (!window.confirm("¿Quieres eliminar este registro menstrual?")) return;
        state.periods = state.periods.filter((entry) => entry.id !== period.id);
        saveState();
        renderCycle();
        showToast("Registro eliminado.");
      });
      actions.append(remove);
      entry.append(info, actions);
      return entry;
    }));
  }

  const settingsForm = $("#cycle-settings-form");
  settingsForm.elements.cycleLength.value = state.cycleSettings.cycleLength;
  settingsForm.elements.periodLength.value = state.cycleSettings.periodLength;
  const dayLog = state.cycleDays[dayKey] || {};
  const logForm = $("#cycle-day-form");
  logForm.elements.flow.value = dayLog.flow || "none";
  logForm.elements.pain.value = Number.isInteger(dayLog.pain) ? dayLog.pain : 0;
  logForm.elements.mood.value = dayLog.mood || "stable";
  logForm.elements.notes.value = dayLog.notes || "";
  $("#cycle-pain-value").textContent = logForm.elements.pain.value;

  const logList = $("#cycle-day-list");
  const loggedDays = Object.keys(state.cycleDays)
    .filter((dateKey) => isValidDateKey(dateKey) && dateKey <= dayKey && state.cycleDays[dateKey])
    .sort((left, right) => right.localeCompare(left))
    .slice(0, 7);
  if (!loggedDays.length) {
    logList.innerHTML = '<p class="period-empty">Aún no hay registros diarios.</p>';
  } else {
    logList.replaceChildren(...loggedDays.map((dateKey) => {
      const log = state.cycleDays[dateKey];
      const entry = document.createElement("article");
      entry.className = "cycle-day-entry";
      const date = document.createElement("strong");
      date.textContent = dateKey === dayKey ? "Hoy" : formatPeriodDate(dateKey);
      const summary = document.createElement("span");
      const flowLabels = { none: "Sin flujo", spotting: "Manchado", light: "Flujo leve", medium: "Flujo medio", heavy: "Flujo abundante" };
      const moodLabels = { good: "Buen ánimo", stable: "Ánimo estable", tired: "Cansancio", sensitive: "Sensibilidad", irritable: "Irritabilidad" };
      summary.textContent = `${flowLabels[log.flow] || "Sin flujo"} · Dolor ${Number.isInteger(log.pain) ? log.pain : 0}/10 · ${moodLabels[log.mood] || "Ánimo estable"}`;
      entry.append(date, summary);
      if (typeof log.notes === "string" && log.notes.trim()) {
        const note = document.createElement("p");
        note.textContent = log.notes;
        entry.append(note);
      }
      return entry;
    }));
  }
}

function renderRecipes() {
  renderRecommendedRecipes();
  const list = $("#recipe-list");
  $("#recipe-count").textContent = state.recipes.length;
  if (state.recipes.length === 0) {
    list.innerHTML = '<p class="recipe-empty">Tus recetas guardadas aparecerán aquí.</p>';
    return;
  }
  const categories = [
    { id: "meal", label: "Comidas" },
    { id: "dinner", label: "Cenas" }
  ];
  list.replaceChildren(...categories.map(({ id, label }) => {
    const recipes = state.recipes.filter((recipe) =>
      (recipe.category === "dinner" ? "dinner" : "meal") === id);
    const group = document.createElement("section");
    group.className = "recipe-category-group";
    const heading = document.createElement("h3");
    heading.textContent = `${label} (${recipes.length})`;
    group.append(heading);
    if (recipes.length) recipes.forEach((recipe) => group.append(createRecipeEntry(recipe)));
    else {
      const empty = document.createElement("p");
      empty.className = "recipe-category-empty";
      empty.textContent = `Aún no hay recetas para ${label.toLocaleLowerCase("es-ES")}.`;
      group.append(empty);
    }
    return group;
  }));
}

function createRecipeEntry(recipe) {
  const row = document.createElement("div");
  row.className = "recipe-entry";
  const header = document.createElement("div");
  header.className = "recipe-entry-head";
  const name = document.createElement("span");
  name.className = "recipe-entry-name";
  name.textContent = recipe.name;
  const calories = document.createElement("span");
  calories.className = "recipe-entry-kcal";
  calories.textContent = `${numberFormat.format(recipe.calories / recipe.servings)} kcal / ración`;
  const macros = document.createElement("span");
  macros.className = "recipe-entry-macros";
  macros.textContent = `Por ración: P ${numberFormat.format(recipe.protein / recipe.servings)} g · C ${numberFormat.format(recipe.carbs / recipe.servings)} g · G ${numberFormat.format(recipe.fat / recipe.servings)} g`;
  const preparation = document.createElement("span");
  preparation.className = "recipe-entry-preparation";
  preparation.hidden = !Number.isFinite(recipe.prepMinutes) || !RECIPE_COMPLEXITY_LABELS[recipe.complexity];
  if (!preparation.hidden) {
    preparation.textContent = `${recipe.prepMinutes} min · Dificultad ${RECIPE_COMPLEXITY_LABELS[recipe.complexity]}`;
  }
  const add = document.createElement("button");
  add.type = "button";
  add.className = "recipe-add";
  add.setAttribute("aria-label", `Añadir una ración de ${recipe.name}`);
  add.textContent = "+";
  add.addEventListener("click", () => addMeal({
    name: recipe.name,
    calories: recipe.calories / recipe.servings,
    protein: recipe.protein / recipe.servings,
    carbs: recipe.carbs / recipe.servings,
    fat: recipe.fat / recipe.servings,
    source: "recipe"
  }, "Una ración de tu receta se añadió al día."));
  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "recipe-delete";
  remove.setAttribute("aria-label", `Eliminar receta ${recipe.name}`);
  remove.textContent = "×";
  remove.addEventListener("click", () => {
    state.recipes = state.recipes.filter((entry) => entry.id !== recipe.id);
    removeRecipeFromCalendar(recipe.id);
    saveState();
    renderRecipes();
    renderCalendar();
    showToast("Receta eliminada.");
  });
  header.append(name, calories, add, remove);
  const categoryLabel = document.createElement("label");
  categoryLabel.className = "recipe-category";
  categoryLabel.append(document.createTextNode("Para generación aleatoria"));
  const category = document.createElement("select");
  category.setAttribute("aria-label", `Categoría aleatoria de ${recipe.name}`);
  category.dataset.recipeCategory = recipe.id;
  category.append(new Option("Comida", "meal"), new Option("Cena", "dinner"));
  category.value = recipe.category === "dinner" ? "dinner" : "meal";
  category.addEventListener("change", () => {
    recipe.category = category.value;
    saveState();
    renderRecipes();
    renderCalendar();
    showToast(`La receta se usará al generar ${category.value === "dinner" ? "cenas" : "comidas"}.`);
  });
  categoryLabel.append(category);
  const ingredients = Array.isArray(recipe.ingredients) ? recipe.ingredients : [];
  const details = document.createElement("details");
  details.className = "recipe-ingredients";
  const summary = document.createElement("summary");
  summary.textContent = recipe.instructions?.length
    ? `Ingredientes y preparación (${ingredients.length})`
    : `Ingredientes (${ingredients.length})`;
  const ingredientList = document.createElement("ul");
  if (ingredients.length) {
    ingredients.forEach((ingredient) => {
      const item = document.createElement("li");
      item.textContent = `${ingredient.name}: ${ingredient.amount} ${ingredient.unit}`;
      ingredientList.append(item);
    });
  } else {
    const item = document.createElement("li");
    item.textContent = "Esta receta aún no tiene ingredientes guardados.";
    ingredientList.append(item);
  }
  details.append(summary, ingredientList);
  if (Array.isArray(recipe.instructions) && recipe.instructions.length) {
    const instructions = document.createElement("ol");
    instructions.className = "recipe-instructions";
    recipe.instructions.forEach((step) => {
      const item = document.createElement("li");
      item.textContent = step;
      instructions.append(item);
    });
    details.append(instructions);
  }
  row.append(header, macros, preparation, categoryLabel, details);
  return row;
}

function calculateRecommendationNutrition(recipe) {
  return recipe.ingredients.reduce((total, ingredient) => {
    const factor = ingredient.grams / 100;
    for (const nutrient of ["calories", "protein", "carbs", "fat"]) {
      total[nutrient] += ingredient.nutrition[nutrient] * factor;
    }
    return total;
  }, { calories: 0, protein: 0, carbs: 0, fat: 0 });
}

function getEligibleRecommendedRecipes() {
  const { maxPrepMinutes, complexity, category } = state.recommendationSettings;
  return RECOMMENDED_RECIPES.filter((recipe) => {
    const saved = state.recipes.some((savedRecipe) =>
      savedRecipe.recommendationId === recipe.id
      || savedRecipe.name.toLocaleLowerCase("es-ES") === recipe.name.toLocaleLowerCase("es-ES"));
    return !saved
      && recipe.prepMinutes <= maxPrepMinutes
      && (complexity === "all" || recipe.complexity === complexity)
      && (category === "all" || recipe.category === category)
      && Math.round(calculateRecommendationNutrition(recipe).calories) <= 550;
  });
}

function shuffled(items) {
  const result = [...items];
  for (let index = result.length - 1; index > 0; index--) {
    const otherIndex = Math.floor(Math.random() * (index + 1));
    [result[index], result[otherIndex]] = [result[otherIndex], result[index]];
  }
  return result;
}

function renderRecommendedRecipes() {
  const list = $("#recommended-recipe-list");
  const empty = $("#recommendation-empty");
  const refresh = $("#refresh-recommendations");
  const eligible = getEligibleRecommendedRecipes();
  const eligibleIds = new Set(eligible.map((recipe) => recipe.id));
  let recommendations = displayedRecommendationIds
    .filter((id) => eligibleIds.has(id))
    .map((id) => eligible.find((recipe) => recipe.id === id));
  const recommendationLimit = Math.min(3, eligible.length);
  if (recommendations.length < recommendationLimit) {
    const selectedIds = new Set(recommendations.map((recipe) => recipe.id));
    const remaining = shuffled(eligible.filter((recipe) => !selectedIds.has(recipe.id)));
    recommendations = [...recommendations, ...remaining.slice(0, recommendationLimit - recommendations.length)];
  }
  displayedRecommendationIds = recommendations.map((recipe) => recipe.id);
  empty.hidden = eligible.length > 0;
  refresh.disabled = eligible.length <= recommendations.length;
  list.replaceChildren(...recommendations.map((recommendation) => {
    const nutrition = calculateRecommendationNutrition(recommendation);
    const card = document.createElement("article");
    card.className = "recommended-recipe";
    const heading = document.createElement("div");
    heading.className = "recommendation-title";
    const name = document.createElement("h3");
    name.textContent = recommendation.name;
    const category = document.createElement("span");
    category.className = "recommendation-category";
    category.textContent = recommendation.category === "dinner" ? "Cena" : "Comida";
    heading.append(name, category);
    const calories = document.createElement("p");
    calories.className = "recommendation-calories";
    calories.textContent = `${numberFormat.format(Math.round(nutrition.calories))} kcal por ración · máximo 550 kcal`;
    const preparation = document.createElement("p");
    preparation.className = "recommendation-preparation";
    preparation.textContent = `${recommendation.prepMinutes} min · Dificultad ${RECIPE_COMPLEXITY_LABELS[recommendation.complexity]}`;
    const macros = document.createElement("p");
    macros.className = "recommendation-macros";
    macros.textContent = `P ${numberFormat.format(Math.round(nutrition.protein))} g · C ${numberFormat.format(Math.round(nutrition.carbs))} g · G ${numberFormat.format(Math.round(nutrition.fat))} g`;
    const details = document.createElement("details");
    details.className = "recommendation-details";
    const summary = document.createElement("summary");
    summary.textContent = "Ingredientes y preparación";
    const ingredients = document.createElement("ul");
    recommendation.ingredients.forEach((ingredient) => {
      const item = document.createElement("li");
      item.textContent = `${ingredient.name}: ${ingredient.grams} g`;
      ingredients.append(item);
    });
    const instructions = document.createElement("ol");
    recommendation.instructions.forEach((step) => {
      const item = document.createElement("li");
      item.textContent = step;
      instructions.append(item);
    });
    details.append(summary, ingredients, instructions);
    const save = document.createElement("button");
    save.type = "button";
    save.className = "primary-button recommendation-save";
    save.textContent = "Guardar en mis recetas";
    save.addEventListener("click", () => saveRecommendedRecipe(recommendation));
    card.append(heading, calories, preparation, macros, details, save);
    return card;
  }));
}

function refreshRecommendedRecipes() {
  const eligible = getEligibleRecommendedRecipes();
  if (eligible.length <= displayedRecommendationIds.length) return;
  const displayedIds = new Set(displayedRecommendationIds);
  const newOptions = shuffled(eligible.filter((recipe) => !displayedIds.has(recipe.id)));
  const selected = newOptions.slice(0, 3);
  if (selected.length < Math.min(3, eligible.length)) {
    const selectedIds = new Set(selected.map((recipe) => recipe.id));
    const remaining = shuffled(eligible.filter((recipe) => !selectedIds.has(recipe.id)));
    selected.push(...remaining.slice(0, Math.min(3, eligible.length) - selected.length));
  }
  displayedRecommendationIds = selected.map((recipe) => recipe.id);
  renderRecommendedRecipes();
}

function updateRecommendationSettings() {
  const maxPrepMinutes = Number($("#recommendation-time-filter").value);
  const complexity = $("#recommendation-complexity-filter").value;
  const category = $("#recommendation-category-filter").value;
  if (!RECIPE_PREP_TIMES.includes(maxPrepMinutes)
    || !RECIPE_COMPLEXITY_FILTERS.has(complexity)
    || !RECIPE_CATEGORIES.has(category)) {
    showToast("Selecciona un tipo, tiempo y complejidad válidos.");
    return;
  }
  state.recommendationSettings = { maxPrepMinutes, complexity, category };
  displayedRecommendationIds = [];
  saveState();
  renderRecommendedRecipes();
}

function saveRecommendedRecipe(recommendation) {
  if (state.recipes.some((recipe) => recipe.recommendationId === recommendation.id)) return;
  const nutrition = calculateRecommendationNutrition(recommendation);
  if (Math.round(nutrition.calories) > 550) {
    showToast("Esta recomendación supera el máximo de 550 kcal y no se puede guardar.");
    return;
  }
  state.recipes.unshift({
    id: crypto.randomUUID(),
    recommendationId: recommendation.id,
    name: recommendation.name,
    category: recommendation.category,
    servings: 1,
    prepMinutes: recommendation.prepMinutes,
    complexity: recommendation.complexity,
    ...nutrition,
    ingredients: recommendation.ingredients.map((ingredient) => ({
      name: ingredient.name,
      amount: String(ingredient.grams),
      unit: "g",
      grams: ingredient.grams,
      nutrition: { ...ingredient.nutrition }
    })),
    instructions: [...recommendation.instructions]
  });
  saveState();
  renderRecipes();
  renderCalendar();
  showToast("Receta guardada en tu libro.");
}

function dateKeyFor(year, monthIndex, day) {
  return `${year}-${String(monthIndex + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function getVisibleMealSlots() {
  return state.calendar.thirdMealEnabled
    ? MEAL_SLOTS
    : MEAL_SLOTS.filter((slot) => slot.id !== "breakfast");
}

function createCalendarMealSelect(slot, dateKey, selectedRecipeId = "") {
  const select = document.createElement("select");
  select.dataset.calendarSlot = slot.id;
  select.setAttribute("aria-label", `Cambiar receta de ${slot.label.toLocaleLowerCase("es-ES")}, ${dateKey}`);
  const placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = "Elegir receta";
  select.append(placeholder);

  for (const recipe of state.recipes) {
    const option = document.createElement("option");
    option.value = recipe.id;
    option.textContent = recipe.name;
    select.append(option);
  }
  select.value = selectedRecipeId;
  return select;
}

function renderCalendar() {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(calendarViewMonth)) {
    calendarViewMonth = dayKey.slice(0, 7);
    state.calendar.viewMonth = calendarViewMonth;
  }
  const [viewYear, viewMonth] = calendarViewMonth.split("-").map(Number);
  const monthDate = new Date(viewYear, viewMonth - 1, 1);
  $("#calendar-month-label").textContent = monthFormat.format(monthDate)
    .replace(/^./, (character) => character.toLocaleUpperCase("es-ES"));
  $("#third-meal-toggle").checked = state.calendar.thirdMealEnabled;
  $("#calendar-breakfast-legend").hidden = !state.calendar.thirdMealEnabled;

  const hasRecipes = state.recipes.length > 0;
  $("#generate-menu").disabled = !hasRecipes;
  $("#calendar-empty").hidden = hasRecipes;
  $("#calendar-slot-legend").hidden = !hasRecipes;
  $("#calendar-grid").hidden = !hasRecipes;
  $("#calendar-weekdays").hidden = !hasRecipes;
  $("#calendar-hint").textContent = hasRecipes
    ? "Selecciona una receta para cambiarla; en móvil, toca un día para editar sus comidas."
    : "Añade recetas a tu colección para empezar a planificar el mes.";
  if (!hasRecipes) {
    $("#calendar-grid").replaceChildren();
    return;
  }

  const monthPlans = state.calendar.months[calendarViewMonth] || {};
  const daysInMonth = new Date(viewYear, viewMonth, 0).getDate();
  const leadingDays = (monthDate.getDay() + 6) % 7;
  const cells = [];

  for (let index = 0; index < leadingDays; index += 1) {
    const empty = document.createElement("div");
    empty.className = "calendar-day calendar-placeholder";
    empty.setAttribute("aria-hidden", "true");
    cells.push(empty);
  }

  for (let dayNumber = 1; dayNumber <= daysInMonth; dayNumber += 1) {
    const dateKey = dateKeyFor(viewYear, viewMonth - 1, dayNumber);
    const day = new Date(viewYear, viewMonth - 1, dayNumber);
    const dayCell = document.createElement("article");
    dayCell.className = "calendar-day";
    if (dateKey === dayKey) dayCell.classList.add("calendar-day-today");
    if (day.getDay() === 0 || day.getDay() === 6) dayCell.classList.add("calendar-day-weekend");
    dayCell.dataset.calendarDate = dateKey;

    const heading = document.createElement("div");
    heading.className = "calendar-day-heading";
    const dayLabel = document.createElement("span");
    dayLabel.className = "calendar-day-number";
    dayLabel.textContent = dayNumber;
    heading.append(dayLabel);
    if (dateKey === dayKey) {
      const todayLabel = document.createElement("span");
      todayLabel.className = "calendar-today-label";
      todayLabel.textContent = "Hoy";
      heading.append(todayLabel);
    }

    const slots = document.createElement("div");
    slots.className = "calendar-day-slots";
    const summary = document.createElement("div");
    summary.className = "calendar-day-summary";
    summary.setAttribute("aria-hidden", "true");
    const assignments = monthPlans[dateKey] || {};
    for (const slot of getVisibleMealSlots()) {
      const label = document.createElement("label");
      label.className = "calendar-meal-slot";
      const slotLabel = document.createElement("span");
      slotLabel.textContent = slot.label;
      const select = createCalendarMealSelect(slot, dateKey, assignments[slot.id] || "");
      label.append(slotLabel, select);
      slots.append(label);

      if (assignments[slot.id]) {
        const recipe = state.recipes.find((item) => item.id === assignments[slot.id]);
        const dot = document.createElement("span");
        dot.className = `calendar-meal-dot calendar-meal-${slot.id}`;
        dot.title = `${slot.label}: ${recipe ? recipe.name : "receta"}`;
        summary.append(dot);
      }
    }
    const editButton = document.createElement("button");
    editButton.className = "calendar-day-edit";
    editButton.type = "button";
    editButton.dataset.calendarEdit = dateKey;
    editButton.setAttribute("aria-label", `Editar comidas del ${new Intl.DateTimeFormat("es-ES", {
      weekday: "long", day: "numeric", month: "long"
    }).format(day)}`);
    editButton.textContent = "✎";
    dayCell.append(heading, summary, slots, editButton);
    cells.push(dayCell);
  }

  $("#calendar-grid").replaceChildren(...cells);
}

function openCalendarDayDialog(dateKey) {
  const [year, month, day] = dateKey.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  const title = new Intl.DateTimeFormat("es-ES", {
    weekday: "long", day: "numeric", month: "long"
  }).format(date);
  $("#calendar-day-dialog-title").textContent = title.replace(/^./, (character) => character.toLocaleUpperCase("es-ES"));
  const assignments = state.calendar.months[dateKey.slice(0, 7)]?.[dateKey] || {};
  const fields = getVisibleMealSlots().map((slot) => {
    const label = document.createElement("label");
    label.className = "field";
    label.append(document.createTextNode(slot.label));
    label.append(createCalendarMealSelect(slot, dateKey, assignments[slot.id] || ""));
    return label;
  });
  $("#calendar-day-form").dataset.date = dateKey;
  $("#calendar-day-edit-slots").replaceChildren(...fields);
  $("#calendar-day-dialog").showModal();
}

function shuffle(items) {
  const shuffled = [...items];
  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1));
    [shuffled[index], shuffled[randomIndex]] = [shuffled[randomIndex], shuffled[index]];
  }
  return shuffled;
}

function generateMenu(period, selectedWeekDate = "") {
  if (!state.recipes.length) return false;
  const targetDates = [];
  let firstDate;
  let periodDescription;

  if (period === "week") {
    const [year, month, day] = selectedWeekDate.split("-").map(Number);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(selectedWeekDate)
      || !year || !month || !day
      || dateKeyFor(year, month - 1, day) !== selectedWeekDate) return false;
    firstDate = new Date(year, month - 1, day);
    firstDate.setDate(firstDate.getDate() - ((firstDate.getDay() + 6) % 7));
    for (let offset = 0; offset < 7; offset += 1) {
      const date = new Date(firstDate);
      date.setDate(firstDate.getDate() + offset);
      targetDates.push(dateKeyFor(date.getFullYear(), date.getMonth(), date.getDate()));
    }
    const lastDate = new Date(firstDate);
    lastDate.setDate(firstDate.getDate() + 6);
    const dayMonthFormat = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "long" });
    periodDescription = `la semana del ${dayMonthFormat.format(firstDate)} al ${dayMonthFormat.format(lastDate)}`;
  } else {
    const [year, month] = calendarViewMonth.split("-").map(Number);
    firstDate = new Date(year, month - 1, 1);
    const daysInMonth = new Date(year, month, 0).getDate();
    for (let day = 1; day <= daysInMonth; day += 1) {
      targetDates.push(dateKeyFor(year, month - 1, day));
    }
    periodDescription = `el mes de ${monthFormat.format(firstDate)}`;
  }

  const mealSlots = getVisibleMealSlots();
  const allRecipes = state.recipes.map((recipe) => recipe.id);
  const recipesBySlot = new Map(mealSlots.map((slot) => {
    const recipes = slot.id === "dinner"
      ? state.recipes.filter((recipe) => recipe.category === "dinner").map((recipe) => recipe.id)
      : slot.id === "meal"
        ? state.recipes.filter((recipe) => recipe.category !== "dinner").map((recipe) => recipe.id)
        : allRecipes;
    return [slot.id, recipes];
  }));
  const missingSlot = mealSlots.find((slot) => recipesBySlot.get(slot.id).length === 0);
  if (missingSlot) {
    const message = `Clasifica al menos una receta como ${missingSlot.label.toLocaleLowerCase("es-ES")} para generar el menú.`;
    $("#menu-generator-hint").textContent = message;
    $("#menu-generator-hint").classList.add("error");
    showToast(message);
    return false;
  }

  const hasAssignments = targetDates.some((dateKey) => {
    const assignments = state.calendar.months[dateKey.slice(0, 7)]?.[dateKey];
    return assignments && Object.values(assignments).some(Boolean);
  });
  if (hasAssignments && !window.confirm(`Se sustituirán las recetas de ${periodDescription}. ¿Quieres continuar?`)) {
    return false;
  }

  const nextRecipe = (slot, excluded) => {
    const candidates = recipesBySlot.get(slot.id);
    const available = candidates.filter((recipeId) => !excluded.has(recipeId));
    const pool = available.length ? available : candidates;
    return shuffle(pool)[0];
  };

  for (const dateKey of targetDates) {
    const monthPlan = state.calendar.months[dateKey.slice(0, 7)]
      || (state.calendar.months[dateKey.slice(0, 7)] = {});
    delete monthPlan[dateKey];
    const usedRecipes = new Set();
    const assignments = {};
    for (const slot of mealSlots) {
      const recipeId = nextRecipe(slot, usedRecipes);
      assignments[slot.id] = recipeId;
      usedRecipes.add(recipeId);
    }
    monthPlan[dateKey] = assignments;
  }

  calendarViewMonth = targetDates[0].slice(0, 7);
  state.calendar.viewMonth = calendarViewMonth;
  saveState();
  renderCalendar();
  showToast(`Menú para ${periodDescription} creado.`);
  return true;
}

function updateMenuGeneratorDialog() {
  const period = new FormData($("#menu-generator-form")).get("period");
  const isWeek = period === "week";
  const dateInput = $("#menu-week-date");
  $("#menu-week-field").hidden = !isWeek;
  dateInput.disabled = !isWeek;
  dateInput.required = isWeek;
  $("#menu-generator-hint").classList.remove("error");
  const [year, month] = calendarViewMonth.split("-").map(Number);
  $("#menu-month-description").textContent = `Calendario: ${monthFormat.format(new Date(year, month - 1, 1))}`;
  $("#menu-generator-hint").textContent = isWeek
    ? "La semana empieza el lunes de la fecha elegida."
    : "El mes generado será el que tienes abierto en el calendario.";
}

function removeRecipeFromCalendar(recipeId) {
  for (const month of Object.values(state.calendar.months)) {
    if (!month || typeof month !== "object") continue;
    for (const assignments of Object.values(month)) {
      if (!assignments || typeof assignments !== "object") continue;
      for (const [slot, assignedRecipe] of Object.entries(assignments)) {
        if (assignedRecipe === recipeId) delete assignments[slot];
      }
    }
  }
}

function createIngredientRow(ingredient = {}) {
  const row = document.createElement("div");
  row.className = "ingredient-row";

  const nameLabel = document.createElement("label");
  nameLabel.className = "field ingredient-name ingredient-name-field";
  nameLabel.append(document.createTextNode("Ingrediente"));
  const nameInput = document.createElement("input");
  nameInput.name = "ingredientName";
  nameInput.required = true;
  nameInput.maxLength = 60;
  nameInput.placeholder = "Ej. Tomate";
  nameInput.value = ingredient.name || "";
  nameLabel.append(nameInput);

  const amountLabel = createIngredientField("Cantidad", "ingredientAmount", ingredient.amount || "");
  amountLabel.className = "field ingredient-amount";
  amountLabel.querySelector("input").placeholder = "200";

  const unitLabel = document.createElement("label");
  unitLabel.className = "field ingredient-unit";
  unitLabel.append(document.createTextNode("Medida"));
  const unitSelect = document.createElement("select");
  unitSelect.name = "ingredientUnit";
  ["g", "ml", "unidad", "cda", "cdta", "pizca", "al gusto"].forEach((unit) => {
    const option = document.createElement("option");
    option.value = unit;
    option.textContent = unit === "unidad" ? "unidad(es)" : unit === "cda" ? "cda." : unit === "cdta" ? "cdta." : unit;
    option.selected = ingredient.unit === unit;
    unitSelect.append(option);
  });
  unitLabel.append(unitSelect);

  const amountInput = amountLabel.querySelector("input");
  const remove = document.createElement("button");
  remove.className = "ingredient-remove";
  remove.type = "button";
  remove.disabled = $("#ingredient-list")?.querySelectorAll(".ingredient-row").length === 0;
  remove.setAttribute("aria-label", "Quitar ingrediente");
  remove.textContent = "×";

  const mainFields = document.createElement("div");
  mainFields.className = "ingredient-main";
  mainFields.append(nameLabel, amountLabel, unitLabel, remove);

  const search = document.createElement("div");
  search.className = "ingredient-lookup";
  const searchButton = document.createElement("button");
  searchButton.className = "ingredient-search";
  searchButton.type = "button";
  searchButton.textContent = "Buscar alimento";
  const results = document.createElement("div");
  results.className = "ingredient-search-results";
  results.setAttribute("aria-label", "Resultados de Open Food Facts");
  results.setAttribute("role", "listbox");
  results.hidden = true;
  const searchStatus = document.createElement("span");
  searchStatus.className = "ingredient-search-status";
  searchStatus.setAttribute("role", "status");
  searchStatus.setAttribute("aria-live", "polite");
  search.append(searchButton, results, searchStatus);

  const nutrition = document.createElement("div");
  nutrition.className = "ingredient-nutrient-grid";
  const gramsLabel = createIngredientField(
    "Peso comestible (g)",
    "ingredientGrams",
    ingredient.grams ?? (ingredient.unit === "g" ? ingredient.amount || "" : ""),
    "number"
  );
  const gramsInput = gramsLabel.querySelector("input");
  gramsInput.min = "0.1";
  gramsInput.step = "any";
  gramsLabel.classList.add("ingredient-grams");
  nutrition.append(gramsLabel);

  const values = ingredient.nutrition || {};
  for (const nutrient of INGREDIENT_NUTRIENTS) {
    const label = createIngredientField(nutrient.label, nutrient.input, values[nutrient.key] ?? "", "number");
    const input = label.querySelector("input");
    input.min = "0";
    input.step = "any";
    input.classList.add("ingredient-nutrient");
    input.dataset.nutrient = nutrient.key;
    nutrition.append(label);
  }

  row.append(mainFields, search, nutrition);
  syncIngredientWeight(row);
  return row;
}

function createIngredientField(labelText, inputName, inputValue = "", inputType = "text") {
  const label = document.createElement("label");
  label.className = "field";
  label.append(document.createTextNode(labelText));
  const input = document.createElement("input");
  input.name = inputName;
  input.type = inputType;
  input.required = true;
  input.value = inputValue;
  label.append(input);
  return label;
}

function syncIngredientWeight(row) {
  const amount = row.querySelector('[name="ingredientAmount"]');
  const unit = row.querySelector('[name="ingredientUnit"]');
  const grams = row.querySelector('[name="ingredientGrams"]');
  if (unit.value === "g") {
    const amountGrams = Number(String(amount.value).trim().replace(",", "."));
    grams.value = Number.isFinite(amountGrams) && amountGrams > 0 ? amountGrams : "";
    grams.readOnly = true;
  } else {
    grams.readOnly = false;
  }
}

async function searchIngredient(row) {
  const query = row.querySelector('[name="ingredientName"]').value.trim();
  const button = row.querySelector(".ingredient-search");
  const results = row.querySelector(".ingredient-search-results");
  const status = row.querySelector(".ingredient-search-status");
  if (!query) {
    status.textContent = "Escribe el nombre del ingrediente antes de buscar.";
    row.querySelector('[name="ingredientName"]').focus();
    return;
  }

  button.disabled = true;
  results.replaceChildren();
  results.hidden = true;
  status.textContent = "Buscando en Open Food Facts…";
  try {
    const response = await fetch(`/api/ingredients/search?query=${encodeURIComponent(query)}`);
  const data = await response.json();
    if (!response.ok) {
      throw new Error(data.title || `La búsqueda respondió con el estado ${response.status}.`);
    }
    const products = Array.isArray(data.products) ? data.products : [];
    ingredientSearchResults.set(row, products);
  results.replaceChildren();
    products.forEach((product, index) => {
      const label = product.brand ? `${product.name} · ${product.brand}` : product.name;
    const option = document.createElement("button");
    option.type = "button";
    option.className = "ingredient-result-option";
    option.dataset.resultIndex = String(index);
    option.setAttribute("role", "option");
    option.setAttribute("aria-selected", "false");
    const productName = document.createElement("strong");
    productName.textContent = label;
    const nutrition = document.createElement("span");
    nutrition.textContent = `${numberFormat.format(Math.round(product.calories))} kcal · P ${numberFormat.format(Math.round(product.protein))} g · C ${numberFormat.format(Math.round(product.carbs))} g · G ${numberFormat.format(Math.round(product.fat))} g por 100 g`;
    option.append(productName, nutrition);
    results.append(option);
    });
  results.hidden = products.length === 0;
    status.textContent = products.length
    ? `${products.length} resultados. Pulsa el alimento que coincida para importar sus nutrientes.`
      : "No hay resultados con valores nutricionales completos. Puedes introducirlos manualmente.";
  } catch (error) {
    console.error("No se pudieron buscar los nutrientes del ingrediente.", error);
    ingredientSearchResults.delete(row);
  results.replaceChildren();
  results.hidden = true;
    status.textContent = error instanceof TypeError
      ? "No se pudo conectar con Nutria. Comprueba que la aplicación esté ejecutándose y vuelve a intentarlo."
      : `No se pudo completar la búsqueda: ${error.message}`;
  } finally {
    button.disabled = false;
  }
}

function applyIngredientSearchResult(row, resultIndex) {
  const result = ingredientSearchResults.get(row)?.[resultIndex];
  if (!result) return;
  row.querySelectorAll(".ingredient-result-option").forEach((option) => {
    const selected = Number(option.dataset.resultIndex) === resultIndex;
    option.setAttribute("aria-selected", String(selected));
    option.classList.toggle("selected", selected);
  });
  for (const nutrient of INGREDIENT_NUTRIENTS) {
    row.querySelector(`[name="${nutrient.input}"]`).value = result[nutrient.key];
  }
  updateRecipeNutrition();
  row.querySelector(".ingredient-search-status").textContent =
    `Valores de «${result.name}» importados. Comprueba el producto y la porción.`;
}

function updateRecipeNutrition() {
  const rows = [...$("#ingredient-list").querySelectorAll(".ingredient-row")];
  const totals = { calories: 0, protein: 0, carbs: 0, fat: 0 };
  let complete = rows.length > 0;

  for (const row of rows) {
    const grams = Number(row.querySelector('[name="ingredientGrams"]').value);
    const nutrientValues = {};
    for (const nutrient of INGREDIENT_NUTRIENTS) {
      const input = row.querySelector(`[name="${nutrient.input}"]`);
      const amount = Number(input.value);
      if (input.value === "" || !Number.isFinite(amount) || amount < 0) complete = false;
      nutrientValues[nutrient.key] = amount;
    }
    if (!row.querySelector('[name="ingredientName"]').value.trim()
      || !row.querySelector('[name="ingredientAmount"]').value.trim()
      || !Number.isFinite(grams) || grams <= 0) {
      complete = false;
      continue;
    }
    for (const [key, amount] of Object.entries(nutrientValues)) {
      totals[key] += amount * grams / 100;
    }
  }

  for (const key of ["calories", "protein", "carbs", "fat"]) {
    $(`#recipe-${key}`).value = complete ? String(Number(totals[key].toFixed(2))) : "";
  }
  const servings = Number($("#recipe-form").elements.servings.value);
  const hasValidServings = Number.isInteger(servings) && servings > 0;
  const canSave = complete && hasValidServings;
  $("#recipe-submit").disabled = !canSave;
  $("#recipe-calculation-status").textContent = canSave
    ? `Por receta: ${numberFormat.format(Math.round(totals.calories))} kcal · P ${numberFormat.format(Math.round(totals.protein))} g · C ${numberFormat.format(Math.round(totals.carbs))} g · G ${numberFormat.format(Math.round(totals.fat))} g. Por ración: ${numberFormat.format(Math.round(totals.calories / servings))} kcal.`
    : "Completa los nutrientes por 100 g y el peso de todos los ingredientes para calcular la receta.";
  return canSave;
}

function updateIngredientRemoveButtons() {
  const rows = [...$("#ingredient-list").querySelectorAll(".ingredient-row")];
  rows.forEach((row) => {
    row.querySelector(".ingredient-remove").disabled = rows.length === 1;
  });
}

function resetIngredients() {
  $("#ingredient-list").replaceChildren(createIngredientRow());
  updateIngredientRemoveButtons();
  updateRecipeNutrition();
}

function addMeal(meal, message) {
  getDay().meals.unshift({ ...meal, id: crypto.randomUUID(), createdAt: new Date().toISOString() });
  saveState();
  render();
  showToast(message);
}

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("visible");
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toast.classList.remove("visible"), 2600);
}

function activateTab(tabId, updateHistory = true) {
  const resolvedTab = tabId === "registrar" ? "comida" : tabId;
  const activeTab = TAB_IDS.has(resolvedTab) ? resolvedTab : "inicio";
  document.querySelectorAll("[data-tab-panel]").forEach((panel) => {
    panel.hidden = panel.id !== activeTab;
  });
  document.querySelectorAll("[data-tab]").forEach((link) => {
    const active = link.dataset.tab === activeTab;
    link.classList.toggle("active", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  if (updateHistory && location.hash !== `#${activeTab}`) {
    history.pushState(null, "", `#${activeTab}`);
  }
  window.scrollTo(0, 0);
}

function addWater(amount) {
  const day = getDay();
  day.water += amount;
  day.waterEntries.push(amount);
  saveState();
  render();
  showToast(`${amount} ml de agua añadidos. ¡Bien hecho!`);
}

function undoLastWater() {
  const day = getDay();
  const amount = day.waterEntries.pop();
  if (!amount) return;
  day.water = Math.max(0, day.water - amount);
  saveState();
  render();
  showToast(`${amount} ml de la última toma se han eliminado.`);
}

function value(formData, name) {
  const result = Number(formData.get(name));
  return Number.isFinite(result) && result >= 0 ? result : 0;
}

function getFoodGrams(formData) {
  const amount = value(formData, "amount");
  switch (formData.get("unit")) {
    case "oz": return amount * 28.3495;
    case "portion": return amount * value(formData, "portionWeight");
    default: return amount;
  }
}

function calculateFood(formData) {
  const factor = getFoodGrams(formData) / 100;
  return {
    name: String(formData.get("name") || "").trim(),
    calories: value(formData, "calories") * factor,
    protein: value(formData, "protein") * factor,
    carbs: value(formData, "carbs") * factor,
    fat: value(formData, "fat") * factor
  };
}

function updateCalculation() {
  const form = $("#food-form");
  const data = new FormData(form);
  const name = String(data.get("name") || "").trim();
  if (!name || !form.elements.calories.value) {
    $("#calculation-result").innerHTML = "<span>Para tu cantidad</span><strong>Completa los valores para calcular</strong>";
    return;
  }
  const meal = calculateFood(data);
  $("#calculation-result").replaceChildren();
  const label = document.createElement("span");
  label.textContent = `Para ${numberFormat.format(getFoodGrams(data))} g`;
  const summary = document.createElement("strong");
  summary.textContent = `${numberFormat.format(Math.round(meal.calories))} kcal · P ${numberFormat.format(Math.round(meal.protein))} g · C ${numberFormat.format(Math.round(meal.carbs))} g · G ${numberFormat.format(Math.round(meal.fat))} g`;
  $("#calculation-result").append(label, summary);
}

function setPhotoStatus(message, isError = false) {
  const status = $("#photo-status");
  status.textContent = message;
  status.classList.toggle("error", isError);
}

function readPhoto(file) {
  if (!file) return;
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    setPhotoStatus("Elige una imagen JPG, PNG o WebP.", true);
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    setPhotoStatus("La imagen supera el límite de 10 MB.", true);
    return;
  }
  const reader = new FileReader();
  reader.addEventListener("load", () => {
    $("#photo-preview").src = reader.result;
    $("#photo-preview-wrap").hidden = false;
    $("#photo-drop").hidden = true;
    $("#analyze-photo").disabled = false;
    setPhotoStatus("");
  });
  reader.addEventListener("error", () => setPhotoStatus("No se pudo abrir la imagen. Prueba con otro archivo.", true));
  reader.readAsDataURL(file);
}

function attachEvents() {
  $("#today-label").textContent = new Intl.DateTimeFormat("es-ES", { weekday: "long", day: "numeric", month: "long", year: "numeric" })
    .format(today).replace(/^./, (character) => character.toLocaleUpperCase("es-ES"));
  $("#recommendation-time-filter").value = String(state.recommendationSettings.maxPrepMinutes);
  $("#recommendation-complexity-filter").value = state.recommendationSettings.complexity;
  $("#recommendation-category-filter").value = state.recommendationSettings.category;
  $("#recommendation-time-filter").addEventListener("change", updateRecommendationSettings);
  $("#recommendation-complexity-filter").addEventListener("change", updateRecommendationSettings);
  $("#recommendation-category-filter").addEventListener("change", updateRecommendationSettings);
  $("#refresh-recommendations").addEventListener("click", refreshRecommendedRecipes);
  $("#cycle-log-period-shortcut").addEventListener("click", () => {
    $("#period-start").value = dayKey;
    $("#period-end").value = "";
    $("#period-start").scrollIntoView({ behavior: "smooth", block: "center" });
    $("#period-start").focus();
  });
  $("#cycle-settings-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const cycleLength = Number(form.elements.cycleLength.value);
    const periodLength = Number(form.elements.periodLength.value);
    if (periodLength >= cycleLength) {
      showToast("La duración habitual de la regla debe ser menor que la del ciclo.");
      return;
    }
    state.cycleSettings = { cycleLength, periodLength };
    saveState();
    renderCycle();
    showToast("Se han actualizado tus promedios del ciclo.");
  });
  $("#cycle-day-form").elements.pain.addEventListener("input", (event) => {
    $("#cycle-pain-value").textContent = event.currentTarget.value;
  });
  $("#cycle-day-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    state.cycleDays[dayKey] = {
      flow: form.elements.flow.value,
      pain: Number(form.elements.pain.value),
      mood: form.elements.mood.value,
      notes: form.elements.notes.value.trim()
    };
    saveState();
    renderCycle();
    showToast("Se ha guardado tu registro de hoy.");
  });

  const periodForm = $("#period-form");
  const periodStart = $("#period-start");
  const periodEnd = $("#period-end");
  periodStart.value = dayKey;
  periodStart.max = dayKey;
  periodEnd.max = dayKey;
  periodForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const formData = new FormData(periodForm);
    const start = String(formData.get("start") || "");
    const end = String(formData.get("end") || "");
    if (!isValidDateKey(start) || start > dayKey) {
      showToast("La fecha de inicio debe ser válida y no puede estar en el futuro.");
      return;
    }
    if (end && (!isValidDateKey(end) || end < start || end > dayKey)) {
      showToast("La fecha de fin debe ser válida y posterior o igual al inicio.");
      return;
    }
    const newEnd = end || dayKey;
    const overlaps = state.periods.some((period) => {
      if (!period || !isValidDateKey(period.start)) return false;
      const oldEnd = isValidDateKey(period.end) && period.end >= period.start
        ? period.end
        : addDaysToDateKey(period.start, state.cycleSettings.periodLength - 1);
      return start <= oldEnd && newEnd >= period.start;
    });
    if (overlaps) {
      showToast("Las fechas coinciden con otro registro. Comprueba el historial.");
      return;
    }
    state.periods.push({ id: crypto.randomUUID(), start, end });
    saveState();
    periodForm.reset();
    periodStart.value = dayKey;
    renderCycle();
    showToast("Registro menstrual guardado.");
  });

  $("#food-form").addEventListener("input", updateCalculation);
  $("#food-form").elements.unit.addEventListener("change", (event) => {
    const isPortion = event.target.value === "portion";
    $("#portion-weight-field").hidden = !isPortion;
    $("#food-amount-hint").textContent = isPortion
      ? "Indica los valores nutricionales por cada 100 g y el peso de una ración."
      : event.target.value === "oz"
        ? "Las onzas se convierten a gramos (1 oz = 28,35 g). Indica los valores por cada 100 g."
        : "Indica los valores nutricionales por cada 100 g.";
    updateCalculation();
  });
  $("#food-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const meal = calculateFood(new FormData(event.currentTarget));
    addMeal({ ...meal, source: "food" }, `${meal.name} se añadió a tu día.`);
    event.currentTarget.reset();
    $("#portion-weight-field").hidden = true;
    updateCalculation();
  });

  $("#recipe-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!updateRecipeNutrition() || !form.reportValidity()) return;
    const data = new FormData(form);
    const ingredients = data.getAll("ingredientName").map((name, index) => ({
      name: String(name).trim(),
      amount: String(data.getAll("ingredientAmount")[index]).trim(),
      unit: String(data.getAll("ingredientUnit")[index]),
      grams: Number(data.getAll("ingredientGrams")[index]),
      nutrition: {
        calories: Number(data.getAll("ingredientCalories")[index]),
        protein: Number(data.getAll("ingredientProtein")[index]),
        carbs: Number(data.getAll("ingredientCarbs")[index]),
        fat: Number(data.getAll("ingredientFat")[index])
      }
    }));
    const recipe = {
      id: crypto.randomUUID(),
      name: String(data.get("name")).trim(),
      category: String(data.get("category")),
      servings: Math.max(1, value(data, "servings")),
      calories: value(data, "calories"),
      protein: value(data, "protein"),
      carbs: value(data, "carbs"),
      fat: value(data, "fat"),
      ingredients: ingredients.filter((ingredient) => ingredient.name && ingredient.amount)
    };
    state.recipes.unshift(recipe);
    saveState();
    renderRecipes();
    renderCalendar();
    event.currentTarget.reset();
    resetIngredients();
    updateRecipeNutrition();
    showToast("Receta guardada. Añádela al día cuando quieras.");
  });
  $("#add-ingredient").addEventListener("click", () => {
    $("#ingredient-list").append(createIngredientRow());
    updateIngredientRemoveButtons();
    updateRecipeNutrition();
    $("#ingredient-list .ingredient-row:last-child input").focus();
  });
  $("#ingredient-list").addEventListener("click", (event) => {
    const searchButton = event.target.closest(".ingredient-search");
    if (searchButton) {
      searchIngredient(searchButton.closest(".ingredient-row"));
      return;
    }
    const resultOption = event.target.closest(".ingredient-result-option");
    if (resultOption) {
      applyIngredientSearchResult(
        resultOption.closest(".ingredient-row"),
        Number(resultOption.dataset.resultIndex)
      );
      return;
    }
    const removeButton = event.target.closest(".ingredient-remove");
    if (!removeButton || removeButton.disabled) return;
    removeButton.closest(".ingredient-row").remove();
    updateIngredientRemoveButtons();
    updateRecipeNutrition();
  });
  $("#ingredient-list").addEventListener("change", (event) => {
    const row = event.target.closest(".ingredient-row");
    if (!row) return;
    if (event.target.name === "ingredientUnit") syncIngredientWeight(row);
    updateRecipeNutrition();
  });
  $("#recipe-form").addEventListener("input", (event) => {
    const row = event.target.closest(".ingredient-row");
    if (row && event.target.name === "ingredientAmount"
      && row.querySelector('[name="ingredientUnit"]').value === "g") {
      syncIngredientWeight(row);
    }
    updateRecipeNutrition();
  });

  document.querySelectorAll("[data-water-amount]").forEach((button) => {
    button.addEventListener("click", () => addWater(Number(button.dataset.waterAmount)));
  });
  $("#undo-water").addEventListener("click", undoLastWater);
  $("#calendar-previous").addEventListener("click", () => changeCalendarMonth(-1));
  $("#calendar-next").addEventListener("click", () => changeCalendarMonth(1));
  $("#third-meal-toggle").addEventListener("change", (event) => {
    state.calendar.thirdMealEnabled = event.currentTarget.checked;
    saveState();
    renderCalendar();
  });
  const menuDialog = $("#menu-generator-dialog");
  $("#generate-menu").addEventListener("click", () => {
    const form = $("#menu-generator-form");
    form.elements.period.value = "month";
    $("#menu-week-date").value = dayKey;
    updateMenuGeneratorDialog();
    menuDialog.showModal();
  });
  $("#menu-generator-form").addEventListener("change", updateMenuGeneratorDialog);
  $("#menu-generator-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const period = new FormData(form).get("period");
    if (generateMenu(period, $("#menu-week-date").value)) menuDialog.close();
  });
  $("#calendar-grid").addEventListener("click", (event) => {
    const dateCell = event.target.closest("[data-calendar-date]");
    if (dateCell && window.matchMedia("(max-width: 860px)").matches) {
      openCalendarDayDialog(dateCell.dataset.calendarDate);
    }
  });
  $("#calendar-grid").addEventListener("change", (event) => {
    const select = event.target.closest("[data-calendar-slot]");
    const dateCell = event.target.closest("[data-calendar-date]");
    if (!select || !dateCell) return;
    const month = calendarViewMonth;
    const days = state.calendar.months[month] || (state.calendar.months[month] = {});
    const assignments = days[dateCell.dataset.calendarDate] || (days[dateCell.dataset.calendarDate] = {});
    if (select.value) assignments[select.dataset.calendarSlot] = select.value;
    else delete assignments[select.dataset.calendarSlot];
    if (!Object.values(assignments).some(Boolean)) delete days[dateCell.dataset.calendarDate];
    saveState();
    renderCalendar();
  });
  $("#calendar-day-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const dateKey = form.dataset.date;
    const monthKey = dateKey.slice(0, 7);
    const monthPlan = state.calendar.months[monthKey] || (state.calendar.months[monthKey] = {});
    const assignments = {};
    form.querySelectorAll("[data-calendar-slot]").forEach((select) => {
      if (select.value) assignments[select.dataset.calendarSlot] = select.value;
    });
    if (Object.keys(assignments).length) monthPlan[dateKey] = assignments;
    else delete monthPlan[dateKey];
    saveState();
    renderCalendar();
    $("#calendar-day-dialog").close();
  });

  $("#meal-photo").addEventListener("change", (event) => readPhoto(event.currentTarget.files[0]));
  const dropZone = $("#photo-drop");
  dropZone.addEventListener("dragover", (event) => { event.preventDefault(); dropZone.classList.add("dragging"); });
  dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragging"));
  dropZone.addEventListener("drop", (event) => {
    event.preventDefault();
    dropZone.classList.remove("dragging");
    const file = event.dataTransfer.files[0];
    if (file) {
      $("#meal-photo").files = event.dataTransfer.files;
      readPhoto(file);
    }
  });
  $("#remove-photo").addEventListener("click", () => {
    $("#meal-photo").value = "";
    $("#photo-preview").removeAttribute("src");
    $("#photo-preview-wrap").hidden = true;
    dropZone.hidden = false;
    $("#analyze-photo").disabled = true;
    setPhotoStatus("");
  });
  $("#photo-form").addEventListener("submit", analyzePhoto);

  const goalsDialog = $("#goals-dialog");
  const goalsForm = $("#goals-form");
  $("#goals-button").addEventListener("click", () => {
    Object.entries(state.goals).forEach(([key, amount]) => { goalsForm.elements[key].value = amount; });
    goalsForm.elements.theme.value = state.theme;
    goalsDialog.showModal();
  });
  goalsForm.addEventListener("change", (event) => {
    if (event.target.name === "theme" && APP_THEMES.has(event.target.value)) {
      applyTheme(event.target.value);
    }
  });
  goalsDialog.addEventListener("close", () => {
    goalsForm.elements.theme.value = state.theme;
    applyTheme(state.theme);
  });
  document.querySelectorAll(".close-dialog").forEach((button) => button.addEventListener("click", () => {
    button.closest("dialog").close();
  }));
  goalsForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    for (const key of Object.keys(state.goals)) state.goals[key] = Math.max(1, value(data, key));
    const selectedTheme = data.get("theme");
    if (APP_THEMES.has(selectedTheme)) state.theme = selectedTheme;
    applyTheme(state.theme);
    saveState();
    render();
    goalsDialog.close();
    showToast("Tus ajustes se han actualizado.");
  });
  $("#clear-day").addEventListener("click", () => {
    if (!getDay().meals.length && !getDay().water) {
      showToast("No hay ningún registro de hoy para borrar.");
      return;
    }
    if (!window.confirm("¿Quieres borrar las comidas y el agua registrados hoy?")) return;
    delete state.days[dayKey];
    saveState();
    render();
    showToast("El registro de hoy se ha borrado.");
  });
  document.addEventListener("click", (event) => {
    const link = event.target.closest("[data-tab]");
    if (!link) return;
    event.preventDefault();
    activateTab(link.dataset.tab);
  });
  window.addEventListener("popstate", () => activateTab(location.hash.slice(1), false));
}

function changeCalendarMonth(offset) {
  const [year, month] = calendarViewMonth.split("-").map(Number);
  const date = new Date(year, month - 1 + offset, 1);
  calendarViewMonth = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
  state.calendar.viewMonth = calendarViewMonth;
  saveState();
  renderCalendar();
}

async function analyzePhoto(event) {
  event.preventDefault();
  const file = $("#meal-photo").files[0];
  if (!file) return;
  const button = $("#analyze-photo");
  button.disabled = true;
  button.firstChild.textContent = "Analizando… ";
  setPhotoStatus("Estamos preparando el análisis. Puede tardar unos segundos.");
  try {
    const data = new FormData();
    data.append("photo", file);
    data.append("name", $("#photo-meal-name").value.trim());
    const response = await fetch("/api/analyze-meal", { method: "POST", body: data });
    if (!response.ok) {
      const details = await response.json().catch(() => ({}));
      throw new Error(details.title || details.error || `El servicio de análisis respondió con el estado ${response.status}.`);
    }
    const result = await response.json();
    const fields = ["calories", "protein", "carbs", "fat"];
    if (!result || !fields.every((field) => Number.isFinite(result[field]) && result[field] >= 0)) {
      throw new Error("El servicio devolvió datos nutricionales incompletos. No se añadió la comida.");
    }
    const name = String($("#photo-meal-name").value.trim() || result.name || "Comida analizada").slice(0, 60);
    addMeal({ name, calories: result.calories, protein: result.protein, carbs: result.carbs, fat: result.fat, source: "photo" }, "La estimación se añadió a tu día. Recuerda que es orientativa.");
    setPhotoStatus(`Estimación orientativa: ${numberFormat.format(Math.round(result.calories))} kcal · P ${numberFormat.format(Math.round(result.protein))} g · C ${numberFormat.format(Math.round(result.carbs))} g · G ${numberFormat.format(Math.round(result.fat))} g. Comprueba los ingredientes y la porción.`);
  } catch (error) {
    console.error("No se pudo analizar la foto.", error);
    setPhotoStatus(error instanceof TypeError
      ? "No se pudo conectar con Nutria. Comprueba que el servidor esté ejecutándose y vuelve a intentarlo."
      : `No se pudo completar el análisis: ${error.message}`, true);
  } finally {
    button.disabled = false;
    button.firstChild.textContent = "Analizar con IA ";
  }
}

resetIngredients();
attachEvents();
activateTab(location.hash.slice(1), false);
render();
