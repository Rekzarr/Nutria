using System.Globalization;
using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using Nutria.Endpoints;

namespace Nutria.Services;

/// <summary>Searches the public Open Food Facts database for ingredient nutrition.</summary>
public sealed class OpenFoodFactsService(HttpClient httpClient) : IOpenFoodFactsService
{
    private const string SearchEndpoint = "https://world.openfoodfacts.org/cgi/search.pl";
    private const int ResultLimit = 8;
    private const int MaxAttempts = 3;

    /// <inheritdoc />
    public async Task<IReadOnlyList<IngredientSearchResultResponse>> SearchAsync(
        string query,
        CancellationToken cancellationToken)
    {
        var parameters = new Dictionary<string, string>
        {
            ["search_terms"] = query,
            ["search_simple"] = "1",
            ["action"] = "process",
            ["json"] = "1",
            ["lc"] = "es",
            ["page_size"] = ResultLimit.ToString(CultureInfo.InvariantCulture),
            ["fields"] = "code,product_name_es,product_name,brands,nutriments"
        };
        var queryString = string.Join("&", parameters.Select(parameter =>
            $"{Uri.EscapeDataString(parameter.Key)}={Uri.EscapeDataString(parameter.Value)}"));

        for (var attempt = 1; ; attempt++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, $"{SearchEndpoint}?{queryString}");
            request.Headers.UserAgent.ParseAdd("Nutria/1.0");
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            using var response = await httpClient.SendAsync(
                request,
                HttpCompletionOption.ResponseHeadersRead,
                cancellationToken);
            if (IsTransient(response.StatusCode) && attempt < MaxAttempts)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(350 * attempt), cancellationToken);
                continue;
            }
            response.EnsureSuccessStatusCode();

            await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var document = await JsonDocument.ParseAsync(
                responseStream,
                cancellationToken: cancellationToken);
            if (!document.RootElement.TryGetProperty("products", out var products)
                || products.ValueKind != JsonValueKind.Array)
            {
                throw new JsonException("Open Food Facts did not return a products array.");
            }

            return products.EnumerateArray()
                .Select(TryMapProduct)
                .Where(product => product is not null)
                .Select(product => product!)
                .Take(ResultLimit)
                .ToArray();
        }
    }

    private static bool IsTransient(HttpStatusCode statusCode) =>
        statusCode is HttpStatusCode.RequestTimeout or HttpStatusCode.TooManyRequests
        || (int)statusCode >= 500;

    private static IngredientSearchResultResponse? TryMapProduct(JsonElement product)
    {
        var code = ReadString(product, "code");
        var name = ReadString(product, "product_name_es");
        if (string.IsNullOrWhiteSpace(name))
        {
            name = ReadString(product, "product_name");
        }
        var brand = ReadString(product, "brands");
        if (string.IsNullOrWhiteSpace(name)
            || !product.TryGetProperty("nutriments", out var nutriments)
            || nutriments.ValueKind != JsonValueKind.Object)
        {
            return null;
        }

        if (!TryReadNutrient(nutriments, "energy-kcal_100g", out var calories))
        {
            if (!TryReadNutrient(nutriments, "energy-kj_100g", out var energyKilojoules))
            {
                return null;
            }
            calories = energyKilojoules / 4.184;
        }

        if (!TryReadNutrient(nutriments, "proteins_100g", out var protein)
            || !TryReadNutrient(nutriments, "carbohydrates_100g", out var carbs)
            || !TryReadNutrient(nutriments, "fat_100g", out var fat)
            || calories > 10000
            || protein > 1000
            || carbs > 1000
            || fat > 1000)
        {
            return null;
        }

        return new IngredientSearchResultResponse(code, name, brand, calories, protein, carbs, fat);
    }

    private static string ReadString(JsonElement element, string propertyName) =>
        element.TryGetProperty(propertyName, out var property)
        && property.ValueKind == JsonValueKind.String
            ? property.GetString() ?? ""
            : "";

    private static bool TryReadNutrient(JsonElement element, string propertyName, out double amount)
    {
        amount = 0;
        if (!element.TryGetProperty(propertyName, out var property))
        {
            return false;
        }

        var parsed = property.ValueKind switch
        {
            JsonValueKind.Number => property.TryGetDouble(out amount),
            JsonValueKind.String => double.TryParse(
                property.GetString(),
                NumberStyles.Float,
                CultureInfo.InvariantCulture,
                out amount),
            _ => false
        };
        return parsed && double.IsFinite(amount) && amount >= 0;
    }
}
