package com.movofeeds.service

import com.movofeeds.models.CustomerAnalyticsResponse
import com.movofeeds.models.CustomerTierCounts
import com.movofeeds.models.MonthlySignupCount
import com.movofeeds.models.TopCustomer
import com.movofeeds.repository.OrderRepository
import com.movofeeds.repository.UserRecord
import com.movofeeds.repository.UserRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class CustomerAnalyticsService(
    private val userRepository: UserRepository = UserRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) {

    private val zoneId = ZoneId.of("Africa/Nairobi")

    // Tier thresholds based on completed orders
    private val vipThreshold = 10
    private val regularThreshold = 1

    fun getAnalytics(): CustomerAnalyticsResponse {

        val allCustomers = userRepository.findAllCustomers()

        val today = LocalDate.now(zoneId)
        val currentMonthStart = today.withDayOfMonth(1)

        // =========================================================
        // TOTAL + NEW THIS MONTH
        // =========================================================

        val totalCustomers = allCustomers.size

        val newThisMonth = allCustomers.count { user ->
            val date = epochToDate(user.createdAt)
            date >= currentMonthStart && date <= today
        }

        // ---------------------------------------------------------
        // Compute growth vs. previous month for honest comparison.
        // ---------------------------------------------------------
        val previousMonthStart = currentMonthStart.minusMonths(1)
        val previousMonthEnd = currentMonthStart.minusDays(1)

        val lastMonthCount = allCustomers.count { user ->
            val date = epochToDate(user.createdAt)
            date >= previousMonthStart && date <= previousMonthEnd
        }

        val growthPercent = if (lastMonthCount == 0) {
            if (newThisMonth > 0) 100.0 else 0.0
        } else {
            ((newThisMonth - lastMonthCount).toDouble() / lastMonthCount * 100.0)
                .let { roundTo1(it) }
        }

        // =========================================================
        // TIERS
        // =========================================================
        //
        // We compute per-customer completed order count and
        // bucket customers into tiers. Inactive overrides all.
        // =========================================================

        var newCount = 0
        var regularCount = 0
        var vipCount = 0
        var inactiveCount = 0

        val allStats = mutableListOf<CustomerStats>()

        allCustomers.forEach { user ->

            val completedOrders = orderRepository.countByCustomerId(user.id)
            val completedRevenue = orderRepository.sumCompletedRevenueByCustomerId(user.id)

            allStats += CustomerStats(
                user = user,
                completedOrders = completedOrders,
                completedRevenue = completedRevenue
            )

            if (!user.isActive) {
                inactiveCount++
            } else if (completedOrders >= vipThreshold) {
                vipCount++
            } else if (completedOrders >= regularThreshold) {
                regularCount++
            } else {
                newCount++
            }
        }

        val tiers = CustomerTierCounts(
            new = newCount,
            regular = regularCount,
            vip = vipCount,
            inactive = inactiveCount
        )

        // =========================================================
        // SIGNUP TREND — last 6 months
        // =========================================================

        val signupTrend = (5 downTo 0).map { offset ->
            val monthStart = currentMonthStart.minusMonths(offset.toLong())
            val monthEnd = monthStart.plusMonths(1)

            val count = allCustomers.count { user ->
                val date = epochToDate(user.createdAt)
                date >= monthStart && date < monthEnd
            }

            MonthlySignupCount(
                month = monthStart.month
                    .getDisplayName(TextStyle.SHORT, Locale.US),
                count = count
            )
        }

        // =========================================================
        // TOP CUSTOMERS
        // =========================================================

        val topByOrders = allStats
            .filter { it.completedOrders > 0 }
            .sortedByDescending { it.completedOrders }
            .take(10)
            .map { it.toTopCustomer() }

        val topByRevenue = allStats
            .filter { it.completedRevenue > BigDecimal.ZERO }
            .sortedByDescending { it.completedRevenue }
            .take(10)
            .map { it.toTopCustomer() }

        return CustomerAnalyticsResponse(
            totalCustomers = totalCustomers,
            newThisMonth = newThisMonth,
            growthPercent = growthPercent,
            tiers = tiers,
            signupTrend = signupTrend,
            topCustomersByOrders = topByOrders,
            topCustomersByRevenue = topByRevenue
        )
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun epochToDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalDate()

    private fun roundTo1(value: Double): Double =
        (value * 10.0).let { Math.round(it) / 10.0 }

    private data class CustomerStats(
        val user: UserRecord,
        val completedOrders: Int,
        val completedRevenue: BigDecimal
    ) {
        fun toTopCustomer(): TopCustomer =
            TopCustomer(
                id = user.id,
                name = user.name,
                phone = user.phone,
                email = user.email,
                totalOrders = completedOrders,
                totalRevenue = completedRevenue
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString(),
                isActive = user.isActive,
                createdAt = user.createdAt
            )
    }
}