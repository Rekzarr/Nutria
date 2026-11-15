using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.AspNetCore.Mvc;
using Nutria.Services;

namespace Nutria.Endpoints;

/// <summary>Multipart request used to estimate the nutrition of a meal photo.</summary>
public sealed record AnalyzeMealRequest(IFormFile Photo, string? Name);

/// <summary>Estimated nutrients for one visible meal portion.</summary>
public sealed record MealAnalysisResponse(
    string Name,
    double Calories,
    double Protein,
    double Carbs,
    double Fat);

/// <summary>Maps meal photo analysis routes.</summary>
public static class MealAnalysisEndpoints
{
    private const long MaxUploadBytes = 10 * 1024 * 1024;
    private static readonly HashSet<string> SupportedMediaTypes =
        new(StringComparer.OrdinalIgnoreCase) { "image/jpeg", "image/png", "image/webp" };

    /// <summary>Maps the meal analysis endpoint.</summary>
    public static WebApplication MapMealAnalysisEndpoints(this WebApplication app)
    {
        app.MapPost("/api/analyze-meal", HandleAsync)
            .WithName("AnalyzeMealPhoto")
            .WithSummary("Estimate meal nutrients from a photo")
            .WithDescription("Accepts a JPG, PNG, or WebP photo up to 10 MB and returns an approximate nutrition estimate.")
            .Accepts<AnalyzeMealRequest>("multipart/form-data")
            .Produces<MealAnalysisResponse>(StatusCodes.Status200OK)
            .Produces<ProblemDetails>(StatusCodes.Status400BadRequest)
            .Produces<ProblemDetails>(StatusCodes.Status502BadGateway)
            .Produces<ProblemDetails>(StatusCodes.Status503ServiceUnavailable)
            .Produces<ProblemDetails>(StatusCodes.Status504GatewayTimeout)
            .Produces(StatusCodes.Status413PayloadTooLarge)
            .Produces(StatusCodes.Status429TooManyRequests)
            .RequireRateLimiting("meal-analysis");

        return app;
    }

    private static async Task<Results<
        Ok<MealAnalysisResponse>,
        BadRequest<ProblemDetails>,
        ProblemHttpResult>> HandleAsync(
        HttpRequest httpRequest,
        IMealAnalysisService mealAnalysisService,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        if (!httpRequest.HasFormContentType)
        {
            return TypedResults.BadRequest(new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Se esperaba una solicitud multipart/form-data."
            });
        }

        IFormCollection form;
        try
        {
            form = await httpRequest.ReadFormAsync(cancellationToken);
        }
        catch (InvalidDataException)
        {
            return TypedResults.Problem(
                title: "El archivo supera el límite permitido.",
                statusCode: StatusCodes.Status413PayloadTooLarge);
        }

        var photo = form.Files.GetFile("photo");
        if (photo is null || photo.Length == 0)
        {
            return TypedResults.BadRequest(new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Añade una foto antes de solicitar el análisis."
            });
        }

        if (photo.Length > MaxUploadBytes)
        {
            return TypedResults.Problem(
                title: "La foto supera el límite de 10 MB.",
                statusCode: StatusCodes.Status413PayloadTooLarge);
        }

        if (!SupportedMediaTypes.Contains(photo.ContentType))
        {
            return TypedResults.BadRequest(new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "El formato debe ser JPG, PNG o WebP."
            });
        }

        var name = form["name"].ToString().Trim();
        if (name.Length > 80)
        {
            return TypedResults.BadRequest(new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "El nombre del plato no puede superar los 80 caracteres."
            });
        }

        var request = new AnalyzeMealRequest(photo, name);
        try
        {
            var result = await mealAnalysisService.AnalyzeAsync(request, cancellationToken);
            return TypedResults.Ok(result);
        }
        catch (AiProviderException exception)
        {
            loggerFactory.CreateLogger("MealAnalysis").LogWarning(
                "AI meal estimation failed with status {StatusCode}.",
                exception.StatusCode);

            return TypedResults.Problem(
                title: exception.StatusCode == StatusCodes.Status503ServiceUnavailable
                    ? "El análisis con IA no está configurado o no está disponible. Revisa OPENAI_API_KEY en la configuración del servidor."
                    : "El servicio de IA no pudo completar la estimación.",
                statusCode: exception.StatusCode);
        }
        catch (HttpRequestException exception)
        {
            loggerFactory.CreateLogger("MealAnalysis").LogWarning(
                exception,
                "The AI provider could not be reached.");
            return TypedResults.Problem(
                title: "No se pudo conectar con el servicio de IA.",
                statusCode: StatusCodes.Status502BadGateway);
        }
        catch (TaskCanceledException exception) when (!cancellationToken.IsCancellationRequested)
        {
            loggerFactory.CreateLogger("MealAnalysis").LogWarning(
                exception,
                "The AI provider timed out.");
            return TypedResults.Problem(
                title: "El análisis tardó demasiado. Inténtalo de nuevo.",
                statusCode: StatusCodes.Status504GatewayTimeout);
        }
    }
}
