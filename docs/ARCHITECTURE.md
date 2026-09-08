# Arquitectura de LocalRAG

Documento tecnico que explica como esta construido LocalRAG, como interactuan sus partes y los conceptos RAG tal como se implementan en este proyecto.

---

## Objetivo del proyecto

LocalRAG es un asistente de **Generacion Aumentada por Recuperacion (RAG)** 100% local. Nacio como proyecto de aprendizaje y portafolio con estos objetivos concretos:

- Ejecutar un flujo RAG real sin servicios cloud.
- Soportar multiples formatos de documento en una sola app.
- Implementar capas de validacion y mejora del resultado (CRAG, Self-RAG, Agentic RAG).
- Mantener una separacion clara entre presentacion (frontend), logica (backend) y almacenamiento.

No busca competir con sistemas empresariales; busca ser un ejemplo completo, legible y ejecutable de arquitectura RAG moderna sobre un stack accesible.

---

## Stack elegido y por que

| Capa | Eleccion | Motivo |
|------|----------|--------|
| Backend | Spring Boot 3.3.4 + Java 21 | Madurez, ecosistema y soporte oficial para Spring AI. |
| IA/Embeddings | Ollama | Correr modelos locales sin dependencias cloud ni APIs pagas. |
| Embedding | bge-m3 | Buen balance entre tamanio y calidad multilingue. |
| Chat | qwen3-8b-fast | Modelo pequeno/rapido razonable para QA local. |
| Base de datos | PostgreSQL 16 + pgvector | SQL familiar + soporte nativo para vectores y full-text. |
| OCR | Tesseract 5 + PyMuPDF | Cobertura decente para PDFs escaneados sin coste. |
| Frontend | React 18 + TypeScript + Vite | Carga rapida, estado predecible y DX simple. |

---

## Estructura del proyecto

```
LocalRAG/
├── README.md
├── START.md
├── STOP.md
├── start-environment.ps1
├── stop-environment.ps1
├── backend/
│   ├── pom.xml
│   ├── .env
│   ├── scripts/
│   │   └── ocr_pdf.py
│   └── src/main/
│       ├── java/com/localrag/
│       │   ├── LocalRagApplication.java
│       │   ├── controller/
│       │   ├── entity/
│       │   ├── exception/
│       │   ├── rag/
│       │   ├── repository/
│       │   ├── service/
│       │   └── dto/
│       └── resources/
│           ├── application.properties
│           └── db/migration/
├── docs/
│   ├── ARCHITECTURE.md
│   └── rag-approaches.md
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── api/
        ├── components/
        ├── context/
        ├── i18n/
        ├── pages/
        └── types/
```

Cada carpeta tiene una responsabilidad concreta y se evita mezclar logica de UI con logica de dominio.

---

## Backend: organizacion y responsabilidades

### Entradas: controllers

Los controllers exponen endpoints REST y se limitan a:
- parsear request
- validar lo basico
- delegar a services
- armar responses

Entidades principales:
- `DocumentosController` - endpoints de documentos
- `ChatController` - consultas y health check
- `HealthController` / `LogsController` - diagnostico

### Logica: services y motor RAG

Aqui vive el comportamiento real del sistema.

**RagDocumentService**
- decide como leer un archivo segun su tipo
- ejecuta OCR cuando corresponde
- aplica chunking
- genera embeddings
- persiste resultados

**RagQueryService**
- reescribe la consulta
- ejecuta la busqueda hibrida
- aplica CRAG
- genera respuesta con Self-RAG
- ejecuta la capa Agentic RAG

**DocumentoService y DocumentRelationService**
- gestionan metadata de documentos y relaciones

### Almacenamiento: repositories

- `DocumentoRepository`, `DocumentoChunkRepository`, `ConversationRepository`, `MessageRepository`, `DocumentRelationRepository`
- Traducen operaciones sobre entidades a SQL sin exponer JPA al resto del codigo.

### Estructura de datos: entities

El modelo relacional esta pensado para documentos y conversaciones:

- `documento` - metadata del archivo original
- `documento_chunk` - fragmentos con texto y metadatos
- `conversation` - sesion de chat
- `message` - turno individual del chat
- `document_relation` - vinculaciones semantica entre documentos

---

## Frontend: organizacion y responsabilidades

### Estado global

`context/AppContext.tsx` mantiene:
- idioma activo (`es` / `en`)
- tema (`light` / `dark`)

Evita props drilling y centraliza cambios globales.

### Internacionalizacion

`i18n/translations.ts` contiene las cadenas en espanol e ingles. Los componentes consumen traducciones por clave en vez de textos hardcodeados.

### Cliente HTTP

`api/documentsApi.ts`, `api/chatApi.ts` y `api/logsApi.ts` encapsulan las llamadas al backend. Si cambia la URL o el formato, el cambio se concentra en estos archivos.

### Componentes principales

