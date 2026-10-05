# MkulimaFeeds — Architecture Deep Dive

This document explains **how** the MkulimaFeeds system is structured, **why** certain design decisions were made, and **how a request flows** from the Android app to the PostgreSQL database and back.

---

## 📑 Table of Contents

- [System Overview](#-system-overview)
- [Technology Choices](#-technology-choices)
- [Request Lifecycle — End to End](#-request-lifecycle--end-to-end)
- [Backend Layering](#-backend-layering)
- [Database Schema](#-database-schema)
- [Authentication & Authorization](#-authentication--authorization)
- [Order State Machine](#-order-state-machine)
- [Inventory Integrity](#-inventory-integrity)
- [OTP Flow — Security First](#-otp-flow--security-first)
- [Settings-Driven Business Rules](#-settings-driven-business-rules)
- [Notifications](#-notifications)
- [File Uploads](#-file-uploads)
- [What's Next (Scalability Notes)](#-whats-next-scalability-notes)

---

## 🏗️ System Overview

MkulimaFeeds is a **four-role mobile commerce platform** for the Kenyan animal-feed industry. It consists of three independently deployable tiers:

```
┌──────────────────────────────────────────────────────────────────┐
│                     Android App (Kotlin, MVVM)                   │
│  Activities · ViewModels · StateFlow · Ktor Client · Material 3  │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              │ HTTPS · JWT Bearer · JSON
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                      Ktor Backend (Kotlin)                       │
│   Routes → Services → Repositories → Exposed ORM → PostgreSQL    │
│   RBAC Guard · JWT Auth · OTP Service · Notifications            │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              │ JDBC
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                           PostgreSQL                             │
│   10 tables · Money as DECIMAL(12,2) · Timestamps as BIGINT (ms) │
└──────────────────────────────────────────────────────────────────┘
```

**Deployment targets:**
- Backend: any container host (Docker Compose provided)
- Database: managed PostgreSQL (or self-hosted)
- Android: Google Play Store (APK/AAB)

---

## 🧩 Technology Choices

### Why Kotlin everywhere?

- **Zero language-switching cost.** Frontend and backend share mental models.
- **Coroutines** work identically on both — the Android app and the Ktor server both use `suspend` functions and structured concurrency.
- **Data classes** map cleanly to serialization on both ends.
- **Null safety** eliminates an entire class of runtime crashes.

### Why Ktor (not Spring Boot)?

- **Lightweight.** Spring Boot's 60+ transitive dependencies are overkill for an API of this size.
- **Coroutine-native.** Controllers are `suspend` functions; no thread-per-request model.
- **Minimal magic.** No annotation processing, no hidden component scanning — the code reads top-to-bottom.
- **Fast cold start** (~1 second vs. Spring's 5-15 seconds) which matters for CI and container scaling.

### Why Exposed (not Hibernate)?

- **SQL-first DSL.** Exposed queries look like SQL: `UsersTable.selectAll().where { UsersTable.email eq "..." }`.
- **No lazy-loading surprises.** N+1 queries are explicit, not hidden behind entity proxies.
- **Type-safe** — misspelled column names fail at compile time.
- **Transaction blocks** are explicit and predictable, which matters for the multi-step order creation flow.

### Why JWT (not sessions)?

- **Stateless** — the backend can be scaled horizontally without sticky sessions.
- **Works naturally with mobile clients** — no cookie jar, just an `Authorization` header.
- **Contains role claim** — RBAC checks don't need a database hit on every request.

---

## 🔄 Request Lifecycle — End to End

Let's trace a **real** request: a customer placing an order from the Android app.

### 1. Android — User taps "Place Order"

**File:** `CheckoutReviewActivity.kt`

```kotlin
btnPayLater.setOnClickListener {
    val request = buildOrderRequest() ?: return@setOnClickListener
    createPayLaterOrder(request)
}
```

The activity builds a `CreateOrderRequest` from cart contents + delivery details, then calls the ViewModel.

### 2. Android — ViewModel → Repository → Ktor Client

**File:** `OrderViewModel.kt` → `OrderRepository.kt` → `ApiService.kt`

```kotlin
suspend fun createOrder(request: CreateOrderRequest, token: String): Order =
    apiService.createOrder(request, token).toOrder()
```

The Ktor `HttpClient` (configured with `ContentNegotiation`, timeouts, and OkHttp engine) serializes the request as JSON and POSTs to:

```
POST http://<host>:8080/api/orders
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{ "telephone": "+254712345678", "location": "...", "items": [...] }
```

### 3. Backend — Route handler receives the request

**File:** `routes/OrderRoutes.kt`

```kotlin
authenticate("auth-jwt") {
    post("/api/orders") {
        val principal = call.principal<JWTPrincipal>() ?: return@post ...
        val customerId = principal.payload.getClaim("userId").asLong()
        val request = call.receive<CreateOrderRequest>()
        val items = request.items.map { OrderItemInput(it.productId, it.quantity) }
        val order = orderService.createOrder(customerId, ..., items)
        call.respond(HttpStatusCode.Created, order.toResponse())
    }
}
```

Ktor's JWT plugin has already:
- Verified the token signature with `JWT_SECRET`
- Checked `exp` (expiry) and `iss` (issuer)
- Attached a `JWTPrincipal` to the call context

### 4. Backend — Service layer applies business rules

**File:** `service/OrderService.kt`

```kotlin
fun createOrder(customerId, telephone, location, neededDate, notes, items): OrderRecord {
    require(items.isNotEmpty()) { "Order must contain at least one product" }

    val orderItems = mutableListOf<CreateOrderItemRecord>()
    var totalAmount = BigDecimal.ZERO

    items.forEach { item ->
        val product = productRepository.findById(item.productId)
            ?: throw NoSuchElementException("Product ${item.productId} not found")

        require(product.isActive) { "Product ${product.name} is not available" }

        val quantity = parseQuantity(item.quantity)
        val subtotal = product.price.multiply(quantity).setScale(2, RoundingMode.HALF_UP)

        orderItems += CreateOrderItemRecord(product.id, quantity, product.price, subtotal)
        totalAmount = totalAmount.add(subtotal)
    }

    return orderRepository.createOrder(customerId, ..., orderItems, totalAmount, "RETAIL")
}
```

Notice: **prices are fetched server-side**. The client never sends prices — this prevents tampering.

### 5. Backend — Repository persists to PostgreSQL

**File:** `repository/OrderRepository.kt`

```kotlin
fun createOrder(...): OrderRecord = transaction {
    val now = System.currentTimeMillis()
    val orderId = OrdersTable.insertAndGetId { ... }.value
    items.forEach { item -> OrderItemsTable.insert { ... } }
    findById(orderId) ?: error("Failed to retrieve newly created order")
}
```

The entire operation runs in **a single Exposed transaction**. If any insert fails, everything rolls back — no orphaned orders.

### 6. Backend — Notification side-effect (non-blocking)

Back in `OrderService.createOrder()`, after the DB write:

```kotlin
try {
    notificationService.createNotification(
        userId = customerId,
        orderId = updatedOrder.id,
        type = "ORDER_STATUS_UPDATE",
        title = "Order Confirmed",
        message = "Your order #${updatedOrder.id} has been received."
    )
} catch (_: Exception) {
    // Order was created; notification failure is non-fatal
}
```

**Design principle:** notifications are best-effort. If the notification insert fails, the order still succeeds.

### 7. Android — Response decoded, UI updated

Back in the ViewModel:

```kotlin
orderViewModel.createOrder(
    request = request,
    token = token,
    onSuccess = { order ->
        runOnUiThread {
            CartManager.clearCart()
            openSuccessScreen(orderId = order.id)
        }
    },
    onError = { error -> /* show toast */ }
)
```

The cart is cleared, the user is navigated to `OrderSuccessActivity`, and the order flow is complete.

**Total latency (measured):** ~150-400ms on localhost, ~800-1500ms on a remote server — dominated by the two DB round-trips (order + items).

---

## 🧱 Backend Layering

The backend follows a strict **four-layer** architecture:

```
┌──────────────────────────────────────────────────────┐
│  routes/          HTTP handling, RBAC checks, JSON   │  Thin
│                   parsing. No business logic.        │
├──────────────────────────────────────────────────────┤
│  service/         Business rules, validation,        │  Thick
│                   orchestration, side-effects.       │
├──────────────────────────────────────────────────────┤
│  repository/      Data access. Exposed queries.      │  Thin
│                   Transactions. No business logic.   │
├──────────────────────────────────────────────────────┤
│  database/tables/ Exposed table definitions.         │  Structural
└──────────────────────────────────────────────────────┘
```

### Rules we enforce

| Layer | Can depend on | Cannot depend on |
|---|---|---|
| Routes | Services | Repositories, Exposed |
| Services | Repositories | Ktor, HTTP, Exposed |
| Repositories | Tables, Exposed | Services, Ktor |
| Tables | Exposed | Anything else |

**Why this matters:** when we later swap PostgreSQL for another DB, only the `repository/` layer changes. When we add a GraphQL layer, only `routes/` changes. Services remain untouched.

### Example of the layering in action

```kotlin
// routes/OrderRoutes.kt — HTTP only
put("/api/admin/orders/{id}/status") {
    RbacGuard.require(principal, Permission.UPDATE_ANY_ORDER_STATUS)
    val status = call.receive<Map<String, String>>()["status"]!!
    call.respond(orderService.updateOrderStatus(id, status).toResponse())
}

// service/OrderService.kt — business rules
fun updateOrderStatus(id: Long, status: String): OrderRecord {
    val order = orderRepository.findById(id) ?: throw NoSuchElementException(...)
    require(isValidTransition(order.status, status)) { "Invalid transition" }
    val updated = orderRepository.updateStatus(id, status)!!
    notifyCustomer(updated)
    return updated
}

// repository/OrderRepository.kt — data access
fun updateStatus(id: Long, status: String): OrderRecord? = transaction {
    OrdersTable.update({ OrdersTable.id eq id }) {
        it[OrdersTable.status] = status
        it[OrdersTable.updatedAt] = System.currentTimeMillis()
    }
    findById(id)
}
```

---

## 🗄️ Database Schema

Ten tables, all using **`LongIdTable`** (auto-incrementing `BIGINT` primary keys).

### Schema diagram

```
users ─────┬───< orders >─────┬───< order_items >───── products
           │                  │
           │                  └───< notifications
           │
           ├───< saved_locations
           │
           ├───< verification_codes
           │
           ├───< role_audit_log
           │
           └───< auth_attempts

settings (standalone)
```

### Table reference

#### `users` — central user table

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `name` | VARCHAR(150) | |
| `phone` | VARCHAR(20) | nullable, unique |
| `email` | VARCHAR(150) | nullable, unique |
| `password_hash` | VARCHAR(255) | bcrypt |
| `role` | VARCHAR(30) | CUSTOMER · DEALER · STAFF · ADMIN |
| `status` | VARCHAR(20) | ACTIVE · PENDING · SUSPENDED · INACTIVE |
| `dealer_status` | VARCHAR(20) | nullable: PENDING · APPROVED · SUSPENDED · REVOKED |
| `is_active` | BOOLEAN | soft-delete flag |
| `created_at` | BIGINT | epoch ms |
| `updated_at` | BIGINT | epoch ms |
| Dealer-only: | | |
| `business_name` | VARCHAR(200) | nullable |
| `business_region` | VARCHAR(100) | nullable |
| `account_manager_name` | VARCHAR(150) | nullable |
| `account_manager_email` | VARCHAR(200) | nullable |
| `credit_limit` | DECIMAL(14,2) | default 0 |
| `credit_used` | DECIMAL(14,2) | default 0 |
| `payment_terms` | VARCHAR(20) | default PREPAID |
| `lifetime_bulk_orders` | INTEGER | default 0 |

**Design note:** `phone` and `email` are nullable with **unique indexes**. This allows "email-only" or "phone-only" signup without the unique constraints colliding on empty strings. This is why the code stores `""` as `NULL`:

```kotlin
val phoneValue: String? = phone.trim().ifBlank { null }
```

#### `products`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `name` | VARCHAR(150) | |
| `description` | TEXT | nullable |
| `category` | VARCHAR(100) | free-text (POULTRY, DAIRY, etc.) |
| `unit` | VARCHAR(30) | KG · BAG · PIECE |
| `price` | DECIMAL(12,2) | |
| `stock_quantity` | DECIMAL(12,2) | default 0 |
| `is_active` | BOOLEAN | soft-delete |
| `created_at`, `updated_at` | BIGINT | |

#### `orders`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `customer_id` | FK → users | |
| `assigned_dealer_id` | FK → users | nullable |
| `telephone` | VARCHAR(20) | delivery contact |
| `location` | VARCHAR(255) | delivery address |
| `needed_date` | BIGINT | epoch ms |
| `status` | VARCHAR(30) | see state machine |
| `notes` | TEXT | nullable |
| `order_type` | VARCHAR(20) | RETAIL · BULK |
| `total_amount` | DECIMAL(12,2) | computed server-side |
| `created_at`, `updated_at` | BIGINT | |

#### `order_items`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `order_id` | FK → orders | |
| `product_id` | FK → products | |
| `quantity` | DECIMAL(12,2) | |
| `unit_price` | DECIMAL(12,2) | frozen at order time |
| `subtotal` | DECIMAL(12,2) | quantity × unit_price |
| `created_at` | BIGINT | |

**Design note:** `unit_price` is **denormalized**. If the product's price changes later, the order history still shows the price the customer paid.

#### `settings`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `key` | VARCHAR(100) | unique, e.g. "bulk_order.min_value" |
| `value` | TEXT | always stored as string |
| `value_type` | VARCHAR(20) | INT · DECIMAL · STRING · BOOLEAN · ENUM |
| `category` | VARCHAR(50) | BULK_ORDER · DEALER · GENERAL · BRANDING |
| `description` | VARCHAR(500) | human-readable |
| `updated_at` | BIGINT | |
| `updated_by_user_id` | BIGINT | nullable — null for seed values |

**Design note:** every value is stored as a **string** with a type tag. This means one table can hold integers, booleans, and enums without schema changes. The service layer parses based on `value_type`.

#### `verification_codes`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `user_id` | BIGINT | nullable (null during register) |
| `purpose` | VARCHAR(30) | REGISTER · PASSWORD_RESET |
| `channel` | VARCHAR(10) | EMAIL · PHONE |
| `destination` | VARCHAR(255) | email or phone the code went to |
| `code_hash` | VARCHAR(255) | bcrypt hash |
| `expires_at` | BIGINT | 10 min after creation |
| `verified_at` | BIGINT | nullable — non-null means consumed |
| `attempts` | INTEGER | default 0, capped at 5 |
| `created_at` | BIGINT | |

#### `auth_attempts`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT (PK) | |
| `identifier` | VARCHAR(255) | e.g. "email:foo@bar.com" |
| `attempt_type` | VARCHAR(30) | OTP_REQUEST · OTP_VERIFY · LOGIN_FAIL |
| `created_at` | BIGINT | |

**Design note:** rate limiting is done by **counting rows** within a time window. This is simpler than maintaining counters and horizontally scalable.

#### `saved_locations`, `role_audit_log`, `notifications`

Standard tables — see `backend/src/main/kotlin/database/tables/` for exact definitions.

### Why timestamps are `BIGINT` (epoch ms)

- **Portable** — same on Java, Kotlin, JS, Swift.
- **Sortable** — numeric comparison, no timezone ambiguity.
- **Convertible** — `Instant.ofEpochMilli(ts).atZone(ZoneId.of("Africa/Nairobi")).toLocalDate()`.

### Why money is `DECIMAL(12,2)`

**Never use floating-point for money.** `0.1 + 0.2` isn't `0.3` in a `Double`. In Kotlin the mapping is `BigDecimal`:

```kotlin
val subtotal = product.price.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
```

---

## 🔐 Authentication & Authorization

### JWT structure

Tokens are issued by `JwtService.generateToken()` after successful login:

```json
{
  "iss": "movofeeds-api",
  "aud": "movofeeds-client",
  "userId": 1,
  "email": "jane@example.com",
  "role": "CUSTOMER",
  "iat": 1727894400,
  "exp": 1727980800
}
```

Signed with HMAC-SHA256 (`JWT_SECRET`), expires in **24 hours**.

### Verification pipeline

Ktor's `Authentication` plugin runs on every request to a protected route:

```kotlin
jwt("auth-jwt") {
    realm = JwtConfig.realm
    verifier(JWT.require(Algorithm.HMAC256(JwtConfig.secret))
        .withIssuer(JwtConfig.issuer)
        .withAudience(JwtConfig.audience)
        .build())
    validate { credential ->
        val userId = credential.payload.getClaim("userId").asLong()
        val role = credential.payload.getClaim("role").asString()
        if (userId != null && !role.isNullOrBlank()) JWTPrincipal(credential.payload) else null
    }
}
```

If validation fails, Ktor returns **401 Unauthorized** before the route handler runs.

### RBAC — the permission engine

Each role is mapped to a **set** of discrete permissions in `RolePermissions.kt`:

```kotlin
"CUSTOMER" to setOf(BROWSE_CATALOG, PLACE_RETAIL_ORDER, VIEW_OWN_ORDERS, ...),
"DEALER"   to setOf(... CUSTOMER's + PLACE_BULK_ORDER, VIEW_ASSIGNED_ORDERS, ...),
"STAFF"    to setOf(... DEALER's + MANAGE_PRODUCTS, VIEW_ANALYTICS, ...),
"ADMIN"    to Permission.values().toSet(),
```

Routes call `RbacGuard.require(principal, Permission.X)` at the top of the handler:

```kotlin
try {
    RbacGuard.require(principal, Permission.MANAGE_PRODUCTS)
} catch (e: IllegalAccessException) {
    return@post call.respond(HttpStatusCode.Forbidden, ...)
}
```

**Design principles:**

1. **Deny by default.** Unknown roles get an empty permission set.
2. **Explicit, not hierarchical.** A role's permissions are self-documenting — no inherited surprises.
3. **Single source of truth.** To change who can do what, edit one file.

### Why not annotations?

Spring's `@PreAuthorize("hasRole('ADMIN')")` is convenient but:
- Reflective — no compile-time check that the permission name is valid.
- Tied to the framework.
- Harder to unit-test in isolation.

`RbacGuard.require()` is a **plain function call** — testable, explicit, and portable.

---

## 🔄 Order State Machine

Orders progress through a strict state machine. Invalid transitions are rejected at both the admin and dealer APIs.

```
                    ┌─────────────┐
                    │   PENDING   │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       ┌─────────────┐           ┌──────────────┐
       │  CONFIRMED  │           │  CANCELLED   │◄─── terminal
       └──────┬──────┘           └──────────────┘
              │                          ▲
              ▼                          │
       ┌─────────────┐                   │
       │ PROCESSING  │───────────────────┤
       └──────┬──────┘                   │
              │                          │
              ▼                          │
       ┌─────────────┐                   │
       │    READY    │───────────────────┘
       └──────┬──────┘
              │
              ▼
       ┌─────────────┐
       │  COMPLETED  │◄─── terminal
       └─────────────┘
```

### Transition rules

| From | Allowed to |
|---|---|
| PENDING | CONFIRMED, CANCELLED |
| CONFIRMED | PROCESSING, CANCELLED |
| PROCESSING | READY, CANCELLED |
| READY | COMPLETED |
| COMPLETED | *(terminal)* |
| CANCELLED | *(terminal)* |

### Role-based restrictions

- **Admin** — can perform any transition.
- **Dealer** — can only advance: `PENDING → CONFIRMED → PROCESSING → READY → COMPLETED`. Cannot cancel.
- **Customer** — can only cancel their own order, and only while `PENDING` or `CONFIRMED`.

### Side effects on transition

- **On CONFIRMED** — inventory is deducted for every item. Fails atomically if stock insufficient.
- **On CANCELLED from CONFIRMED/PROCESSING** — inventory is restored.
- **On any transition** — a notification is sent to the customer (and to the assigned dealer if applicable).

---

## 📦 Inventory Integrity

Stock is adjusted **inside the same transaction** as the status change — so if anything fails, the whole operation rolls back.

### Deduction on CONFIRMED

```kotlin
if (currentStatus != "CONFIRMED" && status == "CONFIRMED") {
    orderItems.forEach { item ->
        val product = ProductsTable.selectAll().where { ProductsTable.id eq item.productId }
            .singleOrNull() ?: error("Product ${item.productId} not found")

        val currentStock = product[ProductsTable.stockQuantity]
        require(currentStock >= item.quantity) {
            "Insufficient stock for ${product[ProductsTable.name]}. " +
            "Available: $currentStock, requested: ${item.quantity}"
        }

        ProductsTable.update({ ProductsTable.id eq item.productId }) {
            it[stockQuantity] = currentStock.subtract(item.quantity)
            it[updatedAt] = System.currentTimeMillis()
        }
    }
}
```

### Restoration on CANCELLED

```kotlin
if (status == "CANCELLED" && (currentStatus == "CONFIRMED" || currentStatus == "PROCESSING")) {
    orderItems.forEach { item ->
        val currentStock = product[ProductsTable.stockQuantity]
        ProductsTable.update(...) {
            it[stockQuantity] = currentStock.add(item.quantity)
        }
    }
}
```

**Why restoration only from CONFIRMED/PROCESSING?**
- If cancelled from PENDING, stock was never deducted — nothing to restore.
- If cancelled from READY, it's too late — the order has already left the warehouse.

---

## 🔐 OTP Flow — Security First

The OTP flow is used for **registration** and **password reset**. It's designed to resist common attacks.

### Send flow

```
1. Check rate limit:       max 3 OTP requests per destination per hour
2. Invalidate previous:    any earlier active code is marked verified
3. Generate:               SecureRandom 6-digit code (not Math.random)
4. Hash:                   bcrypt(cost=12) — never stored plaintext
5. Persist:                insert into verification_codes with expires_at
6. Log attempt:            insert into auth_attempts for rate limiting
7. Send:                   email via Resend, or SMS via Africa's Talking
```

### Verify flow

```
1. Fetch active code:      WHERE destination = ? AND verified_at IS NULL AND expires_at > now
2. Check expiry:           reject if past expires_at
3. Check attempts:         reject if attempts >= 5
4. Compare hash:           bcrypt.verify(input, code_hash)
5. On mismatch:            increment attempts, return remaining count
6. On match:               set verified_at = now, mark code consumed
```

### Attack mitigations

| Attack | Mitigation |
|---|---|
| Brute force | 5 attempts max per code + rate limit |
| Code reuse | Single-use: `verified_at` set on success |
| Stale codes | Previous codes invalidated on new send |
| Enumeration | `/forgot-password/initiate` always returns 200 |
| Code leak | Bcrypt hash in DB, never plaintext logs |
| Time-of-check race | `verifyCode()` runs inside a transaction |

### Why bcrypt for OTPs?

The code space is only **10^6 = 1,000,000**. A fast hash (SHA-256) is cracked in milliseconds. Bcrypt's cost factor 12 (~250ms per hash) makes brute-force impractical even if the DB leaks.

---

## ⚙️ Settings-Driven Business Rules

Business rules are stored in `settings`, editable at runtime without redeploying.

### Defaults (from `SettingsService.DEFAULTS`)

```kotlin
"bulk_order.min_value" to "25000",
"bulk_order.min_qty_per_item" to "100",
"bulk_order.max_qty_per_item" to "5000",
"bulk_order.lead_time_days" to "3",
"dealer.min_completed_orders" to "0",
"dealer.min_account_age_days" to "0",
...
```

### Typed accessors

```kotlin
fun getInt(key: String, default: Int): Int {
    val record = repository.findByKey(key) ?: return default
    return record.value.toIntOrNull() ?: default
}

fun getDecimal(key: String, default: BigDecimal): BigDecimal { ... }
fun getBoolean(key: String, default: Boolean): Boolean { ... }
```

Services call these accessors — they never touch the DB directly for settings.

### Where they're consumed

- **`BulkOrderService`** — validates dealer bulk orders against `bulk_order.*` settings.
- **`RoleService.promoteToDealer()`** — checks `dealer.min_completed_orders` and `dealer.min_account_age_days` before allowing promotion.
- **`UserProfileService.computeDealerInfo()`** — reads tier thresholds (`dealer.tier_gold_threshold`, etc.) to compute dealer tier progress.

### Atomic bulk update

Admin updates run in a **single transaction**:

```kotlin
fun updateAll(changes: List<Pair<String, String>>, adminId: Long): List<SettingRecord> =
    transaction {
        changes.forEach { (key, newValue) ->
            SettingsTable.update({ SettingsTable.key eq key }) {
                it[value] = newValue
                it[updatedAt] = System.currentTimeMillis()
                it[updatedByUserId] = adminId
            }
        }
        changes.mapNotNull { findByKey(it.first) }
    }
```

All validation happens **before** the transaction starts — if any value fails its type check, nothing is written.

---

## 🔔 Notifications

Notifications are created as a **side effect** of order events:

- New order placed → notify customer
- Order status changed → notify customer + assigned dealer
- Dealer assigned → notify the dealer
- Role changed → *(future: notify the affected user)*

### Non-blocking design

```kotlin
try {
    notificationService.createNotification(...)
} catch (_: Exception) {
    // Order update remains successful even if notification fails
}
```

**Why?** Notifications are "nice to have". A transient DB error on the notification insert shouldn't roll back an order status change that the user already sees.

### Future: async notification queue

For production scale, notifications should be dispatched to a background queue (e.g., Kafka, RabbitMQ, or a simple in-process channel). This would:
- Decouple notification latency from API latency
- Allow retries without failing the main operation
- Enable richer types (email, SMS, push) without blocking

The current synchronous design is fine for a single-instance deployment.

---

## 📤 File Uploads

Product images are stored on the **backend filesystem** at `uploads/products/`.

### Upload flow

```
PUT /api/products/{id}/image
Content-Type: multipart/form-data
File field: "file"

1. RbacGuard.require(MANAGE_UPLOADS)
2. Verify product exists
3. Read entire file into memory (part.provider().readRemaining().readBytes())
4. Validate non-empty
5. Ensure upload directory exists and is writable
6. Delete existing images for this product (id.jpg, id.jpeg, id.png, id.webp)
7. Write new bytes to <id>.<extension>
8. Return { imageUrl: "/api/products/{id}/image" }
```

### Why direct write (no temp file + rename)?

Windows holds file handles open when writing, which breaks the classic "write temp → rename" pattern. Direct `writeBytes()` is atomic enough for our purposes and avoids the entire class of rename bugs.

### Retrieval

```
GET /api/products/{id}/image
```

Looks for `id.jpg`, `id.jpeg`, `id.png`, or `id.webp` — whichever exists — and streams it.

### Limitations (future work)

- **No CDN** — images are served from the backend host. For production, use S3/Cloudflare R2 + a CDN.
- **No image optimization** — serve the raw file. Production would generate thumbnails.
- **Local disk** — doesn't survive container restarts. In Docker, this is mounted as a volume (`backend_uploads`).
- **No virus scanning** — deferred.

---

## 🚀 What's Next (Scalability Notes)

The current design is a **single-instance backend** with an **in-memory** store for two things. These are the first things to address for horizontal scaling.

### In-memory storage — the two hot spots

1. **`AuthPendingStorage`** — holds partially-registered users until OTP verification.
    - **Problem:** lost on restart; not shared across instances.
    - **Fix:** back with Redis or a `pending_registrations` table.

2. **`AuthService.resetTokens`** — a `ConcurrentHashMap` of password reset tokens.
    - **Problem:** same as above.
    - **Fix:** Redis with TTL, or a `password_reset_tokens` table with `expires_at`.

Both are flagged in code with clear comments (`// PRODUCTION NOTE: ...`).

### Database connection pooling

Exposed uses HikariCP under the hood, but we haven't tuned pool size. For high traffic:
```kotlin
HikariConfig().apply {
    maximumPoolSize = 20
    minimumIdle = 5
    connectionTimeout = 10_000
}
```

### Rate limiting per IP

Currently, rate limiting is per destination (email/phone). A malicious actor could enumerate many identifiers. Adding IP-based rate limiting (via a Ktor plugin like `RateLimit`) closes that gap.

### Caching

Analytics endpoints recompute everything on every request. A 5-minute cache (Caffeine or Redis) would cut DB load substantially for admin dashboards.

### Observability

Add:
- **Structured JSON logging** (currently plain text via Logback)
- **Request tracing** (`traceId` propagated through logs)
- **Metrics** — Prometheus exporter for request latency, error rates

### Container orchestration

`docker-compose.yml` currently runs a single backend instance. For scale:
- Kubernetes deployment with 3+ replicas
- Nginx or Traefik load balancer
- Managed Postgres (RDS, Cloud SQL, etc.)

None of these are needed for a portfolio demo. They're the natural next step for a real production deployment.

---

## 📚 Further Reading

- [REST API Reference](api.md)
- [README — Getting Started](../README.md#-getting-started)
- [Backend source](../backend/src/main/kotlin)
- [Android source](../android-app/app/src/main/java)

---

*Last updated: 2026-10-05*