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
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.mkulimafeeds.account.DealerAccountActivity
import com.mkulimafeeds.util.applyBottomNavInsets
class DealerOrdersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dealer_orders)
        applyBottomNavInsets(
            root = findViewById(R.id.dealerOrdersRoot),
            bottomNav = findViewById(R.id.bottomNavDealer)
        )

        setupNavigation()
        highlightOrdersTab()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        highlightOrdersTab()
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.ordersTabLayout)

        tabLayout.addTab(tabLayout.newTab().setText("Assigned"))
        tabLayout.addTab(tabLayout.newTab().setText("Bulk"))
        tabLayout.addTab(tabLayout.newTab().setText("Customers"))

        // Load first tab by default
        loadFragment(DealerAssignedOrdersFragment())

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val fragment = when (tab.position) {
                    0 -> DealerAssignedOrdersFragment()
                    1 -> DealerBulkOrdersFragment()
                    2 -> DealerCustomersFragment()
                    else -> DealerAssignedOrdersFragment()
                }
                loadFragment(fragment)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.ordersFragmentContainer, fragment)
            .commit()
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
        findViewById<View>(R.id.navInsights).setOnClickListener {
            startActivity(Intent(this, DealerInsightsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, DealerAccountActivity::class.java))
            finish()
        }
    }

    private fun highlightOrdersTab() {
        setActiveTab(R.id.icOrders, R.id.tvOrders)
        setInactiveTab(R.id.icDashboard, R.id.tvDashboard)
        setInactiveTab(R.id.icCatalog, R.id.tvCatalog)
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
}