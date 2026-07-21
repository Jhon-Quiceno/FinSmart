# Bot de Telegram de KoroFin

Registrá gastos desde Telegram escribiendo texto libre (por ejemplo "Uber 15000") y vinculá tu cuenta de KoroFin con un código de un solo uso. El bot corre como un workflow de n8n que llama al backend de KoroFin.

> **El Telegram Trigger de n8n siempre funciona por webhook** (no hace *long-polling*): al activar el workflow, n8n le pide a Telegram que registre una URL propia como webhook, y Telegram exige que esa URL sea **HTTPS pública**. Como n8n corre en `http://localhost:5678`, hace falta un túnel HTTPS hacia tu máquina — el Paso 2.5 explica cómo levantarlo con Cloudflare Tunnel (gratis, sin cuenta).

## Qué hace el bot

- **Vincular cuenta**: enviás `/start <código>` (el código lo generás desde KoroFin → Configuración → Integraciones) y el bot confirma la vinculación de tu chat de Telegram con tu usuario.
- **Registrar gastos o ingresos por texto**: un mensaje libre (ej. `Uber 15000`, `Me pagaron 50000`) se interpreta y se guarda como gasto o ingreso según corresponda.
- **Registrar gastos o ingresos por foto**: mandás una foto de un recibo/factura y el bot lee el monto y el comercio con IA. Si la foto no parece un recibo real, o el monto no es interpretable, no crea nada y te lo avisa.
- **Preguntar por un resumen**: "cuánto gasté en comida", "resumen", "balance" — el bot calcula el total real de tus movimientos (por defecto, del mes en curso) y te responde, sin crear ningún gasto/ingreso nuevo.

> ⚠️ **La rama de fotos (`Descargar foto` → `Armar data URI` → `Registrar recibo`) se validó a nivel backend** (el endpoint `/api/integrations/telegram/receipts` fue probado con imágenes reales) **pero no se pudo probar mandándole una foto real al bot por Telegram** durante el desarrollo — no había un bot creado todavía en esa sesión. Al activar el workflow por primera vez, probá mandarle una foto de un recibo real y confirmá que responde bien antes de confiar en el flujo completo.

## Quick path

1. Crear el bot en BotFather y copiar el token (Paso 1).
2. Definir `TELEGRAM_WEBHOOK_SECRET` en `.env` y levantar `docker compose up -d` (Paso 2).
3. Levantar un túnel HTTPS hacia n8n y apuntarlo con `N8N_HOST`/`N8N_PROTOCOL`/`N8N_WEBHOOK_URL` (Paso 2.5) — obligatorio para que el Telegram Trigger pueda registrar su webhook.
4. Importar `n8n/workflows/telegram-expense-bot.json` en n8n, asignar la credencial de Telegram y activar el workflow (Paso 3).
5. Vincular tu cuenta con `/start <código>` (Paso 4) y probar con `Uber 15000` (Paso 5).

## Paso 1 — Crear el bot con BotFather

Si todavía no tenés un bot de Telegram, se crea en unos minutos:

1. Abrí Telegram y buscá **@BotFather** (el bot oficial para crear bots).
2. Enviale `/newbot`.
3. Elegí un nombre visible (ej. `KoroFin Bot`) y un username que termine en `bot` (ej. `korofin_bot`).
4. BotFather responde con un **token** (formato `123456789:AAExxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`). Copialo y guardalo — lo vas a necesitar en el Paso 3.

Opcional pero recomendado, para que el bot se vea prolijo:

- `/setdescription` → una descripción corta, ej. "Registrá tus gastos y vinculá tu cuenta de KoroFin".
- `/setcommands` → definí el comando de arranque:
  ```
  start - Vincular cuenta
  ```

## Paso 2 — Configurar el secreto compartido

El backend y n8n verifican un secreto compartido (`TELEGRAM_WEBHOOK_SECRET`) en cada llamada, para que nadie más pueda invocar los endpoints de Telegram del backend.

1. Generá un valor aleatorio largo. Cualquiera de estas opciones sirve:
   - Bash / Git Bash: `openssl rand -hex 32`
   - PowerShell: `-join ((1..48) | ForEach-Object {'{0:x}' -f (Get-Random -Maximum 16)})`
