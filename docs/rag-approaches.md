# Enfoques de RAG implementados

Este documento explica los diferentes estilos de arquitectura RAG implementados en LocalRAG y como se relacionan entre si.

---

## 1. RAG Lineal (flujo base)

Flujo fijo: pregunta → busqueda → respuesta. Nunca vuelve atras.

```
Pregunta del usuario
         │
         ▼
[Reescritura de consulta]
         │
         ▼
[Busqueda hibrida]
┌─────────────────────┐
│ Vector similarity   │
│ + Full-text search  │
└─────────┬───────────┘
          │
          ▼
[Contexto recuperado]
         │
         ▼
[LLM genera respuesta]
         │
         ▼
[Respuesta + fuentes]
```

**Caracteristicas:**
- Una sola pasada.
- No evalua la calidad de la busqueda.
- No permite que el LLM pida mas contexto.

**Implementado en:** `RagQueryService.hybridSearch()` + `generateAnswer()`

---

## 2. CRAG (Corrective RAG)

Anade un paso de evaluacion de calidad despues de buscar. Si los resultados son malos, se corrige.

```
Pregunta del usuario
         │
         ▼
[Busqueda hibrida]
         │
         ▼
[Evaluacion de calidad]
┌──────────────────────────┐
│ Score promedio de        │
│ similitud (cosine)       │
│ ¿Es >= RAG_CRG_MIN_SCORE?│
└─────────┬────────────────┘
          │
    ┌─────┴─────┐
    │           │
 ALTO        BAJO
    │           │
    │           ▼
    │    [Correccion]
    │    - Re-query ( Reescribir )
    │    - Buscar en web (futuro)
    │    - Fallback a contexto general
    │           │
    └─────┬─────┘
          │
          ▼
[Contexto corregido]
         │
         ▼
[LLM genera respuesta]
```

**Parametro clave:** `RAG_CRG_MIN_SCORE` (default: 0.3)

**Implementado en:** `RagQueryService.ask()` - despues de `hybridSearch()`

---

## 3. Self-RAG

El LLM genera tokens de reflexion durante la generacion. Puede pedir mas contexto o auto-evaluar.

```
Pregunta del usuario
         │
         ▼
[Busqueda inicial]
         │
         ▼
[Prompt con contexto + instrucciones de reflexion]
         │
         ▼
[LLM genera con tokens especiales]
┌──────────────────────────────────┐
│ [Retrieve]  → Necesito mas       │
│ [IsRelevant] → ¿Este contexto es │
│                relevante?        │
│ [Support]   → ¿Apoya mi respuesta?│
│ [Utility]   → ¿Es util esta      │
│                respuesta?        │
└──────────────┬───────────────────┘
               │
       ┌───────┴───────┐
       │               │
 [Retrieve]      [Generar]
       │               │
       ▼               ▼
[Busqueda adicional]  [Respuesta]
       │               final
       ▼
[Contexto ampliado]
       │
       ▼
[Generar respuesta final]
```

**Tokens de reflexion:**
- `[Retrieve]` - Solicita buscar mas informacion
- `[IsRelevant]` - Evalua si el contexto es relevante
- `[Support]` - Verifica si el contexto apoya la respuesta
- `[Utility]` - Evalua la utilidad de la respuesta

**Implementado en:** Prompt de `generateAnswer()` en `RagQueryService`

---

## 4. Agentic RAG

Un agente orquesta todo el flujo. Puede iterar, cambiar estrategia, usar herramientas.

```
                   ┌─────────────────────┐
                   │    Agentic RAG      │
                   │  (Orquestador)      │
                   │                     │
                   │ - Evalua respuesta  │
                   │ - Decide iterar     │
                   │ - Cambia estrategia │
                   └──────────┬──────────┘
                              │
                   ┌──────────▼──────────┐
                   │   Decision:         │
                   │   ¿Iterar?          │
                   │   ¿Cambiar metodo?  │
                   │   ¿Parar?           │
                   └──────────┬──────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
   [CRAG]              [Self-RAG]           [Herramientas]
   Evalua calidad      El LLM se auto-      Web search,
   de la busqueda      critica             DB query, etc.
         │                    │
         └────────────────────┴────────────────────┘
                              │
                              ▼
                   [Respuesta final]
```

**Estrategias disponibles:**
- `CRAG` - Evalua calidad de la busqueda
- `SELF_RAG` - Generacion con reflexion
- `RETRIE` - Busqueda directa

**Iteraciones maximas:** Configurable via `RAG_MAX_ITERATIONS` (default: 3)

**Implementado en:** `RagQueryService.agenticRAG()` - envuelve el metodo `ask()`

---

## 5. Combinacion completa

Los cuatro se usan juntos en una cadena con retroalimentacion:

```
Pregunta
   │
   ▼
[Agente] → "Esta pregunta necesita retrieval?"
   │
   ▼
[CRAG] → buscar → evaluar calidad
   ├─ OK → continuar
   └─ LOW → corregir → volver a buscar
   │
   ▼
[Self-RAG] → LLM genera con reflexion
   ├─ [Retrieve] → volver a CRAG
   ├─ [IsRelevant] → evaluar
   └─ [Support] → generar
   │
   ▼
[Agente] → "¿Esta respuesta es buena?"
   ├─ Si → output
   └─ No → reiniciar con otra estrategia
```

---

## Niveles de complejidad

| Nivel | Componentes | Complejidad | ¿Cuando usar? |
|-------|-------------|-------------|---------------|
| 1 | RAG Lineal | Baja | Documentos pequenos, preguntas directas |
| 2 | + CRAG | Media | Cuando la busqueda puede fallar |
| 3 | + Self-RAG | Media-Alta | Cuando el LLM necesita auto-reflexionar |
| 4 | + Agente | Alta | Casos complejos, multiples fuentes, iteracion |

Cada nivel anade una capa sin destruir la anterior.

---

## Parametros configuracion

| Parametro | Default | Descripcion |
|-----------|---------|-------------|
| `RAG_CHUNK_SIZE` | 1000 | Tamano de chunk en tokens |
| `RAG_CHUNK_OVERLAP` | 150 | Overlap entre chunks |
| `RAG_TOP_K` | 5 | Resultados a recuperar |
| `RAG_CRG_MIN_SCORE` | 0.3 | Score minimo CRAG |
| `RAG_MAX_ITERATIONS` | 3 | Maximo iteraciones agente |
| `OLLAMA_TEMPERATURE` | 0.2 | Temperatura del LLM |

---

## Archivos de implementacion

| Componente | Archivo |
|------------|---------|
| RAG Core | `backend/src/main/java/com/localrag/rag/RagQueryService.java` |
| Documentos | `backend/src/main/java/com/localrag/rag/RagDocumentService.java` |
| Chunking | `backend/src/main/java/com/localrag/service/DocumentoService.java` |
| Controlador Chat | `backend/src/main/java/com/localrag/controller/ChatController.java` |
| Controlador Docs | `backend/src/main/java/com/localrag/controller/DocumentosController.java` |
