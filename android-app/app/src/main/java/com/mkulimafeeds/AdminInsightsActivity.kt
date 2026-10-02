package com.mkulimafeeds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.mkulimafeeds.account.AdminAccountActivity
import com.mkulimafeeds.util.applyBottomNavInsets

class AdminInsightsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_insights)

        applyBottomNavInsets(
            root = findViewById(R.id.adminInsightsRoot),
            bottomNav = findViewById(R.id.bottomNavAdmin)
        )
        setupNavigation()
        highlightInsightsTab()
        setupCards()
    }

    override fun onResume() {
        super.onResume()
        highlightInsightsTab()
    }

    private fun setupCards() {
        findViewById<MaterialCardView>(R.id.cardSalesInsights).setOnClickListener {
            startActivity(Intent(this, SalesInsightsActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardAnalyticsDashboard).setOnClickListener {
            startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardProductPerformance).setOnClickListener {
            startActivity(Intent(this, ProductPerformanceActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardDealerPerformance).setOnClickListener {
            startActivity(Intent(this, DealerPerformanceActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardCustomerGrowth).setOnClickListener {
            startActivity(Intent(this, CustomerGrowthActivity::class.java))
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrderActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, AdminAccountActivity::class.java))
            finish()
        }
    }

    private fun highlightInsightsTab() {
        setActiveTab(R.id.icInsights, R.id.tvInsights)
        setInactiveTab(R.id.icDashboard, R.id.tvDashboard)
        setInactiveTab(R.id.icProducts, R.id.tvProducts)
        setInactiveTab(R.id.icOrders, R.id.tvOrders)
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
}