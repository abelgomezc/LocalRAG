# Detener entorno LocalRAG

Scripts y comandos para detener el entorno completo de forma sencilla.

## Opcion 1: Detener TODO con un script (recomendado)

```powershell
.\stop-environment.ps1
```

Este script detiene:
- Backend Spring Boot (puerto 8080)
- Frontend React (puerto 5173)
- Ollama (puerto 11434)

## Opcion 2: Detener manualmente paso a paso

### 1. Detener Backend

Si el backend esta corriendo en una terminal, presionar `Ctrl+C` en esa terminal.

Alternativamente, matar el proceso por puerto:

```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue).OwningProcess -Force
```

### 2. Detener Frontend

Si el frontend esta corriendo en una terminal, presionar `Ctrl+C` en esa terminal.

Alternativamente, matar el proceso por puerto:

```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue).OwningProcess -Force
```

### 3. Detener Ollama

```powershell
# Matar proceso de Ollama
Stop-Process -Name "ollama" -Force -ErrorAction SilentlyContinue
```

## Verificar que todo se detuvo

```powershell
# Verificar que no hay procesos en los puertos
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
Get-Process ollama -ErrorAction SilentlyContinue
```

Si no hay resultados, todo esta detenido correctamente.
