package com.mkulimafeeds
import com.mkulimafeeds.account.AdminAccountActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.AnalyticsOverviewResponse
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AnalyticsRepository
import com.mkulimafeeds.data.repository.AuthRepository
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.util.Locale
import com.mkulimafeeds.util.applyBottomNavInsets
class AdminDashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AdminDashboard"
    }

    // =========================================================
    // REPOSITORIES
    // =========================================================

    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var productRepository: ProductRepository

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)

        tokenManager = TokenManager(applicationContext)
        authRepository = AuthRepository(
            apiService = NetworkModule.apiService,
            tokenManager = tokenManager
        )
        analyticsRepository = AnalyticsRepository(NetworkModule.apiService)
        productRepository = ProductRepository(NetworkModule.apiService)

        setupWindowInsets()
        setupQuickActions()
        setupAdminNavigation()
        highlightDashboardTab()

        verifyAuthentication()

        loadAnalytics()
        loadLowStockAlerts()
    }

    override fun onResume() {
        super.onResume()
        highlightDashboardTab()
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    private fun setupAdminNavigation() {
        findViewById<View>(R.id.navProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrderActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navInsights).setOnClickListener {
            startActivity(Intent(this, AdminInsightsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, AdminAccountActivity::class.java))
            finish()
        }
        // navDashboard = current, no-op
    }

    private fun highlightDashboardTab() {
        setActiveTab(R.id.icDashboard, R.id.tvDashboard)
        setInactiveTab(R.id.icProducts, R.id.tvProducts)
        setInactiveTab(R.id.icOrders, R.id.tvOrders)
        setInactiveTab(R.id.icInsights, R.id.tvInsights)
        setInactiveTab(R.id.icAccount, R.id.tvAccount)
    }

    private fun setActiveTab(ic: Int, tv: Int) {
        findViewById<ImageView>(ic).setColorFilter(
            ContextCompat.getColor(this, R.color.brand_dark_green)
        )
        findViewById<TextView>(tv).setTextColor(
            ContextCompat.getColor(this, R.color.brand_dark_green)
        )
        findViewById<TextView>(tv).setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun setInactiveTab(ic: Int, tv: Int) {
        findViewById<ImageView>(ic).setColorFilter(
            ContextCompat.getColor(this, R.color.text_grey)
        )
        findViewById<TextView>(tv).setTextColor(
            ContextCompat.getColor(this, R.color.text_grey)
        )
        findViewById<TextView>(tv).setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    // =========================================================
    // AUTH VERIFICATION
    // =========================================================

    private fun verifyAuthentication() {
        lifecycleScope.launch {
            try {
                val response = authRepository.getCurrentUser()
                Log.d(
                    TAG,
                    "Auth verified: userId=${response.userId}, " +
                            "email=${response.email}, role=${response.role}"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Auth verification failed: ${e.message}")
            }
        }
    }

    // =========================================================
    // ANALYTICS (KPI CARDS)
    // =========================================================

    private fun loadAnalytics() {
        val token = tokenManager.getToken()
        if (token.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val overview = analyticsRepository.getOverview(token)
                bindKpis(overview)
            } catch (e: Exception) {
                Log.w(TAG, "Analytics fetch failed: ${e.message}")
            }
        }
    }

    private fun bindKpis(overview: AnalyticsOverviewResponse) {
        tvRevenueValue.text = "KES ${overview.monthly.revenue}"
        tvRevenueGrowth.text = formatGrowth(overview.monthly.growth)
        tvRevenueGrowth.setTextColor(growthColor(overview.monthly.growth))

        val last7DaysTotal = overview.dailyOrders.sumOf { it.count }
        tvActiveOrdersValue.text = last7DaysTotal.toString()
        tvActiveOrdersSubtitle.text = "Last 7 days"

        tvFarmersValue.text = formatNumber(overview.customerGrowth.totalCustomers)
        tvFarmersSubtitle.text = "+${overview.customerGrowth.newCustomers} new this month"
    }

    // =========================================================
    // LOW STOCK + STOCK HEALTH
    // =========================================================

    private fun loadLowStockAlerts() {
        lifecycleScope.launch {
            try {
                val products = productRepository.getProducts()

                if (products.isEmpty()) {
                    renderNoLowStock()
                    tvStockHealthValue.text = "0%"
                    tvStockHealthSubtitle.text = "No products"
                    return@launch
                }

                val inStockCount = products.count { it.stock > 0 }
                val healthPercent =
                    (inStockCount.toDouble() / products.size * 100).toInt()

                tvStockHealthValue.text = "$healthPercent%"

                val lowStockCount = products.count { it.stock in 1..9 }
                tvStockHealthSubtitle.text =
                    if (lowStockCount > 0) "$lowStockCount SKUs low" else "All healthy"

                val lowStock = products
                    .filter { it.stock in 0..9 }
                    .sortedBy { it.stock }
                    .take(3)

                if (lowStock.isEmpty()) {
                    renderNoLowStock()
                } else {
                    renderLowStockRows(lowStock)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Low stock fetch failed: ${e.message}")
                renderNoLowStock()
            }
        }
    }

    private fun renderLowStockRows(products: List<Product>) {
        val container = findViewById<LinearLayout>(R.id.lowStockContainer)
        val noItems = findViewById<TextView>(R.id.tvNoLowStock)

        container.removeAllViews()
        container.addView(noItems)
        noItems.visibility = View.GONE

        products.forEachIndexed { index, product ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_low_stock_row, container, false)

            val tvName = row.findViewById<TextView>(R.id.tvLowStockName)
            val tvQty = row.findViewById<TextView>(R.id.tvLowStockQty)

            tvName.text = product.name
            tvQty.text = if (product.stock == 0) {
                "Out of stock"
            } else {
                "${product.stock} ${product.unit} left"
            }

            if (index > 0) {
                val params = row.layoutParams as LinearLayout.LayoutParams
                params.topMargin = dp(8)
                row.layoutParams = params
            }

            container.addView(row)
        }
    }

    private fun renderNoLowStock() {
        val container = findViewById<LinearLayout>(R.id.lowStockContainer)
        val noItems = findViewById<TextView>(R.id.tvNoLowStock)

        container.removeAllViews()
        container.addView(noItems)
        noItems.visibility = View.VISIBLE
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun formatGrowth(growth: Double): String =
        if (growth >= 0) {
            "↑ %.1f%% vs last month".format(growth)
        } else {
            "↓ %.1f%% vs last month".format(-growth)
        }

    private fun growthColor(growth: Double): Int =
        ContextCompat.getColor(this, R.color.brand_blue)

    private fun formatNumber(value: Int): String =
        String.format(Locale.getDefault(), "%,d", value)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // =========================================================
    // WINDOW INSETS
    // =========================================================

    private fun setupWindowInsets() {
            applyBottomNavInsets(
                root = findViewById(R.id.admin_dashboard_root),
                bottomNav = findViewById(R.id.bottomNavAdmin)
            )
        }


    // =========================================================
    // QUICK ACTIONS
    // =========================================================

    private fun setupQuickActions() {
        findViewById<View>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductActivity::class.java))
        }
        findViewById<View>(R.id.btnManageOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrderActivity::class.java))
        }
        findViewById<View>(R.id.btnManageCustomers).setOnClickListener {
            startActivity(Intent(this, AdminUserActivity::class.java))
        }
        findViewById<View>(R.id.btnDealerApplications).setOnClickListener {
            startActivity(Intent(this, DealerApplicationsActivity::class.java))
        }
        findViewById<View>(R.id.btnAuditLog).setOnClickListener {
            startActivity(Intent(this, AuditLogActivity::class.java))
        }
        findViewById<View>(R.id.btnAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
        }
        findViewById<View>(R.id.btnProductPerformance).setOnClickListener {
            startActivity(Intent(this, ProductPerformanceActivity::class.java))
        }
        findViewById<View>(R.id.btnBusinessRules).setOnClickListener {
            startActivity(Intent(this, BusinessRulesActivity::class.java))
        }
        findViewById<View>(R.id.cardRevenue).setOnClickListener {
            startActivity(Intent(this, SalesInsightsActivity::class.java))
        }
    }

    // =========================================================
    // KPI VIEW REFERENCES
    // =========================================================

    private val tvRevenueValue: TextView
        get() = findViewById(R.id.tvRevenueValue)

    private val tvRevenueGrowth: TextView
        get() = findViewById(R.id.tvRevenueGrowth)

    private val tvActiveOrdersValue: TextView
        get() = findViewById(R.id.tvActiveOrdersValue)

    private val tvActiveOrdersSubtitle: TextView
        get() = findViewById(R.id.tvActiveOrdersSubtitle)

    private val tvStockHealthValue: TextView
        get() = findViewById(R.id.tvStockHealthValue)

    private val tvStockHealthSubtitle: TextView
        get() = findViewById(R.id.tvStockHealthSubtitle)

    private val tvFarmersValue: TextView
        get() = findViewById(R.id.tvFarmersValue)

    private val tvFarmersSubtitle: TextView
        get() = findViewById(R.id.tvFarmersSubtitle)
}