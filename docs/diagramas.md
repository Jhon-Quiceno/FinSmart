# Diagramas de arquitectura

Cuatro diagramas del proyecto, pensados para cubrir las vistas que más ayudan a entender el sistema de punta a punta: arquitectura general, modelo de datos, flujo de usuario y un caso de secuencia representativo. Se generaron en tres herramientas distintas (Excalidraw, Miro, Lucid) con el mismo contenido en cada una.

## 1. Arquitectura de alto nivel

Mapa de las capas del sistema: frontend (Next.js), el bot de Telegram vía n8n, los dominios del backend (Spring Boot), la base de datos, y la cadena de failover de proveedores de IA. Marca en naranja/rojo los puntos débiles detectados en la auditoría original (documento ya eliminado tras cerrar los hallazgos — ver `docs/sprints/sprint2.md`, sección "Validación end-to-end y hallazgos"): extracción de PDF sin OCR (sigue así, gap conocido de `extractos/`), y el (entonces) failover de visión inexistente.

> **Desactualizado desde la validación del 2026-07-22** (ver `docs/sprints/sprint2.md`): el catálogo de proveedores creció de 3 a 5 (se sumó **Gemini** y se dejó **Groq** listo sin key), el orden de failover de visión pasó a ser Gemini → NVIDIA → OpenRouter (antes solo NVIDIA), y ahora existe prioridad configurable **por tipo de tarea** (`app.ai.task-priority.*`), no solo un orden global. Los diagramas de abajo no reflejan esto — quedan como snapshot del diseño original; no se regeneraron para no invertir el esfuerzo de las 3 herramientas en un cierre de sprint.

- Excalidraw: https://excalidraw.com/#json=K5flXuqtOtqOAfRov4RDg,nAxTAH_14WmYsFTkWfP9Uw
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ (dentro del board, frame "Arquitectura de alto nivel")
- Lucid: https://lucid.app/lucidchart/ea3efc54-c1ef-46c3-84ad-4bb25b5dcd00/edit

## 2. Modelo Entidad-Relación (ERD)

Las 19 entidades JPA reales del backend (`usuario`, `gastos`, `ingresos`, `deudas`, `tarjetas`, `servicios`, `analisis`, `ia`, `integraciones`) con sus relaciones. Se construyó inventariando las clases `@Entity` del código, no es un diagrama especulativo.

- Excalidraw: https://excalidraw.com/#json=n7LtfSd7rawGu1faXyPHt,UQA76I-g-L5jR7NWvurwqg — versión **simplificada** a mano alzada (solo nombres de entidad y relaciones principales, sin cada atributo, por legibilidad en un lienzo dibujado).
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ (frame "Modelo Entidad-Relación")
- Lucid: https://lucid.app/lucidchart/b34cba53-96e9-43bb-84e6-a9799580f4bc/edit — versión **completa**, ERD real editable con todos los atributos y claves foráneas.

## 3. User flow: registrar un gasto (web vs. Telegram)

Compara los dos caminos que tiene el usuario para registrar un movimiento: formulario manual en el dashboard, o mensaje/foto al bot de Telegram (con el paso de clasificación por IA y validación de monto plausible en el camino de foto).

- Excalidraw: https://excalidraw.com/#json=PrenE04HxiZddxPDCHHg8,RFSrF8FjwnOhcXRIiDZl6A
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ (frame "User flow: registrar un gasto")
- Lucid: https://lucid.app/lucidchart/e3fc4f99-2ad4-48f4-ae00-c8b8bf426f8d/edit

## 4. Secuencia: gasto por foto en Telegram

El caso más específico e interesante del sistema al momento de dibujarlo: Usuario → Telegram → n8n → Backend → `ReceiptExtractionService` → `AiChatOrchestrator` → NVIDIA. En esa versión, marcaba que la llamada de visión no tenía failover (bypassaba el catálogo de proveedores) — hallazgo que ya se corrigió en el sprint 2 (ver `AiChatOrchestrator#completeVision`, ahora itera todos los proveedores con modelo de visión configurado).

- Excalidraw: https://excalidraw.com/#json=aLpDouyNJuPOA6zrjRODr,q9Tf0GAQnd9lWXeaU2F5Pw
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ (frame "Secuencia: gasto por foto en Telegram")
- Lucid: https://lucid.app/lucidchart/679eb329-79c7-4185-a2f0-7b5ee503e614/edit

---

**Nota**: los links de Excalidraw apuntan a una escena pública en excalidraw.com (cualquiera con el link puede ver/editar una copia); los de Lucid y Miro requieren estar logueado con la cuenta donde se crearon (o que se comparta el acceso desde esa cuenta).
