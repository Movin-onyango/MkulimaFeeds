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
import com.mkulimafeeds.account.CustomerAccountActivity
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.customer.CustomerOrderRepository
import com.mkulimafeeds.domain.model.CustomerOrder
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModel
import com.mkulimafeeds.presentation.customer.orders.CustomerOrderViewModelFactory
import com.mkulimafeeds.presentation.customer.orders.MyOrdersAdapter
import kotlinx.coroutines.launch

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var adapter: MyOrdersAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var etSearch: EditText

    private lateinit var emptyStateContainer: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView

    private lateinit var tokenManager: TokenManager

    private val orderViewModel: CustomerOrderViewModel by viewModels {
        CustomerOrderViewModelFactory(
            CustomerOrderRepository(NetworkModule.apiService)
        )
    }

    private var allOrders: List<CustomerOrder> = emptyList()
    private var selectedStatus = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_orders)
        applyBottomNavInsets(
            root = findViewById(R.id.orders_root),
            bottomNav = findViewById(R.id.bottomNavCustomer)
        )


        tokenManager = TokenManager(this)

        rvOrders = findViewById(R.id.rvMyOrders)
        etSearch = findViewById(R.id.etSearchOrders)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupNavigation()
        highlightActiveTab()
        setupViewModel()

        loadOrders()
    }

    override fun onResume() {
        super.onResume()

        highlightActiveTab()

        if (::tokenManager.isInitialized) {
            loadOrders()
        }

        updateCartBadge()
    }

    // =========================================================
    // BOTTOM NAVIGATION (inline — no helper)
    // =========================================================

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navCatalog).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, CustomerAccountActivity::class.java))
            finish()
        }

        // navOrders = current, no-op
    }

    private fun highlightActiveTab() {
        setInactiveTab(R.id.icHome, R.id.tvHome)
        setInactiveTab(R.id.icCatalog, R.id.tvCatalog)
        setInactiveTab(R.id.icCart, R.id.tvCart)
        setActiveTab(R.id.icOrders, R.id.tvOrders)
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
    // LIST + FILTERS
    // =========================================================

    private fun setupRecyclerView() {
        adapter = MyOrdersAdapter(emptyList()) { order ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_ID", order.id)
            startActivity(intent)
        }

        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter
    }

    private fun setupViewModel() {
        lifecycleScope.launch {
            orderViewModel.uiState.collect { state ->
                if (state.isLoading) {
                    // Loading indicator can be added later.
                }

                state.error?.let { error ->
                    Toast.makeText(
                        this@MyOrdersActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                    orderViewModel.clearError()
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
        orderViewModel.loadOrders(token)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    filterOrders()
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun setupFilters() {
        val chips = listOf(
            R.id.chipAllOrders,
            R.id.chipPending,
            R.id.chipConfirmed,
            R.id.chipProcessing,
            R.id.chipReady,
            R.id.chipCompleted,
            R.id.chipCancelled
        )

        val statuses = listOf(
            "All", "PENDING", "CONFIRMED", "PROCESSING",
            "READY", "COMPLETED", "CANCELLED"
        )

        chips.forEachIndexed { index, chipId ->
            findViewById<TextView>(chipId).setOnClickListener {
                selectedStatus = statuses[index]
                updateSelectedChip(chipId, chips)
                filterOrders()
            }
        }

        updateSelectedChip(R.id.chipAllOrders, chips)
    }

    private fun updateSelectedChip(selectedId: Int, chipIds: List<Int>) {
        chipIds.forEach { chipId ->
            val chip = findViewById<TextView>(chipId)
            if (chipId == selectedId) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
        }
    }

    private fun filterOrders() {
        val query = etSearch.text.toString().trim()

        var filtered =
            if (selectedStatus == "All") {
                allOrders
            } else {
                allOrders.filter { order ->
                    order.status.name.equals(selectedStatus, ignoreCase = true)
                }
            }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { order ->
                order.id.toString().contains(query, ignoreCase = true)
            }
        }

        adapter.updateList(filtered)

        if (filtered.isEmpty()) {
            rvOrders.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
            showEmptyState()
        } else {
            rvOrders.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        tvEmptyTitle.text =
            if (selectedStatus == "All") "No Orders Yet"
            else "No $selectedStatus Orders"

        tvEmptyMessage.text =
            if (selectedStatus == "All") "You haven't placed any orders yet."
            else "You don't have any orders with this status."
    }

    // =========================================================
    // CART BADGE
    // =========================================================

    private fun updateCartBadge() {
        val count = CartManager.getItems().sumOf { it.quantity }
        val badge = findViewById<TextView>(R.id.tvCartBadge) ?: return

        if (count > 0) {
            badge.text = if (count > 9) "9+" else count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}