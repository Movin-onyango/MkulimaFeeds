package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProductPerformanceResponse(
    val name: String,
    val orders: Int,
    val revenue: String,
    val growth: Double
)

@Serializable
data class RegionDemandResponse(
    val name: String,
    val percentage: Int,
    val growth: Double
)

@Serializable
data class DealerPerformanceResponse(
    val id: Long,
    val name: String,
    val location: String,
    val revenue: String,
    val growth: Double
)

@Serializable
data class CustomerGrowthResponse(
    val newCustomers: Int,
    val totalCustomers: Int,
    val percentage: Double
)

@Serializable
data class RevenuePeriodResponse(
    val period: String,
    val revenue: String,
    val growth: Double
)

@Serializable
data class RevenueTrendResponse(
    val month: String,
    val amount: String
)

@Serializable
data class DailyOrderResponse(
    val day: String,
    val count: Int
)

@Serializable
data class CategoryBreakdownResponse(
    val name: String,
    val percentage: Int
)

@Serializable
data class AnalyticsOverviewResponse(
    val today: RevenuePeriodResponse,
    val weekly: RevenuePeriodResponse,
    val monthly: RevenuePeriodResponse,
    val customerGrowth: CustomerGrowthResponse,
    val topProducts: List<ProductPerformanceResponse>,
    val regionalDemand: List<RegionDemandResponse>,
    val dealerPerformance: List<DealerPerformanceResponse>,
    val revenueTrends: List<RevenueTrendResponse>,
    val dailyOrders: List<DailyOrderResponse>,
    val categoryBreakdown: List<CategoryBreakdownResponse>
)
