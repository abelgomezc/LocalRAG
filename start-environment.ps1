# LocalRAG - Script para levantar el entorno completo
# Ejecutar desde la raíz del proyecto en PowerShell

$ErrorActionPreference = "Stop"

Write-Host "=== Levantando entorno LocalRAG ===" -ForegroundColor Cyan

# 1. Cargar variables de entorno desde backend/.env
Write-Host "[0/4] Cargando variables de entorno..." -ForegroundColor Yellow
$envFile = Join-Path $PSScriptRoot "backend\.env"
$envVars = @{}
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            $envVars[$name] = $value
        }
    }
    Write-Host "  Variables cargadas desde backend\.env" -ForegroundColor Green
} else {
    Write-Host "  ADVERTENCIA: No se encontró backend\.env. Usando valores por defecto." -ForegroundColor Red
}

# 2. Verificar e iniciar PostgreSQL
Write-Host "[1/4] Verificando PostgreSQL..." -ForegroundColor Yellow
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService) {
    if ($pgService.Status -ne "Running") {
        Start-Service $pgService
        Start-Sleep -Seconds 2
        Write-Host "  PostgreSQL iniciado." -ForegroundColor Green
    } else {
        Write-Host "  PostgreSQL ya está corriendo." -ForegroundColor Green
    }
} else {
    Write-Host "  ADVERTENCIA: No se encontró el servicio PostgreSQL. Inícialo manualmente." -ForegroundColor Red
}

# 3. Verificar e iniciar Ollama
Write-Host "[2/4] Verificando Ollama..." -ForegroundColor Yellow
$ollamaProcess = Get-Process ollama -ErrorAction SilentlyContinue
if (-not $ollamaProcess) {
    Start-Process ollama -WindowStyle Hidden
    Start-Sleep -Seconds 3
    Write-Host "  Ollama iniciado." -ForegroundColor Green
} else {
    Write-Host "  Ollama ya está corriendo." -ForegroundColor Green
}

# 4. Verificar puertos disponibles
Write-Host "[3/4] Verificando puertos..." -ForegroundColor Yellow
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "  Puerto 8080 ocupado. Deteniendo proceso anterior..." -ForegroundColor Yellow
    Stop-Process -Id $port8080.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

$port5173 = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
if ($port5173) {
    Write-Host "  Puerto 5173 ocupado. Deteniendo proceso anterior..." -ForegroundColor Yellow
    Stop-Process -Id $port5173.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}
Write-Host "  Puertos verificados." -ForegroundColor Green

# 5. Iniciar Backend y Frontend
Write-Host "[4/4] Iniciando servicios..." -ForegroundColor Yellow

$backendPath = Join-Path $PSScriptRoot "backend"
$backendLog = Join-Path $PSScriptRoot "logs\backend.log"
$frontendPath = Join-Path $PSScriptRoot "frontend"
$frontendLog = Join-Path $PSScriptRoot "logs\frontend.log"

if (-not (Test-Path (Join-Path $PSScriptRoot "logs"))) {
    New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot "logs") -Force | Out-Null
}

# Construir comando para backend con variables de entorno
$backendCmd = ""
foreach ($key in $envVars.Keys) {
    $backendCmd += "set `"$key=$($envVars[$key])`" && "
}
$backendCmd += "cd /d `"$backendPath`" && mvn spring-boot:run > `"$backendLog`" 2>&1"

Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $backendCmd -WindowStyle Hidden
Write-Host "  Backend iniciado (log: logs/backend.log)" -ForegroundColor Green

# Iniciar Frontend
$frontendCmd = "cd /d `"$frontendPath`" && npm run dev > `"$frontendLog`" 2>&1"
Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $frontendCmd -WindowStyle Hidden
Write-Host "  Frontend iniciado (log: logs/frontend.log)" -ForegroundColor Green

Write-Host ""
Write-Host "=== Entorno levantado ===" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8080" -ForegroundColor White
Write-Host "Frontend: http://localhost:5173" -ForegroundColor White
Write-Host ""
Write-Host "Para ver logs:" -ForegroundColor Gray
Write-Host "  Backend:  Get-Content logs/backend.log -Wait" -ForegroundColor Gray
Write-Host "  Frontend: Get-Content logs/frontend.log -Wait" -ForegroundColor Gray
Write-Host ""
