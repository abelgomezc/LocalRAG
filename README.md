# Local RAG Assistant

Asistente de **Retrieval-Augmented Generation (RAG)** local para consultar múltiples documentos mediante lenguaje natural. Proyecto de aprendizaje y portafolio desarrollado por Abel Gomez.

## Estado actual

Funcional y productivo. Incluye subida de documentos, búsqueda híbrida (vectorial + full-text), generación de respuestas con contexto recuperado, relaciones entre documentos, historial de conversación persistente en base de datos y visualización del grafo de documentos.

## Stack tecnológico

| Capa | Tecnología |
|------|------------|
| Backend | Java 21, Spring Boot 3.3.4, Spring AI 1.0.0 |
| Base de datos | PostgreSQL + pgvector |
| IA/Embeddings | Ollama (qwen3-8b-fast, bge-m3) |
| Frontend | React 18, TypeScript, Vite, Axios |
| Build | Maven 3.9+ (backend), npm (frontend) |

## Características

- Subida y procesamiento de documentos PDF, TXT, Markdown, Word, Excel y CSV.
- Chunking con `TokenTextSplitter` y embeddings con `bge-m3`.
- Búsqueda híbrida: similitud vectorial + full-text search en PostgreSQL.
- Respuestas generadas por LLM con contexto recuperado y citas de fuentes.
- Relaciones entre documentos con visualización en grafo.
- Historial de conversación persistente en base de datos por sesión.
- Health check de aplicación, Ollama y base de datos.

## Requisitos

- Java 21
- Maven 3.9+
- Node.js 18+
- PostgreSQL 18 (con extensión pgvector)
- Ollama

## Modelos Ollama

```bash
ollama pull qwen3-8b-fast
ollama pull bge-m3
```

## Configuración

### Backend — variables de entorno

```env
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=1234

OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen3-8b-fast
OLLAMA_EMBEDDING_MODEL=bge-m3
OLLAMA_TEMPERATURE=0.2

RAG_CHUNK_SIZE=1000
RAG_CHUNK_OVERLAP=150
RAG_TOP_K=5
RAG_MAX_FILE_SIZE=50MB
```

### Backend — `application.properties`

```properties
spring.application.name=local-rag-assistant
server.port=8080

spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/local_rag}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:1234}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
spring.ai.ollama.chat.options.model=${OLLAMA_CHAT_MODEL:qwen3-8b-fast}
spring.ai.ollama.chat.options.temperature=${OLLAMA_TEMPERATURE:0.2}
spring.ai.ollama.embedding.options.model=${OLLAMA_EMBEDDING_MODEL:bge-m3}

rag.chunk-size=${RAG_CHUNK_SIZE:1000}
rag.chunk-overlap=${RAG_CHUNK_OVERLAP:150}
rag.top-k=${RAG_TOP_K:5}
rag.max-file-size=50MB

spring.servlet.multipart.max-file-size=${rag.max-file-size}
spring.servlet.multipart.max-request-size=${rag.max-file-size}
```

## Ejecución

### Script automático (recomendado)

```powershell
.\start-environment.ps1
```

### Manual

#### Backend

```powershell
cd backend
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "1234"
mvn spring-boot:run
```

Backend: `http://localhost:8080`

#### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## API

### Documentos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/documents/upload` | Sube y procesa documentos (PDF, TXT, MD, DOCX, XLSX, CSV). Máximo 5 archivos por request. |
| `GET` | `/api/documents` | Lista documentos procesados. |
| `GET` | `/api/documents/{id}/content` | Obtiene el contenido textual de un documento para visualizarlo. |
| `DELETE` | `/api/documents/{id}` | Elimina un documento, sus vectores y el historial de chat asociado. |
| `DELETE` | `/api/documents` | Elimina todos los documentos, vectores, relaciones y el historial de chat. |

### Relaciones entre documentos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/documents/relations` | Crea una relación entre dos documentos. |
| `GET` | `/api/documents/relations` | Lista todas las relaciones. |
| `DELETE` | `/api/documents/relations/{id}` | Elimina una relación. |

### Chat

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/chat` | Consulta sobre los documentos. Acepta `conversationId` para historial. |
| `GET` | `/api/health` | Health check de aplicación, Ollama y base de datos. |

## Arquitectura

### Flujo de consulta

```
Pregunta del usuario
    ?
Reescritura de consulta (LLM)
    ?
Búsqueda híbrida
    ??? Vector similarity search (SimpleVectorStore / PgVector)
    ??? Full-text search (PostgreSQL tsvector)
    ?