- `DocumentUpload` - seleccion y carga de archivos
- `DocumentList` - inventario de documentos procesados
- `ChatWindow` / `ChatMessage` - interfaz conversacional
- `DocumentViewer` - previsualizacion de contenido
- `DocumentGraph` / `DocumentRelations` - grafo y administracion de vinculos
- `SourceList` - referencias recuperadas para una respuesta
- `LoadingIndicator` - feedback de carga

### Routing

`pages/HomePage.tsx` actua como layout principal y orquesta la disposicion de los componentes.

---

## Flujo: como se usa el sistema desde el usuario

### 1. Cargar documentos

- El usuario sube hasta 5 archivos.
- El backend almacena los archivos en `uploads/`.
- El sistema extrae texto:
  - nativo cuando es posible
  - por OCR si detecta PDF escaneado
- Divide el texto en chunks.
- Genera embeddings con `bge-m3`.
- Guarda chunks en PostgreSQL y en el vector store.

### 2. Consultar

- El usuario escribe una pregunta en el chat.
- El backend reescribe la consulta.
- Busca por similitud vectorial y por palabras clave.
- Evalua si la busqueda fue buena (CRAG).
- Genera la respuesta con instrucciones de auto-verificacion (Self-RAG).
- Evalua si la respuesta es usable (Agentic RAG).
- Devuelve la respuesta y las fuentes.

### 3. Vincular documentos

- El usuario crea relaciones entre documentos.
- El frontend muestra un grafo dirigido.
- Esto permite capturar conocimiento estructural: capitulos, anexos, contratos relacionados, etc.

### 4. Conversar con memoria

- Cada sesion conserva su historial.
- El historial se almacena en `conversations` y `messages`.
- El usuario puede continuar una conversacion anterior usando el `conversationId`.

---

## Conceptos RAG en el contexto de este proyecto

### Recuperacion

Es la etapa donde el sistema busca fragmentos relevantes. En este proyecto se hace de dos formas:

- **Densa**: embeddings con `bge-m3`, comparados por similitud.
- **Explicita**: busqueda por palabras clave con `ts_rank`.

Ambas se combinan para reducir falsos negativos.

### Chunking

Los documentos no se almacenan completos. Se dividen en fragmentos manejables. En LocalRAG se usa un tamanio de 1000 tokens con 150 de solapamiento. Esto ayuda a preservar contexto en bordes sin perder precision.

### Embeddings

Convierte texto en vectores numericos. `bge-m3` genera representaciones que capturan significado, por lo que dos frases similares terminan cerca en el espacio vectorial.

### RAG lineal

Es el flujo basico: pregunta -> busqueda -> respuesta. Es util para documentacion pequena y preguntas directas, pero no corrige fallos.

### CRAG (Corrective RAG)

Agrega una validacion intermedia. Si la busqueda devuelve fragmentos poco relevantes, el sistema corrige antes de generar. En la practica reduce respuestas alucinadas cuando el tema esta poco cubierto por los documentos cargados.

### Self-RAG

El modelo genera la respuesta con puntos de control internos. En vez de solo "responder", puede marcar si necesita mas contexto o si la respuesta esta bien fundamentada. Esto ayuda a mejorar la calidad sin cambiar el modelo.

### Agentic RAG

Es la capa de decision superior. Evalua la respuesta y decide si repetir, reformular o entregar el resultado. Convierte el flujo en un proceso iterativo controlado en vez de una unica pasada.

---

## Decisiones tecnicas relevantes

### ¿Por que PostgreSQL ademas del vector store?

PostgreSQL almacena el contenido real y permite busqueda full-text. El vector store complementa la busqueda semantica. Esta combinacion da precision sin perder cobertura.

### ¿Por que SimpleVectorStore y no pgvector directo para vectores?

En esta etapa del proyecto se prefirio simplicidad y claridad pedagogica. SimpleVectorStore permite iterar rapido. En el futuro pgvector directo escala mejor en produccion.

### ¿Por que Ollama?

Permite cambiar de modelo sin modificar contratos ni pagar APIs. El costo es controlado y el experimento queda en la maquina.

### ¿Por que no Flyway activo?

Las entidades ya generan el esquema con `ddl-auto=update`. Flyway queda disponible cuando se necesite control total de migraciones sin tocar codigo de entidades.

---

## Configuracion relevante

### Backend

```
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen3-8b-fast
spring.ai.ollama.embedding.options.model=bge-m3
rag.chunk-size=1000
rag.chunk-overlap=150
rag.top-k=5
rag.ocr-enabled=true
rag.crg-min-score=0.3
rag.max-iterations=3
```

### Frontend

- Puerto: `5173`
- Proxy hacia backend: `/api` y `/uploads`
- Temas e idioma gestionados por `AppContext`

---

## Limitaciones actuales

- Maximo 5 archivos por carga.
- El vector store en memoria no escala igual que una solucion nativa de vectores.
- OCR depende de Tesseract y su entrenamiento de idioma.
- Los modelos de Ollama consumen recursos locales significativos.
