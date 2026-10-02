package com.movofeeds.service

import com.movofeeds.models.DealerApplicationResponse
import com.movofeeds.models.PermissionInfo
import com.movofeeds.models.RoleAuditResponse
import com.movofeeds.models.RoleInfo
import com.movofeeds.models.RoleMatrixResponse
import com.movofeeds.repository.RoleAuditLogRepository
import com.movofeeds.repository.RoleAuditRecord
import com.movofeeds.repository.UserRecord
import com.movofeeds.repository.UserRepository
import com.movofeeds.security.Permission
import com.movofeeds.security.RolePermissions

class RoleService(
    private val userRepository: UserRepository = UserRepository(),
    private val auditRepository: RoleAuditLogRepository = RoleAuditLogRepository(),
    private val settingsService: SettingsService = SettingsService()
) {

    // =========================================================
    // ROLE MATRIX (for admin UI)
    // =========================================================

    fun getRoleMatrix(): RoleMatrixResponse {
        val roleDescriptions = mapOf(
            "CUSTOMER" to "Retail buyer. Can browse catalog and place retail orders.",
            "DEALER" to "Wholesale buyer and fulfiller. Can place bulk orders and manage assigned orders.",
            "STAFF" to "Internal operations. Can manage products, orders, and view analytics.",
            "ADMIN" to "Full system access. Can manage users, roles, and business rules."
        )

        val roles = RolePermissions.knownRoles().map { role ->
            RoleInfo(
                role = role,
                description = roleDescriptions[role] ?: "",
                permissions = RolePermissions
                    .permissionsFor(role)
                    .map { it.name }
                    .sorted()
            )
        }.sortedBy { it.role }

        val permissions = Permission.values().map {
            PermissionInfo(
                name = it.name,
                description = describePermission(it)
            )
        }.sortedBy { it.name }

        return RoleMatrixResponse(
            roles = roles,
            permissions = permissions
        )
    }

    private fun describePermission(permission: Permission): String =
        when (permission) {
            Permission.BROWSE_CATALOG -> "Browse products"
            Permission.PLACE_RETAIL_ORDER -> "Place retail orders"
            Permission.VIEW_OWN_ORDERS -> "View own orders"
            Permission.CANCEL_OWN_ORDER -> "Cancel own orders"
            Permission.VIEW_OWN_PROFILE -> "View own profile"
            Permission.EDIT_OWN_PROFILE -> "Edit own profile"
            Permission.MANAGE_OWN_LOCATIONS -> "Manage saved locations"
            Permission.DELETE_OWN_ACCOUNT -> "Delete own account"
            Permission.PLACE_BULK_ORDER -> "Place bulk orders"
            Permission.VIEW_DEALER_PRICING -> "View dealer pricing"
            Permission.VIEW_ASSIGNED_ORDERS -> "View assigned orders"
            Permission.UPDATE_ASSIGNED_ORDER_STATUS -> "Update assigned order status"
            Permission.VIEW_ASSIGNED_CUSTOMERS -> "View assigned customers"
            Permission.VIEW_ALL_ORDERS -> "View all orders"
            Permission.UPDATE_ANY_ORDER_STATUS -> "Update any order status"
            Permission.ASSIGN_DEALERS_TO_ORDERS -> "Assign dealers to orders"
            Permission.MANAGE_PRODUCTS -> "Manage products"
            Permission.MANAGE_UPLOADS -> "Upload product images"
            Permission.VIEW_ANALYTICS -> "View analytics"
            Permission.VIEW_ALL_USERS -> "View all users"
            Permission.MANAGE_SETTINGS -> "Manage business rules"
            Permission.PROMOTE_DEALERS -> "Promote customers to dealers"
            Permission.DEMOTE_DEALERS -> "Demote dealers to customers"
            Permission.CHANGE_USER_ROLES -> "Change any user's role"
            Permission.SUSPEND_USERS -> "Suspend users"
            Permission.DEACTIVATE_USERS -> "Deactivate users"
            Permission.CREATE_STAFF -> "Create staff accounts"
            Permission.VIEW_AUDIT_LOG -> "View role audit log"
        }

    // =========================================================
    // PROMOTE CUSTOMER → DEALER
    // =========================================================

    fun promoteToDealer(
        userId: Long,
        adminId: Long,
        reason: String?
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        // Preconditions
        if (user.role.equals("DEALER", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("User is already a dealer")
            )
        }
        if (user.role.equals("ADMIN", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Cannot demote an admin to dealer")
            )
        }
        if (!user.isActive || user.status.equals("SUSPENDED", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Cannot promote a suspended or inactive user")
            )
        }

        // Check configurable minimums
        val minCompletedOrders = settingsService.getInt(
            "dealer.min_completed_orders",
            0
        )
        val minAccountAgeDays = settingsService.getInt(
            "dealer.min_account_age_days",
            0
        )

        val completedOrders = try {
            com.movofeeds.repository.OrderRepository()
                .countByCustomerId(userId)
        } catch (_: Exception) { 0 }

        if (completedOrders < minCompletedOrders) {
            return Result.failure(
                IllegalArgumentException(
                    "User has $completedOrders completed orders, " +
                            "minimum required is $minCompletedOrders"
                )
            )
        }

        val accountAgeDays = (
                (System.currentTimeMillis() - user.createdAt) /
                        (24L * 60L * 60L * 1000L)
                ).toInt()

        if (accountAgeDays < minAccountAgeDays) {
            return Result.failure(
                IllegalArgumentException(
                    "Account is $accountAgeDays days old, " +
                            "minimum required is $minAccountAgeDays"
                )
            )
        }

        // Perform the change
        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = "DEALER",
            newStatus = "ACTIVE",
            newDealerStatus = "APPROVED"
        ) ?: return Result.failure(
            IllegalStateException("Failed to promote user")
        )

        // Audit
        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = "DEALER",
            changedByUserId = adminId,
            reason = reason ?: "Promoted to dealer"
        )

        return Result.success(updated)
    }

    // =========================================================
    // DEMOTE DEALER → CUSTOMER
    // =========================================================

    fun demoteToCustomer(
        userId: Long,
        adminId: Long,
        reason: String?
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (!user.role.equals("DEALER", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("User is not a dealer")
            )
        }

        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = "CUSTOMER",
            newStatus = "ACTIVE",
            newDealerStatus = "REVOKED"
        ) ?: return Result.failure(
            IllegalStateException("Failed to demote user")
        )

        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = "CUSTOMER",
            changedByUserId = adminId,
            reason = reason ?: "Demoted from dealer"
        )

        return Result.success(updated)
    }

    // =========================================================
    // SUSPEND / REACTIVATE
    // =========================================================

    fun suspendUser(
        userId: Long,
        adminId: Long,
        reason: String
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (user.role.equals("ADMIN", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Cannot suspend an admin")
            )
        }
        if (userId == adminId) {
            return Result.failure(
                IllegalArgumentException("Cannot suspend your own account")
            )
        }

        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = user.role,
            newStatus = "SUSPENDED",
            newDealerStatus = user.dealerStatus
        ) ?: return Result.failure(
            IllegalStateException("Failed to suspend user")
        )

        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = user.role,
            changedByUserId = adminId,
            reason = "SUSPENDED: $reason"
        )

        return Result.success(updated)
    }

    fun reactivateUser(
        userId: Long,
        adminId: Long,
        reason: String?
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (!user.status.equals("SUSPENDED", ignoreCase = true) &&
            !user.status.equals("INACTIVE", ignoreCase = true)
        ) {
            return Result.failure(
                IllegalArgumentException("User is not suspended or inactive")
            )
        }

        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = user.role,
            newStatus = "ACTIVE",
            newDealerStatus = user.dealerStatus
        ) ?: return Result.failure(
            IllegalStateException("Failed to reactivate user")
        )

        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = user.role,
            changedByUserId = adminId,
            reason = reason ?: "REACTIVATED"
        )

        return Result.success(updated)
    }

    // =========================================================
    // DEALER APPLICATIONS
    // =========================================================

    fun getPendingDealerApplications(): List<DealerApplicationResponse> =
        userRepository
            .findPendingDealerApplications()
            .map { it.toApplicationResponse() }

    /**
     * Approve a pending dealer application.
     * Equivalent to promoteToDealer but skips the minimum
     * requirements check — the admin is explicitly vouching.
     */
    fun approveDealerApplication(
        userId: Long,
        adminId: Long,
        reason: String?
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (!user.dealerStatus.equals("PENDING", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException(
                    "User does not have a pending dealer application"
                )
            )
        }

        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = "DEALER",
            newStatus = "ACTIVE",
            newDealerStatus = "APPROVED"
        ) ?: return Result.failure(
            IllegalStateException("Failed to approve application")
        )

        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = "DEALER",
            changedByUserId = adminId,
            reason = reason ?: "Dealer application approved"
        )

        return Result.success(updated)
    }

    fun rejectDealerApplication(
        userId: Long,
        adminId: Long,
        reason: String?
    ): Result<UserRecord> {

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (!user.dealerStatus.equals("PENDING", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException(
                    "User does not have a pending dealer application"
                )
            )
        }

        val updated = userRepository.updateRoleAndStatus(
            id = userId,
            newRole = "CUSTOMER",
            newStatus = "ACTIVE",
            newDealerStatus = "REVOKED"
        ) ?: return Result.failure(
            IllegalStateException("Failed to reject application")
        )

        auditRepository.log(
            userId = userId,
            oldRole = user.role,
            newRole = "CUSTOMER",
            changedByUserId = adminId,
            reason = reason ?: "Dealer application rejected"
        )

        return Result.success(updated)
    }

    // =========================================================
    // AUDIT LOG
    // =========================================================

    fun getRecentAuditLog(
        limit: Int = 100
    ): List<RoleAuditResponse> =
        auditRepository
            .findRecent(limit)
            .mapNotNull { record ->
                val targetUser = userRepository.findById(record.userId)
                val admin = userRepository.findById(record.changedByUserId)

                if (targetUser == null || admin == null) {
                    null
                } else {
                    record.toResponse(
                        targetName = targetUser.name,
                        adminName = admin.name
                    )
                }
            }

    // =========================================================
    // MAPPERS
    // =========================================================

    private fun UserRecord.toApplicationResponse() =
        DealerApplicationResponse(
            id = id,
            name = name,
            email = email,
            phone = phone,
            status = status,
            dealerStatus = dealerStatus ?: "PENDING",
            createdAt = createdAt
        )

    private fun RoleAuditRecord.toResponse(
        targetName: String,
        adminName: String
    ) =
        RoleAuditResponse(
            id = id,
            userId = userId,
            userName = targetName,
            oldRole = oldRole,
            newRole = newRole,
            changedByUserId = changedByUserId,
            changedByName = adminName,
            reason = reason,
            createdAt = createdAt
        )
}