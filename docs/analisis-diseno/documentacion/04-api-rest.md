# API REST — KoroFin

> **Propósito:** Documentar el diseño de la API REST, los endpoints disponibles y los contratos de entrada/salida.

---

## 1. Base URL

```
Entorno local:  http://localhost:8080/api
Entorno prod:   https://api.finsmart.app/api
```

---

## 2. Autenticación

Todas las rutas excepto `/users/register` y `/users/login` requieren el header:

```
Authorization: Bearer {accessToken}
```

El refresh token se envía como cookie `HttpOnly` en `/users/login` y `/users/refresh`.

---

## 3. Endpoints por Dominio

### 3.1 Usuarios

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/users/register` | Registrar nuevo usuario | No |
| POST | `/users/login` | Iniciar sesión (devuelve JWT + cookie) | No |
| POST | `/users/refresh` | Renovar access token | Cookie |
| POST | `/users/logout` | Cerrar sesión (revoca refresh) | Sí |
| PUT | `/users/profile` | Actualizar nombre/email | Sí |
| PUT | `/users/password` | Cambiar contraseña (requiere actual) | Sí |

**POST /users/register**
```json
// Request
{ "email": "usuario@email.com", "password": "Str0ng!Pass", "name": "Juan Pérez" }

// Response 201
{ "id": 1, "email": "usuario@email.com", "name": "Juan Pérez", "createdAt": "2026-07-05T10:00:00" }
```

**POST /users/login**
```json
// Request
{ "email": "usuario@email.com", "password": "Str0ng!Pass" }

// Response 200
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": 1, "email": "usuario@email.com", "name": "Juan Pérez" }
}
// Set-Cookie: refreshToken=...; HttpOnly; Secure; Path=/api/users/refresh; Max-Age=604800
```

### 3.2 Categorías

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/categories` | Listar categorías del usuario |
| POST | `/categories` | Crear categoría |
| PUT | `/categories/{id}` | Actualizar categoría |
| DELETE | `/categories/{id}` | Eliminar categoría |

```json
// POST /categories
{ "name": "Comida", "type": "EXPENSE", "color": "#10b981", "icon": "utensils" }
```

### 3.3 Ingresos

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/incomes?page=0&size=10&month=7&year=2026&source=salario` | Listar (paginado) |
| POST | `/incomes` | Crear ingreso |
| PUT | `/incomes/{id}` | Actualizar ingreso |
| DELETE | `/incomes/{id}` | Eliminar ingreso |

```json
// POST /incomes
{ "amount": 2500.00, "source": "Salario", "date": "2026-07-01", "categoryId": 5, "description": "Salario julio" }
```

### 3.4 Gastos

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/expenses?page=0&size=10&categoryId=3&startDate=2026-07-01&endDate=2026-07-31` | Listar (paginado) |
| POST | `/expenses` | Crear gasto |
| PUT | `/expenses/{id}` | Actualizar gasto |
| DELETE | `/expenses/{id}` | Eliminar gasto |

```json
// POST /expenses
{ "amount": 45.50, "description": "Cena en restaurant", "date": "2026-07-05", "categoryId": 3, "paymentMethod": "DEBIT_CARD" }
```

### 3.5 Deudas

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/debts` | Listar deudas |
| POST | `/debts` | Crear deuda |
| PUT | `/debts/{id}` | Actualizar deuda (sin tocar abonos) |
| DELETE | `/debts/{id}` | Eliminar deuda |
| POST | `/debts/{id}/payments` | Registrar abono |
| GET | `/debts/{id}/payments` | Historial de abonos |

```json
// POST /debts
{ "name": "Préstamo personal", "totalAmount": 5000.00, "interestRate": 12.5, "dueDate": "2026-12-31" }

