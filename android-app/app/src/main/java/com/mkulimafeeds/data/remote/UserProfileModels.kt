package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val user: ProfileUser,
    val stats: ProfileStats,
    val dealer: DealerInfo? = null    // NEW — null for non-dealers
)

@Serializable
data class ProfileUser(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val isActive: Boolean,
    val memberSince: Long
)

@Serializable
data class ProfileStats(
    val totalOrders: Int,
    val completedOrders: Int,
    val totalSpent: String,
    val tier: String
)

// NEW — dealer enrichment block
@Serializable
data class DealerInfo(
    val businessName: String? = null,
    val businessRegion: String? = null,
    val accountManagerName: String? = null,
    val accountManagerEmail: String? = null,
    val currentTier: String,
    val nextTier: String? = null,
    val ordersToNextTier: Int,
    val tierProgressPercent: Int,
    val creditLimit: String,
    val creditUsed: String,
    val creditAvailable: String,
    val paymentTerms: String,
    val lifetimeBulkOrders: Int
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val phone: String
)

@Serializable
data class SavedLocationResponse(
    val id: Long,
    val label: String,
    val address: String,
    val isDefault: Boolean,
    val createdAt: Long
)

@Serializable
data class CreateSavedLocationRequest(
    val label: String,
    val address: String,
    val isDefault: Boolean = false
)

@Serializable
data class UpdateSavedLocationRequest(
    val label: String,
    val address: String,
    val isDefault: Boolean = false
)

@Serializable
data class DeleteAccountRequest(
    val password: String,
    val confirmation: String
)