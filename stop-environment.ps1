# LocalRAG - Script para detener el entorno completo
# Ejecutar desde la raíz del proyecto en PowerShell

$ErrorActionPreference = "SilentlyContinue"

Write-Host "=== Deteniendo entorno LocalRAG ===" -ForegroundColor Cyan

# 1. Detener Backend (puerto 8080)
Write-Host "[1/4] Deteniendo Backend..." -ForegroundColor Yellow
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    Stop-Process -Id $port8080.OwningProcess -Force
    Write-Host "  Backend detenido." -ForegroundColor Green
} else {
    Write-Host "  Backend no estaba corriendo." -ForegroundColor Gray
}

# 2. Detener Frontend (puerto 5173)
Write-Host "[2/4] Deteniendo Frontend..." -ForegroundColor Yellow
$port5173 = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
if ($port5173) {
    Stop-Process -Id $port5173.OwningProcess -Force
    Write-Host "  Frontend detenido." -ForegroundColor Green
} else {
    Write-Host "  Frontend no estaba corriendo." -ForegroundColor Gray
}

# 3. Detener Ollama
Write-Host "[3/4] Deteniendo Ollama..." -ForegroundColor Yellow
$ollamaProcess = Get-Process ollama -ErrorAction SilentlyContinue
if ($ollamaProcess) {
    Stop-Process -Name "ollama" -Force
    Write-Host "  Ollama detenido." -ForegroundColor Green
} else {
    Write-Host "  Ollama no estaba corriendo." -ForegroundColor Gray
}

# 4. Preguntar si desea detener PostgreSQL
Write-Host "[4/4] PostgreSQL..." -ForegroundColor Yellow
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService -and $pgService.Status -eq "Running") {
    $response = Read-Host "  ¿Deseas detener PostgreSQL también? (s/N)"
    if ($response -eq "s" -or $response -eq "S") {
        Stop-Service $pgService
        Write-Host "  PostgreSQL detenido." -ForegroundColor Green
    } else {
        Write-Host "  PostgreSQL se mantiene corriendo." -ForegroundColor Gray
    }
} else {
    Write-Host "  PostgreSQL no está corriendo." -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Entorno detenido ===" -ForegroundColor Cyan
