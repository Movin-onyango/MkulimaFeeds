package com.mkulimafeeds

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mkulimafeeds.account.AdminAccountActivity
import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository
import com.mkulimafeeds.presentation.products.ProductViewModel
import kotlinx.coroutines.launch
import com.mkulimafeeds.util.applyBottomNavInsets


class AdminProductActivity : AppCompatActivity() {

    private lateinit var adapter: AdminProductAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvCount: TextView

    private lateinit var chipAll: Chip
    private lateinit var chipInStock: Chip
    private lateinit var chipLowStock: Chip

    private lateinit var productRepository: ProductRepository
    private lateinit var tokenManager: TokenManager

    private val productViewModel: ProductViewModel by viewModels()

    private enum class StockFilter {
        ALL,
        IN_STOCK,
        LOW_STOCK
    }

    private var currentFilter = StockFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_product)

        productRepository =
            ProductRepository(NetworkModule.apiService)

        tokenManager =
            TokenManager(applicationContext)

        rvProducts = findViewById(R.id.rvAdminProducts)
        etSearch = findViewById(R.id.etSearchProducts)
        tvCount = findViewById(R.id.tvProductCount)

        chipAll = findViewById(R.id.chipAllItems)
        chipInStock = findViewById(R.id.chipInStock)
        chipLowStock = findViewById(R.id.chipLowStock)

        // Toolbar: top inset only (status bar).
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.toolbar)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )

            insets
        }
        applyBottomNavInsets(
            root = findViewById(R.id.adminProductRoot),
            bottomNav = findViewById(R.id.bottomNavAdmin)
        )
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupAdminNavigation()
        highlightProductsTab()
        observeProducts()

        findViewById<View>(R.id.fabAddProduct).setOnClickListener {
            val intent =
                Intent(this, AdminEditProductActivity::class.java)

            intent.putExtra("IS_EDIT", false)
            startActivity(intent)
        }

        productViewModel.loadProducts()
    }

    override fun onResume() {
        super.onResume()
        productViewModel.loadProducts()
        highlightProductsTab()
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    private fun setupAdminNavigation() {
        findViewById<View>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
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
        // navProducts = current, no-op
    }

    private fun highlightProductsTab() {
        setActiveTab(R.id.icProducts, R.id.tvProducts)
        setInactiveTab(R.id.icDashboard, R.id.tvDashboard)
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
    // RECYCLER + OBSERVE
    // =========================================================

    private fun setupRecyclerView() {

        adapter = AdminProductAdapter(
            emptyList(),
            onEditClick = { product ->
                onEditProductClicked(product.id)
            },
            onDeleteClick = { product ->
                showDeleteConfirmation(product)
            }
        )

        rvProducts.layoutManager =
            LinearLayoutManager(this)

        rvProducts.adapter = adapter
    }

    private fun observeProducts() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                productViewModel.uiState.collect { state ->

                    updateProductList(
                        state.products
                    )
                }
            }
        }
    }

    // =========================================================
    // SEARCH + FILTERS
    // =========================================================

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
                    productViewModel.searchProducts(
                        s?.toString()?.trim().orEmpty()
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupFilters() {

        chipAll.setOnClickListener {
            currentFilter = StockFilter.ALL
            updateProductList(
                productViewModel.uiState.value.products
            )
        }

        chipInStock.setOnClickListener {
            currentFilter = StockFilter.IN_STOCK
            updateProductList(
                productViewModel.uiState.value.products
            )
        }

        chipLowStock.setOnClickListener {
            currentFilter = StockFilter.LOW_STOCK
            updateProductList(
                productViewModel.uiState.value.products
            )
        }
    }

    private fun updateProductList(
        allProducts: List<com.mkulimafeeds.data.model.Product>
    ) {

        val products = when (currentFilter) {

            StockFilter.ALL ->
                allProducts

            StockFilter.IN_STOCK ->
                allProducts.filter {
                    it.stock > 5
                }

            StockFilter.LOW_STOCK ->
                allProducts.filter {
                    it.stock <= 5
                }
        }

        adapter.updateList(products)
        updateCount(products.size)
    }

    private fun updateCount(count: Int) {
        tvCount.text = "Products ($count)"
    }

    // =========================================================
    // EDIT / DELETE
    // =========================================================

    private fun onEditProductClicked(
        productId: String
    ) {

        val intent =
            Intent(
                this,
                AdminEditProductActivity::class.java
            )

        intent.putExtra("IS_EDIT", true)
        intent.putExtra("PRODUCT_ID", productId)

        startActivity(intent)
    }

    private fun showDeleteConfirmation(
        product: com.mkulimafeeds.data.model.Product
    ) {

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage(
                "Are you sure you want to delete \"${product.name}\"?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteProduct(product)
            }
            .show()
    }

    private fun deleteProduct(
        product: com.mkulimafeeds.data.model.Product
    ) {

        val backendId =
            product.id.toLongOrNull()

        if (backendId == null) {
            Toast.makeText(
                this,
                "Invalid product ID",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Your session has expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                productRepository.deleteProduct(
                    id = backendId,
                    token = token
                )

                Toast.makeText(
                    this@AdminProductActivity,
                    "Product deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                productViewModel.loadProducts()

            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminProductActivity,
                    "Failed to delete product: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}