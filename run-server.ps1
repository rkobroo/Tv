# Simple HTTP Server in PowerShell
# This script starts a basic HTTP server for serving static files

param(
    [int]$Port = 3000
)

$RootDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🎬 Live Sports TV Server" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📂 Root Directory: $RootDirectory" -ForegroundColor Green
Write-Host "🌐 Server URL: http://localhost:$Port" -ForegroundColor Green
Write-Host "📺 Open in browser: http://localhost:$Port" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Create HTTP listener
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")

try {
    $listener.Start()
    Write-Host "✅ Server started successfully!" -ForegroundColor Green
    Write-Host ""

    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $path = $request.Url.LocalPath
        if ($path -eq "/" -or $path -eq "") {
            $path = "/index.html"
        }

        $filePath = Join-Path $RootDirectory $path.TrimStart('/')

        try {
            if (Test-Path $filePath -PathType Leaf) {
                $content = [System.IO.File]::ReadAllBytes($filePath)
                $response.ContentType = Get-ContentType $filePath
                $response.ContentLength64 = $content.Length
                $response.OutputStream.Write($content, 0, $content.Length)
                Write-Host "✅ $($request.HttpMethod) $path" -ForegroundColor Green
            }
            else {
                $response.StatusCode = 404
                $response.ContentType = "text/html"
                $notFound = [System.Text.Encoding]::UTF8.GetBytes("<h1>404 - Not Found</h1>")
                $response.OutputStream.Write($notFound, 0, $notFound.Length)
                Write-Host "❌ $($request.HttpMethod) $path - 404 Not Found" -ForegroundColor Red
            }
        }
        catch {
            $response.StatusCode = 500
            $response.ContentType = "text/plain"
            $error = [System.Text.Encoding]::UTF8.GetBytes("Internal Server Error: $_")
            $response.OutputStream.Write($error, 0, $error.Length)
            Write-Host "⚠️  Error handling $path : $_" -ForegroundColor Yellow
        }
        finally {
            $response.OutputStream.Close()
        }
    }
}
catch {
    Write-Host "❌ Error starting server: $_" -ForegroundColor Red
    exit 1
}
finally {
    if ($listener.IsListening) {
        $listener.Stop()
    }
    $listener.Dispose()
}

function Get-ContentType {
    param([string]$FilePath)
    
    $extension = [System.IO.Path]::GetExtension($FilePath).ToLower()
    
    $mimeTypes = @{
        ".html" = "text/html; charset=utf-8"
        ".htm"  = "text/html; charset=utf-8"
        ".css"  = "text/css"
        ".js"   = "application/javascript"
        ".json" = "application/json"
        ".png"  = "image/png"
        ".jpg"  = "image/jpeg"
        ".jpeg" = "image/jpeg"
        ".gif"  = "image/gif"
        ".svg"  = "image/svg+xml"
        ".ico"  = "image/x-icon"
        ".txt"  = "text/plain"
        ".xml"  = "application/xml"
    }
    
    return $mimeTypes[$extension] ?? "application/octet-stream"
}
