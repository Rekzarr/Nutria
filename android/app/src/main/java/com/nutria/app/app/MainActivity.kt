package com.nutria.app.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nutria.app.R
import com.nutria.app.feature.activity.ActivityFeatureScreen
import com.nutria.app.feature.activity.HealthConnectStepsFeature
import com.nutria.app.core.data.AppTheme
import com.nutria.app.core.data.CalendarPreferencesRepository
import com.nutria.app.core.data.CalendarRepository
import com.nutria.app.core.data.CycleRepository
import com.nutria.app.core.data.GoalsRepository
import com.nutria.app.core.data.RecipeRepository
import com.nutria.app.core.data.SettingsPreferences
import com.nutria.app.core.data.SettingsPreferencesRepository
import com.nutria.app.feature.settings.SettingsScreen
import com.nutria.app.feature.diary.DiaryScreen
import com.nutria.app.feature.diary.DailyNutritionSummaryCard
import com.nutria.app.feature.diary.DiaryViewModel
import com.nutria.app.feature.diary.DiaryViewModelFactory
import com.nutria.app.feature.diary.WaterLogCard
import com.nutria.app.feature.cycle.CycleScreen
import com.nutria.app.feature.calendar.MealCalendarScreen
import com.nutria.app.feature.photo.PhotoAnalysisScreen
import com.nutria.app.feature.recipes.RecipesScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var diaryViewModel by mutableStateOf<DiaryViewModel?>(null)
    private var cycleRepository by mutableStateOf<CycleRepository?>(null)
    private var recipeRepository by mutableStateOf<RecipeRepository?>(null)
    private var calendarRepository by mutableStateOf<CalendarRepository?>(null)
    private var storageInitError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as NutriaApplication
            val goalsRepository = remember(app) { app.goalsRepository() }
            val preferencesRepository = remember(app) { app.settingsPreferencesRepository() }
            val calendarPreferencesRepository = remember(app) { app.calendarPreferencesRepository() }
            val recommendationPreferencesRepository =
                remember(app) { app.recommendationPreferencesRepository() }
            val apiClientFactory = remember(app) { app.apiClientFactory() }
            val preferences by preferencesRepository.observeSettings()
                .collectAsState(initial = SettingsPreferences())
            var previewTheme by remember { mutableStateOf<AppTheme?>(null) }
            NutriaMaterialTheme(theme = previewTheme ?: preferences.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        try {
                            val repository = (application as NutriaApplication).diaryRepository()
                            diaryViewModel = ViewModelProvider(
                                this@MainActivity,
                                DiaryViewModelFactory(repository),
                            )[DiaryViewModel::class.java]
                            cycleRepository = (application as NutriaApplication).cycleRepository()
                            recipeRepository = (application as NutriaApplication).recipeRepository()
                            calendarRepository = (application as NutriaApplication).calendarRepository()
                            storageInitError = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            storageInitError = failure.message
                                ?: "No se pudo abrir el almacenamiento local de Nutria."
                        }
                    }
                    NutriaNavigationShell(
                        contentResolver = contentResolver,
                        apiClientFactory = apiClientFactory,
                        diaryViewModel = diaryViewModel,
                        cycleRepository = cycleRepository,
                        recipeRepository = recipeRepository,
                        calendarRepository = calendarRepository,
                        calendarPreferencesRepository = calendarPreferencesRepository,
                        recommendationPreferencesRepository = recommendationPreferencesRepository,
                        storageInitError = storageInitError,
                        goalsRepository = goalsRepository,
                        preferencesRepository = preferencesRepository,
                        onPreviewTheme = { previewTheme = it },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NutriaNavigationShell(
    contentResolver: android.content.ContentResolver,
    apiClientFactory: com.nutria.app.data.remote.ConfiguredNutriaApiClientFactory,
    diaryViewModel: DiaryViewModel?,
    cycleRepository: CycleRepository?,
    recipeRepository: RecipeRepository?,
    calendarRepository: CalendarRepository?,
    calendarPreferencesRepository: CalendarPreferencesRepository,
    recommendationPreferencesRepository: com.nutria.app.core.data.RecommendationPreferencesRepository,
    storageInitError: String?,
    goalsRepository: GoalsRepository,
    preferencesRepository: SettingsPreferencesRepository,
    onPreviewTheme: (AppTheme) -> Unit,
) {
    var backStack by rememberSaveable { mutableStateOf(listOf(AppDestination.INICIO.route)) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentDestination = AppDestination.fromRoute(backStack.lastOrNull())
    val openNavigationDescription = stringResource(R.string.navigation_open)

    fun navigateTo(destination: AppDestination) {
        backStack = NavigationHistory.navigate(backStack, destination.route)
    }

    BackHandler(enabled = drawerState.isOpen || backStack.size > 1) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            backStack = NavigationHistory.back(backStack)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.drawer_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 28.dp, top = 28.dp, bottom = 16.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppDestination.primary.forEach { destination ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = currentDestination == destination,
                            icon = {
                                Text(
                                    text = stringResource(destination.iconRes),
                                    modifier = Modifier.clearAndSetSemantics { }
                                )
                            },
                            onClick = {
                                navigateTo(destination)
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.drawer_encouragement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.navigation_menu_icon),
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = openNavigationDescription
                                }
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = stringResource(currentDestination.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = SpanishFormats.formatDate(java.time.LocalDate.now()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { navigateTo(AppDestination.AJUSTES) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(text = stringResource(R.string.destination_settings))
                        }
                    }
                )
            }
        ) { contentPadding ->
            ShellDestinationContent(
                destination = currentDestination,
                diaryViewModel = diaryViewModel,
                contentResolver = contentResolver,
                apiClientFactory = apiClientFactory,
                cycleRepository = cycleRepository,
                recipeRepository = recipeRepository,
                calendarRepository = calendarRepository,
                calendarPreferencesRepository = calendarPreferencesRepository,
                recommendationPreferencesRepository = recommendationPreferencesRepository,
                storageInitError = storageInitError,
                goalsRepository = goalsRepository,
                preferencesRepository = preferencesRepository,
                onPreviewTheme = onPreviewTheme,
                onOpenActivity = { navigateTo(AppDestination.ACTIVIDAD) },
                onOpenRecipes = { navigateTo(AppDestination.RECETAS) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun ShellDestinationContent(
    contentResolver: android.content.ContentResolver,
    apiClientFactory: com.nutria.app.data.remote.ConfiguredNutriaApiClientFactory,
    destination: AppDestination,
    diaryViewModel: DiaryViewModel?,
    cycleRepository: CycleRepository?,
    recipeRepository: RecipeRepository?,
    calendarRepository: CalendarRepository?,
    calendarPreferencesRepository: CalendarPreferencesRepository,
    recommendationPreferencesRepository: com.nutria.app.core.data.RecommendationPreferencesRepository,
    storageInitError: String?,
    goalsRepository: GoalsRepository,
    preferencesRepository: SettingsPreferencesRepository,
    onPreviewTheme: (AppTheme) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenRecipes: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (destination == AppDestination.AJUSTES) {
        SettingsScreen(
            goalsRepository = goalsRepository,
            preferencesRepository = preferencesRepository,
            onPreviewTheme = onPreviewTheme,
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (destination) {
            AppDestination.INICIO -> {
                Text(
                    text = stringResource(R.string.shell_welcome_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.shell_welcome_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                diaryViewModel?.let {
                    DailyNutritionSummaryCard(
                        viewModel = it,
                        goalsRepository = goalsRepository,
                    )
                }
                diaryViewModel?.let { WaterLogCard(viewModel = it) }
                HealthConnectStepsFeature(
                    lifecycleOwner = LocalLifecycleOwner.current,
                    compact = true,
                    onOpenActivity = onOpenActivity,
                )
            }
            AppDestination.COMIDA -> {
                if (diaryViewModel != null) {
                    DiaryScreen(
                        viewModel = diaryViewModel,
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = storageInitError?.let {
                                "No se pudo abrir el diario en este dispositivo. Revisa el almacenamiento y vuelve a intentarlo."
                            } ?: "Preparando el almacenamiento privado de tu diario…",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            AppDestination.FOTO -> {
                if (diaryViewModel != null) {
                    PhotoAnalysisScreen(
                        contentResolver = contentResolver,
                        clientFactory = apiClientFactory,
                        diaryViewModel = diaryViewModel,
                    )
                } else {
                    Text(
                        text = storageInitError
                            ?: stringResource(R.string.shell_storage_unavailable),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            AppDestination.RECETAS -> {
                if (recipeRepository != null) {
                    RecipesScreen(
                        contentResolver = contentResolver,
                        repository = recipeRepository,
                        recommendationPreferencesRepository = recommendationPreferencesRepository,
                        diaryViewModel = diaryViewModel,
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = storageInitError
                                ?: "Preparando el almacenamiento privado de tus recetas…",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
            AppDestination.PLAN -> {
                if (calendarRepository != null && recipeRepository != null) {
                    MealCalendarScreen(
                        calendarRepository = calendarRepository,
                        recipeRepository = recipeRepository,
                        preferencesRepository = calendarPreferencesRepository,
                        onOpenRecipes = onOpenRecipes,
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = storageInitError
                                ?: "Preparando el calendario privado de tus comidas…",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
            AppDestination.CICLO -> {
                if (cycleRepository != null) {
                    CycleScreen(repository = cycleRepository)
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = storageInitError?.let {
                                "No se pudo abrir el almacenamiento privado del ciclo."
                            } ?: "Preparando el almacenamiento privado de tu ciclo…",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            AppDestination.ACTIVIDAD -> ActivityFeatureScreen()
            else -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.shell_not_ready_icon),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.shell_feature_not_ready),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

