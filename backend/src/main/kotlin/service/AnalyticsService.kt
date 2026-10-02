package com.movofeeds.service

import com.movofeeds.models.AnalyticsOverviewResponse
import com.movofeeds.models.CategoryBreakdownResponse
import com.movofeeds.models.CustomerGrowthResponse
import com.movofeeds.models.DailyOrderResponse
import com.movofeeds.models.DealerPerformanceResponse
import com.movofeeds.models.ProductPerformanceResponse
import com.movofeeds.models.RegionDemandResponse
import com.movofeeds.models.RevenuePeriodResponse
import com.movofeeds.models.RevenueTrendResponse
import com.movofeeds.repository.OrderRecord
import com.movofeeds.repository.OrderRepository
import com.movofeeds.repository.ProductRepository
import com.movofeeds.repository.UserRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class AnalyticsService(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) {

    private val zoneId =
        ZoneId.of("Africa/Nairobi")

    fun getOverview(): AnalyticsOverviewResponse {

        val orders =
            orderRepository.findAll()

        val users =
            userRepository.findAll()

        val products =
            productRepository.findAll()

        val today =
            LocalDate.now(zoneId)

        val weekStart =
            today.with(
                java.time.temporal.TemporalAdjusters
                    .previousOrSame(
                        DayOfWeek.MONDAY
                    )
            )

        val monthStart =
            today.withDayOfMonth(1)

        val previousWeekStart =
            weekStart.minusWeeks(1)

        val previousMonthStart =
            monthStart.minusMonths(1)

        val previousMonthEnd =
            monthStart.minusDays(1)

        val todayRevenue =
            calculateCompletedRevenue(
                orders,
                today,
                today.plusDays(1)
            )

        val yesterdayRevenue =
            calculateCompletedRevenue(
                orders,
                today.minusDays(1),
                today
            )

        val weeklyRevenue =
            calculateCompletedRevenue(
                orders,
                weekStart,
                today.plusDays(1)
            )

        val previousWeeklyRevenue =
            calculateCompletedRevenue(
                orders,
                previousWeekStart,
                weekStart
            )

        val monthlyRevenue =
            calculateCompletedRevenue(
                orders,
                monthStart,
                today.plusDays(1)
            )

        val previousMonthlyRevenue =
            calculateCompletedRevenue(
                orders,
                previousMonthStart,
                monthStart
            )

        return AnalyticsOverviewResponse(

            today =
                RevenuePeriodResponse(
                    period = "Today",
                    revenue = money(todayRevenue),
                    growth =
                        percentageChange(
                            yesterdayRevenue,
                            todayRevenue
                        )
                ),

            weekly =
                RevenuePeriodResponse(
                    period = "Weekly",
                    revenue = money(weeklyRevenue),
                    growth =
                        percentageChange(
                            previousWeeklyRevenue,
                            weeklyRevenue
                        )
                ),

            monthly =
                RevenuePeriodResponse(
                    period = "Monthly",
                    revenue = money(monthlyRevenue),
                    growth =
                        percentageChange(
                            previousMonthlyRevenue,
                            monthlyRevenue
                        )
                ),

            customerGrowth =
                calculateCustomerGrowth(
                    users,
                    today,
                    previousMonthStart,
                    monthStart
                ),

            topProducts =
                calculateTopProducts(
                    orders
                ),

            regionalDemand =
                calculateRegionalDemand(
                    orders,
                    previousMonthStart,
                    monthStart
                ),

            dealerPerformance =
                calculateDealerPerformance(
                    orders,
                    users,
                    previousMonthStart,
                    monthStart
                ),

            revenueTrends =
                calculateRevenueTrends(
                    orders,
                    today
                ),

            dailyOrders =
                calculateDailyOrders(
                    orders,
                    today
                ),

            categoryBreakdown =
                calculateCategoryBreakdown(
                    orders,
                    products
                )
        )
    }

    private fun calculateCompletedRevenue(
        orders: List<OrderRecord>,
        startDate: LocalDate,
        endDateExclusive: LocalDate
    ): BigDecimal {

        return orders
            .filter { order ->

                order.status
                    .equals(
                        "COMPLETED",
                        ignoreCase = true
                    ) &&
                        isDateInRange(
                            order.createdAt,
                            startDate,
                            endDateExclusive
                        )
            }
            .fold(
                BigDecimal.ZERO
            ) { total, order ->

                total.add(
                    order.totalAmount
                )
            }
            .setScale(
                2,
                RoundingMode.HALF_UP
            )
    }

    private fun calculateTopProducts(
        orders: List<OrderRecord>
    ): List<ProductPerformanceResponse> {

        val currentMonth =
            LocalDate.now(zoneId)
                .withDayOfMonth(1)

        val previousMonth =
            currentMonth.minusMonths(1)

        val currentRevenue =
            mutableMapOf<Long, BigDecimal>()

        val previousRevenue =
            mutableMapOf<Long, BigDecimal>()

        val productOrders =
            mutableMapOf<Long, Int>()

        val productNames =
            mutableMapOf<Long, String>()

        orders
            .filter {
                it.status.equals(
                    "COMPLETED",
                    ignoreCase = true
                )
            }
            .forEach { order ->

                val orderDate =
                    epochToDate(
                        order.createdAt
                    )

                order.items.forEach { item ->

                    productNames[item.productId] =
                        item.productName

                    productOrders[item.productId] =
                        (productOrders[item.productId]
                            ?: 0) + 1

                    if (orderDate >= currentMonth) {

                        currentRevenue[item.productId] =
                            (
                                    currentRevenue[item.productId]
                                        ?: BigDecimal.ZERO
                                    ).add(
                                    item.subtotal
                                )
                    }

                    if (
                        orderDate >= previousMonth &&
                        orderDate < currentMonth
                    ) {

                        previousRevenue[item.productId] =
                            (
                                    previousRevenue[item.productId]
                                        ?: BigDecimal.ZERO
                                    ).add(
                                    item.subtotal
                                )
                    }
                }
            }

        return productOrders
            .keys
            .map { productId ->

                val current =
                    currentRevenue[productId]
                        ?: BigDecimal.ZERO

                val previous =
                    previousRevenue[productId]
                        ?: BigDecimal.ZERO

                ProductPerformanceResponse(
                    name =
                        productNames[productId]
                            ?: "Unknown Product",

                    orders =
                        productOrders[productId]
                            ?: 0,

                    revenue =
                        money(current),

                    growth =
                        percentageChange(
                            previous,
                            current
                        )
                )
            }
            .sortedByDescending {
                BigDecimal(it.revenue)
            }
            .take(5)
    }

    private fun calculateRegionalDemand(
        orders: List<OrderRecord>,
        previousMonthStart: LocalDate,
        currentMonthStart: LocalDate
    ): List<RegionDemandResponse> {

        val activeOrders =
            orders.filter {
                !it.status.equals(
                    "CANCELLED",
                    ignoreCase = true
                )
            }

        if (activeOrders.isEmpty()) {
            return emptyList()
        }

        val total =
            activeOrders.size

        val grouped =
            activeOrders
                .groupingBy {
                    it.location.trim()
                }
                .eachCount()

        return grouped
            .entries
            .sortedByDescending {
                it.value
            }
            .take(5)
            .map { entry ->

                val percentage =
                    (
                            entry.value.toDouble() /
                                    total.toDouble() *
                                    100.0
                            ).toInt()

                RegionDemandResponse(
                    name = entry.key,
                    percentage = percentage,
                    growth = 0.0
                )
            }
    }

    private fun calculateDealerPerformance(
        orders: List<OrderRecord>,
        users: List<com.movofeeds.repository.UserRecord>,
        previousMonthStart: LocalDate,
        currentMonthStart: LocalDate
    ): List<DealerPerformanceResponse> {

        val dealers =
            users.filter {
                it.role.equals(
                    "DEALER",
                    ignoreCase = true
                )
            }

        return dealers
            .map { dealer ->

                val dealerOrders =
                    orders.filter {
                        it.assignedDealerId ==
                                dealer.id &&
                                it.status.equals(
                                    "COMPLETED",
                                    ignoreCase = true
                                )
                    }

                val currentRevenue =
                    dealerOrders
                        .filter {
                            epochToDate(
                                it.createdAt
                            ) >= currentMonthStart
                        }
                        .fold(
                            BigDecimal.ZERO
                        ) { total, order ->

                            total.add(
                                order.totalAmount
                            )
                        }

                val previousRevenue =
                    dealerOrders
                        .filter {

                            val date =
                                epochToDate(
                                    it.createdAt
                                )

                            date >= previousMonthStart &&
                                    date < currentMonthStart
                        }
                        .fold(
                            BigDecimal.ZERO
                        ) { total, order ->

                            total.add(
                                order.totalAmount
                            )
                        }

                val location =
                    dealerOrders
                        .groupingBy {
                            it.location.trim()
                        }
                        .eachCount()
                        .maxByOrNull {
                            it.value
                        }
                        ?.key
                        ?: "N/A"

                DealerPerformanceResponse(
                    id = dealer.id,
                    name = dealer.name,
                    location = location,
                    revenue = money(
                        currentRevenue
                    ),
                    growth =
                        percentageChange(
                            previousRevenue,
                            currentRevenue
                        )
                )
            }
            .sortedByDescending {
                BigDecimal(it.revenue)
            }
    }

    private fun calculateCustomerGrowth(
        users: List<com.movofeeds.repository.UserRecord>,
        today: LocalDate,
        previousMonthStart: LocalDate,
        currentMonthStart: LocalDate
    ): CustomerGrowthResponse {

        val customers =
            users.filter {
                it.role.equals(
                    "CUSTOMER",
                    ignoreCase = true
                )
            }

        val newCustomers =
            customers.count {
                epochToDate(
                    it.createdAt
                ) >= currentMonthStart &&
                        epochToDate(
                            it.createdAt
                        ) <= today
            }

        val previousPeriodCustomers =
            customers.count {
                val date =
                    epochToDate(
                        it.createdAt
                    )

                date >= previousMonthStart &&
                        date < currentMonthStart
            }

        return CustomerGrowthResponse(
            newCustomers = newCustomers,
            totalCustomers = customers.size,
            percentage =
                percentageChange(
                    previousPeriodCustomers
                        .toBigDecimal(),
                    newCustomers
                        .toBigDecimal()
                )
        )
    }

    private fun calculateRevenueTrends(
        orders: List<OrderRecord>,
        today: LocalDate
    ): List<RevenueTrendResponse> {

        val year =
            today.year

        val formatter =
            Locale.getDefault()

        return (1..12).map { month ->

            val start =
                LocalDate.of(
                    year,
                    month,
                    1
                )

            val end =
                if (month == 12) {
                    LocalDate.of(
                        year + 1,
                        1,
                        1
                    )
                } else {
                    LocalDate.of(
                        year,
                        month + 1,
                        1
                    )
                }

            val revenue =
                calculateCompletedRevenue(
                    orders,
                    start,
                    end
                )

            RevenueTrendResponse(
                month =
                    start.month.getDisplayName(
                        TextStyle.SHORT,
                        formatter
                    ),
                amount = money(revenue)
            )
        }
    }

    private fun calculateDailyOrders(
        orders: List<OrderRecord>,
        today: LocalDate
    ): List<DailyOrderResponse> {

        return (6 downTo 0).map { offset ->

            val date =
                today.minusDays(
                    offset.toLong()
                )

            val nextDate =
                date.plusDays(1)

            val count =
                orders.count {

                    !it.status.equals(
                        "CANCELLED",
                        ignoreCase = true
                    ) &&
                            isDateInRange(
                                it.createdAt,
                                date,
                                nextDate
                            )
                }

            DailyOrderResponse(
                day =
                    date.dayOfWeek.getDisplayName(
                        TextStyle.SHORT,
                        Locale.getDefault()
                    ),
                count = count
            )
        }
    }

    private fun calculateCategoryBreakdown(
        orders: List<OrderRecord>,
        products: List<com.movofeeds.repository.ProductRecord>
    ): List<CategoryBreakdownResponse> {

        val categoryByProductId =
            products.associate {
                it.id to it.category
            }

        val quantities =
            mutableMapOf<String, BigDecimal>()

        var total =
            BigDecimal.ZERO

        orders
            .filter {
                it.status.equals(
                    "COMPLETED",
                    ignoreCase = true
                )
            }
            .forEach { order ->

                order.items.forEach { item ->

                    val category =
                        categoryByProductId[
                            item.productId
                        ]?.ifBlank {
                            "Other"
                        } ?: "Other"

                    quantities[category] =
                        (
                                quantities[category]
                                    ?: BigDecimal.ZERO
                                ).add(
                                item.quantity
                            )

                    total =
                        total.add(
                            item.quantity
                        )
                }
            }

        if (total <= BigDecimal.ZERO) {
            return emptyList()
        }

        return quantities
            .entries
            .sortedByDescending {
                it.value
            }
            .map { entry ->

                CategoryBreakdownResponse(
                    name = entry.key,
                    percentage =
                        (
                                entry.value
                                    .divide(
                                        total,
                                        4,
                                        RoundingMode.HALF_UP
                                    )
                                    .toDouble() *
                                        100.0
                                ).toInt()
                )
            }
    }

    private fun isDateInRange(
        timestamp: Long,
        start: LocalDate,
        endExclusive: LocalDate
    ): Boolean {

        val date =
            epochToDate(timestamp)

        return date >= start &&
                date < endExclusive
    }

    private fun epochToDate(
        timestamp: Long
    ): LocalDate {

        return java.time.Instant
            .ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalDate()
    }

    private fun money(
        amount: BigDecimal
    ): String {

        return amount
            .setScale(
                2,
                RoundingMode.HALF_UP
            )
            .toPlainString()
    }

    private fun percentageChange(
        previous: BigDecimal,
        current: BigDecimal
    ): Double {

        if (previous.compareTo(
                BigDecimal.ZERO
            ) == 0
        ) {

            return if (
                current.compareTo(
                    BigDecimal.ZERO
                ) > 0
            ) {
                100.0
            } else {
                0.0
            }
        }

        return current
            .subtract(previous)
            .divide(
                previous,
                4,
                RoundingMode.HALF_UP
            )
            .multiply(
                BigDecimal("100")
            )
            .setScale(
                1,
                RoundingMode.HALF_UP
            )
            .toDouble()
    }
}