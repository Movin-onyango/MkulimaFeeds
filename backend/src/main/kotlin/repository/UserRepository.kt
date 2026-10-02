package com.movofeeds.repository

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import com.movofeeds.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

data class UserRecord(
    val id: Long,
    val name: String,
    val phone: String,      // non-null in-memory; "" when DB has NULL
    val email: String,      // non-null in-memory; "" when DB has NULL
    val passwordHash: String,
    val role: String,
    val status: String,
    val dealerStatus: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,

    // ---------------------------------------------
    // Dealer-specific fields (defaults for non-dealers)
    // ---------------------------------------------
    val businessName: String? = null,
    val businessRegion: String? = null,
    val accountManagerName: String? = null,
    val accountManagerEmail: String? = null,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val creditUsed: BigDecimal = BigDecimal.ZERO,
    val paymentTerms: String = "",
    val lifetimeBulkOrders: Int = 0
)

class UserRepository {

    fun existsByEmail(email: String): Boolean =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .count() > 0
        }

    fun existsByPhone(phone: String): Boolean =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.phone eq phone }
                .count() > 0
        }

    fun findById(id: Long): UserRecord? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.toUserRecord()
        }

    fun findByEmail(email: String): UserRecord? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.toUserRecord()
        }

    fun findByPhone(phone: String): UserRecord? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.phone eq phone }
                .singleOrNull()
                ?.toUserRecord()
        }

    fun findActiveDealers(): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .where {
                    (UsersTable.role eq "DEALER") and
                            (UsersTable.isActive eq true)
                }
                .map { it.toUserRecord() }
        }

    /**
     * Create a user.
     *
     * Empty-string phone/email are stored as NULL so the unique
     * indexes don't collide between multiple users that registered
     * with only the other identifier.
     */
    fun createUser(
        name: String,
        phone: String,
        email: String,
        passwordHash: String,
        role: String = "CUSTOMER"
    ): UserRecord =
        transaction {
            val now = System.currentTimeMillis()

            // Empty string → NULL, otherwise store the value
            val phoneValue: String? = phone.trim().ifBlank { null }
            val emailValue: String? = email.trim().ifBlank { null }

            val id = UsersTable.insertAndGetId {
                it[UsersTable.name] = name
                it[UsersTable.phone] = phoneValue
                it[UsersTable.email] = emailValue
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.role] = role
                it[UsersTable.isActive] = true
                it[UsersTable.createdAt] = now
                it[UsersTable.updatedAt] = now
            }.value

            findById(id)
                ?: error("Failed to retrieve newly created user")
        }

    fun update(
        id: Long,
        name: String,
        phone: String,
        email: String
    ): UserRecord? =
        transaction {
            val phoneValue: String? = phone.trim().ifBlank { null }
            val emailValue: String? = email.trim().ifBlank { null }

            val updated = UsersTable.update(
                where = { UsersTable.id eq id }
            ) {
                it[UsersTable.name] = name
                it[UsersTable.phone] = phoneValue
                it[UsersTable.email] = emailValue
                it[UsersTable.updatedAt] = System.currentTimeMillis()
            }

            if (updated == 0) {
                null
            } else {
                findById(id)
            }
        }

    fun findAll(): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .orderBy(UsersTable.createdAt to SortOrder.DESC)
                .map { it.toUserRecord() }
        }

    fun findAllCustomers(): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.role eq "CUSTOMER" }
                .orderBy(UsersTable.createdAt to SortOrder.DESC)
                .map { it.toUserRecord() }
        }

    /**
     * Find users by their account status.
     */
    fun findByStatus(status: String): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.status eq status.uppercase() }
                .orderBy(UsersTable.createdAt to SortOrder.DESC)
                .map { it.toUserRecord() }
        }

    /**
     * Find all users with a given role and status.
     */
    fun findByRoleAndStatus(
        role: String,
        status: String
    ): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .where {
                    (UsersTable.role eq role.uppercase()) and
                            (UsersTable.status eq status.uppercase())
                }
                .orderBy(UsersTable.createdAt to SortOrder.DESC)
                .map { it.toUserRecord() }
        }

    /**
     * Find all pending dealer applications.
     * A pending dealer has role=CUSTOMER but dealerStatus=PENDING.
     */
    fun findPendingDealerApplications(): List<UserRecord> =
        transaction {
            UsersTable
                .selectAll()
                .where {
                    (UsersTable.dealerStatus eq "PENDING")
                }
                .orderBy(UsersTable.createdAt to SortOrder.DESC)
                .map { it.toUserRecord() }
        }

    /**
     * Update the role AND status AND dealer status in one
     * atomic operation. Used by RoleService.
     */
    fun updateRoleAndStatus(
        id: Long,
        newRole: String,
        newStatus: String,
        newDealerStatus: String?
    ): UserRecord? =
        transaction {
            val updated = UsersTable.update(
                where = { UsersTable.id eq id }
            ) {
                it[role] = newRole.uppercase()
                it[status] = newStatus.uppercase()
                it[dealerStatus] = newDealerStatus?.uppercase()
                it[updatedAt] = System.currentTimeMillis()
            }

            if (updated == 0) {
                null
            } else {
                findById(id)
            }
        }

    /**
     * Count active admins — used to prevent demoting the last admin.
     */
    fun countActiveAdmins(): Int =
        transaction {
            UsersTable
                .selectAll()
                .where {
                    (UsersTable.role eq "ADMIN") and
                            (UsersTable.status eq "ACTIVE")
                }
                .count()
                .toInt()
        }

    fun changePassword(
        id: Long,
        passwordHash: String
    ): Boolean =
        transaction {
            val updated = UsersTable.update(
                where = { UsersTable.id eq id }
            ) {
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.updatedAt] = System.currentTimeMillis()
            }
            updated > 0
        }

    fun deactivate(id: Long): Boolean =
        transaction {
            val updated = UsersTable.update(
                where = { UsersTable.id eq id }
            ) {
                it[UsersTable.isActive] = false
                it[UsersTable.updatedAt] = System.currentTimeMillis()
            }
            updated > 0
        }

    /**
     * Update a dealer's business + credit profile.
     * Only meaningful for users with role = DEALER.
     */
    fun updateDealerInfo(
        id: Long,
        businessName: String?,
        businessRegion: String?,
        accountManagerName: String?,
        accountManagerEmail: String?,
        creditLimit: BigDecimal,
        creditUsed: BigDecimal,
        paymentTerms: String,
        lifetimeBulkOrders: Int
    ): UserRecord? =
        transaction {
            val updated = UsersTable.update(
                where = { UsersTable.id eq id }
            ) {
                it[UsersTable.businessName] = businessName
                it[UsersTable.businessRegion] = businessRegion
                it[UsersTable.accountManagerName] = accountManagerName
                it[UsersTable.accountManagerEmail] = accountManagerEmail
                it[UsersTable.creditLimit] = creditLimit
                it[UsersTable.creditUsed] = creditUsed
                it[UsersTable.paymentTerms] = paymentTerms
                it[UsersTable.lifetimeBulkOrders] = lifetimeBulkOrders
                it[UsersTable.updatedAt] = System.currentTimeMillis()
            }

            if (updated == 0) {
                null
            } else {
                findById(id)
            }
        }

    private fun ResultRow.toUserRecord(): UserRecord =
        UserRecord(
            id = this[UsersTable.id].value,
            name = this[UsersTable.name],
            phone = this[UsersTable.phone].orEmpty(),
            email = this[UsersTable.email].orEmpty(),
            passwordHash = this[UsersTable.passwordHash],
            status = this[UsersTable.status],
            dealerStatus = this[UsersTable.dealerStatus],
            role = this[UsersTable.role],
            isActive = this[UsersTable.isActive],
            createdAt = this[UsersTable.createdAt],
            updatedAt = this[UsersTable.updatedAt],

            // Dealer fields — nullable-safe reads with sensible defaults
            businessName = this[UsersTable.businessName],
            businessRegion = this[UsersTable.businessRegion],
            accountManagerName = this[UsersTable.accountManagerName],
            accountManagerEmail = this[UsersTable.accountManagerEmail],
            creditLimit = this[UsersTable.creditLimit] ?: BigDecimal.ZERO,
            creditUsed = this[UsersTable.creditUsed] ?: BigDecimal.ZERO,
            paymentTerms = this[UsersTable.paymentTerms] ?: "",
            lifetimeBulkOrders = this[UsersTable.lifetimeBulkOrders] ?: 0
        )
}