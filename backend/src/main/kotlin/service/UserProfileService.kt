package com.movofeeds.service

import com.movofeeds.models.DealerInfo
import com.movofeeds.models.ProfileStats
import com.movofeeds.models.ProfileUser
import com.movofeeds.models.UserProfileResponse
import com.movofeeds.repository.OrderRepository
import com.movofeeds.repository.UserRecord
import com.movofeeds.repository.UserRepository
import java.math.BigDecimal
import java.math.RoundingMode

class UserProfileService(
    private val userRepository: UserRepository = UserRepository(),
    private val orderRepository: OrderRepository = OrderRepository(),
    private val settingsService: SettingsService = SettingsService()   // NEW
) {

    /**
     * Builds the full profile summary for a user:
     * - Basic account info
     * - Order stats (counts + total spent)
     * - Tier (derived from completed order count)
     * - Dealer info (only for DEALER role; null otherwise)
     */
    fun getProfile(userId: Long): UserProfileResponse? {

        val user = userRepository.findById(userId)
            ?: return null

        val completedOrders = orderRepository.countByCustomerId(userId)
        val totalSpent = orderRepository.sumCompletedRevenueByCustomerId(userId)
        val totalOrders = orderRepository.countAllOrdersByCustomerId(userId)

        val tier = computeTier(
            isActive = user.isActive,
            completedOrders = completedOrders
        )

        // NEW — compute dealer info for dealers only
        val dealerInfo: DealerInfo? = if (user.role.equals("DEALER", ignoreCase = true)) {
            computeDealerInfo(user)
        } else null

        return UserProfileResponse(
            user = ProfileUser(
                id = user.id,
                name = user.name,
                email = user.email,
                phone = user.phone,
                role = user.role,
                isActive = user.isActive,
                memberSince = user.createdAt
            ),
            stats = ProfileStats(
                totalOrders = totalOrders,
                completedOrders = completedOrders,
                totalSpent = totalSpent
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString(),
                tier = tier
            ),
            dealer = dealerInfo    // NEW
        )
    }

    // NEW — dealer enrichment
    private fun computeDealerInfo(user: UserRecord): DealerInfo {
        val lifetimeOrders = user.lifetimeBulkOrders

        // Read tier thresholds from settings
        val bronzeThreshold = settingsService.getInt("dealer.tier_bronze_threshold", 0)
        val silverThreshold = settingsService.getInt("dealer.tier_silver_threshold", 100)
        val goldThreshold = settingsService.getInt("dealer.tier_gold_threshold", 500)
        val platinumThreshold = settingsService.getInt("dealer.tier_platinum_threshold", 2000)

        val bronzeLabel = settingsService.getString("dealer.tier_bronze_label", "BRONZE")
        val silverLabel = settingsService.getString("dealer.tier_silver_label", "SILVER")
        val goldLabel = settingsService.getString("dealer.tier_gold_label", "GOLD")
        val platinumLabel = settingsService.getString("dealer.tier_platinum_label", "PLATINUM")

        // Determine current tier
        val currentTier: String
        val nextTier: String?
        val currentThreshold: Int
        val nextThreshold: Int?

        when {
            lifetimeOrders >= platinumThreshold -> {
                currentTier = platinumLabel
                nextTier = null
                currentThreshold = platinumThreshold
                nextThreshold = null
            }
            lifetimeOrders >= goldThreshold -> {
                currentTier = goldLabel
                nextTier = platinumLabel
                currentThreshold = goldThreshold
                nextThreshold = platinumThreshold
            }
            lifetimeOrders >= silverThreshold -> {
                currentTier = silverLabel
                nextTier = goldLabel
                currentThreshold = silverThreshold
                nextThreshold = goldThreshold
            }
            lifetimeOrders >= bronzeThreshold -> {
                currentTier = bronzeLabel
                nextTier = silverLabel
                currentThreshold = bronzeThreshold
                nextThreshold = silverThreshold
            }
            else -> {
                currentTier = bronzeLabel
                nextTier = silverLabel
                currentThreshold = bronzeThreshold
                nextThreshold = silverThreshold
            }
        }

        val ordersToNext = if (nextThreshold != null) {
            (nextThreshold - lifetimeOrders).coerceAtLeast(0)
        } else 0

        val tierProgress = if (nextThreshold != null && nextThreshold > currentThreshold) {
            val range = nextThreshold - currentThreshold
            val progress = lifetimeOrders - currentThreshold
            ((progress.toDouble() / range) * 100).toInt().coerceIn(0, 100)
        } else 100

        val creditLimit = user.creditLimit
        val creditUsed = user.creditUsed
        val creditAvailable = creditLimit.subtract(creditUsed).coerceAtLeast(BigDecimal.ZERO)

        return DealerInfo(
            businessName = user.businessName,
            businessRegion = user.businessRegion,
            accountManagerName = user.accountManagerName,
            accountManagerEmail = user.accountManagerEmail,
            currentTier = currentTier,
            nextTier = nextTier,
            ordersToNextTier = ordersToNext,
            tierProgressPercent = tierProgress,
            creditLimit = creditLimit.toPlainString(),
            creditUsed = creditUsed.toPlainString(),
            creditAvailable = creditAvailable.toPlainString(),
            paymentTerms = user.paymentTerms,
            lifetimeBulkOrders = lifetimeOrders
        )
    }

    /**
     * Tier thresholds:
     *   VIP       → 10+ completed orders
     *   REGULAR   → 1..9 completed orders
     *   NEW       → 0 completed orders
     *   INACTIVE  → account deactivated
     */
    private fun computeTier(
        isActive: Boolean,
        completedOrders: Int
    ): String = when {
        !isActive -> "INACTIVE"
        completedOrders >= 10 -> "VIP"
        completedOrders >= 1 -> "REGULAR"
        else -> "NEW"
    }

    /**
     * Update the authenticated user's own profile.
     * Uniqueness checks on email/phone (excluding self).
     */
    fun updateOwnProfile(
        userId: Long,
        name: String,
        email: String,
        phone: String
    ): Result<UserRecord> {

        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return Result.failure(
                IllegalArgumentException("Name must be at least 2 characters")
            )
        }

        val existing = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        // ---------------------------------------------
        // Validate email uniqueness (if provided and changed)
        // ---------------------------------------------
        if (trimmedEmail.isNotBlank() && trimmedEmail != existing.email) {
            val owner = userRepository.findByEmail(trimmedEmail)
            if (owner != null && owner.id != userId) {
                return Result.failure(
                    IllegalArgumentException(
                        "That email is already in use by another account"
                    )
                )
            }
        }

        // ---------------------------------------------
        // Validate phone uniqueness (if provided and changed)
        // ---------------------------------------------
        if (trimmedPhone.isNotBlank() && trimmedPhone != existing.phone) {
            val owner = userRepository.findByPhone(trimmedPhone)
            if (owner != null && owner.id != userId) {
                return Result.failure(
                    IllegalArgumentException(
                        "That phone number is already in use by another account"
                    )
                )
            }
        }

        val updated = userRepository.update(
            id = userId,
            name = trimmedName,
            phone = trimmedPhone,
            email = trimmedEmail
        )

        return if (updated != null) {
            Result.success(updated)
        } else {
            Result.failure(
                IllegalStateException("Failed to update profile")
            )
        }
    }

    /**
     * Soft-delete the authenticated user's account.
     * Requires password confirmation and an explicit "DELETE" string.
     * Preserves order history for audit.
     */
    fun deleteOwnAccount(
        userId: Long,
        password: String,
        confirmation: String
    ): Result<Unit> {

        if (confirmation.trim().uppercase() != "DELETE") {
            return Result.failure(
                IllegalArgumentException(
                    "Please type DELETE to confirm account deletion"
                )
            )
        }

        val user = userRepository.findById(userId)
            ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

        if (!com.movofeeds.security.PasswordHasher.verify(
                password,
                user.passwordHash
            )
        ) {
            return Result.failure(
                IllegalArgumentException("Incorrect password")
            )
        }

        // Soft delete
        val ok = userRepository.deactivate(userId)

        return if (ok) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException("Failed to delete account")
            )
        }
    }
}