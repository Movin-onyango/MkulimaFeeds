# Architecture Deep Dive

## Request Lifecycle — "POST /api/orders"

1. Android taps "Place Order" → `CheckoutReviewActivity` builds a `CreateOrderRequest`
2. `OrderViewModel.createOrder()` → `OrderRepository.createOrder()`
3. Ktor Client POSTs to `http://<host>:8080/api/orders` with `Authorization: Bearer <JWT>`
4. Backend `orderRoutes` receives request → `call.principal<JWTPrincipal>()` extracts user
5. `OrderService.createOrder()` validates input, fetches products, calculates totals
6. `OrderRepository.createOrder()` inserts into `orders` + `order_items` (single transaction)
7. `NotificationService` fires "order created" notification
8. Response returns `OrderResponse` JSON → Android updates UI state

## RBAC Model

Every protected route calls `RbacGuard.require(principal, Permission.X)`.
Permissions are defined in `Permissions.kt` and mapped to roles in `RolePermissions.kt`.

Roles:
- **CUSTOMER** — browse, order, track
- **DEALER** — customer + assigned orders + bulk ordering
- **STAFF** — dealer + product/order management + analytics
- **ADMIN** — full system access

## Order State Machine
PENDING ──► CONFIRMED ──► PROCESSING ──► READY ──► COMPLETED
│ │ │
└──► CANCELLED ◄───────────┘


Stock is decremented when entering CONFIRMED and restored if CANCELLED from CONFIRMED/PROCESSING.