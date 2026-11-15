# CampusFlow Demo Reset Script
# Drops and recreates the development database, reseeds clean demo data, and prepares the backend.

[CmdletBinding()]
param (
    [string]$DbUser = "postgres",
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "        JKLU CampusFlow - Demo Database Reset Tool        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# Prompt securely for PostgreSQL password at runtime (never persisted to disk)
$securePass = Read-Host -Prompt "Enter PostgreSQL password for user '$DbUser'" -AsSecureString
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePass)
$plainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

$env:PGPASSWORD = $plainPass

try {
    Write-Host "[1/4] Terminating active connections to 'campusflow'..." -ForegroundColor Yellow
    $termSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'campusflow' AND pid <> pg_backend_pid();"
    & psql -h $DbHost -p $DbPort -U $DbUser -d postgres -c $termSql | Out-Null

    Write-Host "[2/4] Dropping and recreating 'campusflow' database..." -ForegroundColor Yellow
    & psql -h $DbHost -p $DbPort -U $DbUser -d postgres -c "DROP DATABASE IF EXISTS campusflow;" | Out-Null
    & psql -h $DbHost -p $DbPort -U $DbUser -d postgres -c "CREATE DATABASE campusflow;" | Out-Null
    & psql -h $DbHost -p $DbPort -U $DbUser -d campusflow -c "CREATE EXTENSION IF NOT EXISTS btree_gist;" | Out-Null
    Write-Host "Database 'campusflow' recreated successfully with btree_gist extension." -ForegroundColor Green

    Write-Host "[3/4] Stopping any existing Java backend processes..." -ForegroundColor Yellow
    Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like "*campusflow*" -or $_.CommandLine -like "*campusflow*" } | Stop-Process -Force -ErrorAction SilentlyContinue

    Write-Host "[4/4] Clean Demo Environment Ready!" -ForegroundColor Green
    Write-Host ""
    Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Demo Credentials for Presentation:" -ForegroundColor Cyan
    Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "  Staff Account:   staff@demo.jklu.test" -ForegroundColor White
    Write-Host "  Student Account: student@demo.jklu.test" -ForegroundColor White
    Write-Host "  Admin Account:   admin@demo.jklu.test" -ForegroundColor White
    Write-Host "  Password:        (Configured in DEMO_USER_PASSWORD / VITE_DEMO_PASSWORD)" -ForegroundColor White
    Write-Host ""
    Write-Host "To start the backend in dev,demo mode:" -ForegroundColor Cyan
    Write-Host '  cd backend; $env:SPRING_PROFILES_ACTIVE="dev,demo"; .\mvnw.cmd spring-boot:run' -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To start the frontend:" -ForegroundColor Cyan
    Write-Host "  cd frontend; npm run dev" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Browser URL: http://localhost:5173" -ForegroundColor Green
    Write-Host "----------------------------------------------------------" -ForegroundColor Cyan
} finally {
    $env:PGPASSWORD = $null
    $plainPass = $null
}
