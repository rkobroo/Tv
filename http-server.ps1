param(
    [int]$Port = 8000
)

Write-Host "╔════════════════════════════════════════════╗"
Write-Host "║   Live Sports TV - HTTP Server           ║"
Write-Host "╚════════════════════════════════════════════╝"
Write-Host ""
Write-Host "🌐 Server starting on port $Port..."
Write-Host ""

# Create HTTP listener
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://+:$Port/")
$listener.Start()

Write-Host "✅ Server is running!"
Write-Host "📺 Open your browser: http://localhost:$Port"
Write-Host ""
Write-Host "Files:"
Write-Host "  - http://localhost:$Port/"
Write-Host "  - http://localhost:$Port/index.html"
Write-Host "  - http://localhost:$Port/START_HERE.html"
Write-Host ""
Write-Host "Press Ctrl+C to stop"
Write-Host ""

$html_content = @{
    "/" = @{
        content = (Get-Content -Path "$PSScriptRoot\index.html" -Raw -ErrorAction SilentlyContinue) ?? "<h1>index.html not found</h1>"
        type = "text/html"
    }
    "/index.html" = @{
        content = (Get-Content -Path "$PSScriptRoot\index.html" -Raw -ErrorAction SilentlyContinue) ?? "<h1>index.html not found</h1>"
        type = "text/html"
    }
    "/START_HERE.html" = @{
        content = (Get-Content -Path "$PSScriptRoot\START_HERE.html" -Raw -ErrorAction SilentlyContinue) ?? "<h1>START_HERE.html not found</h1>"
        type = "text/html"
    }
    "/RUN_INFO.html" = @{
        content = (Get-Content -Path "$PSScriptRoot\RUN_INFO.html" -Raw -ErrorAction SilentlyContinue) ?? "<h1>RUN_INFO.html not found</h1>"
        type = "text/html"
    }
}

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        
        $path = $request.Url.LocalPath
        if ($path -eq "/" -or $path -eq "") { $path = "/index.html" }
        
        Write-Host "📡 $($request.HttpMethod) $path" -ForegroundColor Cyan
        
        try {
            if ($html_content.ContainsKey($path)) {
                $buffer = [System.Text.Encoding]::UTF8.GetBytes($html_content[$path].content)
                $response.ContentType = $html_content[$path].type
                $response.ContentLength64 = $buffer.Length
                $response.OutputStream.Write($buffer, 0, $buffer.Length)
                $response.StatusCode = 200
                Write-Host "  ✅ 200 OK" -ForegroundColor Green
            } else {
                $file = Join-Path $PSScriptRoot ($path.TrimStart('/'))
                
                if (Test-Path $file -PathType Leaf) {
                    $buffer = [System.IO.File]::ReadAllBytes($file)
                    $ext = [System.IO.Path]::GetExtension($file).ToLower()
                    
                    $contentTypes = @{
                        '.html' = 'text/html; charset=utf-8'
                        '.css' = 'text/css'
                        '.js' = 'application/javascript'
                        '.json' = 'application/json'
                        '.png' = 'image/png'
                        '.jpg' = 'image/jpeg'
                        '.svg' = 'image/svg+xml'
                    }
                    
                    $response.ContentType = $contentTypes[$ext] ?? 'application/octet-stream'
                    $response.ContentLength64 = $buffer.Length
                    $response.OutputStream.Write($buffer, 0, $buffer.Length)
                    $response.StatusCode = 200
                    Write-Host "  ✅ 200 OK" -ForegroundColor Green
                } else {
                    $notFound = [System.Text.Encoding]::UTF8.GetBytes("<h1>404 - Not Found</h1><p>$path</p>")
                    $response.ContentType = 'text/html'
                    $response.ContentLength64 = $notFound.Length
                    $response.OutputStream.Write($notFound, 0, $notFound.Length)
                    $response.StatusCode = 404
                    Write-Host "  ❌ 404 Not Found" -ForegroundColor Red
                }
            }
        }
        catch {
            Write-Host "  ⚠️  Error: $_" -ForegroundColor Yellow
            $response.StatusCode = 500
        }
        finally {
            $response.OutputStream.Close()
        }
    }
}
finally {
    $listener.Stop()
    $listener.Dispose()
    Write-Host ""
    Write-Host "👋 Server stopped"
}
