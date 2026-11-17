package com.nutria.app.feature.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nutria.app.app.SpanishFormats
import com.nutria.app.data.remote.IngredientProduct
import com.nutria.app.data.remote.OpenFoodFactsClient
import com.nutria.app.data.remote.NutriaApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun IngredientSearchCard(
    onProductSelected: (IngredientProduct) -> Unit,
) {
    val client = remember { OpenFoodFactsClient() }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<IngredientProduct>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.ingredient_search_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.ingredient_search_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(MAX_QUERY_LENGTH) },
                label = { Text(stringResource(R.string.ingredient_search_label)) },
                singleLine = true,
                enabled = !isSearching,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        isSearching = true
                        results = emptyList()
                        status = null
                        statusIsError = false
                        try {
                            when (val response = client.searchIngredients(query.trim())) {
                                is NutriaApiResult.Success -> {
                                    results = response.value
                                    if (results.isEmpty()) {
                                        status = "No se encontraron productos con nutrientes completos."
                                    }
                                }
                                is NutriaApiResult.Failure -> {
                                    status = response.error.message
                                    statusIsError = true
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            status = failure.message
                                ?: "No se pudo completar la búsqueda de ingredientes."
                            statusIsError = true
                        } finally {
                            isSearching = false
                        }
                    }
                },
                enabled = query.trim().isNotEmpty() && !isSearching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSearching) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.ingredient_search_button))
                }
            }
            status?.let { message ->
                Text(
                    text = message,
                    color = if (statusIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            results.forEach { product ->
                TextButton(
                    onClick = { onProductSelected(product) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = product.name,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (product.brand.isNotBlank()) {
                            Text(
                                text = product.brand,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.ingredient_search_nutrients,
                                SpanishFormats.formatNumber(product.caloriesPer100g),
                                SpanishFormats.formatNumber(product.proteinPer100g),
                                SpanishFormats.formatNumber(product.carbsPer100g),
                                SpanishFormats.formatNumber(product.fatPer100g),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_QUERY_LENGTH = 80
