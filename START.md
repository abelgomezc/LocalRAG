# Levantar entorno LocalRAG

Scripts y comandos para levantar el entorno completo de forma sencilla.

## Requisitos previos

| Requisito | Version minima | Notas |
|-----------|---------------|-------|
| Java | 21 | JDK, no solo JRE |
| Maven | 3.9+ | Backend |
| Node.js | 18+ | Frontend |
| PostgreSQL | 16+ | Con extension `pgvector` |
| Ollama | 0.1+ | Para LLM y embeddings |
| Python | 3.8+ | Opcional, solo para OCR |
| Tesseract | 5.x | Opcional, solo para OCR |

## Tesseract OCR (opcional)

Requerido solo para PDFs escaneados (paginas como imagen sin texto extraible).

### Windows

```powershell
winget install --id UB-Mannheim.TesseractOCR --accept-source-agreements --accept-package-agreements
mkdir C:\tesseract\tessdata
curl -L -o C:\tesseract\tessdata\spa.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/spa.traineddata
pip install pymupdf
```

### Linux/macOS

```bash
sudo apt install tesseract-ocr tesseract-ocr-spa
pip install pymupdf
```

El script `start-environment.ps1` verifica y configura todo automaticamente.

## Paso 1: Iniciar PostgreSQL

```powershell
# Verificar si PostgreSQL esta corriendo
Get-Service -Name postgresql* -ErrorAction SilentlyContinue

# Si no esta corriendo, iniciarlo
Start-Service postgresql-x64-16
```

Verificar que la base de datos `local_rag` existe y tiene la extension `pgvector`:

```sql
CREATE DATABASE IF NOT EXISTS local_rag;
\c local_rag
CREATE EXTENSION IF NOT EXISTS vector;
```

## Paso 2: Iniciar Ollama

```powershell
# Verificar si Ollama esta corriendo
ollama list

# Si no esta corriendo, iniciar Ollama en segundo plano
Start-Process ollama -WindowStyle Hidden
```

## Paso 3: Levantar TODO con un script (recomendado)

```powershell
.\start-environment.ps1
```

Este script levanta automaticamente:
- Backend Spring Boot (puerto 8080)
- Frontend React (puerto 5173)

## Paso 4: Levantar manualmente (alternativa)

### Backend

```powershell
cd backend
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "1234"
mvn spring-boot:run
```

Backend: http://localhost:8080

### Frontend (en otra terminal)

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
4. (Opcional) Subir un PDF escaneado y preguntar sobre su contenido (requiere Tesseract)

## Solucion de problemas

### PostgreSQL no inicia

- Verificar que el servicio este instalado: `Get-Service postgresql*`
- Verificar logs en: `C:\Program Files\PostgreSQL\16\data\log\`

### Ollama no responde

- Verificar que este corriendo: `ollama list`
- Si falla, ejecutar: `ollama serve`
- Verificar que los modelos estan descargados:
  ```bash
  ollama list | findstr qwen3
  ollama list | findstr bge
  ```

### Backend no compila

- Verificar Java 21: `java -version`
- Limpiar y compilar: `cd backend && mvn clean compile`

### Frontend no inicia

- Verificar Node.js: `node -version`
- Reinstalar dependencias: `cd frontend && Remove-Item -Recurse -Force node_modules && npm install`

### Puerto ocupado

Si el puerto 8080 o 5173 esta ocupado, modificar en:
- Backend: `application.properties` → `server.port=8081`
- Frontend: `vite.config.ts` → `server.port=5174`
