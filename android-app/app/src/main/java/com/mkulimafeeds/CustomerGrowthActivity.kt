package com.mkulimafeeds

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.CustomerAnalyticsResponse
import com.mkulimafeeds.data.remote.MonthlySignupCount
import com.mkulimafeeds.data.repository.CustomerAnalyticsRepository
import kotlinx.coroutines.launch
import java.util.Locale

class CustomerGrowthActivity : AppCompatActivity() {

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var rvTopCustomers: RecyclerView
    private lateinit var tvNoTopCustomers: TextView
    private lateinit var spinnerSortMode: Spinner
    private lateinit var topCustomersAdapter: TopCustomerAdapter

    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    // =========================================================
    // STATE
    // =========================================================

    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val repository by lazy { CustomerAnalyticsRepository() }

    private var analyticsResponse: CustomerAnalyticsResponse? = null

    private val sortLabels = listOf(
        "Top by Orders",
        "Top by Revenue"
    )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_customer_growth
        )

        initializeViews()
        setupWindowInsets()
        setupRecyclerView()
        setupSortSpinner()
        setupNavigation()
        setupClickListeners()

        loadAnalytics()
    }

    // =========================================================
    // SETUP
    // =========================================================

    private fun initializeViews() {

        rvTopCustomers =
            findViewById(R.id.rvTopCustomers)

        tvNoTopCustomers =
            findViewById(R.id.tvNoTopCustomers)

        spinnerSortMode =
            findViewById(R.id.spinnerSortMode)

        loadingOverlay =
            findViewById(R.id.loadingOverlay)

        tvLoadingText =
            findViewById(R.id.tvLoadingText)
    }

    private fun setupWindowInsets() {

        val rootView =
            findViewById<View>(R.id.customerGrowthRoot)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {

        topCustomersAdapter =
            TopCustomerAdapter(
                emptyList(),
                TopCustomerAdapter.SortMode.BY_ORDERS
            )

        rvTopCustomers.layoutManager =
            LinearLayoutManager(this)

        rvTopCustomers.adapter =
            topCustomersAdapter
    }

    private fun setupSortSpinner() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sortLabels
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinnerSortMode.adapter = adapter

        spinnerSortMode.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    renderTopCustomers()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit
            }
    }

    private fun setupNavigation() {

        val bottomNav =
            findViewById<BottomNavigationView>(R.id.adminBottomNav)

        bottomNav.selectedItemId = R.id.admin_users

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_home -> {
                    startActivity(
                        Intent(this, AdminDashboardActivity::class.java)
                    )
                    finish()
                    true
                }
                R.id.admin_products -> {
                    startActivity(
                        Intent(this, AdminProductActivity::class.java)
                    )
                    finish()
                    true
                }
                R.id.admin_orders -> {
                    startActivity(
                        Intent(this, AdminOrderActivity::class.java)
                    )
                    finish()
                    true
                }
                R.id.admin_users -> true
                else -> false
            }
        }
    }

    private fun setupClickListeners() {

        findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { finish() }
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun showLoading(
        message: String = "Loading customer analytics..."
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

        val token = tokenManager.getToken()

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

                val response = repository.getAnalytics(token)

                analyticsResponse = response

                renderHero(response)
                renderTiers(response)
                renderTopCustomers()
                renderSignupTrend(response.signupTrend)

            } catch (e: Exception) {

                Toast.makeText(
                    this@CustomerGrowthActivity,
                    e.message ?: "Failed to load customer analytics",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                hideLoading()
            }
        }
    }

    // =========================================================
    // HERO
    // =========================================================

    private fun renderHero(response: CustomerAnalyticsResponse) {

        findViewById<TextView>(R.id.tvTotalCustomers).text =
            String.format(Locale.getDefault(), "%,d", response.totalCustomers)

        findViewById<TextView>(R.id.tvNewThisMonth).text =
            "+${response.newThisMonth} new this month"

        val growthText = String.format(
            Locale.getDefault(),
            "%+.1f%% vs last month",
            response.growthPercent
        )

        findViewById<TextView>(R.id.tvGrowthPercent).text = growthText
    }

    // =========================================================
    // TIERS
    // =========================================================

    private fun renderTiers(response: CustomerAnalyticsResponse) {

        val tiers = response.tiers
        val total = response.totalCustomers.coerceAtLeast(1)

        bindTier(
            R.id.tvTierNewCount,
            R.id.progressTierNew,
            tiers.new,
            total
        )
        bindTier(
            R.id.tvTierRegularCount,
            R.id.progressTierRegular,
            tiers.regular,
            total
        )
        bindTier(
            R.id.tvTierVipCount,
            R.id.progressTierVip,
            tiers.vip,
            total
        )
        bindTier(
            R.id.tvTierInactiveCount,
            R.id.progressTierInactive,
            tiers.inactive,
            total
        )
    }

    private fun bindTier(
        countViewId: Int,
        progressViewId: Int,
        count: Int,
        total: Int
    ) {
        val percent = (count.toDouble() / total * 100)
            .toInt()
            .coerceIn(0, 100)

        findViewById<TextView>(countViewId).text =
            "$count (${percent}%)"

        findViewById<LinearProgressIndicator>(progressViewId)
            .progress = percent
    }

    // =========================================================
    // TOP CUSTOMERS
    // =========================================================

    private fun renderTopCustomers() {

        val response = analyticsResponse ?: return

        val sortMode = when (spinnerSortMode.selectedItemPosition) {
            1 -> TopCustomerAdapter.SortMode.BY_REVENUE
            else -> TopCustomerAdapter.SortMode.BY_ORDERS
        }

        val customers = when (sortMode) {
            TopCustomerAdapter.SortMode.BY_ORDERS ->
                response.topCustomersByOrders
            TopCustomerAdapter.SortMode.BY_REVENUE ->
                response.topCustomersByRevenue
        }

        // Update adapter with new mode + list
        topCustomersAdapter =
            TopCustomerAdapter(customers, sortMode)
        rvTopCustomers.adapter = topCustomersAdapter

        if (customers.isEmpty()) {
            rvTopCustomers.visibility = View.GONE
            tvNoTopCustomers.visibility = View.VISIBLE
        } else {
            rvTopCustomers.visibility = View.VISIBLE
            tvNoTopCustomers.visibility = View.GONE
        }
    }

    // =========================================================
    // SIGNUP TREND CHART
    // =========================================================

    private fun renderSignupTrend(
        trend: List<MonthlySignupCount>
    ) {

        val container =
            findViewById<LinearLayout>(R.id.signupTrendContainer)

        container.removeAllViews()

        if (trend.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No signup data available"
                textSize = 13f
                setTextColor(Color.rgb(117, 117, 117))
            }
            container.addView(emptyView)
            return
        }

        val maxCount = trend.maxOfOrNull { it.count }
            ?.coerceAtLeast(1) ?: 1

        trend.forEach { entry ->

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
            }

            val barHeight =
                if (entry.count == 0) {
                    6
                } else {
                    (entry.count.toDouble() / maxCount * 130)
                        .toInt()
                        .coerceIn(12, 130)
                }

            val countLabel = TextView(this).apply {
                text = entry.count.toString()
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(26, 28, 30))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    barHeight
                )
                setBackgroundColor(
                    if (entry.count == maxCount) {
                        Color.rgb(0, 82, 255)
                    } else {
                        Color.rgb(224, 231, 255)
                    }
                )
            }

            val monthLabel = TextView(this).apply {
                text = entry.month
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(117, 117, 117))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                }
            }

            column.addView(countLabel)
            column.addView(bar)
            column.addView(monthLabel)

            container.addView(column)
        }
    }
}