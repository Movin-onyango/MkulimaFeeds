# MkulimaFeeds — REST API Reference

Base URL: `http://localhost:8080` (local) — all paths are relative.

**Authentication:** All authenticated endpoints require a JWT in the header:

```
Authorization: Bearer <token>
```

Tokens are issued on successful login and expire after **24 hours**.

**Content-Type:** All request bodies are `application/json` unless stated otherwise.

---

## 📑 Table of Contents

- [Authentication](#-authentication)
- [Public Branding](#-public-branding)
- [User Profile](#-user-profile)
- [Saved Locations](#-saved-locations)
- [Products](#-products)
- [Customer Orders](#-customer-orders)
- [Admin — Users](#-admin--users)
- [Admin — Orders](#-admin--orders)
- [Admin — Dealers](#-admin--dealers)
- [Admin — Analytics](#-admin--analytics)
- [Admin — Roles & Permissions](#-admin--roles--permissions)
- [Admin — Business Rules](#-admin--business-rules)
- [Admin — Audit Log](#-admin--audit-log)
- [Dealer — Orders](#-dealer--orders)
- [Dealer — Customers](#-dealer--customers)
- [Dealer — Bulk Orders](#-dealer--bulk-orders)
- [Notifications](#-notifications)
- [Health](#-health)
- [Permission Reference](#-permission-reference)

---

## 🔐 Authentication

### POST `/api/auth/login`
Authenticate with email **or** phone number.

**Auth required:** No

**Request:**
```json
{
  "identifier": "user@example.com",
  "password": "your-password"
}
```

**Response** `200 OK`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "user": {
    "id": 1,
    "name": "Jane Farmer",
    "phone": "+254712345678",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "isActive": true,
    "createdAt": 1727894400000,
    "updatedAt": 1727894400000
  }
}
```

**Errors:**
- `400` — Invalid credentials / account suspended / pending

---

### POST `/api/auth/register/initiate`
Start the two-step registration. Sends an OTP to email **or** phone.

**Auth required:** No

**Request:**
```json
{
  "name": "Jane Farmer",
  "email": "jane@example.com",
  "phone": null,
  "password": "secure-password"
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Verification code sent to your email.",
  "channel": "EMAIL",
  "destination": "jane@example.com",
  "expiresInSeconds": 600
}
```

**Errors:**
- `400` — Missing identifier / invalid email / already registered / disposable email

---

### POST `/api/auth/register/verify`
Complete registration by verifying the OTP.

**Auth required:** No

**Request:**
```json
{
  "destination": "jane@example.com",
  "code": "123456"
}
```

**Response** `201 Created`:
```json
{
  "token": "eyJhbGci...",
  "user": { ... }
}
```

**Errors:**
- `400` — Invalid code / expired / no pending registration

---

### POST `/api/auth/forgot-password/initiate`
Start the password reset flow. Sends OTP to the account's email **and** phone (if both exist).

**Auth required:** No

**Request:**
```json
{
  "identifier": "user@example.com"
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "If the account exists, verification codes have been sent.",
  "emailDestination": "j***@example.com",
  "phoneDestination": "+2547***78"
}
```

> Note: The response is always `200` regardless of whether the account exists — this prevents user enumeration.

---

### POST `/api/auth/forgot-password/verify`
Verify codes sent by email and/or phone.

**Auth required:** No

**Request:**
```json
{
  "identifier": "user@example.com",
  "emailCode": "123456",
  "phoneCode": null
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Codes verified. You may now reset your password.",
  "resetToken": "8f3a2c1b-..."
}
```

**Errors:**
- `400` — Invalid or missing code

---

### POST `/api/auth/forgot-password/reset`
Reset the password using the token from the previous step.

**Auth required:** No

**Request:**
```json
{
  "identifier": "user@example.com",
  "resetToken": "8f3a2c1b-...",
  "newPassword": "new-secure-password"
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Password has been reset successfully."
}
```

---

### GET `/api/auth/me`
Return the authenticated user's identity.

**Auth required:** Yes

**Response** `200 OK`:
```json
{
  "status": "OK",
  "userId": 1,
  "email": "user@example.com",
  "role": "CUSTOMER"
}
```

---

### PUT `/api/auth/change-password`
Change the authenticated user's password.

**Auth required:** Yes

**Request:**
```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Password changed successfully"
}
```

**Errors:**
- `400` — Incorrect current password / new password too short / same as current

---

## 🎨 Public Branding

### GET `/api/public/branding`
Return public branding settings (app name, support contacts, URLs).

**Auth required:** No

**Response** `200 OK`:
```json
{
  "branding.app_name": "MkulimaFeeds",
  "branding.company_name": "MkulimaFeeds Ltd",
  "branding.support_email": "support@mkulimafeeds.co.ke",
  "branding.support_phone": "+254700000000",
  "branding.whatsapp_number": "254700000000",
  "branding.website_url": "https://mkulimafeeds.co.ke",
  "branding.terms_url": "https://mkulimafeeds.co.ke/terms",
  "branding.privacy_url": "https://mkulimafeeds.co.ke/privacy",
  "branding.about_text": "...",
  "branding.version_label": "1.0.0"
}
```

---

## 👤 User Profile

### GET `/api/users/me`
Get the authenticated user's basic record.

**Auth required:** Yes

**Response** `200 OK`:
```json
{
  "status": "OK",
  "user": { ... }
}
```

---

### GET `/api/users/me/profile`
Get the full profile summary including stats, tier, and dealer info.

**Auth required:** Yes

**Response** `200 OK`:
```json
{
  "user": {
    "id": 1,
    "name": "Jane Farmer",
    "email": "jane@example.com",
    "phone": "+254712345678",
    "role": "CUSTOMER",
    "isActive": true,
    "memberSince": 1727894400000
  },
  "stats": {
    "totalOrders": 10,
    "completedOrders": 5,
    "totalSpent": "68000.00",
    "tier": "REGULAR"
  },
  "dealer": null
}
```

For a dealer, `dealer` is populated:
```json
"dealer": {
  "businessName": "Movo Distributors Ltd",
  "businessRegion": "Nairobi",
  "accountManagerName": "Sarah W.",
  "accountManagerEmail": "sarah@mkulimafeeds.co.ke",
  "currentTier": "SILVER",
  "nextTier": "GOLD",
  "ordersToNextTier": 260,
  "tierProgressPercent": 48,
  "creditLimit": "500000.00",
  "creditUsed": "150000.00",
  "creditAvailable": "350000.00",
  "paymentTerms": "NET_30",
  "lifetimeBulkOrders": 240
}
```

---

### PUT `/api/users/me`
Update the authenticated user's name, email, and phone.

**Auth required:** Yes

**Request:**
```json
{
  "name": "Jane K. Farmer",
  "email": "jane@example.com",
  "phone": "+254712345678"
}
```

**Errors:**
- `400` — Name too short / email or phone already in use

---

### POST `/api/users/me/delete`
Soft-delete the authenticated account. Requires password confirmation.

**Auth required:** Yes

**Request:**
```json
{
  "password": "your-password",
  "confirmation": "DELETE"
}
```

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Account deleted"
}
```

---

## 📍 Saved Locations

### GET `/api/users/me/locations`
List all saved delivery locations for the user.

**Auth required:** Yes

**Response** `200 OK`:
```json
[
  {
    "id": 1,
    "label": "Home",
    "address": "Kikuyu, Kiambu County",
    "isDefault": true,
    "createdAt": 1727894400000
  }
]
```

---

### POST `/api/users/me/locations`
Create a new saved location.

**Auth required:** Yes

**Request:**
```json
{
  "label": "Farm",
  "address": "Rware, Nyeri",
  "isDefault": false
}
```

**Response** `201 Created`:
```json
{
  "id": 2,
  "label": "Farm",
  "address": "Rware, Nyeri",
  "isDefault": false,
  "createdAt": 1727980800000
}
```

---

### PUT `/api/users/me/locations/{id}`
Update a saved location. Ownership is verified.

**Auth required:** Yes

**Request:** Same shape as create.

---

### DELETE `/api/users/me/locations/{id}`
Delete a saved location.

**Auth required:** Yes

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Location deleted"
}
```

---

## 🛍️ Products

### GET `/api/products`
List all active products. Public catalog — no auth required.

**Auth required:** No

**Response** `200 OK`:
```json
[
  {
    "id": 1,
    "name": "Layers Mash",
    "description": "High-quality poultry feed",
    "category": "POULTRY",
    "unit": "KG",
    "price": "2750.00",
    "stockQuantity": "100.00",
    "isActive": true,
    "createdAt": 1727894400000,
    "updatedAt": 1727894400000,
    "imageUrl": "/api/products/1/image"
  }
]
```

---

### GET `/api/products/{id}`
Get a single product by ID.

**Auth required:** No

---

### GET `/api/products/{id}/image`
Stream the product image file (JPG/PNG/WEBP).

**Auth required:** No

**Response:** Binary image data.

---

### POST `/api/products`
Create a new product.

**Auth required:** Yes — requires `MANAGE_PRODUCTS`

**Request:**
```json
{
  "name": "Chick Mash",
  "description": "Starter feed for chicks",
  "category": "POULTRY",
  "unit": "KG",
  "price": "2800.00",
  "stockQuantity": "150"
}
```

**Response** `201 Created` — ProductResponse.

---

### PUT `/api/products/{id}`
Update an existing product.

**Auth required:** Yes — requires `MANAGE_PRODUCTS`

**Request:** Partial update — all fields optional.

---

### PUT `/api/products/{id}/image`
Upload or replace a product image. `multipart/form-data` with a `file` field.

**Auth required:** Yes — requires `MANAGE_UPLOADS`

**Response** `200 OK`:
```json
{
  "status": "OK",
  "message": "Product image uploaded successfully",
  "imageUrl": "/api/products/1/image"
}
```

**Constraints:**
- Max size: 20 MB
- Allowed extensions: `.jpg`, `.jpeg`, `.png`, `.webp`

---

### DELETE `/api/products/{id}`
Soft-delete (deactivate) a product.

**Auth required:** Yes — requires `MANAGE_PRODUCTS`

---

## 🧾 Customer Orders

### POST `/api/orders`
Place a new retail order.

**Auth required:** Yes — requires `PLACE_RETAIL_ORDER`

**Request:**
```json
{
  "telephone": "+254712345678",
  "location": "Greenhouse Office Park, Ngong Road",
  "neededDate": 1730419200000,
  "notes": "Please deliver before 10 AM",
  "items": [
    { "productId": 1, "quantity": "5" },
    { "productId": 2, "quantity": "2" }
  ]
}
```

**Response** `201 Created` — OrderResponse with computed totals and items.

**Errors:**
- `400` — Empty cart / invalid quantity / product not active
- `404` — Product not found

---

### GET `/api/orders`
List the authenticated customer's own orders.

**Auth required:** Yes — requires `VIEW_OWN_ORDERS`

**Response:** Array of OrderResponse.

---

### GET `/api/orders/{id}`
Get a specific order. Customers can only access their own.

**Auth required:** Yes

**Errors:**
- `403` — Not your order
- `404` — Order not found

---

### PUT `/api/orders/{id}/cancel`
Cancel an order. Only PENDING or CONFIRMED orders can be cancelled.

**Auth required:** Yes — requires `CANCEL_OWN_ORDER`

**Errors:**
- `400` — Order is not cancellable in its current state
- `403` — Not your order

---

## 🛡️ Admin — Users

### GET `/api/admin/users`
List all users in the system.

**Auth required:** Yes — requires `VIEW_ALL_USERS`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Jane Farmer",
    "phone": "+254712345678",
    "email": "jane@example.com",
    "role": "CUSTOMER",
    "status": "ACTIVE",
    "dealerStatus": null,
    "isActive": true,
    "createdAt": 1727894400000,
    "updatedAt": 1727894400000,
    "totalOrders": 10
  }
]
```

---

### GET `/api/admin/users/{id}`
Get a single user.

**Auth required:** Yes — requires `VIEW_ALL_USERS`

---

### POST `/api/admin/users`
Create a new user (staff-managed).

**Auth required:** Yes — requires `CREATE_STAFF`

**Request:**
```json
{
  "name": "New User",
  "phone": "+254712345678",
  "email": "new@example.com",
  "password": "secure-password",
  "role": "CUSTOMER"
}
```

---

### PUT `/api/admin/users/{id}`
Update a user's profile fields.

**Auth required:** Yes — requires `CHANGE_USER_ROLES`

---

### PUT `/api/admin/users/{id}/deactivate`
Soft-delete (deactivate) a user.

**Auth required:** Yes — requires `DEACTIVATE_USERS`

---

## 🛡️ Admin — Orders

### GET `/api/admin/orders`
List every order in the system.

**Auth required:** Yes — requires `VIEW_ALL_ORDERS`

---

### GET `/api/admin/orders/{id}`
Get a single order with full details.

**Auth required:** Yes — requires `VIEW_ALL_ORDERS`

---

### PUT `/api/admin/orders/{id}/status`
Update an order's status. Validates the state transition.

**Auth required:** Yes — requires `UPDATE_ANY_ORDER_STATUS`

**Request:**
```json
{ "status": "CONFIRMED" }
```

**Valid transitions:**
```
PENDING     → CONFIRMED | CANCELLED
CONFIRMED   → PROCESSING | CANCELLED
PROCESSING  → READY | CANCELLED
READY       → COMPLETED
COMPLETED   → (terminal)
CANCELLED   → (terminal)
```

**Side effects:**
- On `CONFIRMED`: product stock is decremented (fails if insufficient)
- On `CANCELLED` from CONFIRMED/PROCESSING: stock is restored
- A notification is sent to the customer and the assigned dealer

---

### PUT `/api/admin/orders/{id}/assign-dealer`
Assign a dealer to an order.

**Auth required:** Yes — requires `ASSIGN_DEALERS_TO_ORDERS`

**Request:**
```json
{ "dealerId": 5 }
```

**Side effects:** Notification sent to the assigned dealer.

---

## 🛡️ Admin — Dealers

### GET `/api/admin/dealers`
List all active dealers.

**Auth required:** Yes — requires `VIEW_ALL_USERS`

---

## 📊 Admin — Analytics

### GET `/api/admin/analytics`
Full business intelligence payload for the admin dashboard.

**Auth required:** Yes — requires `VIEW_ANALYTICS`

**Response:**
```json
{
  "today": { "period": "Today", "revenue": "15000.00", "growth": 12.5 },
  "weekly": { "period": "Weekly", "revenue": "82000.00", "growth": -3.2 },
  "monthly": { "period": "Monthly", "revenue": "340000.00", "growth": 18.7 },
  "customerGrowth": {
    "newCustomers": 24,
    "totalCustomers": 380,
    "percentage": 6.7
  },
  "topProducts": [ ... ],
  "regionalDemand": [ ... ],
  "dealerPerformance": [ ... ],
  "revenueTrends": [ ... ],
  "dailyOrders": [ ... ],
  "categoryBreakdown": [ ... ]
}
```

---

### GET `/api/admin/customers/analytics`
Detailed customer analytics: tiers, signup trend, top customers.

**Auth required:** Yes — requires a management role

---

## 🔑 Admin — Roles & Permissions

### GET `/api/admin/roles`
Return the role matrix — every role and its permissions.

**Auth required:** Yes — requires `VIEW_ALL_USERS`

**Response:**
```json
{
  "roles": [
    {
      "role": "CUSTOMER",
      "description": "Retail buyer...",
      "permissions": ["BROWSE_CATALOG", "PLACE_RETAIL_ORDER", ...]
    }
  ],
  "permissions": [
    { "name": "MANAGE_PRODUCTS", "description": "Manage products" }
  ]
}
```

---

### POST `/api/admin/users/{id}/promote`
Promote a customer to dealer.

**Auth required:** Yes — requires `PROMOTE_DEALERS`

**Request:** `{ "reason": "Approved for Nairobi region" }`

---

### POST `/api/admin/users/{id}/demote`
Demote a dealer to customer.

**Auth required:** Yes — requires `DEMOTE_DEALERS`

---

### POST `/api/admin/users/{id}/suspend`
Suspend a user. Blocks login.

**Auth required:** Yes — requires `SUSPEND_USERS`

**Request:** `{ "reason": "Violation of terms" }` — reason required.

---

### POST `/api/admin/users/{id}/reactivate`
Reactivate a suspended or inactive user.

**Auth required:** Yes — requires `SUSPEND_USERS`

---

### GET `/api/admin/dealer-applications`
List pending dealer applications.

**Auth required:** Yes — requires `PROMOTE_DEALERS`

---

### POST `/api/admin/dealer-applications/{id}/approve`
Approve a dealer application (bypasses promotion minimums).

**Auth required:** Yes — requires `PROMOTE_DEALERS`

---

### POST `/api/admin/dealer-applications/{id}/reject`
Reject a dealer application.

**Auth required:** Yes — requires `PROMOTE_DEALERS`

---

## ⚙️ Admin — Business Rules

### GET `/api/admin/settings`
Get all settings.

**Auth required:** Yes — requires `MANAGE_SETTINGS`

**Response:**
```json
[
  {
    "key": "bulk_order.min_value",
    "value": "25000",
    "valueType": "DECIMAL",
    "category": "BULK_ORDER",
    "description": "Minimum total value for bulk orders",
    "enumOptions": null,
    "updatedAt": 1727894400000
  }
]
```

---

### GET `/api/admin/settings/{category}`
Get settings in a specific category.

Categories: `BULK_ORDER`, `DEALER`, `GENERAL`, `BRANDING`

**Auth required:** Yes — requires `MANAGE_SETTINGS`

---

### PUT `/api/admin/settings`
Bulk-update multiple settings atomically.

**Auth required:** Yes — requires `MANAGE_SETTINGS`

**Request:**
```json
{
  "changes": [
    { "key": "bulk_order.min_value", "value": "30000" },
    { "key": "dealer.min_completed_orders", "value": "5" }
  ]
}
```

**Response:**
```json
{
  "status": "OK",
  "message": "Settings updated",
  "updated": [ ... ]
}
```

All values are validated against their declared `valueType` before being persisted.

---

### POST `/api/admin/settings/reset`
Reset all settings to factory defaults.

**Auth required:** Yes — requires a management role

---

## 📜 Admin — Audit Log

### GET `/api/admin/audit-log?limit=100`
Recent role-change history.

**Auth required:** Yes — requires `VIEW_AUDIT_LOG`

**Query params:**
- `limit` (optional, default 100, max 500)

**Response:**
```json
[
  {
    "id": 42,
    "userId": 15,
    "userName": "Jane Farmer",
    "oldRole": "CUSTOMER",
    "newRole": "DEALER",
    "changedByUserId": 1,
    "changedByName": "Admin User",
    "reason": "Approved dealer application",
    "createdAt": 1727894400000
  }
]
```

---

## 🚚 Dealer — Orders

### GET `/api/dealer/orders`
List orders assigned to the authenticated dealer.

**Auth required:** Yes — requires `VIEW_ASSIGNED_ORDERS`

---

### GET `/api/dealer/orders/{id}`
Get a single order. Dealers can only access orders assigned to them.

**Auth required:** Yes — requires `VIEW_ASSIGNED_ORDERS`

**Errors:**
- `403` — Not assigned to you
- `404` — Order not found

---

### PUT `/api/dealer/orders/{id}/status`
Update an assigned order's status.

**Auth required:** Yes — requires `UPDATE_ASSIGNED_ORDER_STATUS`

**Request:** `{ "status": "PROCESSING" }`

**Dealer-allowed transitions:**
```
PENDING     → CONFIRMED
CONFIRMED   → PROCESSING
PROCESSING  → READY
READY       → COMPLETED
```

**Side effects:** Customer receives a status-update notification.

---

## 👥 Dealer — Customers

### GET `/api/dealer/customers`
List customers who have orders assigned to this dealer.

**Auth required:** Yes — requires `VIEW_ASSIGNED_CUSTOMERS`

**Response:**
```json
[
  {
    "id": 8,
    "name": "Test Customer",
    "phone": "+254712345678",
    "email": "customer@example.com",
    "totalOrders": 3,
    "activeOrders": 1,
    "completedOrders": 2
  }
]
```

---

## 📦 Dealer — Bulk Orders

### POST `/api/dealer/orders/bulk/validate`
Dry-run validation of a bulk order against business rules.

**Auth required:** Yes — requires `PLACE_BULK_ORDER`

**Response:**
```json
{
  "valid": true,
  "errors": [],
  "minimumOrderValue": "25000.00",
  "minimumQtyPerItem": 100,
  "maximumQtyPerItem": 5000,
  "leadTimeDays": 3,
  "paymentTerms": "PREPAID"
}
```

---

### POST `/api/dealer/orders/bulk`
Create a bulk order. Validates against settings (min value, quantity limits, lead time).

**Auth required:** Yes — requires `PLACE_BULK_ORDER`

**Errors:**
- `400` — Validation failed (see `errors[]` in response)

---

### GET `/api/dealer/orders/bulk`
List the dealer's own bulk orders.

**Auth required:** Yes — requires `PLACE_BULK_ORDER`

---

## 🔔 Notifications

### GET `/api/notifications`
List the authenticated user's notifications, newest first.

**Auth required:** Yes

**Response:**
```json
[
  {
    "id": 101,
    "userId": 1,
    "orderId": 12,
    "type": "ORDER_STATUS_UPDATE",
    "title": "Order Status Updated",
    "message": "Your order #12 is now CONFIRMED.",
    "isRead": false,
    "createdAt": 1727894400000
  }
]
```

---

### PUT `/api/notifications/{id}/read`
Mark a single notification as read.

**Auth required:** Yes

---

### PUT `/api/notifications/read-all`
Mark all notifications as read.

**Auth required:** Yes

---

## ❤️ Health

### GET `/api/health`
Simple liveness check for monitoring.

**Auth required:** No

**Response** `200 OK`:
```json
{
  "status": "OK",
  "service": "MovoFeeds Backend",
  "message": "Backend is running"
}
```

---

## 🔑 Permission Reference

Every permission is enforced server-side via `RbacGuard.require(principal, Permission.X)`.
Unknown roles default to **no permissions** (deny by default).

| Permission | Customer | Dealer | Staff | Admin |
|---|:---:|:---:|:---:|:---:|
| `BROWSE_CATALOG` | ✅ | ✅ | ✅ | ✅ |
| `PLACE_RETAIL_ORDER` | ✅ | ✅ | ✅ | ✅ |
| `VIEW_OWN_ORDERS` | ✅ | ✅ | ✅ | ✅ |
| `CANCEL_OWN_ORDER` | ✅ | ✅ | ✅ | ✅ |
| `VIEW_OWN_PROFILE` | ✅ | ✅ | ✅ | ✅ |
| `EDIT_OWN_PROFILE` | ✅ | ✅ | ✅ | ✅ |
| `MANAGE_OWN_LOCATIONS` | ✅ | ✅ | ✅ | ✅ |
| `DELETE_OWN_ACCOUNT` | ✅ | ✅ | ✅ | ✅ |
| `PLACE_BULK_ORDER` | | ✅ | | ✅ |
| `VIEW_DEALER_PRICING` | | ✅ | | ✅ |
| `VIEW_ASSIGNED_ORDERS` | | ✅ | | ✅ |
| `UPDATE_ASSIGNED_ORDER_STATUS` | | ✅ | | ✅ |
| `VIEW_ASSIGNED_CUSTOMERS` | | ✅ | | ✅ |
| `VIEW_ALL_ORDERS` | | | ✅ | ✅ |
| `UPDATE_ANY_ORDER_STATUS` | | | ✅ | ✅ |
| `ASSIGN_DEALERS_TO_ORDERS` | | | ✅ | ✅ |
| `MANAGE_PRODUCTS` | | | ✅ | ✅ |
| `MANAGE_UPLOADS` | | | ✅ | ✅ |
| `VIEW_ANALYTICS` | | | ✅ | ✅ |
| `VIEW_ALL_USERS` | | | ✅ | ✅ |
| `MANAGE_SETTINGS` | | | | ✅ |
| `PROMOTE_DEALERS` | | | | ✅ |
| `DEMOTE_DEALERS` | | | | ✅ |
| `CHANGE_USER_ROLES` | | | | ✅ |
| `SUSPEND_USERS` | | | | ✅ |
| `DEACTIVATE_USERS` | | | | ✅ |
| `CREATE_STAFF` | | | | ✅ |
| `VIEW_AUDIT_LOG` | | | | ✅ |

---

## 🚨 Error Response Format

All errors follow the same shape:

```json
{
  "status": "ERROR",
  "message": "Human-readable error message"
}
```

**Standard HTTP codes used:**

| Code | Meaning |
|---|---|
| `200` | Success |
| `201` | Created |
| `400` | Bad request — invalid input or business-rule violation |
| `401` | Unauthorized — missing or invalid JWT |
| `403` | Forbidden — authenticated but lacking permission |
| `404` | Not found |
| `500` | Server error |

---

*Last updated: 2026-10-05*