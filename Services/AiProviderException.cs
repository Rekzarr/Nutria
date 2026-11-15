namespace Nutria.Services;

/// <summary>Represents a safe, expected failure while contacting the configured AI provider.</summary>
public sealed class AiProviderException(string message, int statusCode) : Exception(message)
{
    /// <summary>Gets the HTTP status code to return to the client.</summary>
    public int StatusCode { get; } = statusCode;
}
