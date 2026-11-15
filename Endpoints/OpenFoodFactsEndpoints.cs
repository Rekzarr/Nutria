using System.Text.Json;
using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.AspNetCore.Mvc;
using Nutria.Services;

namespace Nutria.Endpoints;

/// <summary>One Open Food Facts product and its nutrient values per 100 grams.</summary>
public sealed record IngredientSearchResultResponse(
    string Code,
    string Name,
    string Brand,
    double Calories,
    double Protein,
    double Carbs,
    double Fat);

/// <summary>Products matching an ingredient search.</summary>
public sealed record IngredientSearchResponse(IReadOnlyList<IngredientSearchResultResponse> Products);

/// <summary>Maps Open Food Facts ingredient lookup routes.</summary>
public static class OpenFoodFactsEndpoints
{
    /// <summary>Maps the ingredient lookup endpoint.</summary>
    public static WebApplication MapOpenFoodFactsEndpoints(this WebApplication app)
    {
        app.MapGet("/api/ingredients/search", async Task<Results<
            Ok<IngredientSearchResponse>,
            BadRequest<ProblemDetails>,
            ProblemHttpResult>> (
            [FromQuery] string? query,
            IOpenFoodFactsService openFoodFactsService,
            ILoggerFactory loggerFactory,
            CancellationToken cancellationToken) =>
        {
            var searchTerm = query?.Trim();
            if (string.IsNullOrWhiteSpace(searchTerm) || searchTerm.Length > 80)
            {
                return TypedResults.BadRequest(new ProblemDetails
                {
                    Status = StatusCodes.Status400BadRequest,
                    Title = "Escribe un ingrediente de hasta 80 caracteres."
                });
            }

            try
            {
                var products = await openFoodFactsService.SearchAsync(searchTerm, cancellationToken);
                return TypedResults.Ok(new IngredientSearchResponse(products));
            }
            catch (HttpRequestException exception)
            {
                loggerFactory.CreateLogger("IngredientSearch").LogWarning(
                    "Open Food Facts ingredient search failed with status {StatusCode}.",
                    exception.StatusCode);
                return TypedResults.Problem(
                    title: "Open Food Facts no está disponible en este momento.",
                    statusCode: StatusCodes.Status502BadGateway);
            }
            catch (JsonException exception)
            {
                loggerFactory.CreateLogger("IngredientSearch").LogWarning(
                    exception,
                    "Open Food Facts returned an invalid ingredient search response.");
                return TypedResults.Problem(
                    title: "La base de alimentos devolvió una respuesta no válida.",
                    statusCode: StatusCodes.Status502BadGateway);
            }
            catch (TaskCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                loggerFactory.CreateLogger("IngredientSearch").LogWarning(
                    "Open Food Facts ingredient search timed out.");
                return TypedResults.Problem(
                    title: "La búsqueda de alimentos tardó demasiado. Inténtalo de nuevo.",
                    statusCode: StatusCodes.Status504GatewayTimeout);
            }
        })
            .WithName("SearchIngredientNutrition")
            .WithSummary("Search ingredient nutrition")
            .WithDescription("Searches Open Food Facts by ingredient name and returns product nutrients per 100 grams.")
            .Produces<IngredientSearchResponse>(StatusCodes.Status200OK)
            .Produces<ProblemDetails>(StatusCodes.Status400BadRequest)
            .Produces<ProblemDetails>(StatusCodes.Status502BadGateway)
            .Produces<ProblemDetails>(StatusCodes.Status504GatewayTimeout)
            .Produces(StatusCodes.Status429TooManyRequests)
            .RequireRateLimiting("ingredient-search");

        return app;
    }
}
