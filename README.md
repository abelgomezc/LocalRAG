# Local RAG Assistant

Proyecto de aprendizaje y portafolio desarrollado por Abel Gomez.

Implementación de **Retrieval-Augmented Generation (RAG)** utilizando:
- Java 21
- Spring Boot 3.3.4
- Spring AI 1.0.0
- Ollama
- PostgreSQL
- React + TypeScript + Vite

## Estado

Completo y funcional. Usa `SimpleVectorStore` en memoria por defecto. Ver sección "Vector Store" para opciones de persistencia.

## Tecnologías

### Backend
- Java 21
- Spring Boot 3.3.4
- Spring AI 1.0.0
- Spring Web
- Spring Data JPA
- PostgreSQL 18
- Ollama
- Maven 3.9+

### Frontend
- React 18
- TypeScript
- Vite
- Axios

## Requisitos

- Java 21
- Maven 3.9+
- Node.js 18+
- PostgreSQL 18 (o compatible)
- Ollama

## Modelos Ollama

El proyecto está configurado para usar:
- **Chat**: `qwen3-8b-fast:latest`
- **Embeddings**: `bge-m3:latest`

Si no los tienes instalados:

```bash
ollama pull qwen3-8b-fast
ollama pull bge-m3
```

## Configuración

Backend — `backend/.env`:

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

Backend — `backend/src/main/resources/application.properties`:

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

### Opción 1: Script automático (recomendado)

```powershell
.\start-environment.ps1
```

### Opción 2: Manual

#### Backend

```powershell
cd backend
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "1234"
mvn spring-boot:run
```

Backend: http://localhost:8080

#### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173

## Endpoints

### Documentos

- `POST /api/documents/upload` — Sube y procesa un documento
- `GET /api/documents` — Lista documentos procesados
- `DELETE /api/documents/{id}` — Elimina un documento y sus vectores

### Chat

- `POST /api/chat` — Realiza una pregunta sobre los documentos
- `GET /api/health` — Health check

## Ejemplo de uso

1. Abrir `http://localhost:5173`
2. Subir un documento PDF, TXT o Markdown desde el panel lateral.
3. Esperar a que el documento se procese y se generen los embeddings.
4. Escribir una pregunta en el chat.
5. El sistema buscará semánticamente los chunks relevantes y generará una respuesta con las fuentes.

## Arquitectura

```
Documento
    ?
Extracción (PDF / TXT / MD)
    ?
Chunking (TokenTextSplitter)
    ?
Embeddings (Ollama)
    ?
Vector Store (SimpleVectorStore / PgVector)
    ?
Pregunta del usuario
    ?
Embedding de la pregunta
    ?
Similarity Search
    ?
Top-K chunks relevantes
    ?
Contexto
    ?
Ollama (LLM)
    ?
Respuesta + Fuentes
```

## Vector Store

### Actual: SimpleVectorStore (memoria)

Por defecto el proyecto usa `SimpleVectorStore` de Spring AI, que guarda los embeddings en memoria. Es ideal para pruebas locales rápidas.

**Ventaja**: No requiere dependencias adicionales.
**Desventaja**: Al reiniciar el backend se pierden los embeddings; hay que volver a procesar los documentos.

### Opción: pgvector con Docker

Si quieres persistencia real con PostgreSQL + pgvector, puedes levantar pgvector en Docker:

```powershell
docker run -d --name pgvector `
  -e POSTGRES_PASSWORD=1234 `
  -p 5432:5432 `
  pgvector/pgvector:pg18
```

Luego, para cambiar el proyecto a pgvector:

1. En `backend/pom.xml`, reemplazar:
   - `spring-ai-vector-store` por `spring-ai-starter-vector-store-pgvector`
2. Eliminar `VectorStoreConfig.java`
3. En `application.properties`, agregar:
   ```properties
   spring.ai.vectorstore.pgvector.index-type=HNSW
   spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
   spring.ai.vectorstore.pgvector.dimensions=1024
   ```
4. Crear la extensión en PostgreSQL:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

## Decisiones técnicas

- **Spring AI**: Abstrae el acceso al LLM, embeddings y vector store.
- **SimpleVectorStore**: Para prototipado rápido sin infraestructura adicional.
- **Ollama**: Ejecuta el LLM y el modelo de embeddings localmente.
- **React + Vite**: Interfaz moderna sin dependencias innecesarias.

## Limitaciones

- No hay autenticación ni multi-tenancy.
- No hay streaming de respuestas.
- No hay historial de conversación persistente.
- El tamaño máximo de archivo está limitado a 50 MB por defecto.
- Solo se soportan PDF, TXT y Markdown.
- Con `SimpleVectorStore`, los embeddings se pierden al reiniciar el backend.

## Posibles mejoras

- Streaming de respuestas.
- Historial de chat.
- Soporte para más formatos (Word, Excel, etc.).
- Re-indexado automático.
- Métricas de calidad de retrieval.
- Tests de integración con Testcontainers.
- Migración a PgVector para persistencia permanente.
