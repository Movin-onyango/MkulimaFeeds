package com.mkulimafeeds

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mkulimafeeds.account.CustomerAccountActivity
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.ProductRepository
import kotlinx.coroutines.launch
import com.mkulimafeeds.util.applyBottomNavInsets
class CatalogActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var search: EditText

    private lateinit var productRepository: ProductRepository
    private lateinit var catalogAdapter: CatalogProductAdapter

    private var allProducts: List<Product> = emptyList()
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_catalog)

        // Root: top + sides only. Bottom inset consumed by nav bar.
        applyBottomNavInsets(
            root = findViewById(R.id.catalog_root),
            bottomNav = findViewById(R.id.bottomNavCustomer)
        )
/*
        // Bottom nav: consume bottom system bar inset.
        val bottomNav = findViewById<View>(R.id.bottomNavCustomer)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBars.bottom
            )
            insets
        }
*/
        rvProducts = findViewById(R.id.rvProducts)
        categoryContainer = findViewById(R.id.categoryContainer)
        emptyState = findViewById(R.id.emptyStateCatalog)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        search = findViewById(R.id.etCatalogSearch)

        productRepository = ProductRepository(NetworkModule.apiService)

        catalogAdapter = CatalogProductAdapter(
            products = emptyList(),
            onAddToCart = ::addToCart,
            onProductClick = ::openProductDetail
        )

        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = catalogAdapter

        setupSearch()
        setupTopBarSearch()
        setupNavigation()
        highlightActiveTab()

        loadProducts()
    }

    override fun onResume() {
        super.onResume()

        highlightActiveTab()
        updateCartBadge()

        if (::productRepository.isInitialized && ::catalogAdapter.isInitialized) {
            loadProducts()
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION (inline — no helper)
    // =========================================================

    private fun setupNavigation() {

        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navOrders).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, CustomerAccountActivity::class.java))
            finish()
        }

        // navCatalog = current, no-op
    }

    private fun highlightActiveTab() {
        setInactiveTab(R.id.icHome, R.id.tvHome)
        setActiveTab(R.id.icCatalog, R.id.tvCatalog)
        setInactiveTab(R.id.icCart, R.id.tvCart)
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

    // =========================================================
    // TOP-BAR SEARCH ICON → FOCUS INLINE FIELD
    // =========================================================

    private fun setupTopBarSearch() {
        findViewById<ImageView>(R.id.btnSearchCatalog)
            .setOnClickListener {
                search.requestFocus()

                val imm = getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                imm.showSoftInput(
                    search,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
    }

    // =========================================================
    // DATA
    // =========================================================

    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                val products = productRepository.getProducts()
                allProducts = products
                renderCategories()
                applyFilters()
            } catch (e: Exception) {
                allProducts = emptyList()
                catalogAdapter.submitList(emptyList())
                showEmptyState(
                    "Unable to load products",
                    "Please check your connection and try again."
                )
            }
        }
    }

    private fun renderCategories() {
        categoryContainer.removeAllViews()

        val categories =
            listOf("All") +
                    allProducts
                        .map { it.category.trim() }
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                        .sortedBy { it.lowercase() }

        categories.forEach { category ->
            val chip = TextView(this)
            chip.text = category
            chip.gravity = Gravity.CENTER
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            chip.setPadding(dp(20), dp(9), dp(20), dp(9))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = dp(8)
            chip.layoutParams = params
            chip.isClickable = true
            chip.isFocusable = true

            chip.setOnClickListener {
                selectedCategory = category
                renderCategories()
                applyFilters()
            }

            categoryContainer.addView(chip)
        }

        updateCategoryStyles()
    }

    private fun updateCategoryStyles() {
        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView

            val isSelected = chip.text.toString().equals(
                selectedCategory,
                ignoreCase = true
            )

            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_role_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_role_unselected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            }
        }
    }

    private fun setupSearch() {
        search.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    applyFilters()
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
    }

    private fun applyFilters() {
        val query = search.text.toString().trim()

        val filtered = allProducts.filter { product ->
            val categoryMatches =
                selectedCategory == "All" ||
                        product.category.equals(selectedCategory, ignoreCase = true)

            val searchMatches =
                query.isBlank() ||
                        product.name.contains(query, ignoreCase = true) ||
                        product.category.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true)

            categoryMatches && searchMatches
        }

        catalogAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            showEmptyState(
                "No products found",
                "Try another category or search term."
            )
        } else {
            emptyState.visibility = View.GONE
            rvProducts.visibility = View.VISIBLE
        }

        updateCategoryStyles()
    }

    private fun showEmptyState(title: String, subtitle: String) {
        tvEmptyMessage.text = title
        tvEmptySubtitle.text = subtitle
        emptyState.visibility = View.VISIBLE
        rvProducts.visibility = View.GONE
    }

    private fun addToCart(product: Product) {
        if (product.stock <= 0) return

        val numericId = product.id.toIntOrNull()
        if (numericId == null) {
            Snackbar.make(
                findViewById(R.id.catalog_root),
                "Invalid product ID",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val item = CartItem(
            id = numericId,
            name = product.name,
            price = product.price,
            sku = product.sku.ifBlank { "PRODUCT-${product.id}" },
            imageRes = product.imageResId ?: R.drawable.ic_launcher_background
        )

        CartManager.addItem(item)
        updateCartBadge()

        Snackbar.make(
            findViewById(R.id.catalog_root),
            getString(R.string.msg_added_to_cart, product.name),
            Snackbar.LENGTH_LONG
        )
            .setAction("VIEW CART") {
                startActivity(Intent(this, CartActivity::class.java))
            }
            .setActionTextColor(ContextCompat.getColor(this, R.color.brand_blue))
            .show()
    }

    private fun openProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
        startActivity(intent)
    }

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

    private fun dp(value: Int): Int =
        TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                resources.displayMetrics
            )
            .toInt()
}