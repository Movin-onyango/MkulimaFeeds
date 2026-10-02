package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

/**
 * Aggregated analytics about the customer base.
 *
 * Mirrors the backend CustomerAnalyticsResponse.
 */
@Serializable
data class CustomerAnalyticsResponse(
    val totalCustomers: Int,
    val newThisMonth: Int,
    val growthPercent: Double,
    val tiers: CustomerTierCounts,
    val signupTrend: List<MonthlySignupCount>,
    val topCustomersByOrders: List<TopCustomer>,
    val topCustomersByRevenue: List<TopCustomer>
)

@Serializable
data class CustomerTierCounts(
    val new: Int,
    val regular: Int,
    val vip: Int,
    val inactive: Int
)

@Serializable
data class MonthlySignupCount(
    val month: String,
    val count: Int
)

@Serializable
data class TopCustomer(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val totalOrders: Int,
    val totalRevenue: String,
    val isActive: Boolean,
    val createdAt: Long
)