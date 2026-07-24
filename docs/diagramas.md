# Diagramas de arquitectura

Cuatro diagramas del proyecto, pensados para cubrir las vistas que más ayudan a entender el sistema de punta a punta: arquitectura general, modelo de datos, flujo de usuario y un caso de secuencia representativo. Se generaron originalmente en tres herramientas distintas (Excalidraw, Miro, Lucid) con el mismo contenido en cada una; los diagramas 1, 3 y 4 se actualizaron el 2026-07-23 para reflejar los hallazgos del cierre de sprint 2 (ver `docs/sprints/sprint2.md`, sección "Validación end-to-end y hallazgos"). Cada herramienta permitió un tipo de actualización distinto (ver nota al pie de cada sección) — ya no tienen exactamente el mismo contenido pieza por pieza, pero sí la misma información.

## 1. Arquitectura de alto nivel

Mapa de las capas del sistema: frontend (Next.js), el bot de Telegram vía n8n (hoy solo local, no desplegado en ningún servidor de producción — ver `docs/runbook-produccion.md`), los dominios del backend (Spring Boot), la base de datos, y la cadena de failover de proveedores de IA.

**Actualizado 2026-07-23**: el catálogo de proveedores creció de 3 a 5 — orden global de intento (`AiProviderRegistry.DEFAULT_PRIORITY`): Gemini → NVIDIA → OpenCode → OpenRouter → Groq (Groq catalogado pero sin key real todavía, se marca inactivo). Se agregó la anotación de prioridad configurable **por tipo de tarea** (`app.ai.task-priority.*`), que puede reemplazar el orden global para una sola operación (chat, categorización, insights...) sin afectar a las demás. El failover de visión (solo para fotos de recibo) es un camino más angosto — Gemini → NVIDIA → OpenRouter, los únicos con modelo de visión configurado — y ya se muestra arreglado (antes bypasseaba directo a NVIDIA sin failover real). Sigue marcado en rojo el gap conocido y sin resolver: extracción de PDF de extractos bancarios (`extractos/`) sin OCR, sin quality-gate de texto corrupto, sin verificación cruzada de montos.

- Excalidraw: https://excalidraw.com/#json=6qGU_L58qj82DfqbROGhK,HAgA-z16_bSz8dL_wCC0-g (escena nueva — Excalidraw no permite editar un link ya publicado)
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ — frame original "Arquitectura de alto nivel" **sin tocar** (es un objeto de diagrama generado por IA, opaco para edición vía API) + frame nuevo **"Actualizacion 2026-07-23 - Arquitectura de alto nivel"** al lado, con el contenido de arriba
- Lucid: https://lucid.app/lucidchart/ea3efc54-c1ef-46c3-84ad-4bb25b5dcd00/edit (editado en el mismo documento)

## 2. Modelo Entidad-Relación (ERD)

Las 19 entidades JPA reales del backend (`usuario`, `gastos`, `ingresos`, `deudas`, `tarjetas`, `servicios`, `analisis`, `ia`, `integraciones`) con sus relaciones. Se construyó inventariando las clases `@Entity` del código, no es un diagrama especulativo.

**Verificado 2026-07-23: sigue vigente, no se regeneró.** El conteo de entidades (19) no cambió, y `AiUsageEvent` (la entidad de telemetría de IA) ya estaba en el código desde antes de dibujar este diagrama — no hay nada nuevo que agregar.

- Excalidraw: https://excalidraw.com/#json=n7LtfSd7rawGu1faXyPHt,UQA76I-g-L5jR7NWvurwqg — versión **simplificada** a mano alzada (solo nombres de entidad y relaciones principales, sin cada atributo, por legibilidad en un lienzo dibujado).
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ (frame "Modelo Entidad-Relación")
- Lucid: https://lucid.app/lucidchart/b34cba53-96e9-43bb-84e6-a9799580f4bc/edit — versión **completa**, ERD real editable con todos los atributos y claves foráneas.

## 3. User flow: registrar un gasto (web vs. Telegram)

Compara los dos caminos que tiene el usuario para registrar un movimiento: formulario manual en el dashboard, o mensaje/foto al bot de Telegram (con el paso de clasificación por IA y validación de monto plausible en el camino de foto).

**Actualizado 2026-07-23**: se agregó el tramo final del camino de Telegram — tras registrar el movimiento, cuando el usuario vuelve a la pestaña del navegador, el hook `useRefreshOnFocus` (listener `visibilitychange`/`focus` + poll de respaldo cada 30s, conectado desde `use-analysis`, `use-expenses`, `use-incomes`) invalida el cache y el dashboard/listas se actualizan solos, sin recargar la página a mano (comportamiento anterior).

- Excalidraw: https://excalidraw.com/#json=e3L4vPVkQnq7eZTUfh-X0,OOSMMg1t1Og0WShH4egq3A (escena nueva)
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ — frame original **sin tocar** + frame nuevo **"Actualizacion 2026-07-23 - User flow: registrar un gasto"** al lado
- Lucid: https://lucid.app/lucidchart/e3fc4f99-2ad4-48f4-ae00-c8b8bf426f8d/edit (editado en el mismo documento)

## 4. Secuencia: gasto por foto en Telegram

El caso más específico e interesante del sistema al momento de dibujarlo: Usuario → Telegram → n8n → Backend → `ReceiptExtractionService` → `AiChatOrchestrator` → proveedor de IA con modelo de visión.

**Actualizado 2026-07-23**: la llamada de visión ya no bypassea a un único proveedor — `AiChatOrchestrator#completeVision` itera todos los proveedores habilitados con modelo de visión configurado, en orden Gemini → NVIDIA → OpenRouter (el primero que responda con éxito gana). El orden importa tanto como tener failover: un JSON bien formado pero con datos alucinados (ej. NVIDIA leyendo mal un recibo) es indistinguible de uno correcto a nivel HTTP/parsing, por eso Gemini —el más consistente en pruebas reales— va primero. También se muestra la mejora de prompt en `ReceiptExtractionService` (ignorar texto legal/tributario del recibo — NIT, "Gran Contribuyente", resoluciones DIAN, teléfonos — al identificar el comercio) y una nota sobre el fix de infraestructura de n8n (`N8N_DEFAULT_BINARY_DATA_MODE=default`, necesario para que el nodo que arma el base64 de la foto funcione).

- Excalidraw: https://excalidraw.com/#json=E0J2u4UYcU3egR0SM95UR,Dnoxrd4rbWWYwBPZpjOWWw (escena nueva)
- Miro: https://miro.com/app/board/uXjVH5Hda5c=/ — frame original **sin tocar** + frame nuevo **"Actualizacion 2026-07-23 - Secuencia: gasto por foto en Telegram"** al lado
- Lucid: https://lucid.app/lucidchart/6b050575-211d-41bb-833b-600c59f11a9e/edit — **documento nuevo**, reemplaza al anterior (`679eb329-79c7-4185-a2f0-7b5ee503e614`). El contenido de un diagrama de secuencia UML en Lucid no se pudo editar vía API (no expone los items internos), y tampoco se pudo marcar el documento viejo como obsoleto (403, sin permiso de edición de metadata sobre ese documento) — **queda huérfano y desactualizado**, no lo uses.

---

**Nota**: los links de Excalidraw apuntan a una escena pública en excalidraw.com (cualquiera con el link puede ver/editar una copia); los de Lucid y Miro requieren estar logueado con la cuenta donde se crearon (o que se comparta el acceso desde esa cuenta).
