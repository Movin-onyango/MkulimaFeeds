package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

// =========================================================
// ROLE MATRIX
// =========================================================

@Serializable
data class PermissionInfo(
    val name: String,
    val description: String
)

@Serializable
data class RoleInfo(
    val role: String,
    val description: String,
    val permissions: List<String>
)

@Serializable
data class RoleMatrixResponse(
    val roles: List<RoleInfo>,
    val permissions: List<PermissionInfo>
)

// =========================================================
// ROLE CHANGE ACTIONS
// =========================================================

@Serializable
data class ChangeRoleRequest(
    val newRole: String,
    val reason: String? = null
)

@Serializable
data class SuspendUserRequest(
    val reason: String
)

@Serializable
data class PromoteDealerRequest(
    val reason: String? = null
)

@Serializable
data class ReviewDealerApplicationRequest(
    val reason: String? = null
)

// =========================================================
// AUDIT LOG
// =========================================================

@Serializable
data class RoleAuditResponse(
    val id: Long,
    val userId: Long,
    val userName: String,
    val oldRole: String,
    val newRole: String,
    val changedByUserId: Long,
    val changedByName: String,
    val reason: String?,
    val createdAt: Long
)

// =========================================================
// DEALER APPLICATIONS
// =========================================================

@Serializable
data class DealerApplicationResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val status: String,
    val dealerStatus: String,
    val createdAt: Long
)

// =========================================================
// BULK ORDERS
// =========================================================

@Serializable
data class BulkOrderRequest(
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)

@Serializable
data class BulkOrderValidationResponse(
    val valid: Boolean,
    val errors: List<String>,
    val minimumOrderValue: String,
    val minimumQtyPerItem: Int,
    val maximumQtyPerItem: Int,
    val leadTimeDays: Int,
    val paymentTerms: String
)