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
import com.mkulimafeeds.account.DealerAccountActivity
import com.mkulimafeeds.util.applyBottomNavInsets
class DealerInsightsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dealer_insights)
        applyBottomNavInsets(
            root = findViewById(R.id.dealerInsightsRoot),
            bottomNav = findViewById(R.id.bottomNavDealer)
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
        findViewById<MaterialCardView>(R.id.cardPerformance).setOnClickListener {
            startActivity(Intent(this, DealerPerformanceActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardReports).setOnClickListener {
            startActivity(Intent(this, DealerReportsActivity::class.java))
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, DealerDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navCatalog).setOnClickListener {
            startActivity(Intent(this, DealerCatalogActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, DealerOrdersActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, DealerAccountActivity::class.java))
            finish()
        }
    }

    private fun highlightInsightsTab() {
        setActiveTab(R.id.icInsights, R.id.tvInsights)
        setInactiveTab(R.id.icDashboard, R.id.tvDashboard)
        setInactiveTab(R.id.icCatalog, R.id.tvCatalog)
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