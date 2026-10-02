package com.movofeeds.models

import kotlinx.serialization.Serializable

/**
 * Aggregated analytics about the customer base.
 *
 * Combines:
 *  - existing customer growth metrics (from AnalyticsService)
 *  - per-customer order and revenue stats (from OrderRepository)
 *  - tier distribution and signup trend
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

/**
 * Count of customers in each tier.
 *
 * Tiers are derived from a customer's completed
 * order count:
 *   NEW       → 0 completed orders
 *   REGULAR   → 1..9 completed orders
 *   VIP       → 10+ completed orders
 *   INACTIVE  → any customer whose account is deactivated
 */
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