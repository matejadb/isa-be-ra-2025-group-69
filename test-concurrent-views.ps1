# Concurrent View Count Test Script
# This script simulates multiple users concurrently viewing the same video
# Run this script while the backend server is running

param(
    [int]$VideoId = 1,
    [int]$NumberOfConcurrentRequests = 50,
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Concurrent Video View Count Test" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  - Video ID: $VideoId"
Write-Host "  - Concurrent Requests: $NumberOfConcurrentRequests"
Write-Host "  - Base URL: $BaseUrl"
Write-Host ""

# Get initial view count
try {
    $initialResponse = Invoke-RestMethod -Uri "$BaseUrl/api/videos/$VideoId/views" -Method Get
    $initialViewCount = $initialResponse.viewCount
    Write-Host "Initial view count: $initialViewCount" -ForegroundColor Green
} catch {
    Write-Host "Error getting initial view count. Make sure the server is running and video exists." -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting concurrent requests..." -ForegroundColor Yellow
$startTime = Get-Date

# Create an array to store jobs
$jobs = @()

# Launch concurrent requests
for ($i = 1; $i -le $NumberOfConcurrentRequests; $i++) {
    $job = Start-Job -ScriptBlock {
        param($url, $videoId)
        try {
            $response = Invoke-RestMethod -Uri "$url/api/videos/$videoId/view" -Method Post -ContentType "application/json"
            return @{
                Success = $true
                ViewCount = $response.viewCount
                ThreadId = [System.Threading.Thread]::CurrentThread.ManagedThreadId
            }
        } catch {
            return @{
                Success = $false
                Error = $_.Exception.Message
            }
        }
    } -ArgumentList $BaseUrl, $VideoId

    $jobs += $job

    # Show progress
    if ($i % 10 -eq 0) {
        Write-Host "  Launched $i requests..." -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Waiting for all requests to complete..." -ForegroundColor Yellow

# Wait for all jobs to complete
$jobs | Wait-Job | Out-Null

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds

# Collect results
$results = $jobs | Receive-Job
$jobs | Remove-Job

# Analyze results
$successCount = ($results | Where-Object { $_.Success -eq $true }).Count
$errorCount = ($results | Where-Object { $_.Success -eq $false }).Count

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Test Results" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Get final view count
try {
    $finalResponse = Invoke-RestMethod -Uri "$BaseUrl/api/videos/$VideoId/views" -Method Get
    $finalViewCount = $finalResponse.viewCount

    Write-Host "Execution Summary:" -ForegroundColor Yellow
    Write-Host "  - Total requests sent: $NumberOfConcurrentRequests"
    Write-Host "  - Successful requests: $successCount" -ForegroundColor Green
    Write-Host "  - Failed requests: $errorCount" $(if($errorCount -gt 0){"[WARNING]" | Write-Host -ForegroundColor Red} else {""})
    Write-Host "  - Time taken: $([math]::Round($duration, 2)) seconds"
    Write-Host "  - Requests per second: $([math]::Round($NumberOfConcurrentRequests / $duration, 2))"
    Write-Host ""

    Write-Host "View Count Results:" -ForegroundColor Yellow
    Write-Host "  - Initial view count: $initialViewCount"
    Write-Host "  - Final view count: $finalViewCount"
    Write-Host "  - Expected increment: $successCount"
    Write-Host "  - Actual increment: $($finalViewCount - $initialViewCount)"
    Write-Host ""

    # Verify consistency
    $expectedFinalCount = $initialViewCount + $successCount
    if ($finalViewCount -eq $expectedFinalCount) {
        Write-Host "✓ SUCCESS: View count is consistent!" -ForegroundColor Green
        Write-Host "  All $successCount concurrent requests were correctly counted." -ForegroundColor Green
    } else {
        Write-Host "✗ FAILURE: View count inconsistency detected!" -ForegroundColor Red
        Write-Host "  Expected: $expectedFinalCount, Got: $finalViewCount" -ForegroundColor Red
        Write-Host "  Difference: $($expectedFinalCount - $finalViewCount)" -ForegroundColor Red
    }

} catch {
    Write-Host "Error getting final view count: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan

# Display errors if any
if ($errorCount -gt 0) {
    Write-Host ""
    Write-Host "Errors encountered:" -ForegroundColor Red
    $results | Where-Object { $_.Success -eq $false } | ForEach-Object {
        Write-Host "  - $($_.Error)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Cyan
Write-Host ""