Fusión de resultados por chunk
    ?
Top-K chunks relevantes
    ?
Relaciones entre documentos involucrados (filtradas por documentos recuperados)
    ?
Historial de conversación (últimos 6 turnos por sesión)
    ?
Prompt enriquecido
    ?
Ollama (LLM)
    ?
Respuesta + fuentes citadas
```

### Estructura del backend

```
backend/src/main/java/com/localrag/
??? controller/
?   ??? ChatController.java          # Endpoint de chat
?   ??? DocumentosController.java    # Upload, listado y eliminación de documentos
?   ??? HealthController.java        # Health check
?   ??? LogsController.java          # Logs del sistema
??? dto/
?   ??? request/
?   ?   ??? ChatRequest.java         # Pregunta, idioma, conversationId
?   ?   ??? DocumentRelationRequest.java
?   ??? response/
?       ??? ChatResponse.java        # Respuesta + fuentes
?       ??? DocumentRelationResponse.java
?       ??? DocumentoUploadResponse.java
??? entity/
?   ??? Documento.java               # Metadatos del documento
?   ??? DocumentoChunk.java          # Chunk con contenido y metadatos JSONB
?   ??? DocumentRelation.java        # Relación source -> target
??? exception/
?   ??? DocumentProcessingException.java
?   ??? OllamaConnectionException.java
?   ??? RagException.java
??? rag/
?   ??? RagQueryService.java         # Lógica principal de RAG
?   ??? RagDocumentService.java      # Ingesta y chunking
??? repository/
?   ??? DocumentoChunkRepository.java
?   ??? DocumentRelationRepository.java
?   ??? DocumentoRepository.java
??? service/
?   ??? DocumentoService.java
?   ??? DocumentRelationService.java
?   ??? LogService.java
??? config/
    ??? VectorStoreConfig.java
    ??? WebConfig.java
```

### Estructura del frontend

```
frontend/src/
??? api/
?   ??? chatApi.ts                   # Cliente HTTP para chat
?   ??? documentsApi.ts              # Cliente HTTP para documentos
??? components/
?   ??? ChatWindow.tsx               # Ventana de chat
?   ??? ChatMessage.tsx              # Render de mensaje + fuentes
?   ??? DocumentUpload.tsx           # Upload de archivos
?   ??? DocumentList.tsx             # Lista de documentos
?   ??? DocumentGraph.tsx            # Grafo SVG de relaciones
?   ??? DocumentRelations.tsx        # CRUD de relaciones
?   ??? DocumentViewer.tsx           # Visor de PDF/texto
??? context/
?   ??? AppContext.tsx               # Estado global: idioma, tema
??? pages/
?   ??? HomePage.tsx                 # Layout principal y orquestación
??? types/
?   ??? chat.types.ts
?   ??? document.types.ts
??? i18n/
    ??? translations.ts              # Traducciones ES/EN
```

### Esquema de base de datos

```sql
documentos
  id, nombre_archivo, tipo_archivo, tamano_bytes, estado, total_chunks, fechas

documento_chunks
  id, documento_id, chunk_numero, contenido, metadatos (JSONB), content_tsv, fechas

document_relations
  id, source_document_id, target_document_id, description

conversations
  id, conversation_id (único), created_at, updated_at

messages
  id, conversation_id, role, content (TEXT), created_at
```

## Decisiones técnicas

- **Spring AI**: Abstrae LLM, embeddings y vector store.
- **SimpleVectorStore**: Prototipado rápido sin infraestructura adicional.
- **PostgreSQL + tsvector**: Búsqueda full-text nativa para complementar la búsqueda vectorial.
- **Ollama**: Ejecuta LLM y embeddings localmente sin dependencias externas.
- **React + Vite**: Interfaz moderna sin dependencias innecesarias.
- **Historial persistente**: Las conversaciones se guardan en PostgreSQL y sobreviven a reinicios. Se limpian al eliminar todos los documentos.

## Limitaciones

- No hay autenticación ni multi-tenancy.
- No hay streaming de respuestas.
- Máximo 5 documentos por instalación.
- Solo PDF, TXT, Markdown, Word, Excel y CSV.
- Con `SimpleVectorStore`, los embeddings se pierden al reiniciar el backend.

## Posibles mejoras

- Streaming de respuestas (Server-Sent Events).
- Soporte para más formatos (PDF escaneado con OCR, etc.).
- Re-indexado automático al modificar documentos.
- Métricas de calidad de retrieval.
- Tests de integración con Testcontainers.
- Migración a pgvector para persistencia permanente de embeddings.