// POST /debts/1/payments → Response 201 (crea Expense vinculado)
{ "amount": 500.00, "notes": "Abono julio", "paymentDate": "2026-07-05" }
// Response incluye "expenseId": 42
```

### 3.6 Servicios Recurrentes

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/recurring` | Listar servicios |
| POST | `/recurring` | Crear servicio |
| PUT | `/recurring/{id}` | Actualizar servicio |
| DELETE | `/recurring/{id}` | Eliminar servicio |
| PATCH | `/recurring/{id}/toggle` | Activar/desactivar |
| PATCH | `/recurring/{id}/pay` | Marcar como pagado (crea Expense) |

```json
// POST /recurring
{ "name": "Netflix", "amount": 16.90, "frequency": "MONTHLY", "nextPaymentDate": "2026-07-15" }

// PATCH /recurring/1/pay → Response 200
{ "recurringPayment": { ... }, "expenseId": 43 }
```

### 3.7 Motor Financiero

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/analysis/summary` | Resumen completo del período |
| GET | `/analysis/recommendations` | Recomendaciones y alertas |
| GET | `/analysis/prediction` | Predicción fin de mes |

**GET /analysis/summary** — Resumen con todo en una llamada:
```json
{
  "balance": { "totalIncome": 2500.00, "totalExpense": 1800.00, "balance": 700.00, "month": 7, "year": 2026 },
  "savings": { "monthlySavings": 700.00, "projectedAnnual": 8400.00 },
  "ratios": { "expenseRatio": 72.0, "debtRatio": 35.0 },
  "topCategories": [ { "categoryName": "Comida", "totalAmount": 650.00, "percentage": 36.1 } ],
  "monthlySeries": [ { "month": 2, "year": 2026, "totalIncome": 2400, "totalExpense": 1900 } ],
  "recentTransactions": [ { "type": "EXPENSE", "amount": 45.50, ... } ]
}
```

### 3.8 Asistente IA

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/ai/chat` | Enviar mensaje al asistente |
| GET | `/ai/chat/history` | Historial de conversación |
| GET | `/ai/insights` | Insights generados |
| POST | `/ai/insights/generate` | Generar nuevo insight |
| POST | `/ai/categorize` | Clasificar gasto por IA |
| GET | `/ai/providers/status` | Estado de proveedores (solo lectura) |

```json
// POST /ai/chat
{ "message": "¿En qué me estoy gastando más este mes?" }
// Response 200
{ "reply": "Este mes tu mayor gasto ha sido en Comida con $650 (36% del total).", "provider": "nvidia-nim", "model": "llama-3-70b" }
```

### 3.9 Notificaciones

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/notifications?page=0&size=20` | Listar notificaciones |
| GET | `/notifications/unread-count` | Contador de no leídas |
| PATCH | `/notifications/{id}/read` | Marcar como leída |
| PATCH | `/notifications/read-all` | Marcar todas como leídas |
| GET | `/notifications/preferences` | Obtener preferencias |
| PUT | `/notifications/preferences` | Actualizar preferencias |

### 3.10 Reportes

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/reports/monthly?month=7&year=2026` | Reporte mensual detallado |
| GET | `/reports/movements?month=7&year=2026` | Movimientos del período |
| GET | `/reports/export?month=7&year=2026&format=csv` | Exportar a CSV |

---

## 4. Formato de Respuesta de Error

Todos los errores siguen esta estructura:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "El campo 'amount' es obligatorio y debe ser un número positivo",
  "timestamp": "2026-07-05T10:00:00",
  "path": "/api/expenses",
  "errors": [
    { "field": "amount", "message": "Debe ser un número positivo" }
  ]
}
```

Códigos de estado usados:
- `200` — Éxito
- `201` — Creado
- `204` — Sin contenido (DELETE)
- `400` — Error de validación
- `401` — No autenticado / Token expirado
- `403` — No autorizado
- `404` — Recurso no encontrado
- `409` — Conflicto (email duplicado, etc.)
- `500` — Error interno

---

*Documento de API REST — KoroFin MVP — 34 endpoints*