2. Agregalo al `.env` de la raíz del repo:
   ```
   TELEGRAM_WEBHOOK_SECRET=<el valor generado>
   ```
3. Levantá (o reiniciá) los contenedores para que backend y n8n lo reciban:
   ```
   docker compose up -d
   ```

> `docker-compose.yml` ya pasa esta variable a los servicios `app` y `n8n`. El workflow la referencia como `{{ $env.TELEGRAM_WEBHOOK_SECRET }}`; no hace falta tocar nada más en n8n.

## Paso 2.5 — Levantar un túnel HTTPS hacia n8n

Telegram solo acepta URLs de webhook con HTTPS. En local, la forma más simple sin registrarse en nada es [Cloudflare Tunnel](https://github.com/cloudflare/cloudflared) en modo *quick tunnel*.

1. Instalar `cloudflared` (una vez):
   ```
   winget install --id Cloudflare.cloudflared -e
   ```
2. Con n8n corriendo (`docker compose up -d n8n`), levantar el túnel apuntando al puerto local:
   ```
   cloudflared tunnel --url http://localhost:5678
   ```
   La consola imprime una URL del tipo `https://algo-al-azar.trycloudflare.com` — copiala.
3. Decirle a n8n cuál es esa URL pública, y reiniciar el contenedor con esas variables:
   ```bash
   export N8N_HOST="algo-al-azar.trycloudflare.com"      # sin https:// ni barra final
   export N8N_PROTOCOL="https"
   export N8N_WEBHOOK_URL="https://algo-al-azar.trycloudflare.com/"  # con barra final
   docker compose up -d n8n
   ```
4. Recién ahora activar (o reactivar) el workflow en n8n — así registra el webhook con la URL correcta.

**Importante**: el *quick tunnel* de Cloudflare genera una URL nueva cada vez que lo reiniciás. Si cerrás `cloudflared` o reiniciás la PC, hay que repetir los pasos 2 y 3 con la URL nueva y volver a activar el workflow (desactivarlo y activarlo de nuevo alcanza, no hace falta reimportarlo). Para una URL fija, se necesita una cuenta de Cloudflare con un dominio propio (`cloudflared tunnel` con túnel nombrado) — fuera del alcance de este setup de desarrollo.

## Paso 3 — Importar el workflow en n8n

1. Abrí `http://localhost:5678`.
2. Andá a **Workflows → Import from File** y seleccioná `n8n/workflows/telegram-expense-bot.json`.
3. Creá la credencial de Telegram:
   - En cualquiera de los nodos **Telegram Trigger** o **Responder**, hacé clic en el selector de credencial → **Create New** → tipo **Telegram API**.
   - Pegá el token de BotFather (Paso 1) y guardá. Nombrala, por ejemplo, "KoroFin Telegram Bot".
   - Asigná esa misma credencial en **ambos** nodos (Telegram Trigger y Responder).
4. Activá el workflow con el switch **Active** de la esquina superior — en este momento n8n registra el webhook con Telegram usando la URL del Paso 2.5. Si no configuraste el túnel primero, este paso falla con "Bad webhook: An HTTPS URL must be provided for webhook" (ver tabla de abajo).

## Paso 4 — Vincular tu cuenta

1. En KoroFin, andá a **Configuración → Integraciones → "Vincular Telegram"** y generá el código de un solo uso.
2. En Telegram, abrí una conversación con tu bot y enviá:
   ```
   /start <código>
   ```
3. El bot responde confirmando la vinculación (o el motivo si el código es inválido).

## Paso 5 — Probar

1. Enviale al bot un mensaje de gasto, ej.:
   ```
   Uber 15000
   ```
2. Deberías recibir una respuesta del tipo "✅ Gasto registrado: ...".
3. Verificá que el gasto aparezca en KoroFin, en **/gastos**.

## Solución de problemas

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| Al activar el workflow: "Bad webhook: An HTTPS URL must be provided for webhook" | n8n todavía no tiene configurada una URL pública HTTPS (falta el Paso 2.5, o el túnel se cayó) | Levantar/relevantar `cloudflared tunnel --url http://localhost:5678`, exportar `N8N_HOST`/`N8N_PROTOCOL`/`N8N_WEBHOOK_URL` con la URL nueva, `docker compose up -d n8n`, y volver a activar el workflow |
| El bot dejó de responder después de reiniciar la PC o `cloudflared` | El *quick tunnel* genera una URL nueva cada vez que se reinicia — la que tenía n8n registrada con Telegram ya no existe | Repetir el Paso 2.5 completo con la URL nueva y reactivar el workflow |
| El backend responde 401 en los nodos HTTP Request | `TELEGRAM_WEBHOOK_SECRET` no coincide entre `app` y `n8n`, o no está definido | Confirmar el valor en `.env`, y que ambos contenedores se reiniciaron después de editarlo (`docker compose up -d`) |
| El bot responde "Código inválido o expirado" | El código ya se usó, venció, o se escribió mal | Generar un código nuevo desde Configuración → Integraciones y reenviar `/start <código>` |
| El bot responde que el chat no está vinculado al registrar un gasto | Todavía no se completó el Paso 4 en este chat | Repetir el Paso 4 antes de enviar gastos |
| El bot no responde nada | El workflow no está activo, o la credencial de Telegram no es válida | Revisar que el switch **Active** esté encendido y que el token en la credencial sea el correcto |
| Ante un error (ej. monto no interpretable) el bot responde el genérico "No se pudo procesar el mensaje" en vez del motivo real | Los nodos **HTTP Request** necesitan `options.response.response.neverError: true` para que un 4xx/5xx del backend llegue como el cuerpo real (`{message: "..."}`) en vez de envolverse en un error de Axios | Ya viene seteado en el workflow exportado; si lo armaste a mano o falta, agregalo en **Confirmar vinculo** y **Registrar gasto** |
| Las respuestas del bot traen agregado "This message was sent automatically with n8n" | Es la firma por defecto del nodo Telegram de n8n. Ojo: `appendAttribution: false` solo no alcanza en algunas versiones de n8n — hay un bug conocido ([n8n-io/n8n#18407](https://github.com/n8n-io/n8n/issues/18407)) donde la desactivación no toma efecto a menos que además se fije `parse_mode` | Ya vienen ambos seteados (`appendAttribution: false` + `parse_mode: "HTML"`) en el nodo **Responder** del workflow exportado |
| Al mandar una foto, el bot responde "No pude leer un recibo en esa imagen" incluso con un recibo real y bien iluminado | El monto leído está fuera del rango plausible configurado en el backend (entre $100 y $500.000.000 COP) — por ejemplo, un recibo en otra moneda con montos nominales chicos | Confirmar que el recibo es en pesos colombianos; si el monto es legítimo y cae fuera de rango, ajustar los límites en `TelegramMessageParser` (backend) |
| Al mandar una foto, el bot no responde nada o tarda mucho | El nodo **Descargar foto** necesita la credencial de Telegram asignada (igual que Telegram Trigger y Responder) para poder descargar el archivo | Verificar que la credencial esté asignada en los 3 nodos que la usan |

## Nota sobre el secreto compartido

Si preferís no depender de `{{ $env.TELEGRAM_WEBHOOK_SECRET }}`, podés reemplazar el valor del header `X-Telegram-Webhook-Secret` por el secreto literal en los nodos **HTTP Request** (`Confirmar vinculo`, `Registrar gasto`, `Registrar recibo`). No es lo recomendado (el secreto queda embebido en el workflow exportado), pero es una alternativa válida si tu instancia de n8n no expone variables de entorno a las expresiones.

## Sobre el archivo del workflow

`telegram-expense-bot.json` fue armado para importarse en n8n 1.x/2.x. Al importarlo puede aparecer un aviso menor de "actualizar versión de nodo" en alguno de los nodos — es esperable y no impide que el workflow funcione; simplemente indica que existe una versión de nodo más nueva que la usada en este export.

El flujo tiene 3 ramas después del Telegram Trigger: **¿es una foto?** (sí → descarga el archivo, lo pasa a base64 y lo manda a `/receipts`; no → sigue al siguiente chequeo), **¿es `/start <código>`?** (sí → vincula la cuenta; no → lo manda como texto libre a `/expenses`, que ahora también responde preguntas de resumen sin crear nada). Las tres ramas convergen en **Componer respuesta** → **Responder**.
