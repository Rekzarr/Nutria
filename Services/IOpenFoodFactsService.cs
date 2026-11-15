using Nutria.Endpoints;

namespace Nutria.Services;

/// <summary>Looks up food products and their per-100-gram nutrients.</summary>
public interface IOpenFoodFactsService
{
    /// <summary>Searches products matching an ingredient name.</summary>
    Task<IReadOnlyList<IngredientSearchResultResponse>> SearchAsync(
        string query,
        CancellationToken cancellationToken);
}
