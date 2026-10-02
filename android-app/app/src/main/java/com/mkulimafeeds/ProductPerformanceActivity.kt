package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.AnalyticsRepository
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductPerformanceActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var adapter: ProductPerformanceAdapter

    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

    private val tokenManager by lazy {
        TokenManager(applicationContext)
    }

    private val analyticsRepository by lazy {
        AnalyticsRepository()
    }

    private val productRepository by lazy {
        ProductRepository(
            NetworkModule.apiService
        )
    }

    // ---------------------------------------------------------------------
    // LOADING TRACKER
    // We have two async calls (analytics + inventory).
    // The overlay hides only when both finish.
    // ---------------------------------------------------------------------
    private var pendingLoads: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_product_performance
        )

        val rootView =
            findViewById<View>(
                R.id.performanceRoot
            )

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.adminBottomNav
            )

        rvProducts =
            findViewById(
                R.id.rvProductMetrics
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

        setupRecyclerView()
        setupNavigation(bottomNav)
        setupClickListeners()

        // Start both fetches under a single loading cycle.
        beginLoadingCycle()
    }

    // ---------------------------------------------------------------------
    // LOADING STATE
    // ---------------------------------------------------------------------

    private fun beginLoadingCycle() {
        pendingLoads = 2
        showLoading()
        loadAnalytics()
        loadInventoryHealth()
    }

    private fun markLoadFinished() {
        pendingLoads = (pendingLoads - 1).coerceAtLeast(0)
        if (pendingLoads == 0) {
            hideLoading()
        }
    }

    private fun showLoading(
        message: String = "Loading product performance..."
    ) {
        tvLoadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    // ---------------------------------------------------------------------
    // RECYCLER VIEW
    // ---------------------------------------------------------------------

    private fun setupRecyclerView() {

        adapter =
            ProductPerformanceAdapter(
                emptyList()
            )

        rvProducts.layoutManager =
            LinearLayoutManager(this)

        rvProducts.adapter =
            adapter
    }

    // ---------------------------------------------------------------------
    // PRODUCT PERFORMANCE
    // ---------------------------------------------------------------------

    private fun loadAnalytics() {

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            markLoadFinished()

            return
        }

        lifecycleScope.launch {

            try {

                val response =
                    analyticsRepository.getOverview(
                        token
                    )

                val metrics =
                    response.topProducts.map { product ->

                        ProductMetric(
                            name = product.name,
                            orders = product.orders,
                            revenue =
                                product.revenue
                                    .toDoubleOrNull()
                                    ?: 0.0,
                            growth = product.growth
                        )
                    }

                adapter.submitList(metrics)

            } catch (e: Exception) {

                Toast.makeText(
                    this@ProductPerformanceActivity,
                    e.message
                        ?: "Failed to load product analytics",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                markLoadFinished()
            }
        }
    }

    // ---------------------------------------------------------------------
    // INVENTORY HEALTH
    // ---------------------------------------------------------------------

    private fun loadInventoryHealth() {

        lifecycleScope.launch {

            try {

                val products =
                    productRepository.getProducts()

                if (products.isEmpty()) {

                    showNoInventoryData()

                    return@launch
                }

                val lowestStockProduct =
                    products.minByOrNull {
                        it.stock
                    }

                val maximumStock =
                    products.maxOf {
                        it.stock
                    }

                if (lowestStockProduct == null) {

                    showNoInventoryData()

                    return@launch
                }

                val lowestStock =
                    lowestStockProduct.stock

                val progress =
                    if (maximumStock > 0) {
                        (
                                lowestStock
                                    .toDouble()
                                    .div(maximumStock)
                                    .times(100)
                                )
                            .toInt()
                            .coerceIn(0, 100)
                    } else {
                        0
                    }

                val totalProducts =
                    products.size

                val availableProducts =
                    products.count {
                        it.stock > 0
                    }

                val availability =
                    (
                            availableProducts
                                .toDouble()
                                .div(totalProducts)
                                .times(100)
                            )
                        .toInt()
                        .coerceIn(0, 100)

                val stockUnit =
                    lowestStockProduct.unit
                        .ifBlank { "units" }

                val tvFeedName =
                    findViewById<TextView>(
                        R.id.tvFeedName
                    )

                val tvInventoryStock =
                    findViewById<TextView>(
                        R.id.tvInventoryStock
                    )

                val tvStockStatus =
                    findViewById<TextView>(
                        R.id.tvStockStatus
                    )

                val tvInventoryStockLabel =
                    findViewById<TextView>(
                        R.id.tvInventoryStockLabel
                    )

                val tvInventoryStockDetail =
                    findViewById<TextView>(
                        R.id.tvInventoryStockDetail
                    )

                val inventoryProgress =
                    findViewById<LinearProgressIndicator>(
                        R.id.inventoryStockProgress
                    )

                val tvAvailability =
                    findViewById<TextView>(
                        R.id.tvInventoryAvailability
                    )

                val availabilityProgress =
                    findViewById<LinearProgressIndicator>(
                        R.id.inventoryAvailabilityProgress
                    )

                tvFeedName.text =
                    lowestStockProduct.name

                tvInventoryStock.text =
                    "${lowestStock} ${stockUnit}"

                tvStockStatus.text =
                    if (lowestStock == 0) {
                        "OUT OF STOCK"
                    } else {
                        "LOWEST STOCK"
                    }

                tvInventoryStockLabel.text =
                    "Current stock"

                tvInventoryStockDetail.text =
                    "Lowest stock across ${totalProducts} products"

                inventoryProgress.progress =
                    progress

                tvAvailability.text =
                    "$availability%"

                availabilityProgress.progress =
                    availability

            } catch (e: Exception) {

                Toast.makeText(
                    this@ProductPerformanceActivity,
                    e.message
                        ?: "Failed to load inventory health",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                markLoadFinished()
            }
        }
    }

    private fun showNoInventoryData() {

        findViewById<TextView>(
            R.id.tvFeedName
        ).text = "No inventory data"

        findViewById<TextView>(
            R.id.tvInventoryStock
        ).text = "0 units"

        findViewById<TextView>(
            R.id.tvStockStatus
        ).text = "NO DATA"

        findViewById<LinearProgressIndicator>(
            R.id.inventoryStockProgress
        ).progress = 0

        findViewById<TextView>(
            R.id.tvInventoryStockDetail
        ).text = "No products available"

        findViewById<TextView>(
            R.id.tvInventoryAvailability
        ).text = "0%"

        findViewById<LinearProgressIndicator>(
            R.id.inventoryAvailabilityProgress
        ).progress = 0
    }

    // ---------------------------------------------------------------------
    // CLICK LISTENERS
    // ---------------------------------------------------------------------

    private fun setupClickListeners() {

        findViewById<View>(
            R.id.btnExportPdf
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Export scheduled — check your email shortly.",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<ImageView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }
    }

    // ---------------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------------

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