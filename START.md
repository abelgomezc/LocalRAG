# Levantar entorno LocalRAG

Scripts y comandos para levantar el entorno completo de forma sencilla.

## Requisitos previos

- PostgreSQL 16 instalado y con extensión pgvector habilitada
- Ollama instalado
- Java 21
- Maven 3.9+
- Node.js 18+

## Opción 1: Levantar TODO con un script (recomendado)

Abrir PowerShell en la raíz del proyecto y ejecutar:

```powershell
.\start-environment.ps1
```

Este script levanta automáticamente:
- PostgreSQL (si no está corriendo)
- Ollama (si no está corriendo)
- Backend Spring Boot
- Frontend React

## Opción 2: Levantar manualmente paso a paso

### 1. Iniciar PostgreSQL

```powershell
# Verificar si PostgreSQL está corriendo
Get-Service -Name postgresql* -ErrorAction SilentlyContinue

# Si no está corriendo, iniciarlo
Start-Service postgresql-x64-16
```

### 2. Iniciar Ollama

```powershell
# Verificar si Ollama está corriendo
ollama --version

# Si no está corriendo, iniciar Ollama en segundo plano
Start-Process ollama -WindowStyle Hidden
```

### 3. Iniciar Backend

```powershell
cd backend
mvn spring-boot:run
```

Backend: http://localhost:8080

### 4. Iniciar Frontend (en otra terminal)

```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173

## Verificar que todo funciona

1. Abrir http://localhost:5173
2. Subir un documento PDF, TXT, Markdown, Word, Excel o CSV
3. Hacer una pregunta en el chat

## Solución de problemas

### PostgreSQL no inicia
- Verificar que el servicio esté instalado: `Get-Service postgresql*`
- Verificar logs en: `C:\Program Files\PostgreSQL\16\data\log\`

### Ollama no responde
- Verificar que esté corriendo: `ollama list`
- Si falla, ejecutar: `ollama serve`

### Backend no compila
- Verificar Java 21: `java -version`
- Limpiar y compilar: `cd backend && mvn clean compile`

### Frontend no inicia
- Verificar Node.js: `node -version`
- Reinstalar dependencias: `cd frontend && rm -r node_modules && npm install`

