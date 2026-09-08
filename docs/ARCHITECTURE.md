# Arquitectura de LocalRAG

Este documento describe la arquitectura interna de LocalRAG, sus componentes, patrones de diseno y flujo de datos.

---

## Vision general

LocalRAG sigue una arquitectura **hexagonal / limpia** con separacion clara de responsabilidades:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Frontend   │────▶│ Controller  │────▶│   Service   │
│  (React)    │     │  (REST)     │     │  (RAG Core) │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                                 │
                                    ┌────────────┼────────────┐
                                    ▼            ▼            ▼
                              ┌──────────┐ ┌──────────┐ ┌──────────┐
                              │ Repository│ │ Vector   │ │   LLM    │
                              │  (JPA)    │ │  Store   │ │ (Ollama) │
                              └─────┬─────┘ └────┬─────┘ └──────────┘
                                    │             │
                                    ▼             ▼
                              ┌─────────────────────────┐
                              │    PostgreSQL + pgvector │
                              └─────────────────────────┘
```

---

## Capas

### 1. Frontend (React + TypeScript + Vite)

**Responsabilidad:** Interfaz de usuario y experiencia.

**Estructura:**
- `context/AppContext.tsx` - Estado global (tema, idioma)
- `i18n/translations.ts` - Traducciones ES/EN
- `pages/HomePage.tsx` - Layout principal
- `components/` - Componentes reutilizables (Chat, Documentos, Grafo, Visor)
- `api/` - Cliente HTTP con Axios
- `types/` - Interfaces TypeScript

**Patrones:**
- Componentes funcionales con Hooks
- Context API para estado global
- Proxy de Vite para llamadas al backend

### 2. Backend (Spring Boot)

**Responsabilidad:** API REST, logica de negocio y orquestacion RAG.

**Estructura:**
- `controller/` - Endpoints REST (Documentos, Chat, Health, Logs)
- `service/` - Logica de negocio
- `rag/` - Motor RAG (core del sistema)
- `entity/` - Entidades JPA
- `repository/` - Spring Data JPA
- `dto/` - Objetos de transferencia
- `exception/` - Manejo de errores

**Patrones:**
- RESTful API
- Inyeccion de dependencias (Spring)
- DTOs para request/response
- Excepciones custom con GlobalExceptionHandler

### 3. Motor RAG

**Responsabilidad:** Procesamiento de documentos y generacion de respuestas.

**Componentes:**

#### RagDocumentService (Ingestion)

```
Archivo subido
    │
    ▼
[Validacion] - tipo, tamano, cantidad
    │
    ▼
[Lectura] - PDFReader, MarkdownReader, POI (Word/Excel)
    │
    ▼
[OCR si es necesario] - Tesseract + PyMuPDF
    │
    ▼
[Chunking] - TokenTextSplitter (1000 tokens, 150 overlap)
    │
    ▼
[Embeddings] - bge-m3 via Ollama
    │
    ▼
[Almacenamiento] - Vector Store + PostgreSQL
```

#### RagQueryService (Retrieval + Generation)

```
Pregunta del usuario
    │
    ▼
[Reescritura de consulta] - mejora la pregunta
    │
    ▼
[Busqueda hibrida]
    │
    ├── Vector similarity (bge-m3 embeddings)
    └── Full-text search (PostgreSQL ts_rank)
    │
    ▼
[Evaluacion CRAG] - score promedio de similitud
    │
    ├── ALTO → continuar
    └── BAJO → correccion (re-query, fallback)
    │
    ▼
[Generacion Self-RAG]
    │
    ├── [Retrieve] → mas contexto
    ├── [IsRelevant] → evaluar relevancia
    └── [Support] → generar respuesta
    │
    ▼
[Agente RAG] - evaluacion final y posible iteracion
    │
    ▼
[Respuesta con fuentes]
```

### 4. Persistencia

**PostgreSQL + pgvector:**
- Almacena documentos, chunks, conversaciones, mensajes y relaciones
- Full-text search con configuracion `spanish`
- Vector store complementario en memoria (SimpleVectorStore)

**Flyway:**
- Migraciones de base de datos versionadas
- Actualmente deshabilitado (`spring.flyway.enabled=false`), usa `ddl-auto=update`

---

## Entidades principales

```
Documento (1) ──< (N) DocumentoChunk
   │
   └───< (N) DocumentRelation (N) >─── (N) Documento

