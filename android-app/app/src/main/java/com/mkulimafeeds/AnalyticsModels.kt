package com.mkulimafeeds

data class ProductMetric(
    val name: String,
    val orders: Int,
    val revenue: Double,
    val growth: Double
)

data class RegionMetric(
    val name: String,
    val percentage: Int,
    val growth: Double
)

data class GrowthMetric(
    val newFarmers: Int,
    val totalFarmers: Int,
    val percentage: Double
)

data class DealerMetric(
    val name: String,
    val location: String,
    val revenue: Double,
    val growth: Double
)

data class RevenueData(
    val month: String,
    val amount: Double
)

data class DailyOrderData(
    val day: String,
    val count: Int
)