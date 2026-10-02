package com.mkulimafeeds
import com.mkulimafeeds.util.applyBottomNavInsets
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.mkulimafeeds.account.AdminAccountActivity
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.presentation.orders.OrderViewModel
import kotlinx.coroutines.launch

class AdminOrderActivity : AppCompatActivity() {

    private lateinit var adapter: AdminOrderAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var statusChipGroup: ChipGroup
    private lateinit var tokenManager: TokenManager

    private val orderViewModel: OrderViewModel by viewModels()

    private var allOrders: List<Order> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_order)


        rvOrders = findViewById(R.id.rvAdminOrders)
        etSearch = findViewById(R.id.etSearchOrders)
        statusChipGroup = findViewById(R.id.statusChipGroup)

        tokenManager = TokenManager(this)
        applyBottomNavInsets(
            root = findViewById(R.id.adminOrderRoot),
            bottomNav = findViewById(R.id.bottomNavAdmin)
        )
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupAdminNavigation()
        highlightOrdersTab()
        setupViewModel()

        findViewById<View>(R.id.fabAddOrder).setOnClickListener {
            Toast.makeText(
                this,
                "Manual Order Entry Coming Soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        loadOrders()
    }

    override fun onResume() {
        super.onResume()

        if (::tokenManager.isInitialized) {
            loadOrders()
        }
        highlightOrdersTab()
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    private fun setupAdminNavigation() {
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductActivity::class.java))
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
        // navOrders = current, no-op
    }

    private fun highlightOrdersTab() {
        setActiveTab(R.id.icOrders, R.id.tvOrders)
        setInactiveTab(R.id.icDashboard, R.id.tvDashboard)
        setInactiveTab(R.id.icProducts, R.id.tvProducts)
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
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {

        adapter = AdminOrderAdapter(
            emptyList()
        ) { order ->

            val intent = Intent(
                this,
                AdminOrderDetailActivity::class.java
            )

            intent.putExtra(
                "ORDER_ID",
                order.id
            )

            startActivity(intent)
        }

        rvOrders.layoutManager =
            LinearLayoutManager(this)

        rvOrders.adapter = adapter
    }

    private fun setupViewModel() {

        lifecycleScope.launch {

            orderViewModel.uiState.collect { state ->

                if (state.isLoading) {
                    // We will add a proper loading indicator later.
                }

                state.error?.let { error ->

                    Toast.makeText(
                        this@AdminOrderActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }

                allOrders = state.orders

                filterOrders()
            }
        }
    }

    private fun loadOrders() {

        val token = tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Authentication token not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        orderViewModel.loadAdminOrders(token)
    }

    private fun setupSearch() {

        etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterOrders()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupFilters() {

        statusChipGroup.setOnCheckedStateChangeListener { _, _ ->
            filterOrders()
        }
    }

    private fun filterOrders() {

        val query =
            etSearch.text.toString().trim()

        val checkedChipId =
            statusChipGroup.checkedChipId

        val status = when (checkedChipId) {

            R.id.chipPending ->
                "PENDING"

            R.id.chipConfirmed ->
                "CONFIRMED"

            R.id.chipProcessing ->
                "PROCESSING"

            R.id.chipReady ->
                "READY"

            R.id.chipCompleted ->
                "COMPLETED"

            R.id.chipCancelled ->
                "CANCELLED"

            else ->
                "All"
        }

        var filteredList =
            if (status == "All") {

                allOrders

            } else {

                allOrders.filter {
                    it.status.equals(
                        status,
                        ignoreCase = true
                    )
                }
            }

        if (query.isNotEmpty()) {

            filteredList =
                filteredList.filter {

                    it.id.contains(
                        query,
                        ignoreCase = true
                    ) ||

                            it.customerName.contains(
                                query,
                                ignoreCase = true
                            )
                }
        }

        adapter.updateList(filteredList)
    }
}