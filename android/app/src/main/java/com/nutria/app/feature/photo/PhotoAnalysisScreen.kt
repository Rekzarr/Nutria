package com.nutria.app.feature.photo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nutria.app.R
import com.nutria.app.data.remote.ConfiguredNutriaApiClientFactory
import com.nutria.app.data.remote.MealPhoto
import com.nutria.app.data.remote.NutriaApiResult
import com.nutria.app.core.model.Nutrients
import com.nutria.app.feature.diary.DiaryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoAnalysisScreen(
    contentResolver: ContentResolver,
    clientFactory: ConfiguredNutriaApiClientFactory,
    diaryViewModel: DiaryViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var mealName by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var statusIsError by rememberSaveable { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val selectedUri = selectedUriText?.let(Uri::parse)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUriText = uri?.toString()
        selectedFileName = null
        status = null
        statusIsError = false
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.photo_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { picker.launch(SUPPORTED_MEDIA_TYPES.toTypedArray()) },
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedUri == null) stringResource(R.string.photo_choose)
                        else stringResource(R.string.photo_choose_another),
                    )
                }
                if (selectedUri != null) {
                    Text(
                        text = selectedFileName ?: stringResource(R.string.photo_image_selected),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it.take(DiaryViewModel.MAX_MEAL_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.photo_name_label)) },
                    supportingText = { Text(stringResource(R.string.photo_name_hint)) },
                    enabled = !isAnalyzing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        isAnalyzing = true
                        status = null
                        statusIsError = false
                        scope.launch {
                            try {
                                val mediaType = contentResolver.getType(uri)
                                    ?.substringBefore(';')
                                    ?.lowercase()
                                    ?: throw IllegalArgumentException(
                                        "No se pudo identificar el formato de la imagen.",
                                    )
                                if (mediaType !in SUPPORTED_MEDIA_TYPES) {
                                    throw IllegalArgumentException(
                                        "Selecciona una imagen JPG, PNG o WebP.",
                                    )
                                }
                                val photoFile = withContext(Dispatchers.IO) {
                                    val bytes = contentResolver.openInputStream(uri)?.use { it.readPhotoBytes() }
                                        ?: throw IllegalArgumentException(
                                            "No se pudo abrir la imagen seleccionada.",
                                        )
                                    PhotoFile(
                                        name = displayName(contentResolver, uri),
                                        bytes = bytes,
                                    )
                                }
                                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                selectedFileName = photoFile.name
                                val client = clientFactory.createOrNull()
                                    ?: throw IllegalStateException(
                                        "Configura la dirección del servidor de Nutria en Ajustes.",
                                    )
                                when (val result = client.analyzeMeal(
                                    photo = MealPhoto(
                                        fileName = photoFile.name,
                                        mediaType = mediaType,
                                        bytes = photoFile.bytes,
                                    ),
                                    name = mealName,
                                )) {
                                    is NutriaApiResult.Failure -> {
                                        status = result.error.message
                                        statusIsError = true
                                    }
                                    is NutriaApiResult.Success -> {
                                        val analysis = result.value
                                        val saved = diaryViewModel.addPhotoMeal(
                                            name = analysis.name,
                                            nutrients = Nutrients(
                                                caloriesKcal = analysis.calories,
                                                proteinGrams = analysis.protein,
                                                carbohydratesGrams = analysis.carbs,
                                                fatGrams = analysis.fat,
                                            ),
                                        )
                                        if (saved) {
                                            status = "Estimación orientativa añadida al diario. Revisa los ingredientes y la porción."
                                            statusIsError = false
                                        } else {
                                            statusIsError = true
                                            status = diaryViewModel.state.value.storageError
                                                ?: "No se pudo guardar la estimación en el diario."
                                        }
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                status = failure.message
                                    ?: "No se pudo completar el análisis de la foto."
                                statusIsError = true
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    },
                    enabled = selectedUri != null && !isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.photo_analyze))
                    }
                }
                status?.let { message ->
                    Text(
                        text = message,
                        color = if (statusIsError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun displayName(contentResolver: ContentResolver, uri: Uri): String {
    val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0) cursor.getString(column) else null
            } else {
                null
            }
        }
    return name?.takeIf(String::isNotBlank) ?: uri.lastPathSegment ?: "imagen"
}

private data class PhotoFile(val name: String, val bytes: ByteArray)

private val SUPPORTED_MEDIA_TYPES = setOf("image/jpeg", "image/png", "image/webp")
