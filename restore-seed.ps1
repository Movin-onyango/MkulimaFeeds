# ============================================================
# MkulimaFeeds — inspect old volume, dump, and prepare restore
# ============================================================

# 0. Always work from the project directory
Set-Location C:\Users\movin\AndroidStudioProjects\MkulimaFeeds-Portfolio

Write-Host "`n=== Step 1: Verify Docker daemon ===" -ForegroundColor Cyan
docker version --format '{{.Server.Version}}' 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop is not running. Start it and try again." -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Step 2: Verify live containers ===" -ForegroundColor Cyan
docker compose ps

Write-Host "`n=== Step 3: Baseline — what's in the live DB right now? ===" -ForegroundColor Cyan
docker compose exec -T postgres psql -U mkulimafeeds_app -d mkulimafeeds_db `
    -c "SELECT id, name, email, role FROM users;"

# ------------------------------------------------------------
# Candidate volumes to inspect
# ------------------------------------------------------------
$volumes = @(
    "mkulimafeeds_postgres_data",
    "mkulitest_postgres_data",
    "mkulimafeeds-portfolio_postgres_data"   # the live one, for comparison
)

foreach ($vol in $volumes) {

    Write-Host "`n=====================================================" -ForegroundColor Yellow
    Write-Host "Inspecting volume: $vol" -ForegroundColor Yellow
    Write-Host "=====================================================" -ForegroundColor Yellow

    $containerName = "inspect_$($vol -replace '[^a-zA-Z0-9]','_')"

    # Clean up any leftover inspection container
    docker rm -f $containerName 2>$null | Out-Null

    # Start a throwaway Postgres container against this volume
    docker run --rm -d `
        --name $containerName `
        -e POSTGRES_PASSWORD=temp `
        -v "${vol}:/var/lib/postgresql/data" `
        postgres:16-alpine | Out-Null

    # Wait for it to become ready
    Write-Host "Waiting for Postgres to boot..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 20

    # List databases
    Write-Host "`nDatabases in $vol :" -ForegroundColor Green
    docker exec $containerName psql -U postgres -c "SELECT datname FROM pg_database WHERE datistemplate = false;"

    # Try to query users in the expected database
    Write-Host "`nUser count in ${vol}/mkulimafeeds_db :" -ForegroundColor Green
    docker exec $containerName psql -U postgres -d mkulimafeeds_db `
        -c "SELECT COUNT(*) FROM users;" 2>&1

    Write-Host "`nUser roles in ${vol}/mkulimafeeds_db :" -ForegroundColor Green
    docker exec $containerName psql -U postgres -d mkulimafeeds_db `
        -c "SELECT role, COUNT(*) FROM users GROUP BY role;" 2>&1

    Write-Host "`nProduct count in ${vol}/mkulimafeeds_db :" -ForegroundColor Green
    docker exec $containerName psql -U postgres -d mkulimafeeds_db `
        -c "SELECT COUNT(*) FROM products;" 2>&1

    # Stop the inspection container
    docker stop $containerName | Out-Null
}

Write-Host "`n=== Inspection complete ===" -ForegroundColor Cyan
Write-Host "Review the outputs above. Whichever volume shows >0 users and >0 products is your seed." -ForegroundColor Cyan