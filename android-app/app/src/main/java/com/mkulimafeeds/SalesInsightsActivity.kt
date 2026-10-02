package com.mkulimafeeds

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.AnalyticsOverviewResponse
import com.mkulimafeeds.data.repository.AnalyticsRepository
import kotlinx.coroutines.launch
import java.util.Locale

class SalesInsightsActivity : AppCompatActivity() {

    private lateinit var chipToday: TextView
    private lateinit var chipWeekly: TextView
    private lateinit var chipMonthly: TextView

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvGrowth: TextView
    private lateinit var tvNewFarmers: TextView
    private lateinit var tvCustomerGrowthComparison: TextView

    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    private var analyticsResponse: AnalyticsOverviewResponse? = null

    private val tokenManager by lazy {
        TokenManager(applicationContext)
    }

    private val analyticsRepository by lazy {
        AnalyticsRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_sales_insights
        )

        initializeViews()
        setupNavigation()
        setupFilterListeners()

        val rootView =
            findViewById<View>(
                R.id.salesInsightsRoot
            )

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

        loadAnalytics()
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun showLoading(
        message: String = "Loading sales insights..."
    ) {
        tvLoadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private fun initializeViews() {

        chipToday =
            findViewById(R.id.chipToday)

        chipWeekly =
            findViewById(R.id.chipWeekly)

        chipMonthly =
            findViewById(R.id.chipMonthly)

        tvTotalRevenue =
            findViewById(R.id.tvTotalRevenue)

        tvGrowth =
            findViewById(R.id.tvRevenueComparison)

        tvNewFarmers =
            findViewById(R.id.tvNewFarmersCount)

        tvCustomerGrowthComparison =
            findViewById(R.id.tvCustomerGrowthComparison)

        loadingOverlay =
            findViewById(R.id.loadingOverlay)

        tvLoadingText =
            findViewById(R.id.tvLoadingText)

        findViewById<View>(
            R.id.btnBackSales
        ).setOnClickListener {

            finish()
        }
    }

    // =========================================================
    // LOAD LIVE ANALYTICS
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

                analyticsResponse =
                    response

                // Default view
                updateSalesData(
                    "Weekly",
                    response
                )

                renderDailyOrders(
                    response.dailyOrders
                )

                renderCategoryBreakdown(
                    response.categoryBreakdown
                )

                renderCustomerGrowth(
                    response.customerGrowth
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@SalesInsightsActivity,
                    e.message
                        ?: "Failed to load sales analytics",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                hideLoading()
            }
        }
    }

    // =========================================================
    // SALES PERIOD
    // =========================================================

    private fun updateSalesData(
        period: String,
        response: AnalyticsOverviewResponse
    ) {

        val data =
            when (period) {

                "Today" ->
                    response.today

                "Weekly" ->
                    response.weekly

                "Monthly" ->
                    response.monthly

                else ->
                    response.weekly
            }

        val revenue =
            data.revenue
                .toDoubleOrNull()
                ?: 0.0

        tvTotalRevenue.text =
            String.format(
                Locale.getDefault(),
                "KES %,.0f",
                revenue
            )

        tvGrowth.text =
            String.format(
                Locale.getDefault(),
                "%+.1f%% vs previous period",
                data.growth
            )

        tvGrowth.setTextColor(
            when {

                data.growth > 0 ->
                    Color.rgb(170, 255, 188)

                data.growth < 0 ->
                    Color.rgb(255, 190, 190)

                else ->
                    Color.WHITE
            }
        )
    }

    // =========================================================
    // DAILY ORDERS
    // =========================================================

    private fun renderDailyOrders(
        dailyOrders:
        List<com.mkulimafeeds.data.remote.DailyOrderResponse>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.dailyOrdersChartContainer
            )

        container.removeAllViews()

        if (dailyOrders.isEmpty()) {

            val emptyView =
                TextView(this).apply {

                    text =
                        "No daily order data available"

                    textSize = 13f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.rgb(117, 117, 117)
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                }

            container.addView(emptyView)

            return
        }

        val maximumOrders =
            dailyOrders
                .maxOfOrNull { it.count }
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

            val count =
                TextView(this).apply {

                    text =
                        dailyOrder.count.toString()

                    textSize = 10f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.rgb(26, 28, 30)
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            val barHeight =
                if (dailyOrder.count == 0) {
                    6
                } else {
                    dailyOrder.count
                        .toDouble()
                        .div(maximumOrders)
                        .times(120)
                        .toInt()
                        .coerceIn(12, 120)
                }

            val bar =
                View(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            barHeight
                        )

                    setBackgroundColor(
                        if (
                            dailyOrder.count ==
                            maximumOrders
                        ) {
                            Color.rgb(0, 82, 255)
                        } else {
                            Color.rgb(224, 231, 255)
                        }
                    )
                }

            val day =
                TextView(this).apply {

                    text =
                        dailyOrder.day

                    textSize = 9f

                    gravity =
                        Gravity.CENTER

                    setTextColor(
                        Color.rgb(117, 117, 117)
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 4
                        }
                }

            column.addView(count)
            column.addView(bar)
            column.addView(day)

            container.addView(column)
        }
    }

    // =========================================================
    // CATEGORY BREAKDOWN
    // =========================================================

    private fun renderCategoryBreakdown(
        categories:
        List<com.mkulimafeeds.data.remote.CategoryBreakdownResponse>
    ) {

        val container =
            findViewById<LinearLayout>(
                R.id.breakdown_container
            )

        container.removeAllViews()

        if (categories.isEmpty()) {

            val emptyView =
                TextView(this).apply {

                    text =
                        "No category data available"

                    textSize = 13f

                    setTextColor(
                        Color.rgb(117, 117, 117)
                    )
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
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                }

            val name =
                TextView(this).apply {

                    text =
                        category.name

                    textSize = 13f

                    setTextColor(
                        Color.rgb(26, 28, 30)
                    )
                }

            val percentage =
                TextView(this).apply {

                    text =
                        "${category.percentage}%"

                    textSize = 13f

                    setTextColor(
                        Color.rgb(0, 82, 255)
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    gravity =
                        Gravity.END
                }

            header.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            header.addView(
                percentage,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

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
    // CUSTOMER GROWTH
    // =========================================================

    private fun renderCustomerGrowth(
        customerGrowth:
        com.mkulimafeeds.data.remote.CustomerGrowthResponse
    ) {

        tvNewFarmers.text =
            customerGrowth.newCustomers.toString()

        tvCustomerGrowthComparison.text =
            String.format(
                Locale.getDefault(),
                "%+.1f%%",
                customerGrowth.percentage
            )

        tvCustomerGrowthComparison.setTextColor(
            if (customerGrowth.percentage >= 0) {
                Color.rgb(30, 142, 62)
            } else {
                Color.rgb(185, 28, 28)
            }
        )
    }

    // =========================================================
    // FILTER LISTENERS
    // =========================================================

    private fun setupFilterListeners() {

        val chips =
            listOf(
                chipToday,
                chipWeekly,
                chipMonthly
            )

        chipToday.setOnClickListener {

            updateChipSelection(
                chipToday,
                chips
            )

            analyticsResponse?.let {
                updateSalesData("Today", it)
            }
        }

        chipWeekly.setOnClickListener {

            updateChipSelection(
                chipWeekly,
                chips
            )

            analyticsResponse?.let {
                updateSalesData("Weekly", it)
            }
        }

        chipMonthly.setOnClickListener {

            updateChipSelection(
                chipMonthly,
                chips
            )

            analyticsResponse?.let {
                updateSalesData("Monthly", it)
            }
        }

        updateChipSelection(
            chipWeekly,
            chips
        )
    }

    private fun updateChipSelection(
        selected: TextView,
        allChips: List<TextView>
    ) {

        allChips.forEach { chip ->

            if (chip == selected) {

                chip.setBackgroundResource(
                    R.drawable.bg_chip_selected
                )

                chip.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )

            } else {

                chip.setBackgroundResource(
                    R.drawable.bg_chip_unselected
                )

                chip.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.text_grey
                    )
                )
            }
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun setupNavigation() {

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.adminBottomNav
            )

        // Match the Analytics suite — highlight "Products".
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