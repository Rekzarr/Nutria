using Nutria.Endpoints;

namespace Nutria.Services;

/// <summary>Estimates meal nutrients from an image.</summary>
public interface IMealAnalysisService
{
    /// <summary>Analyzes an uploaded image and returns approximate per-portion nutrients.</summary>
    Task<MealAnalysisResponse> AnalyzeAsync(
        AnalyzeMealRequest request,
        CancellationToken cancellationToken);
}