Conversation (1) ──< (N) Message
```

### Relaciones entre documentos

El sistema soporta un grafo dirigido de documentos:

```
Documento A ──relacion──> Documento B
    │                           │
    └──relacion──> Documento C <──┘
```

Se visualiza como un grafo interactivo en el frontend usando `reactflow`.

---

## Flujos principales

### Flujo: Carga de documento

1. Usuario selecciona archivo(s) en el frontend
2. Frontend envia `POST /api/documents/upload` (multipart/form-data)
3. Backend valida tamano, tipo y cantidad (max 5)
4. Archivo se guarda en `uploads/`
5. `RagDocumentService` procesa el archivo:
   - Determina tipo y lector apropiado
   - Extrae texto (con OCR si es PDF escaneado)
   - Chunking con `TokenTextSplitter`
   - Genera embeddings con `bge-m3`
   - Guarda chunks en PostgreSQL y vector store
6. Retorna `DocumentoUploadResponse` con estado

### Flujo: Consulta (Chat)

1. Usuario envia pregunta en el chat
2. Frontend envia `POST /api/chat` con `question` y `conversationId`
3. `RagQueryService.ask()`:
   - Reescribe la consulta para mejor recuperacion
   - Ejecuta busqueda hibrida (vector + full-text)
   - Evalua calidad con CRAG (score umbral configurable)
   - Si es baja, aplica correccion
   - Genera respuesta con Self-RAG (reflexion LLM)
   - Evalua respuesta final con Agentic RAG
4. Persiste pregunta y respuesta en `messages`
5. Retorna `ChatResponse` con respuesta y fuentes

---

## Configuracion por variables

Todas las configuraciones criticas se exponen como variables de entorno en `application.properties`:

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `DATABASE_USERNAME` | postgres | Usuario PostgreSQL |
| `DATABASE_PASSWORD` | 1234 | Password PostgreSQL |
| `OLLAMA_BASE_URL` | http://localhost:11434 | URL de Ollama |
| `OLLAMA_CHAT_MODEL` | qwen3-8b-fast | Modelo de chat |
| `OLLAMA_EMBEDDING_MODEL` | bge-m3 | Modelo de embeddings |
| `OLLAMA_TEMPERATURE` | 0.2 | Temperatura del LLM |
| `RAG_CHUNK_SIZE` | 1000 | Tamano de chunk en tokens |
| `RAG_CHUNK_OVERLAP` | 150 | Overlap entre chunks |
| `RAG_TOP_K` | 5 | Cantidad de resultados a recuperar |
| `RAG_MAX_FILE_SIZE` | 50MB | Tamano maximo de archivo |
| `RAG_OCR_ENABLED` | true | Habilitar OCR |
| `RAG_CRG_MIN_SCORE` | 0.3 | Score minimo para considerar valida la busqueda |
| `RAG_MAX_ITERATIONS` | 3 | Maximo de iteraciones del agente |
| `TESSDATA_PREFIX` | C:\tesseract\tessdata | Ruta a datos de Tesseract |

---

## Dependencias clave

### Backend (Maven)

| Dependencia | Proposito |
|-------------|-----------|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-data-jpa` | Persistencia |
| `spring-ai-starter-model-ollama` | Integracion con Ollama |
| `spring-ai-vector-store` | Almacenamiento vectorial |
| `spring-ai-pdf-document-reader` | Lectura de PDFs |
| `spring-ai-markdown-document-reader` | Lectura de Markdown |
| `spring-ai-rag` | Utilidades RAG |
| `apache-poi` | Lectura de Word/Excel |
| `flyway-core` | Migraciones de BD |
| `postgresql` | Driver JDBC |

### Frontend (npm)

| Dependencia | Proposito |
|-------------|-----------|
| `react` | UI framework |
| `react-dom` | Renderizado DOM |
| `axios` | Cliente HTTP |
| `vite` | Build tool y dev server |

---

## Limitaciones conocidas

- Maximo 5 archivos por carga (configurable en frontend)
- Memoria limitada para vector store en produccion (usar pgvector nativo para escalar)
- OCR funciona mejor con Tesseract 5.x y datos de idioma instalados
- Modelos grandes pueden requerir GPU para rendimiento aceptable
