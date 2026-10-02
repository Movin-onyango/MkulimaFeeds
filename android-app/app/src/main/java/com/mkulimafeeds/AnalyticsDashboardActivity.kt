package com.mkulimafeeds

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.AnalyticsOverviewResponse
import com.mkulimafeeds.data.remote.CategoryBreakdownResponse
import com.mkulimafeeds.data.remote.DailyOrderResponse
import com.mkulimafeeds.data.remote.RegionDemandResponse
import com.mkulimafeeds.data.remote.RevenueTrendResponse
import com.mkulimafeeds.data.repository.AnalyticsRepository
import kotlinx.coroutines.launch
import java.util.Locale

class AnalyticsDashboardActivity : AppCompatActivity() {

    private val tokenManager by lazy {
        TokenManager(applicationContext)
    }

    private val analyticsRepository by lazy {
        AnalyticsRepository()
    }

    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_analytics_dashboard
        )

        val rootView =
            findViewById<View>(
                R.id.analyticsRoot
            )

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.adminBottomNav
            )

        loadingOverlay =
            findViewById(R.id.loadingOverlay)

        tvLoadingText =
            findViewById(R.id.tvLoadingText)

        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )

            insets
        }

        setupNavigation(bottomNav)
        setupClickListeners()
        loadAnalytics()
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun showLoading(
        message: String = "Loading analytics..."
    ) {
        tvLoadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    // =========================================================
    // LOAD ANALYTICS
    // =========================================================

    private fun loadAnalytics() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        showLoading()

        lifecycleScope.launch {

            try {

                val response =
                    analyticsRepository.getOverview(
                        token
                    )

                populateKPIs(response)
                renderRevenueTrend(response.revenueTrends)
                renderRegionalDemand(response.regionalDemand)
                renderDailyOrders(response.dailyOrders)
                renderCategoryBreakdown(response.categoryBreakdown)

            } catch (e: Exception) {

                Toast.makeText(
                    this@AnalyticsDashboardActivity,
                    e.message
                        ?: "Failed to load analytics",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                hideLoading()
            }
        }
    }

    // =========================================================
    // POPULATE KPI CARDS
    // =========================================================

    private fun populateKPIs(
        response: AnalyticsOverviewResponse
    ) {

        val tvMonthlyRevenue =
            findViewById<TextView>(
                R.id.tvMonthlyRevenue
            )

        val tvTopProductOrders =
            findViewById<TextView>(
                R.id.tvTopProductOrders
            )

        val tvActiveDealers =
            findViewById<TextView>(
                R.id.tvActiveDealers
            )

        val tvCustomerGrowth =
            findViewById<TextView>(
                R.id.tvCustomerGrowth
            )

        // -----------------------------------------------------
        // MONTHLY REVENUE
        // -----------------------------------------------------

        val monthlyRevenue =
            response.monthly.revenue
                .toDoubleOrNull()
                ?: 0.0

        tvMonthlyRevenue.text =
            String.format(
                Locale.getDefault(),
                "KSh %,.0f",
                monthlyRevenue
            )

        // -----------------------------------------------------
        // TOP PRODUCT ORDERS
        // -----------------------------------------------------

        val topProductOrders =
            response.topProducts
                .sumOf { it.orders }

        tvTopProductOrders.text =
            topProductOrders.toString()

        // -----------------------------------------------------
        // ACTIVE DEALERS
        // -----------------------------------------------------

        tvActiveDealers.text =
            response.dealerPerformance
                .size
                .toString()

        // -----------------------------------------------------
        // CUSTOMER GROWTH
        // -----------------------------------------------------

        tvCustomerGrowth.text =
            String.format(
                Locale.getDefault(),
                "%+.1f%%",
                response.customerGrowth.percentage
            )
    }

    // =========================================================
    // REVENUE TREND CHART
    // =========================================================

    private fun renderRevenueTrend(
        trends: List<RevenueTrendResponse>
    ) {

        val chartContainer =
            findViewById<LinearLayout>(
                R.id.revenueChartContainer
            )

        val tvRevenuePeriod =
            findViewById<TextView>(
                R.id.tvRevenuePeriod
            )

        chartContainer.removeAllViews()

        if (trends.isEmpty()) {
            tvRevenuePeriod.text = "No revenue data"
            return
        }

        tvRevenuePeriod.text = "Current Year"

        val values =
            trends.map {
                it.amount
                    .toDoubleOrNull()
                    ?: 0.0
            }

        val maxRevenue =
            values.maxOrNull()
                ?.coerceAtLeast(1.0)
                ?: 1.0

        trends.forEachIndexed { index, trend ->

            val revenue =
                values[index]

            val barHeight =
                (
                        revenue
                            .div(maxRevenue)
                            .times(140)
                        )
                    .toInt()
                    .coerceIn(8, 140)

            val column =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.BOTTOM or
                                Gravity.CENTER_HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1f
                        ).apply {
                            setMargins(2, 0, 2, 0)
                        }
                }

            val valueLabel =
                TextView(this).apply {
                    text = formatCompactRevenue(revenue)
                    textSize = 8f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(117, 117, 117))
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                }

            val bar =
                View(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            barHeight
                        )

                    setBackgroundColor(
                        if (revenue == maxRevenue) {
                            Color.rgb(0, 82, 255)
                        } else {
                            Color.rgb(224, 231, 255)
                        }
                    )
                }

            val month =
                TextView(this).apply {
                    text = trend.month
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(117, 117, 117))
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 4
                        }
                }

            column.addView(valueLabel)
            column.addView(bar)
            column.addView(month)

            chartContainer.addView(column)
        }
    }

    private fun formatCompactRevenue(
        value: Double
    ): String {
        return when {
            value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format(Locale.US, "%.0fK", value / 1_000.0)
            value <= 0.0 -> "0"
            else -> String.format(Locale.US, "%.0f", value)
        }
    }

    // =========================================================
    // REGIONAL DEMAND
    // =========================================================

    private fun renderRegionalDemand(
        regionalDemand: List<RegionDemandResponse>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.regionalDemandContainer
            )

        container.removeAllViews()

        if (regionalDemand.isEmpty()) {

            val emptyView =
                TextView(this).apply {
                    text = "No regional demand data available"
                    textSize = 13f
                    setTextColor(Color.rgb(117, 117, 117))
                }

            container.addView(emptyView)

            return
        }

        regionalDemand.forEach { region ->

            val row =
                LayoutInflater.from(this)
                    .inflate(
                        R.layout.view_performance_metric,
                        container,
                        false
                    )

            val tvMetricLabel =
                row.findViewById<TextView>(
                    R.id.tvMetricLabel
                )

            val tvMetricValue =
                row.findViewById<TextView>(
                    R.id.tvMetricValue
                )

            val progressIndicator =
                row.findViewById<LinearProgressIndicator>(
                    R.id.progressIndicator
                )

            val tvMetricGrowth =
                row.findViewById<TextView>(
                    R.id.tvMetricGrowth
                )

            tvMetricLabel.text =
                region.name

            tvMetricValue.text =
                "${region.percentage}%"

            progressIndicator.progress =
                region.percentage
                    .coerceIn(0, 100)

            tvMetricGrowth.text =
                String.format(
                    Locale.getDefault(),
                    "%+.1f%%",
                    region.growth
                )

            tvMetricGrowth.setTextColor(
                when {
                    region.growth > 0 ->
                        Color.rgb(30, 142, 62)

                    region.growth < 0 ->
                        Color.rgb(185, 28, 28)

                    else ->
                        Color.rgb(117, 117, 117)
                }
            )

            container.addView(row)
        }
    }

    // =========================================================
    // DAILY ORDERS CHART
    // =========================================================

    private fun renderDailyOrders(
        dailyOrders: List<DailyOrderResponse>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.dailyOrdersChartContainer
            )

        container.removeAllViews()

        if (dailyOrders.isEmpty()) {

            val emptyView =
                TextView(this).apply {
                    text = "No order activity available"
                    textSize = 13f
                    setTextColor(Color.rgb(117, 117, 117))
                }

            container.addView(emptyView)

            return
        }

        val counts =
            dailyOrders.map { it.count }

        val maximumOrders =
            counts.maxOrNull()
                ?.coerceAtLeast(1)
                ?: 1

        dailyOrders.forEach { dailyOrder ->

            val column =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.BOTTOM or
                                Gravity.CENTER_HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1f
                        ).apply {
                            setMargins(4, 0, 4, 0)
                        }
                }

            val barHeight =
                if (dailyOrder.count == 0) {
                    6
                } else {
                    (
                            dailyOrder.count
                                .toDouble()
                                .div(maximumOrders)
                                .times(130)
                            )
                        .toInt()
                        .coerceIn(12, 130)
                }

            val bar =
                View(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            barHeight
                        )

                    setBackgroundColor(
                        if (dailyOrder.count == maximumOrders) {
                            Color.rgb(0, 82, 255)
                        } else {
                            Color.rgb(224, 231, 255)
                        }
                    )
                }

            val countLabel =
                TextView(this).apply {
                    text = dailyOrder.count.toString()
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(26, 28, 30))
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 4
                        }
                }

            val dayLabel =
                TextView(this).apply {
                    text = dailyOrder.day
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(117, 117, 117))
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 2
                        }
                }

            column.addView(bar)
            column.addView(countLabel)
            column.addView(dayLabel)

            container.addView(column)
        }
    }

    // =========================================================
    // CATEGORY BREAKDOWN
    // =========================================================

    private fun renderCategoryBreakdown(
        categories: List<CategoryBreakdownResponse>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.categoryBreakdownContainer
            )

        container.removeAllViews()

        if (categories.isEmpty()) {

            val emptyView =
                TextView(this).apply {
                    text = "No category data available"
                    textSize = 13f
                    setTextColor(Color.rgb(117, 117, 117))
                }

            container.addView(emptyView)

            return
        }

        categories.forEach { category ->

            val row =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 16
                        }
                }

            val header =
                RelativeLayout(this).apply {
                    layoutParams =
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                }

            val name =
                TextView(this).apply {
                    text = category.name
                    textSize = 13f
                    setTextColor(Color.rgb(26, 28, 30))
                }

            val percentage =
                TextView(this).apply {
                    text = "${category.percentage}%"
                    textSize = 13f
                    setTextColor(Color.rgb(0, 82, 255))
                    setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )
                }

            header.addView(name)
            header.addView(percentage)

            val percentageParams =
                percentage.layoutParams as RelativeLayout.LayoutParams

            percentageParams.addRule(
                RelativeLayout.ALIGN_PARENT_END
            )

            percentage.layoutParams =
                percentageParams

            val progress =
                LinearProgressIndicator(this).apply {

                    max = 100

                    progress =
                        category.percentage
                            .coerceIn(0, 100)

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 8
                        }

                    trackThickness = 6

                    setIndicatorColor(
                        Color.rgb(0, 82, 255)
                    )

                    setTrackColor(
                        Color.rgb(232, 239, 255)
                    )
                }

            row.addView(header)
            row.addView(progress)

            container.addView(row)
        }
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private fun setupClickListeners() {

        findViewById<View>(
            R.id.btnExportGlobal
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Export scheduled — check your email shortly.",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        // -----------------------------------------------------
        // REVENUE KPI CARD → Sales Insights
        // -----------------------------------------------------
        findViewById<View>(
            R.id.cardSalesInsights
        ).setOnClickListener {
            openSalesInsights()
        }

        // -----------------------------------------------------
        // QUICK MANAGEMENT: Sales Insights (NEW)
        // -----------------------------------------------------
        findViewById<View>(
            R.id.btnSalesInsights
        ).setOnClickListener {
            openSalesInsights()
        }

        // -----------------------------------------------------
        // QUICK MANAGEMENT: Business Intelligence
        // -----------------------------------------------------
        findViewById<View>(
            R.id.btnBusinessIntelligence
        ).setOnClickListener {
            // We're already on the BI dashboard — do nothing.
            Toast.makeText(
                this,
                "You are on the Business Intelligence dashboard",
                Toast.LENGTH_SHORT
            ).show()
        }

        // -----------------------------------------------------
        // QUICK MANAGEMENT: Product Performance
        // -----------------------------------------------------
        findViewById<View>(
            R.id.btnProductPerformance
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProductPerformanceActivity::class.java
                )
            )
        }

        // -----------------------------------------------------
        // QUICK MANAGEMENT: Dealer Performance
        // -----------------------------------------------------
        findViewById<View>(
            R.id.btnDealerPerformance
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    DealerPerformanceActivity::class.java
                )
            )
        }

        // -----------------------------------------------------
        // QUICK MANAGEMENT: Customer Growth
        // -----------------------------------------------------
        findViewById<View>(
            R.id.btnCustomerGrowth
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CustomerGrowthActivity::class.java
                )
            )
        }

        // -----------------------------------------------------
        // KPI CARDS
        // -----------------------------------------------------
        findViewById<View>(
            R.id.cardProductPerformance
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProductPerformanceActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.cardDealerPerformance
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    DealerPerformanceActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.cardCustomerGrowth
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CustomerGrowthActivity::class.java
                )
            )
        }
    }

    // =========================================================
    // NAVIGATION HELPER
    // =========================================================

    private fun openSalesInsights() {
        startActivity(
            Intent(
                this,
                SalesInsightsActivity::class.java
            )
        )
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupNavigation(
        bottomNav: BottomNavigationView
    ) {

        bottomNav.selectedItemId =
            R.id.admin_products

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.admin_home -> {
                    startActivity(
                        Intent(
                            this,
                            AdminDashboardActivity::class.java
                        )
                    )
                    finish()
                    true
                }

                R.id.admin_products -> {
                    startActivity(
                        Intent(
                            this,
                            AdminProductActivity::class.java
                        )
                    )
                    finish()
                    true
                }

                R.id.admin_orders -> {
                    startActivity(
                        Intent(
                            this,
                            AdminOrderActivity::class.java
                        )
                    )
                    finish()
                    true
                }

                R.id.admin_users -> {
                    startActivity(
                        Intent(
                            this,
                            AdminUserActivity::class.java
                        )
                    )
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}