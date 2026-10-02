package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.mkulimafeeds.data.repository.AnalyticsRepository
import kotlinx.coroutines.launch
import java.util.Locale

class DealerPerformanceActivity : AppCompatActivity() {

    private lateinit var rvDealers: RecyclerView
    private lateinit var adapter: DealerPerformanceAdapter

    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingText: TextView

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
            R.layout.activity_dealer_performance
        )

        val rootView =
            findViewById<View>(
                R.id.dealerPerfRoot
            )

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.adminBottomNav
            )

        rvDealers =
            findViewById(
                R.id.rvDealerLeaderboard
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

        loadAnalytics()
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private fun showLoading(
        message: String = "Loading dealer performance..."
    ) {
        tvLoadingText.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {

        adapter =
            DealerPerformanceAdapter(
                emptyList()
            )

        rvDealers.layoutManager =
            LinearLayoutManager(this)

        rvDealers.adapter =
            adapter
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

                // -------------------------------------------------
                // DEALER PERFORMANCE
                // -------------------------------------------------

                val dealerMetrics =
                    response.dealerPerformance.map { dealer ->

                        DealerMetric(
                            name = dealer.name,
                            location = dealer.location,
                            revenue =
                                dealer.revenue
                                    .toDoubleOrNull()
                                    ?: 0.0,
                            growth = dealer.growth
                        )
                    }

                adapter.submitList(dealerMetrics)

                // -------------------------------------------------
                // REGIONAL DEMAND
                // -------------------------------------------------

                renderDemandTrends(
                    response.regionalDemand
                )

                renderTopDemandRegion(
                    response.regionalDemand
                )

                // -------------------------------------------------
                // DEALER COVERAGE
                // -------------------------------------------------

                renderDealerCoverage(
                    response.dealerPerformance
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@DealerPerformanceActivity,
                    e.message
                        ?: "Failed to load dealer analytics",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                hideLoading()
            }
        }
    }

    // =========================================================
    // REGIONAL DEMAND
    // =========================================================

    private fun renderDemandTrends(
        regions:
        List<com.mkulimafeeds.data.remote.RegionDemandResponse>
    ) {

        val demandContainer =
            findViewById<LinearLayout>(
                R.id.demand_container
            )

        demandContainer.removeAllViews()

        if (regions.isEmpty()) {

            val emptyView =
                TextView(this).apply {

                    text =
                        "No regional demand data available"

                    textSize = 13f

                    setTextColor(
                        android.graphics.Color.rgb(
                            117,
                            117,
                            117
                        )
                    )
                }

            demandContainer.addView(
                emptyView
            )

            return
        }

        regions.forEach { region ->

            val view =
                layoutInflater.inflate(
                    R.layout.view_performance_metric,
                    demandContainer,
                    false
                )

            val tvMetricLabel =
                view.findViewById<TextView>(
                    R.id.tvMetricLabel
                )

            val tvMetricValue =
                view.findViewById<TextView>(
                    R.id.tvMetricValue
                )

            val progressIndicator =
                view.findViewById<LinearProgressIndicator>(
                    R.id.progressIndicator
                )

            val tvMetricGrowth =
                view.findViewById<TextView>(
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
                    "Growth: %+.1f%%",
                    region.growth
                )

            tvMetricGrowth.setTextColor(
                when {

                    region.growth > 0 ->
                        android.graphics.Color.rgb(30, 142, 62)

                    region.growth < 0 ->
                        android.graphics.Color.rgb(185, 28, 28)

                    else ->
                        android.graphics.Color.rgb(117, 117, 117)
                }
            )

            demandContainer.addView(view)
        }
    }

    // =========================================================
    // TOP DEMAND REGION
    // =========================================================

    private fun renderTopDemandRegion(
        regions:
        List<com.mkulimafeeds.data.remote.RegionDemandResponse>
    ) {

        val tvTopDemandRegion =
            findViewById<TextView>(
                R.id.tvTopDemandRegion
            )

        val tvTopDemandPercentage =
            findViewById<TextView>(
                R.id.tvTopDemandPercentage
            )

        val topRegion =
            regions.maxByOrNull {
                it.percentage
            }

        if (topRegion == null) {

            tvTopDemandRegion.text =
                "No regional data"

            tvTopDemandPercentage.text =
                "—"

            return
        }

        tvTopDemandRegion.text =
            topRegion.name

        tvTopDemandPercentage.text =
            "${topRegion.percentage}%"
    }

    // =========================================================
    // DEALER COVERAGE
    // =========================================================

    private fun renderDealerCoverage(
        dealers:
        List<com.mkulimafeeds.data.remote.DealerPerformanceResponse>
    ) {

        val tvDealerCoverageCount =
            findViewById<TextView>(
                R.id.tvDealerCoverageCount
            )

        val tvDealerCoverageRegions =
            findViewById<TextView>(
                R.id.tvDealerCoverageRegions
            )

        val tvDealerCoverageRevenue =
            findViewById<TextView>(
                R.id.tvDealerCoverageRevenue
            )

        val dealerCount =
            dealers.size

        val regionCount =
            dealers
                .map {
                    it.location
                        .trim()
                        .uppercase()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .size

        val totalRevenue =
            dealers.sumOf {
                it.revenue
                    .toDoubleOrNull()
                    ?: 0.0
            }

        tvDealerCoverageCount.text =
            "$dealerCount dealers"

        tvDealerCoverageRegions.text =
            "$regionCount regions covered"

        tvDealerCoverageRevenue.text =
            String.format(
                Locale.getDefault(),
                "KES %,.0f total dealer revenue",
                totalRevenue
            )
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private fun setupClickListeners() {

        findViewById<View>(
            R.id.btnExportData
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

    // =========================================================
    // NAVIGATION
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