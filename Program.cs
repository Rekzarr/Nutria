using System.Threading.RateLimiting;
using Microsoft.AspNetCore.Http.Features;
using Nutria.Endpoints;
using Nutria.Services;

var builder = WebApplication.CreateBuilder(args);
const long maxUploadBytes = 10 * 1024 * 1024;
const long maxRequestBytes = maxUploadBytes + 64 * 1024;

builder.WebHost.ConfigureKestrel(options => options.Limits.MaxRequestBodySize = maxRequestBytes);
builder.Services.Configure<FormOptions>(options => options.MultipartBodyLengthLimit = maxRequestBytes);
builder.Services.AddOpenApi();
builder.Services.AddProblemDetails();
builder.Services.AddHttpClient<IMealAnalysisService, OpenAiMealAnalysisService>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(90);
});
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.AddPolicy("meal-analysis", context =>
        RateLimitPartition.GetFixedWindowLimiter(
            context.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            _ => new FixedWindowRateLimiterOptions
            {
                PermitLimit = 10,
                Window = TimeSpan.FromMinutes(1),
                QueueLimit = 0,
                AutoReplenishment = true
            }));
});

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseDefaultFiles();
app.UseStaticFiles();
app.UseRateLimiter();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.MapMealAnalysisEndpoints();

app.Run();
